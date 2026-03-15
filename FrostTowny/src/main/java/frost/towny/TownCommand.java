package frost.towny;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
import java.util.*;

public class TownCommand {

    private FrostTowny plugin;
    private TownManager townManager;

    public TownCommand(FrostTowny plugin) {
        this.plugin = plugin;
        this.townManager = plugin.getTownManager();
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        // Базова перевірка для не-гравців
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("command.only-ingame", "&cThis command can only be used in-game!"));
            return true;
        }

        Player player = (Player) sender;

        // Перевірка дозволу
        if (!player.hasPermission("frosttowny.command.town") && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(TextFormat.colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        // Обробка порожньої команди або "help"
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            switch (subCommand) {
                case "create":
                    handleCreate(player, args);
                    break;
                case "delete":
                case "disband":
                    handleDelete(player, args);
                    break;
                case "info":
                case "show":
                case "i":
                    handleInfo(player, args);
                    break;
                case "join":
                    handleJoin(player, args);
                    break;
                case "leave":
                    handleLeave(player, args);
                    break;
                case "invite":
                    handleInvite(player, args);
                    break;
                case "kick":
                    handleKick(player, args);
                    break;
                case "ban":
                    handleBan(player, args);
                    break;
                case "unban":
                    handleUnban(player, args);
                    break;
                case "claim":
                    handleClaim(player, args);
                    break;
                case "unclaim":
                    handleUnclaim(player, args);
                    break;
                case "deposit":
                case "dep":
                    handleDeposit(player, args);
                    break;
                case "withdraw":
                case "with":
                    handleWithdraw(player, args);
                    break;
                case "balance":
                case "bal":
                    handleBalance(player, args);
                    break;
                case "tax":
                    handleTax(player, args);
                    break;
                case "sethome":
                case "setspawn":
                    handleSetHome(player, args);
                    break;
                case "home":
                case "spawn":
                    handleHome(player, args);
                    break;
                case "assistant":
                case "assist":
                    handleAssistant(player, args);
                    break;
                case "deputy":
                    handleDeputy(player, args);
                    break;
                case "transfer":
                    handleTransfer(player, args);
                    break;
                case "war":
                case "enemy":
                    handleWar(player, args);
                    break;
                case "peace":
                case "truce":
                    handlePeace(player, args);
                    break;
                case "ally":
                    handleAlly(player, args);
                    break;
                case "description":
                case "desc":
                    handleDescription(player, args);
                    break;
                case "tag":
                    handleTag(player, args);
                    break;
                case "plot":
                    handlePlot(player, args);
                    break;
                case "list":
                    handleList(player, args);
                    break;
                case "top":
                    handleTop(player, args);
                    break;
                case "help":
                case "?":
                    showHelp(player);
                    break;
                default:
                    player.sendMessage(plugin.getMessage("command.unknown", "&cUnknown command!"));
                    showHelp(player);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getMessage("error.generic", "&cAn error occurred!"));
            plugin.getLogger().error("Error executing command: " + String.join(" ", args), e);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("frosttowny.create") && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("command.no-permission", "&cYou don't have permission!"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town create <name>"));
            return;
        }

        String townName = args[1];
        townManager.createTown(townName, player, player.getPosition());
    }

    private void handleDelete(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.delete.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.delete.not-mayor", "&cOnly mayor can delete town!"));
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            townManager.deleteTown(town.getName(), player);
        } else {
            player.sendMessage(plugin.getMessage("town.delete.confirmation", "&eType &6/town delete confirm &eto confirm"));
        }
    }

    private void handleInfo(Player player, String[] args) {
        String townName = null;

        if (args.length >= 2) {
            townName = args[1];
        } else {
            townName = townManager.getTownAtChunk(
                    player.getLevel().getName(),
                    player.getFloorX() >> 4,
                    player.getFloorZ() >> 4
            );

            if (townName == null) {
                TownData playerTown = townManager.getPlayerTown(player.getName());
                if (playerTown != null) {
                    townName = playerTown.getName();
                } else {
                    player.sendMessage(plugin.getMessage("town.info.not-in-town", "&cYou are not in a town!"));
                    return;
                }
            }
        }

        TownData town = townManager.getTown(townName);
        if (town == null) {
            player.sendMessage(plugin.getMessage("command.town-not-found", "&cTown not found!"));
            return;
        }

        player.sendMessage(plugin.getMessage("town.info.header", "&6=== {0} ===", town.getName()));
        player.sendMessage(plugin.getMessage("town.info.mayor", "&eMayor: &f{0}", town.getMayor()));
        player.sendMessage(plugin.getMessage("town.info.created", "&eCreated: &f{0} ago", formatTime(town.getCreationTime())));
        player.sendMessage(plugin.getMessage("town.info.balance", "&eBalance: &a${0}", String.format("%.2f", town.getBalance())));
        player.sendMessage(plugin.getMessage("town.info.tax", "&eTax: &f{0}%", String.format("%.1f", town.getTaxRate())));
        player.sendMessage(plugin.getMessage("town.info.chunks", "&eChunks: &f{0}", String.valueOf(town.getChunkCount())));
        player.sendMessage(plugin.getMessage("town.info.residents", "&eResidents: &f{0}/{1}",
                String.valueOf(town.getMemberCount()),
                String.valueOf(plugin.getPluginConfig().getInt("limits.max-residents", 20))
        ));

        if (!town.getDescription().isEmpty()) {
            player.sendMessage(plugin.getMessage("town.info.description", "&7{0}", town.getDescription()));
        }

        if (!town.getTag().isEmpty()) {
            player.sendMessage(plugin.getMessage("town.info.tag", "&eTag: &f[{0}]", town.getTag()));
        }

        if (town.isResident(player.getName())) {
            String rank = town.getRank(player.getName());
            player.sendMessage(plugin.getMessage("town.info.rank", "&eYour rank: &f{0}", rank));
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town join <town>"));
            return;
        }

        String townName = args[1];
        TownData town = townManager.getTown(townName);

        if (town == null) {
            player.sendMessage(plugin.getMessage("command.town-not-found", "&cTown not found!"));
            return;
        }

        if (townManager.addResident(townName, player.getName())) {
            player.sendMessage(plugin.getMessage("town.join.success", "&aJoined {0}!", townName));
            plugin.broadcastToTown(town, "town.join.notify", player.getName());
        } else {
            player.sendMessage(plugin.getMessage("town.join.failed", "&cFailed to join!"));
        }
    }

    private void handleLeave(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.leave.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (town.getMayor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("town.leave.mayor-cant-leave", "&cMayor cannot leave town!"));
            return;
        }

        if (townManager.leaveTown(player)) {
            player.sendMessage(plugin.getMessage("town.leave.success", "&aYou left the town!"));
        } else {
            player.sendMessage(plugin.getMessage("town.leave.failed", "&cFailed to leave!"));
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town invite <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.invite.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canInvite(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.invite.no-permission", "&cNo permission!"));
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayerExact(targetName);

        if (target == null) {
            player.sendMessage(plugin.getMessage("command.player-not-found", "&cPlayer not found!"));
            return;
        }

        town.addInvite(targetName, player.getName());
        player.sendMessage(plugin.getMessage("town.invite.invited", "&aInvited {0}!", targetName));
        target.sendMessage(plugin.getMessage("town.invite.invite-received", "&aYou were invited to {0}!", town.getName()));
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town kick <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.kick.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.kick.no-permission", "&cNo permission!"));
            return;
        }

        String targetName = args[1];

        if (town.getMayor().equalsIgnoreCase(targetName)) {
            player.sendMessage(plugin.getMessage("town.kick.cannot-kick-mayor", "&cCannot kick mayor!"));
            return;
        }

        if (townManager.removeResident(town.getName(), targetName)) {
            player.sendMessage(plugin.getMessage("town.kick.success", "&aKicked {0}!", targetName));

            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(plugin.getMessage("town.kick.kicked", "&cYou were kicked from {0}!", town.getName()));
            }

            plugin.broadcastToTown(town, "town.kick.notify", targetName);
        }
    }

    private void handleBan(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town ban <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.ban.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.ban.no-permission", "&cNo permission!"));
            return;
        }

        String targetName = args[1];
        townManager.banPlayer(town.getName(), player, targetName);
    }

    private void handleUnban(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town unban <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.unban.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.unban.no-permission", "&cNo permission!"));
            return;
        }

        String targetName = args[1];
        townManager.unbanPlayer(town.getName(), player, targetName);
    }

    private void handleClaim(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.claim.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.claim.no-permission-claim", "&cNo permission!"));
            return;
        }

        townManager.claimChunk(player, player.getPosition());
    }

    private void handleUnclaim(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.unclaim.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.unclaim.no-permission", "&cNo permission!"));
            return;
        }

        townManager.unclaimChunk(player, player.getPosition());
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town deposit <amount>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.deposit.not-in-town", "&cYou are not in a town!"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            townManager.depositToTown(town.getName(), player, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("command.invalid-number", "&cInvalid number!"));
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town withdraw <amount>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.withdraw.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canWithdraw(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.withdraw.no-permission-withdraw", "&cNo permission!"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            townManager.withdrawFromTown(town.getName(), player, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("command.invalid-number", "&cInvalid number!"));
        }
    }

    private void handleBalance(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.balance.not-in-town", "&cYou are not in a town!"));
            return;
        }

        player.sendMessage(plugin.getMessage("town.balance.header", "&6=== Town Balance ==="));
        player.sendMessage(plugin.getMessage("town.balance.town-name", "&eTown: &f{0}", town.getName()));
        player.sendMessage(plugin.getMessage("town.balance.balance", "&eBalance: &a${0}", String.format("%.2f", town.getBalance())));
        player.sendMessage(plugin.getMessage("town.balance.chunks", "&eChunks: &f{0}", String.valueOf(town.getChunkCount())));

        double chunkTax = plugin.getPluginConfig().getDouble("economy.chunk-tax", 10.0);
        double nextTax = town.getChunkCount() * chunkTax;
        player.sendMessage(plugin.getMessage("town.balance.next-tax", "&eNext tax: &c${0}", String.format("%.2f", nextTax)));
    }

    private void handleTax(Player player, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town tax set <0-100>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.tax.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.tax.mayor-only", "&cOnly mayor can set tax!"));
            return;
        }

        try {
            double taxRate = Double.parseDouble(args[2]);
            if (townManager.setTaxRate(town.getName(), player, taxRate)) {
                player.sendMessage(plugin.getMessage("town.tax.set", "&aTax set to {0}%!", String.format("%.1f", taxRate)));
                plugin.broadcastToTown(town, "town.tax.notify", String.format("%.1f", taxRate));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("command.invalid-number", "&cInvalid number!"));
        }
    }

    private void handleSetHome(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.home.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.home.no-permission", "&cNo permission!"));
            return;
        }

        String chunkKey = townManager.getChunkKey(player.getPosition());
        if (!town.hasChunk(chunkKey)) {
            player.sendMessage(plugin.getMessage("town.home.no-permission-home", "&cMust be in town territory!"));
            return;
        }

        town.setHome(player.getPosition());
        player.sendMessage(plugin.getMessage("town.home.set", "&aTown home set!"));
    }

    private void handleHome(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.home.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (town.getHome() == null) {
            player.sendMessage(plugin.getMessage("town.home.not-set", "&cTown home not set!"));
            return;
        }

        String homeKey = player.getName() + "_home";
        if (plugin.hasCooldown(homeKey)) {
            long remaining = plugin.getRemainingCooldown(homeKey);
            player.sendMessage(plugin.getMessage("town.home.cooldown", "&eWait {0} seconds!", String.valueOf(remaining)));
            return;
        }

        player.teleport(town.getHome());
        player.sendMessage(plugin.getMessage("town.home.teleported", "&aTeleported!"));
        plugin.setCooldown(homeKey, 30000);
    }

    private void handleAssistant(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town assistant <add/remove> <player>"));
            return;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];

        if (!action.equals("add") && !action.equals("remove")) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town assistant <add/remove> <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.assistant.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.assistant.mayor-only", "&cMayor only!"));
            return;
        }

        if (action.equals("add")) {
            townManager.addAssistant(town.getName(), player, targetName);
        } else {
            townManager.removeAssistant(town.getName(), player, targetName);
        }
    }

    private void handleDeputy(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town deputy <add/remove> <player>"));
            return;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];

        if (!action.equals("add") && !action.equals("remove")) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town deputy <add/remove> <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.deputy.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.deputy.mayor-only", "&cMayor only!"));
            return;
        }

        if (action.equals("add")) {
            townManager.addDeputy(town.getName(), player, targetName);
        } else {
            townManager.removeDeputy(town.getName(), player, targetName);
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town transfer <player>"));
            return;
        }

        String targetName = args[1];
        TownData town = townManager.getPlayerTown(player.getName());

        if (town == null) {
            player.sendMessage(plugin.getMessage("town.transfer.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.transfer.mayor-only", "&cMayor only!"));
            return;
        }

        townManager.transferMayor(town.getName(), player, targetName);
    }

    private void handleWar(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town war <town>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.war.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.getMayor().equalsIgnoreCase(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.war.mayor-only", "&cOnly mayor can declare war!"));
            return;
        }

        String enemyTown = args[1];
        if (townManager.declareWar(town.getName(), enemyTown)) {
            player.sendMessage(plugin.getMessage("town.war.declared", "&cWar declared on {0}!", enemyTown));
            plugin.broadcastToTown(town, "town.war.declared-notify", enemyTown);

            TownData enemy = townManager.getTown(enemyTown);
            if (enemy != null) {
                plugin.broadcastToTown(enemy, "town.war.received", town.getName());
            }
        } else {
            player.sendMessage(plugin.getMessage("town.war.failed", "&cFailed to declare war!"));
        }
    }

    private void handlePeace(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town peace <town>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.peace.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canWithdraw(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.peace.no-permission-peace", "&cNo permission!"));
            return;
        }

        String enemyTown = args[1];
        if (townManager.makePeace(town.getName(), enemyTown)) {
            player.sendMessage(plugin.getMessage("town.peace.made", "&aPeace made with {0}!", enemyTown));
            plugin.broadcastToTown(town, "town.peace.made-notify", enemyTown);

            TownData enemy = townManager.getTown(enemyTown);
            if (enemy != null) {
                plugin.broadcastToTown(enemy, "town.peace.received", town.getName());
            }
        }
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town ally <add/remove> <town>"));
            return;
        }

        String action = args[1].toLowerCase();
        String allyTown = args[2];

        if (!action.equals("add") && !action.equals("remove")) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town ally <add/remove> <town>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.ally.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.ally.no-permission", "&cNo permission!"));
            return;
        }

        if (action.equals("add")) {
            if (townManager.addAlly(town.getName(), allyTown)) {
                player.sendMessage(plugin.getMessage("town.ally.add-success", "&aNow allied with {0}!", allyTown));
                plugin.broadcastToTown(town, "town.ally.added", allyTown);

                TownData ally = townManager.getTown(allyTown);
                if (ally != null) {
                    plugin.broadcastToTown(ally, "town.ally.added", town.getName());
                }
            }
        } else {
            if (townManager.removeAlly(town.getName(), allyTown)) {
                player.sendMessage(plugin.getMessage("town.ally.remove-success", "&aNo longer allied with {0}!", allyTown));
                plugin.broadcastToTown(town, "town.ally.removed", allyTown);
            }
        }
    }

    private void handleDescription(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town description <text>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.description.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.description.no-permission", "&cNo permission!"));
            return;
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (townManager.setDescription(town.getName(), player, description)) {
            player.sendMessage(plugin.getMessage("town.description.set", "&aDescription updated!"));
        }
    }

    private void handleTag(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town tag <TAG>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.tag.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canManage(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.tag.no-permission", "&cNo permission!"));
            return;
        }

        String tag = args[1];

        if (townManager.setTag(town.getName(), player, tag)) {
            player.sendMessage(plugin.getMessage("town.tag.set", "&aTag set to [{0}]!", tag));
        }
    }

    private void handlePlot(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town plot <give/take/info>"));
            return;
        }

        String sub = args[1].toLowerCase();

        switch (sub) {
            case "give":
                handlePlotGive(player, args);
                break;
            case "take":
                handlePlotTake(player, args);
                break;
            case "info":
                handlePlotInfo(player, args);
                break;
            default:
                player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town plot <give/take/info>"));
        }
    }

    private void handlePlotGive(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("command.usage-prefix", "&cUsage: {0}", "/town plot give <player>"));
            return;
        }

        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.plot.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canWithdraw(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.plot.no-permission-plot", "&cNo permission!"));
            return;
        }

        String targetName = args[2];
        townManager.givePlot(player, targetName, player.getPosition());
    }

    private void handlePlotTake(Player player, String[] args) {
        TownData town = townManager.getPlayerTown(player.getName());
        if (town == null) {
            player.sendMessage(plugin.getMessage("town.plot.not-in-town", "&cYou are not in a town!"));
            return;
        }

        if (!town.canWithdraw(player.getName()) && !player.hasPermission("frosttowny.admin")) {
            player.sendMessage(plugin.getMessage("town.plot.no-permission-plot", "&cNo permission!"));
            return;
        }

        townManager.takePlot(player, player.getPosition());
    }

    private void handlePlotInfo(Player player, String[] args) {
        String chunkKey = townManager.getChunkKey(player.getPosition());
        String townName = townManager.getTownAtChunk(
                player.getLevel().getName(),
                player.getFloorX() >> 4,
                player.getFloorZ() >> 4
        );

        player.sendMessage(plugin.getMessage("town.plot.info", "&6=== Plot Information ==="));

        if (townName != null) {
            TownData town = townManager.getTown(townName);
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner != null) {
                player.sendMessage(plugin.getMessage("town.plot.private", "&eOwner: &f{0}", plotOwner));

                if (plotOwner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getMessage("town.plot.your-plot", "&aThis is your private plot!"));
                } else {
                    player.sendMessage(plugin.getMessage("town.plot.not-your-plot", "&cThis is a private plot!", plotOwner));
                }
            } else {
                player.sendMessage(plugin.getMessage("town.plot.public", "&aPublic town land"));

                if (town != null && town.isResident(player.getName())) {
                    player.sendMessage(plugin.getMessage("town.plot.can-build", "&aYou can build here"));
                } else {
                    player.sendMessage(plugin.getMessage("town.plot.cannot-build", "&cYou cannot build here!"));
                }
            }
        } else {
            player.sendMessage(plugin.getMessage("town.plot.unclaimed", "&7Unclaimed land"));
            player.sendMessage(plugin.getMessage("town.plot.can-build", "&aYou can build here"));
        }
    }

    private void handleList(Player player, String[] args) {
        List<TownData> allTowns = townManager.getTownList();
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // Використовуємо сторінку 1 за замовчуванням
            }
        }

        int itemsPerPage = 8;
        int totalPages = (int) Math.ceil((double) allTowns.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));

        if (allTowns.isEmpty()) {
            player.sendMessage(plugin.getMessage("town.list.no-towns", "&cNo towns exist!"));
            return;
        }

        player.sendMessage(plugin.getMessage("town.list.header", "&6=== Town List ({0}) ===", String.valueOf(allTowns.size())));

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allTowns.size());

        for (int i = start; i < end; i++) {
            TownData town = allTowns.get(i);
            player.sendMessage(plugin.getMessage("town.list.format", "&e{0}. &f{1} &7({2} res., ${3})",
                    String.valueOf(i + 1),
                    town.getName(),
                    String.valueOf(town.getMemberCount()),
                    String.format("%.0f", town.getBalance())
            ));
        }

        if (totalPages > 1) {
            player.sendMessage(plugin.getMessage("town.list.page", "&7Page &e{0}&7/&e{1}",
                    String.valueOf(page),
                    String.valueOf(totalPages)
            ));
        }
    }

    private void handleTop(Player player, String[] args) {
        String type = "balance";
        if (args.length >= 2) {
            type = args[1].toLowerCase();
        }

        List<TownData> sortedTowns;
        String header;

        if (type.equals("residents")) {
            sortedTowns = townManager.getTownsSortedByResidents();
            header = plugin.getMessage("town.top.header-residents", "&6=== Top Towns by Residents ===");
        } else {
            sortedTowns = townManager.getTownsSortedByBalance();
            header = plugin.getMessage("town.top.header-balance", "&6=== Top Towns by Balance ===");
        }

        player.sendMessage(header);

        int limit = Math.min(10, sortedTowns.size());
        for (int i = 0; i < limit; i++) {
            TownData town = sortedTowns.get(i);
            player.sendMessage(plugin.getMessage("town.top.format", "&e{0}. &f{1} &7- &a${2} &7({3} res.)",
                    String.valueOf(i + 1),
                    town.getName(),
                    String.format("%.0f", town.getBalance()),
                    String.valueOf(town.getMemberCount())
            ));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(TextFormat.GOLD + "=== " + TextFormat.WHITE + "Town Commands " +
                TextFormat.GOLD + "===");
        player.sendMessage(TextFormat.YELLOW + "/town create <name> " + TextFormat.GRAY + "- Create a town");
        player.sendMessage(TextFormat.YELLOW + "/town delete " + TextFormat.GRAY + "- Delete your town");
        player.sendMessage(TextFormat.YELLOW + "/town info [town] " + TextFormat.GRAY + "- Town information");
        player.sendMessage(TextFormat.YELLOW + "/town join <town> " + TextFormat.GRAY + "- Join a town");
        player.sendMessage(TextFormat.YELLOW + "/town leave " + TextFormat.GRAY + "- Leave your town");
        player.sendMessage(TextFormat.YELLOW + "/town invite <player> " + TextFormat.GRAY + "- Invite player");
        player.sendMessage(TextFormat.YELLOW + "/town kick <player> " + TextFormat.GRAY + "- Kick resident");
        player.sendMessage(TextFormat.YELLOW + "/town ban <player> " + TextFormat.GRAY + "- Ban player");
        player.sendMessage(TextFormat.YELLOW + "/town unban <player> " + TextFormat.GRAY + "- Unban player");
        player.sendMessage(TextFormat.YELLOW + "/town claim " + TextFormat.GRAY + "- Claim chunk");
        player.sendMessage(TextFormat.YELLOW + "/town unclaim " + TextFormat.GRAY + "- Unclaim chunk");
        player.sendMessage(TextFormat.YELLOW + "/town deposit <amount> " + TextFormat.GRAY + "- Deposit money");
        player.sendMessage(TextFormat.YELLOW + "/town withdraw <amount> " + TextFormat.GRAY + "- Withdraw money");
        player.sendMessage(TextFormat.YELLOW + "/town balance " + TextFormat.GRAY + "- Check balance");
        player.sendMessage(TextFormat.YELLOW + "/town tax set <0-100> " + TextFormat.GRAY + "- Set tax rate");
        player.sendMessage(TextFormat.YELLOW + "/town sethome " + TextFormat.GRAY + "- Set town home");
        player.sendMessage(TextFormat.YELLOW + "/town home " + TextFormat.GRAY + "- Teleport to home");
        player.sendMessage(TextFormat.YELLOW + "/town assistant <add/remove> <player> " + TextFormat.GRAY + "- Manage assistants");
        player.sendMessage(TextFormat.YELLOW + "/town deputy <add/remove> <player> " + TextFormat.GRAY + "- Manage deputies");
        player.sendMessage(TextFormat.YELLOW + "/town transfer <player> " + TextFormat.GRAY + "- Transfer mayor");
        player.sendMessage(TextFormat.YELLOW + "/town war <town> " + TextFormat.GRAY + "- Declare war");
        player.sendMessage(TextFormat.YELLOW + "/town peace <town> " + TextFormat.GRAY + "- Make peace");
        player.sendMessage(TextFormat.YELLOW + "/town ally <add/remove> <town> " + TextFormat.GRAY + "- Manage alliances");
        player.sendMessage(TextFormat.YELLOW + "/town description <text> " + TextFormat.GRAY + "- Set description");
        player.sendMessage(TextFormat.YELLOW + "/town tag <TAG> " + TextFormat.GRAY + "- Set town tag");
        player.sendMessage(TextFormat.YELLOW + "/town plot <give/take/info> " + TextFormat.GRAY + "- Manage plots");
        player.sendMessage(TextFormat.YELLOW + "/town list [page] " + TextFormat.GRAY + "- List towns");
        player.sendMessage(TextFormat.YELLOW + "/town top [balance/residents] " + TextFormat.GRAY + "- Top towns");
        player.sendMessage(TextFormat.YELLOW + "/town help " + TextFormat.GRAY + "- Show this help");
    }

    private String formatTime(long time) {
        long diff = System.currentTimeMillis() - time;
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);

        if (days > 0) {
            return days + " days, " + hours + " hours ago";
        } else {
            return hours + " hours ago";
        }
    }
}