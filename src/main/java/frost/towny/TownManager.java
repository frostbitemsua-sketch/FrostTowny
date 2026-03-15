package frost.towny;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;
import java.io.File;
import java.util.*;

public class TownManager {

    private FrostTowny plugin;
    private EconomyAPI economy;
    private Map<String, TownData> towns = new HashMap<>();
    private Map<String, String> playerTowns = new HashMap<>();
    private Map<String, String> plotOwners = new HashMap<>();
    private Map<String, Map<String, Boolean>> chunkFlags = new HashMap<>();
    private Map<String, Long> warDeclarations = new HashMap<>(); // town1:town2 -> timestamp
    private Map<String, Long> lastActivity = new HashMap<>(); // playerName -> timestamp
    private File dataFile;

    public TownManager(FrostTowny plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomyAPI();
        this.dataFile = new File(plugin.getDataFolder(), "towns.yml");
        loadData();
        startActivityTracker();
    }

    // ============ ОСНОВНІ МЕТОДИ ============

    public boolean createTown(String name, Player mayor, Position home) {
        // Валідація
        if (towns.containsKey(name.toLowerCase())) {
            mayor.sendMessage(plugin.getMessage("town.create.already-exists", "Town already exists!", name));
            return false;
        }

        if (playerTowns.containsKey(mayor.getName().toLowerCase())) {
            mayor.sendMessage(plugin.getMessage("town.create.already-in-town", "You're already in a town!", name));
            return false;
        }

        if (!name.matches("[A-Za-z0-9]{3,20}")) {
            mayor.sendMessage(plugin.getMessage("town.create.invalid-name", "Invalid town name!"));
            return false;
        }

        // Перевірка відстані до інших міст
        if (isTooCloseToOtherTown(home)) {
            mayor.sendMessage(plugin.getMessage("town.create.too-close", "Too close to another town!"));
            return false;
        }

        // Перевірка вартості
        double cost = plugin.getPluginConfig().getDouble("town-creation.cost", 1000.0);
        if (economy.myMoney(mayor) < cost) {
            mayor.sendMessage(plugin.getMessage("town.create.not-enough-money", "Not enough money!", String.format("%.2f", cost)));
            return false;
        }

        // Створення міста
        TownData town = new TownData();
        town.setName(name);
        town.setMayor(mayor.getName());
        town.setHome(home);
        town.setCreationTime(System.currentTimeMillis());
        town.setBalance(0.0);
        town.setTaxRate(5.0);
        town.setDescription("Welcome to " + name + "!");
        town.setTag(name.substring(0, Math.min(3, name.length())).toUpperCase());
        town.addResident(mayor.getName());

        // Заявка на домашній чанк
        String chunkKey = getChunkKey(home);
        town.addChunk(chunkKey);

        // Збереження
        towns.put(name.toLowerCase(), town);
        playerTowns.put(mayor.getName().toLowerCase(), name);
        lastActivity.put(mayor.getName().toLowerCase(), System.currentTimeMillis());

        // Списання грошей
        economy.reduceMoney(mayor, cost);

        mayor.sendMessage(plugin.getMessage("town.create.success", "Town created!", name, String.format("%.2f", cost)));
        return true;
    }

    public boolean deleteTown(String townName, Player deleter) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(deleter.getName())) {
            deleter.sendMessage(plugin.getMessage("town.delete.not-mayor", "Only mayor can delete town!"));
            return false;
        }

        // Повернення грошей
        double refund = town.getBalance() * 0.5;
        if (refund > 0) {
            economy.addMoney(deleter, refund);
            deleter.sendMessage(TextFormat.GREEN + "Refunded: $" + String.format("%.2f", refund));
        }

        // Видалення всіх жителів
        for (String resident : new HashSet<>(town.getAllMembers())) {
            playerTowns.remove(resident.toLowerCase());
            lastActivity.remove(resident.toLowerCase());
        }

        // Видалення міста
        towns.remove(townName.toLowerCase());

        // Видалення ділянок
        for (String chunkKey : town.getChunks()) {
            plotOwners.remove(chunkKey);
            chunkFlags.remove(chunkKey);
        }

        // Видалення відносин війни/союзу
        removeTownRelations(townName);

        deleter.sendMessage(plugin.getMessage("town.delete.success", "Town deleted!"));
        return true;
    }

    // ============ УПРАВЛІННЯ ЖИТЕЛЯМИ ============

    public boolean addResident(String townName, String playerName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (playerTowns.containsKey(playerName.toLowerCase())) return false;

        int maxResidents = plugin.getPluginConfig().getInt("limits.max-residents", 20);
        if (town.getMemberCount() >= maxResidents) {
            return false;
        }

        if (town.addResident(playerName)) {
            playerTowns.put(playerName.toLowerCase(), townName);
            lastActivity.put(playerName.toLowerCase(), System.currentTimeMillis());
            return true;
        }

        return false;
    }

    public boolean removeResident(String townName, String playerName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (town.removeResident(playerName)) {
            playerTowns.remove(playerName.toLowerCase());
            lastActivity.remove(playerName.toLowerCase());

            // Повернення ділянок
            for (Map.Entry<String, String> entry : new HashSet<>(plotOwners.entrySet())) {
                if (entry.getValue().equalsIgnoreCase(playerName) && town.getChunks().contains(entry.getKey())) {
                    plotOwners.remove(entry.getKey());
                }
            }

            return true;
        }

        return false;
    }

    public boolean leaveTown(Player player) {
        TownData town = getPlayerTown(player.getName());
        if (town == null) return false;

        if (town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.leave.mayor-cant-leave", "Mayor cannot leave!"));
            return false;
        }

        return removeResident(town.getName(), player.getName());
    }

    // ============ УПРАВЛІННЯ ЧАНКАМИ ============

    public boolean claimChunk(Player player, Position position) {
        TownData town = getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.claim.not-in-town", "You're not in a town!"));
            return false;
        }

        if (!town.canManage(player.getName())) {
            player.sendMessage(plugin.getMessage("town.claim.no-permission-claim", "No permission!"));
            return false;
        }

        int maxChunks = plugin.getPluginConfig().getInt("limits.max-chunks", 50);
        if (town.getChunkCount() >= maxChunks) {
            player.sendMessage(plugin.getMessage("town.claim.max-chunks", "Max chunks!", String.valueOf(maxChunks)));
            return false;
        }

        double claimCost = plugin.getPluginConfig().getDouble("economy.chunk-claim-cost", 50.0);
        if (town.getBalance() < claimCost) {
            player.sendMessage(plugin.getMessage("town.claim.cost", "Not enough money!", String.format("%.2f", claimCost)));
            return false;
        }

        String chunkKey = getChunkKey(position);

        if (getTownAtChunk(position.getLevel().getName(),
                position.getFloorX() >> 4,
                position.getFloorZ() >> 4) != null) {
            player.sendMessage(plugin.getMessage("town.claim.already-claimed", "Already claimed!"));
            return false;
        }

        if (town.getChunkCount() > 0 && plugin.getPluginConfig().getBoolean("chunks.require-adjacency", true)) {
            if (!isChunkAdjacent(town, chunkKey)) {
                player.sendMessage(plugin.getMessage("town.claim.not-adjacent", "Not adjacent!"));
                return false;
            }
        }

        // Максимальний радіус
        int maxRadius = plugin.getPluginConfig().getInt("chunks.max-claim-radius", 10);
        if (town.getHome() != null && getDistanceFromHome(town, chunkKey) > maxRadius) {
            player.sendMessage(plugin.getMessage("town.claim.too-far", "Too far from home!"));
            return false;
        }

        town.setBalance(town.getBalance() - claimCost);
        town.addChunk(chunkKey);

        player.sendMessage(plugin.getMessage("town.claim.success", "Chunk claimed!"));
        return true;
    }

    public boolean unclaimChunk(Player player, Position position) {
        TownData town = getPlayerTown(player.getName());
        if (town == null) return false;

        if (!town.canManage(player.getName())) {
            player.sendMessage(plugin.getMessage("town.unclaim.no-permission", "No permission!"));
            return false;
        }

        String chunkKey = getChunkKey(position);

        if (!town.hasChunk(chunkKey)) {
            player.sendMessage(plugin.getMessage("town.unclaim.not-claimed", "Not your chunk!"));
            return false;
        }

        if (town.getHome() != null && !plugin.getPluginConfig().getBoolean("chunks.allow-unclaim-home", false)) {
            String homeChunkKey = getChunkKey(town.getHome());
            if (homeChunkKey.equals(chunkKey)) {
                player.sendMessage(plugin.getMessage("town.unclaim.home-chunk", "Cannot unclaim home!"));
                return false;
            }
        }

        plotOwners.remove(chunkKey);
        chunkFlags.remove(chunkKey);

        double refund = plugin.getPluginConfig().getDouble("economy.chunk-claim-cost", 50.0) *
                plugin.getPluginConfig().getDouble("economy.chunk-unclaim-refund", 0.25);
        town.setBalance(town.getBalance() + refund);

        town.removeChunk(chunkKey);
        player.sendMessage(plugin.getMessage("town.unclaim.success", "Chunk unclaimed!"));
        return true;
    }

    // ============ ЕКОНОМІКА ============

    public boolean depositToTown(String townName, Player player, double amount) {
        TownData town = getTown(townName);
        if (town == null || !town.isResident(player.getName())) return false;

        if (amount <= 0) {
            player.sendMessage(plugin.getMessage("town.deposit.invalid-amount", "Invalid amount!"));
            return false;
        }

        double maxDeposit = plugin.getPluginConfig().getDouble("economy.max-deposit", 10000.0);
        if (amount > maxDeposit) {
            player.sendMessage(plugin.getMessage("town.deposit.too-much", "Too much!", String.format("%.2f", maxDeposit)));
            return false;
        }

        if (economy.myMoney(player) < amount) {
            player.sendMessage(plugin.getMessage("town.deposit.not-enough-money", "Not enough money!"));
            return false;
        }

        economy.reduceMoney(player, amount);
        town.setBalance(town.getBalance() + amount);

        player.sendMessage(plugin.getMessage("town.deposit.success", "Deposited!",
                String.format("%.2f", amount),
                String.format("%.2f", town.getBalance())));
        return true;
    }

    public boolean withdrawFromTown(String townName, Player player, double amount) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.canWithdraw(player.getName())) {
            player.sendMessage(plugin.getMessage("town.withdraw.no-permission-withdraw", "No permission!"));
            return false;
        }

        if (amount <= 0 || amount > town.getBalance()) {
            player.sendMessage(plugin.getMessage("town.withdraw.not-enough-balance", "Not enough balance!"));
            return false;
        }

        double maxWithdrawal = plugin.getPluginConfig().getDouble("economy.max-withdrawal", 10000.0);
        if (amount > maxWithdrawal) {
            player.sendMessage(plugin.getMessage("town.withdraw.too-much", "Too much!", String.format("%.2f", maxWithdrawal)));
            return false;
        }

        economy.addMoney(player, amount);
        town.setBalance(town.getBalance() - amount);

        player.sendMessage(plugin.getMessage("town.withdraw.success", "Withdrawn!",
                String.format("%.2f", amount),
                String.format("%.2f", town.getBalance())));
        return true;
    }

    public boolean setTaxRate(String townName, Player player, double taxRate) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.tax.mayor-only", "Mayor only!"));
            return false;
        }

        if (taxRate < 0 || taxRate > 100) {
            player.sendMessage(plugin.getMessage("town.tax.invalid-rate", "Invalid rate!"));
            return false;
        }

        town.setTaxRate(taxRate);
        return true;
    }

    public boolean setDescription(String townName, Player player, String description) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.canManage(player.getName())) {
            player.sendMessage(plugin.getMessage("town.description.no-permission", "No permission!"));
            return false;
        }

        int maxLength = plugin.getPluginConfig().getInt("limits.max-description-length", 100);
        if (description.length() > maxLength) {
            player.sendMessage(plugin.getMessage("town.description.too-long", "Too long!", String.valueOf(maxLength)));
            return false;
        }

        town.setDescription(description);
        return true;
    }

    public boolean setTag(String townName, Player player, String tag) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.canManage(player.getName())) {
            player.sendMessage(plugin.getMessage("town.tag.no-permission", "No permission!"));
            return false;
        }

        if (!tag.matches("[A-Z]{3,5}")) {
            player.sendMessage(plugin.getMessage("town.tag.invalid", "Invalid tag!"));
            return false;
        }

        town.setTag(tag);
        return true;
    }

    // ============ РОЛІ ============

    public boolean addAssistant(String townName, Player player, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.assistant.mayor-only", "Mayor only!"));
            return false;
        }

        if (!town.isResident(targetName)) {
            player.sendMessage(plugin.getMessage("town.assistant.not-resident", "Not resident!", targetName));
            return false;
        }

        if (town.addAssistant(targetName)) {
            player.sendMessage(plugin.getMessage("town.assistant.add-success", "Assistant added!", targetName));

            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(plugin.getMessage("town.assistant.promoted", "You are assistant now!", town.getName()));
            }
            return true;
        }

        player.sendMessage(plugin.getMessage("town.assistant.failed", "Failed!"));
        return false;
    }

    public boolean removeAssistant(String townName, Player player, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.assistant.mayor-only", "Mayor only!"));
            return false;
        }

        if (town.removeAssistant(targetName)) {
            player.sendMessage(plugin.getMessage("town.assistant.remove-success", "Assistant removed!", targetName));

            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(plugin.getMessage("town.assistant.demoted", "Assistant removed!", town.getName()));
            }
            return true;
        }

        return false;
    }

    public boolean addDeputy(String townName, Player player, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.deputy.mayor-only", "Mayor only!"));
            return false;
        }

        if (!town.isResident(targetName)) {
            player.sendMessage(plugin.getMessage("town.deputy.not-resident", "Not resident!", targetName));
            return false;
        }

        if (town.addDeputy(targetName)) {
            player.sendMessage(plugin.getMessage("town.deputy.add-success", "Deputy added!", targetName));

            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(plugin.getMessage("town.deputy.promoted", "You are deputy now!", town.getName()));
            }
            return true;
        }

        player.sendMessage(plugin.getMessage("town.deputy.failed", "Failed!"));
        return false;
    }

    public boolean removeDeputy(String townName, Player player, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.deputy.mayor-only", "Mayor only!"));
            return false;
        }

        if (town.removeDeputy(targetName)) {
            player.sendMessage(plugin.getMessage("town.deputy.remove-success", "Deputy removed!", targetName));

            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(plugin.getMessage("town.deputy.demoted", "Deputy removed!", town.getName()));
            }
            return true;
        }

        return false;
    }

    public boolean transferMayor(String townName, Player player, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.transfer.mayor-only", "Mayor only!"));
            return false;
        }

        if (!town.isResident(targetName)) {
            player.sendMessage(plugin.getMessage("town.transfer.not-resident", "Not resident!", targetName));
            return false;
        }

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.transfer.self", "Cannot transfer to yourself!"));
            return false;
        }

        String oldMayor = town.getMayor();
        town.setMayor(targetName);
        town.removeAssistant(oldMayor);
        town.removeDeputy(oldMayor);
        town.addResident(oldMayor);

        player.sendMessage(plugin.getMessage("town.transfer.success", "Transferred!", targetName));

        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target != null) {
            target.sendMessage(plugin.getMessage("town.transfer.received", "You are now mayor!", town.getName()));
        }

        plugin.broadcastToTown(town, "town.transfer.notify", oldMayor, targetName);
        return true;
    }

    // ============ ВІЙНИ ТА СОЮЗИ ============

    public boolean declareWar(String attackerTown, String defenderTown) {
        TownData attacker = getTown(attackerTown);
        TownData defender = getTown(defenderTown);
        if (attacker == null || defender == null) return false;

        if (attacker.isEnemy(defenderTown) || defender.isEnemy(attackerTown)) {
            return false;
        }

        // Перевірка захисту нових міст
        long protectionTime = plugin.getPluginConfig().getLong("town-creation.new-town-protection", 24) * 3600000L;
        long currentTime = System.currentTimeMillis();

        if (currentTime - attacker.getCreationTime() < protectionTime ||
                currentTime - defender.getCreationTime() < protectionTime) {
            return false;
        }

        // Перевірка вартості
        double warCost = plugin.getPluginConfig().getDouble("war.war-declaration-cost", 500.0);
        if (attacker.getBalance() < warCost) {
            return false;
        }

        attacker.setBalance(attacker.getBalance() - warCost);
        attacker.addEnemy(defenderTown);
        defender.addEnemy(attackerTown);

        String warKey = attackerTown.toLowerCase() + ":" + defenderTown.toLowerCase();
        warDeclarations.put(warKey, currentTime);

        return true;
    }

    public boolean makePeace(String town1, String town2) {
        TownData t1 = getTown(town1);
        TownData t2 = getTown(town2);
        if (t1 == null || t2 == null) return false;

        t1.removeEnemy(town2);
        t2.removeEnemy(town1);

        String warKey1 = town1.toLowerCase() + ":" + town2.toLowerCase();
        String warKey2 = town2.toLowerCase() + ":" + town1.toLowerCase();
        warDeclarations.remove(warKey1);
        warDeclarations.remove(warKey2);

        return true;
    }

    public boolean addAlly(String town1, String town2) {
        TownData t1 = getTown(town1);
        TownData t2 = getTown(town2);
        if (t1 == null || t2 == null) return false;

        if (t1.isEnemy(town2) || t2.isEnemy(town1)) {
            return false; // Не можна бути союзником з ворогом
        }

        t1.addAlly(town2);
        t2.addAlly(town1);

        return true;
    }

    public boolean removeAlly(String town1, String town2) {
        TownData t1 = getTown(town1);
        TownData t2 = getTown(town2);
        if (t1 == null || t2 == null) return false;

        t1.removeAlly(town2);
        t2.removeAlly(town1);

        return true;
    }

    // ============ ПРИВАТНІ ДІЛЯНКИ ============

    public boolean givePlot(Player giver, String receiverName, Position position) {
        TownData town = getPlayerTown(giver.getName());
        if (town == null) return false;

        if (!town.canWithdraw(giver.getName())) {
            giver.sendMessage(plugin.getMessage("town.plot.no-permission-plot", "No permission!"));
            return false;
        }

        String chunkKey = getChunkKey(position);

        if (!town.hasChunk(chunkKey)) {
            giver.sendMessage(plugin.getMessage("town.plot.not-town-chunk", "Not your town's chunk!"));
            return false;
        }

        if (!town.isResident(receiverName)) {
            giver.sendMessage(plugin.getMessage("town.plot.receiver-not-resident", "Player not in town!"));
            return false;
        }

        int maxPlots = plugin.getPluginConfig().getInt("plots.max-plots-per-resident", 3);
        int currentPlots = countPlayerPlots(receiverName);
        if (currentPlots >= maxPlots) {
            giver.sendMessage(plugin.getMessage("town.plot.max-plots", "Player has max plots!", String.valueOf(maxPlots)));
            return false;
        }

        plotOwners.put(chunkKey, receiverName.toLowerCase());

        giver.sendMessage(plugin.getMessage("town.plot.give-success", "Plot given!", receiverName));

        Player receiver = plugin.getServer().getPlayerExact(receiverName);
        if (receiver != null) {
            receiver.sendMessage(plugin.getMessage("town.plot.received", "You received a plot!", ""));
        }

        return true;
    }

    public boolean takePlot(Player taker, Position position) {
        TownData town = getPlayerTown(taker.getName());
        if (town == null) return false;

        if (!town.canWithdraw(taker.getName())) {
            taker.sendMessage(plugin.getMessage("town.plot.no-permission-plot", "No permission!"));
            return false;
        }

        String chunkKey = getChunkKey(position);

        if (!town.hasChunk(chunkKey) || !plotOwners.containsKey(chunkKey)) {
            taker.sendMessage(plugin.getMessage("town.plot.not-private", "Not a private plot!"));
            return false;
        }

        String owner = plotOwners.get(chunkKey);
        plotOwners.remove(chunkKey);

        taker.sendMessage(plugin.getMessage("town.plot.take-success", "Plot taken!", ""));

        Player ownerPlayer = plugin.getServer().getPlayerExact(owner);
        if (ownerPlayer != null) {
            ownerPlayer.sendMessage(plugin.getMessage("town.plot.taken", "Your plot was taken!", ""));
        }

        return true;
    }

    // ============ БАН СИСТЕМА ============

    public boolean banPlayer(String townName, Player banner, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.canManage(banner.getName())) {
            banner.sendMessage(plugin.getMessage("town.ban.no-permission", "No permission!"));
            return false;
        }

        if (!town.isResident(targetName) && !town.isBanned(targetName)) {
            banner.sendMessage(plugin.getMessage("town.ban.not-resident", "Not resident!", targetName));
            return false;
        }

        if (town.getMayor().equalsIgnoreCase(targetName)) {
            banner.sendMessage(plugin.getMessage("town.ban.cannot-ban-mayor", "Cannot ban mayor!"));
            return false;
        }

        town.addBan(targetName);
        removeResident(townName, targetName);

        banner.sendMessage(plugin.getMessage("town.ban.success", "Player banned!", targetName));

        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target != null) {
            target.sendMessage(plugin.getMessage("town.ban.banned", "You were banned!", town.getName()));
        }

        return true;
    }

    public boolean unbanPlayer(String townName, Player unbanner, String targetName) {
        TownData town = getTown(townName);
        if (town == null) return false;

        if (!town.canManage(unbanner.getName())) {
            unbanner.sendMessage(plugin.getMessage("town.unban.no-permission", "No permission!"));
            return false;
        }

        if (!town.isBanned(targetName)) {
            unbanner.sendMessage(plugin.getMessage("town.unban.not-banned", "Not banned!", targetName));
            return false;
        }

        town.removeBan(targetName);
        unbanner.sendMessage(plugin.getMessage("town.unban.success", "Player unbanned!", targetName));

        return true;
    }

    // ============ АВТОМАТИЧНІ ЗАДАЧІ ============

    public void processTaxes() {
        double chunkTax = plugin.getPluginConfig().getDouble("economy.chunk-tax", 10.0);

        for (TownData town : towns.values()) {
            double taxAmount = town.getChunkCount() * chunkTax;
            if (taxAmount > 0) {
                if (town.getBalance() >= taxAmount) {
                    town.setBalance(town.getBalance() - taxAmount);

                    Player mayor = plugin.getServer().getPlayerExact(town.getMayor());
                    if (mayor != null && plugin.getPluginConfig().getBoolean("notifications.notify-tax-collection", true)) {
                        mayor.sendMessage(TextFormat.YELLOW + "Town tax paid: $" + String.format("%.2f", taxAmount));
                    }
                } else {
                    // Банкрутство - видалення чанків
                    if (plugin.getPluginConfig().getBoolean("chunks.auto-unclaim-on-bankruptcy", true)) {
                        handleBankruptcy(town);
                    }
                }
            }
        }
    }

    public void processSalaries() {
        double mayorSalary = plugin.getPluginConfig().getDouble("economy.mayor-salary", 50.0);
        double deputySalary = plugin.getPluginConfig().getDouble("economy.deputy-salary", 25.0);
        double assistantSalary = plugin.getPluginConfig().getDouble("economy.assistant-salary", 10.0);

        for (TownData town : towns.values()) {
            double totalSalary = 0;

            // Зарплата мера
            if (mayorSalary > 0) {
                totalSalary += mayorSalary;
            }

            // Зарплати заступників
            for (String deputy : town.getDeputies()) {
                if (deputySalary > 0) {
                    totalSalary += deputySalary;
                }
            }

            // Зарплати помічників
            for (String assistant : town.getAssistants()) {
                if (assistantSalary > 0) {
                    totalSalary += assistantSalary;
                }
            }

            if (totalSalary > 0 && town.getBalance() >= totalSalary) {
                town.setBalance(town.getBalance() - totalSalary);

                // Виплата гравцям
                if (mayorSalary > 0) {
                    Player mayor = plugin.getServer().getPlayerExact(town.getMayor());
                    if (mayor != null) {
                        economy.addMoney(mayor, mayorSalary);
                        mayor.sendMessage(TextFormat.GREEN + "Mayor salary: $" + String.format("%.2f", mayorSalary));
                    }
                }

                for (String deputy : town.getDeputies()) {
                    Player deputyPlayer = plugin.getServer().getPlayerExact(deputy);
                    if (deputyPlayer != null && deputySalary > 0) {
                        economy.addMoney(deputyPlayer, deputySalary);
                        deputyPlayer.sendMessage(TextFormat.GREEN + "Deputy salary: $" + String.format("%.2f", deputySalary));
                    }
                }

                for (String assistant : town.getAssistants()) {
                    Player assistantPlayer = plugin.getServer().getPlayerExact(assistant);
                    if (assistantPlayer != null && assistantSalary > 0) {
                        economy.addMoney(assistantPlayer, assistantSalary);
                        assistantPlayer.sendMessage(TextFormat.GREEN + "Assistant salary: $" + String.format("%.2f", assistantSalary));
                    }
                }
            }
        }
    }

    public void processWars() {
        long currentTime = System.currentTimeMillis();
        long minWarDuration = plugin.getPluginConfig().getLong("war.min-war-duration", 2) * 3600000L;
        long maxWarDuration = plugin.getPluginConfig().getLong("war.max-war-duration", 168) * 3600000L;

        for (Map.Entry<String, Long> entry : new HashMap<>(warDeclarations).entrySet()) {
            long warStart = entry.getValue();
            long warDuration = currentTime - warStart;

            // Автоматичний мир після максимальної тривалості
            if (plugin.getPluginConfig().getBoolean("war.auto-peace-after-duration", true) &&
                    warDuration > maxWarDuration) {
                String[] towns = entry.getKey().split(":");
                if (towns.length == 2) {
                    makePeace(towns[0], towns[1]);

                    TownData town1 = getTown(towns[0]);
                    TownData town2 = getTown(towns[1]);
                    if (town1 != null && town2 != null) {
                        plugin.broadcastToTown(town1, "town.peace.auto", towns[1]);
                        plugin.broadcastToTown(town2, "town.peace.auto", towns[0]);
                    }
                }
            }
        }
    }

    public void cleanupInactiveTowns() {
        long inactivityDays = plugin.getPluginConfig().getLong("inactivity.town-inactivity-days", 30);
        long autoDisbandDays = plugin.getPluginConfig().getLong("inactivity.auto-disband-days", 60);
        long currentTime = System.currentTimeMillis();

        for (TownData town : new HashSet<>(towns.values())) {
            boolean townActive = false;

            // Перевірка активності жителів
            for (String resident : town.getAllMembers()) {
                Long lastActive = lastActivity.get(resident.toLowerCase());
                if (lastActive != null && currentTime - lastActive < inactivityDays * 86400000L) {
                    townActive = true;
                    break;
                }
            }

            if (!townActive) {
                long townAge = currentTime - town.getCreationTime();

                // Автоматичний розпуск старих неактивних міст
                if (townAge > autoDisbandDays * 86400000L) {
                    towns.remove(town.getName().toLowerCase());

                    // Видалення відносин
                    removeTownRelations(town.getName());

                    plugin.getLogger().info("Auto-disbanded inactive town: " + town.getName());
                }
            }
        }
    }

    public void kickInactiveResidents() {
        if (!plugin.getPluginConfig().getBoolean("inactivity.auto-kick-inactive-residents", true)) {
            return;
        }

        long inactivityDays = plugin.getPluginConfig().getLong("inactivity.resident-inactivity-days", 14);
        long currentTime = System.currentTimeMillis();

        for (TownData town : towns.values()) {
            for (String resident : new HashSet<>(town.getAllMembers())) {
                if (resident.equalsIgnoreCase(town.getMayor())) {
                    continue; // Мера не виключаємо
                }

                Long lastActive = lastActivity.get(resident.toLowerCase());
                if (lastActive != null && currentTime - lastActive > inactivityDays * 86400000L) {
                    removeResident(town.getName(), resident);

                    Player residentPlayer = plugin.getServer().getPlayerExact(resident);
                    if (residentPlayer != null) {
                        residentPlayer.sendMessage(plugin.getMessage("town.kick.inactive", "Kicked for inactivity!", town.getName()));
                    }

                    plugin.broadcastToTown(town, "town.kick.inactive-notify", resident);
                }
            }
        }
    }

    // ============ ДОПОМІЖНІ МЕТОДИ ============

    public String getChunkKey(Position pos) {
        return pos.getLevel().getName() + ":" + (pos.getFloorX() >> 4) + ":" + (pos.getFloorZ() >> 4);
    }

    public String getChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    public TownData getTown(String name) {
        return towns.get(name.toLowerCase());
    }

    public TownData getPlayerTown(String playerName) {
        String townName = playerTowns.get(playerName.toLowerCase());
        return townName != null ? getTown(townName) : null;
    }

    public String getPlayerTownName(String playerName) {
        return playerTowns.get(playerName.toLowerCase());
    }

    public String getTownAtChunk(String world, int chunkX, int chunkZ) {
        String chunkKey = getChunkKey(world, chunkX, chunkZ);
        for (Map.Entry<String, TownData> entry : towns.entrySet()) {
            if (entry.getValue().hasChunk(chunkKey)) {
                return entry.getValue().getName();
            }
        }
        return null;
    }

    public String getPlotOwner(String chunkKey) {
        return plotOwners.get(chunkKey);
    }

    public int countPlayerPlots(String playerName) {
        int count = 0;
        for (String owner : plotOwners.values()) {
            if (owner.equalsIgnoreCase(playerName)) {
                count++;
            }
        }
        return count;
    }

    public boolean isChunkAdjacent(TownData town, String chunkKey) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return false;

        String world = parts[0];
        int chunkX = Integer.parseInt(parts[1]);
        int chunkZ = Integer.parseInt(parts[2]);

        String[] adjacent = {
                getChunkKey(world, chunkX + 1, chunkZ),
                getChunkKey(world, chunkX - 1, chunkZ),
                getChunkKey(world, chunkX, chunkZ + 1),
                getChunkKey(world, chunkX, chunkZ - 1)
        };

        for (String adjChunk : adjacent) {
            if (town.hasChunk(adjChunk)) {
                return true;
            }
        }

        return false;
    }

    public double getDistanceFromHome(TownData town, String chunkKey) {
        if (town.getHome() == null) return 0;

        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return 0;

        int chunkX = Integer.parseInt(parts[1]);
        int chunkZ = Integer.parseInt(parts[2]);

        int homeChunkX = town.getHome().getFloorX() >> 4;
        int homeChunkZ = town.getHome().getFloorZ() >> 4;

        int dx = Math.abs(chunkX - homeChunkX);
        int dz = Math.abs(chunkZ - homeChunkZ);

        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean isTooCloseToOtherTown(Position position) {
        int minDistance = plugin.getPluginConfig().getInt("limits.min-town-distance", 5);

        for (TownData town : towns.values()) {
            if (town.getHome() != null && town.getHome().getLevel() != null &&
                    town.getHome().getLevel().getName().equals(position.getLevel().getName())) {

                double distance = position.distance(town.getHome());
                if (distance < minDistance * 16) { // 16 блоків в чанку
                    return true;
                }
            }
        }

        return false;
    }

    private void removeTownRelations(String townName) {
        // Видалення всіх відносин цього міста
        for (TownData town : towns.values()) {
            town.removeEnemy(townName);
            town.removeAlly(townName);
        }

        // Видалення записів про війни
        List<String> toRemove = new ArrayList<>();
        for (String warKey : warDeclarations.keySet()) {
            if (warKey.contains(townName.toLowerCase())) {
                toRemove.add(warKey);
            }
        }
        for (String key : toRemove) {
            warDeclarations.remove(key);
        }
    }

    private void handleBankruptcy(TownData town) {
        // Видалення чанків поки баланс не стане позитивним
        List<String> chunks = new ArrayList<>(town.getChunks());
        double chunkValue = plugin.getPluginConfig().getDouble("economy.chunk-claim-cost", 50.0) *
                plugin.getPluginConfig().getDouble("economy.chunk-unclaim-refund", 0.25);

        Collections.shuffle(chunks);

        for (String chunkKey : chunks) {
            if (town.getHome() != null) {
                String homeChunkKey = getChunkKey(town.getHome());
                if (homeChunkKey.equals(chunkKey)) {
                    continue; // Не видаляємо домашній чанк
                }
            }

            town.removeChunk(chunkKey);
            plotOwners.remove(chunkKey);
            chunkFlags.remove(chunkKey);

            town.setBalance(town.getBalance() + chunkValue);

            if (town.getBalance() >= 0) {
                break;
            }
        }

        plugin.broadcastToTown(town, "town.bankruptcy", "");
    }

    private void startActivityTracker() {
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            // Оновлення активності для онлайн гравців
            for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                lastActivity.put(player.getName().toLowerCase(), currentTime);
            }

            // Виконання завдань по розкладу
            if (currentTime % (3600000) < 1000) { // Кожну годину
                processTaxes();
            }

            if (currentTime % (86400000) < 1000) { // Кожен день
                processSalaries();
                kickInactiveResidents();
            }

            if (currentTime % (604800000) < 1000) { // Кожен тиждень
                cleanupInactiveTowns();
            }
        }, 20 * 60); // Кожну хвилину
    }

    // ============ ЗБЕРЕЖЕННЯ ДАНИХ ============

    private void loadData() {
        if (!dataFile.exists()) return;

        Config config = new Config(dataFile, Config.YAML);
        for (String key : config.getKeys(false)) {
            try {
                TownData town = TownData.fromMap(config.getSection(key));
                towns.put(key.toLowerCase(), town);

                for (String resident : town.getAllMembers()) {
                    playerTowns.put(resident.toLowerCase(), town.getName());
                }

                // Завантаження ділянок
                Map<String, Object> plots = config.getSection(key + ".plots");
                if (plots != null) {
                    for (Map.Entry<String, Object> entry : plots.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            plotOwners.put(entry.getKey(), (String) entry.getValue());
                        }
                    }
                }

                // Завантаження війн
                List<String> wars = config.getStringList(key + ".wars");
                for (String warTown : wars) {
                    String warKey = key.toLowerCase() + ":" + warTown.toLowerCase();
                    warDeclarations.put(warKey, System.currentTimeMillis() - 3600000L); // 1 година тому
                }
            } catch (Exception e) {
                plugin.getLogger().error("Failed to load town: " + key, e);
            }
        }
    }

    public void saveAllData() {
        Config config = new Config(dataFile, Config.YAML);

        for (String key : config.getKeys(false)) {
            config.remove(key);
        }

        for (TownData town : towns.values()) {
            config.set(town.getName().toLowerCase(), town.toMap());

            // Збереження ділянок
            Map<String, String> townPlots = new HashMap<>();
            for (String chunkKey : town.getChunks()) {
                if (plotOwners.containsKey(chunkKey)) {
                    townPlots.put(chunkKey, plotOwners.get(chunkKey));
                }
            }
            if (!townPlots.isEmpty()) {
                config.set(town.getName().toLowerCase() + ".plots", townPlots);
            }

            // Збереження війн
            List<String> wars = new ArrayList<>();
            for (String enemy : town.getEnemies()) {
                wars.add(enemy);
            }
            if (!wars.isEmpty()) {
                config.set(town.getName().toLowerCase() + ".wars", wars);
            }
        }

        config.save();
        plugin.getLogger().info("Saved " + towns.size() + " towns to disk.");
    }

    // ============ ДОДАТКОВІ МЕТОДИ ДЛЯ КОМАНД ============

    public List<TownData> getTownList() {
        return new ArrayList<>(towns.values());
    }

    public List<TownData> getTownsSortedByBalance() {
        List<TownData> sorted = new ArrayList<>(towns.values());
        sorted.sort((t1, t2) -> Double.compare(t2.getBalance(), t1.getBalance()));
        return sorted;
    }

    public List<TownData> getTownsSortedByResidents() {
        List<TownData> sorted = new ArrayList<>(towns.values());
        sorted.sort((t1, t2) -> Integer.compare(t2.getMemberCount(), t1.getMemberCount()));
        return sorted;
    }

    public void updatePlayerActivity(String playerName) {
        lastActivity.put(playerName.toLowerCase(), System.currentTimeMillis());
    }

    public boolean canBuild(Player player, String chunkKey) {
        String townName = getTownAtChunk(
                player.getLevel().getName(),
                player.getFloorX() >> 4,
                player.getFloorZ() >> 4
        );

        if (townName == null) return true; // Не заявлена територія

        TownData town = getTown(townName);
        if (town == null) return false;

        // Перевірка на бан
        if (town.isBanned(player.getName())) return false;

        // Перевірка приватної ділянки
        String plotOwner = getPlotOwner(chunkKey);
        if (plotOwner != null) {
            return plotOwner.equalsIgnoreCase(player.getName());
        }

        // Перевірка жителя
        return town.isResident(player.getName());
    }

    public boolean canAccessChest(Player player, String chunkKey) {
        return canBuild(player, chunkKey); // Та сама логіка
    }

    public boolean canUseDoor(Player player, String chunkKey) {
        return canBuild(player, chunkKey);
    }

    public boolean isPvPAllowed(Player attacker, Player victim) {
        String attackerTownName = getPlayerTownName(attacker.getName());
        String victimTownName = getPlayerTownName(victim.getName());

        // Обидва в містах
        if (attackerTownName != null && victimTownName != null) {
            TownData attackerTown = getTown(attackerTownName);
            TownData victimTown = getTown(victimTownName);

            if (attackerTown != null && victimTown != null) {
                // Війна - дозволяємо PvP
                if (attackerTown.isEnemy(victimTownName) || victimTown.isEnemy(attackerTownName)) {
                    return plugin.getPluginConfig().getBoolean("war.allow-pvp-in-enemy-territory", true);
                }
            }
        }

        // Перевірка прапора PvP в чанку
        String chunkKey = getChunkKey(victim.getPosition());
        String townName = getTownAtChunk(
                victim.getLevel().getName(),
                victim.getFloorX() >> 4,
                victim.getFloorZ() >> 4
        );

        if (townName != null) {
            // Тут можна додати перевірку прапорів міста
            return false; // За замовчуванням PvP вимкнено в містах
        }

        return true; // На дикій землі дозволяємо
    }
}