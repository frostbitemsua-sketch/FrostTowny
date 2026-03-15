package frost.towny;

import cn.nukkit.level.Position;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import java.util.*;

public class TownData {

    private String name;
    private String mayor;
    private Position home;
    private long creationTime;
    private double balance;
    private double taxRate;
    private String description;
    private String tag;

    private Set<String> residents = new HashSet<>();
    private Set<String> assistants = new HashSet<>();
    private Set<String> deputies = new HashSet<>();
    private Set<String> chunks = new HashSet<>();
    private Set<String> enemies = new HashSet<>();
    private Set<String> allies = new HashSet<>();
    private Map<String, String> invites = new HashMap<>(); // playerName -> inviter
    private Set<String> joinRequests = new HashSet<>();
    private Set<String> bannedPlayers = new HashSet<>();

    // Limits
    public static final int MAX_ASSISTANTS = 5;
    public static final int MAX_DEPUTIES = 3;
    public static final int MAX_CHUNKS = 50;
    public static final int MAX_ENEMIES = 10;
    public static final int MAX_ALLIES = 5;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public String getMayor() { return mayor; }
    public void setMayor(String mayor) {
        if (mayor != null) {
            this.mayor = mayor.toLowerCase();
        }
    }

    public Position getHome() { return home; }
    public void setHome(Position home) {
        if (home != null && home.getLevel() != null) {
            this.home = home;
        }
    }

    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) {
        if (creationTime > 0) {
            this.creationTime = creationTime;
        }
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) {
        if (balance >= 0) {
            this.balance = balance;
        }
    }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) {
        if (taxRate >= 0 && taxRate <= 100) {
            this.taxRate = taxRate;
        }
    }

    public String getDescription() {
        return description != null ? description : "";
    }
    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public String getTag() {
        return tag != null ? tag : name != null && name.length() > 3 ? name.substring(0, 3).toUpperCase() : "TWN";
    }
    public void setTag(String tag) {
        if (tag != null && tag.matches("[A-Z]{3,5}")) {
            this.tag = tag.toUpperCase();
        }
    }

    // Safe getters returning copies
    public Set<String> getResidents() { return new HashSet<>(residents); }
    public Set<String> getAssistants() { return new HashSet<>(assistants); }
    public Set<String> getDeputies() { return new HashSet<>(deputies); }
    public Set<String> getChunks() { return new HashSet<>(chunks); }
    public Set<String> getEnemies() { return new HashSet<>(enemies); }
    public Set<String> getAllies() { return new HashSet<>(allies); }
    public Map<String, String> getInvites() { return new HashMap<>(invites); }
    public Set<String> getJoinRequests() { return new HashSet<>(joinRequests); }
    public Set<String> getBannedPlayers() { return new HashSet<>(bannedPlayers); }

    // Management methods
    public boolean addResident(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return false;

        String nameLower = playerName.toLowerCase().trim();
        if (residents.contains(nameLower)) return false;

        // Remove from any pending lists
        invites.remove(nameLower);
        joinRequests.remove(nameLower);
        bannedPlayers.remove(nameLower);

        return residents.add(nameLower);
    }

    public boolean removeResident(String playerName) {
        if (playerName == null) return false;

        String nameLower = playerName.toLowerCase();
        boolean removed = residents.remove(nameLower);

        // Clean up all roles
        assistants.remove(nameLower);
        deputies.remove(nameLower);
        invites.remove(nameLower);
        joinRequests.remove(nameLower);

        // If mayor is removed, assign new mayor
        if (mayor.equals(nameLower) && !residents.isEmpty()) {
            // Try to promote deputy first, then assistant, then any resident
            if (!deputies.isEmpty()) {
                mayor = deputies.iterator().next();
                deputies.remove(mayor);
            } else if (!assistants.isEmpty()) {
                mayor = assistants.iterator().next();
                assistants.remove(mayor);
            } else {
                mayor = residents.iterator().next();
            }
        }

        return removed;
    }

    public boolean isResident(String playerName) {
        return playerName != null && residents.contains(playerName.toLowerCase());
    }

    public String getRank(String playerName) {
        if (playerName == null) return "NONE";

        String nameLower = playerName.toLowerCase();
        if (mayor.equals(nameLower)) return "MAYOR";
        if (deputies.contains(nameLower)) return "DEPUTY";
        if (assistants.contains(nameLower)) return "ASSISTANT";
        if (residents.contains(nameLower)) return "RESIDENT";
        return "NONE";
    }

    public boolean canManage(String playerName) {
        if (playerName == null) return false;

        String nameLower = playerName.toLowerCase();
        return mayor.equals(nameLower) ||
                deputies.contains(nameLower) ||
                assistants.contains(nameLower);
    }

    public boolean canWithdraw(String playerName) {
        if (playerName == null) return false;

        String nameLower = playerName.toLowerCase();
        return mayor.equals(nameLower) ||
                deputies.contains(nameLower);
    }

    public boolean canInvite(String playerName) {
        if (playerName == null) return false;

        String nameLower = playerName.toLowerCase();
        return mayor.equals(nameLower) ||
                deputies.contains(nameLower) ||
                assistants.contains(nameLower);
    }

    public boolean addAssistant(String playerName) {
        if (playerName == null || !isResident(playerName)) return false;
        if (assistants.size() >= MAX_ASSISTANTS) return false;

        String nameLower = playerName.toLowerCase();
        // Can't be mayor or deputy
        if (mayor.equals(nameLower) || deputies.contains(nameLower)) return false;

        return assistants.add(nameLower);
    }

    public boolean removeAssistant(String playerName) {
        return playerName != null && assistants.remove(playerName.toLowerCase());
    }

    public boolean addDeputy(String playerName) {
        if (playerName == null || !isResident(playerName)) return false;
        if (deputies.size() >= MAX_DEPUTIES) return false;

        String nameLower = playerName.toLowerCase();
        // Can't be mayor
        if (mayor.equals(nameLower)) return false;

        // Remove from assistants if they were one
        assistants.remove(nameLower);

        return deputies.add(nameLower);
    }

    public boolean removeDeputy(String playerName) {
        return playerName != null && deputies.remove(playerName.toLowerCase());
    }

    public boolean addChunk(String chunkKey) {
        if (chunkKey == null || chunks.size() >= MAX_CHUNKS) return false;
        return chunks.add(chunkKey);
    }

    public boolean removeChunk(String chunkKey) {
        return chunkKey != null && chunks.remove(chunkKey);
    }

    public boolean hasChunk(String chunkKey) {
        return chunkKey != null && chunks.contains(chunkKey);
    }

    public boolean addEnemy(String townName) {
        if (townName == null || enemies.size() >= MAX_ENEMIES) return false;

        String townLower = townName.toLowerCase();
        // Can't be enemy with self
        if (townLower.equals(name.toLowerCase())) return false;

        // Remove from allies if they were one
        allies.remove(townLower);

        return enemies.add(townLower);
    }

    public boolean removeEnemy(String townName) {
        return townName != null && enemies.remove(townName.toLowerCase());
    }

    public boolean isEnemy(String townName) {
        return townName != null && enemies.contains(townName.toLowerCase());
    }

    public boolean addAlly(String townName) {
        if (townName == null || allies.size() >= MAX_ALLIES) return false;

        String townLower = townName.toLowerCase();
        // Can't be ally with self
        if (townLower.equals(name.toLowerCase())) return false;

        // Remove from enemies if they were one
        enemies.remove(townLower);

        return allies.add(townLower);
    }

    public boolean removeAlly(String townName) {
        return townName != null && allies.remove(townName.toLowerCase());
    }

    public boolean isAlly(String townName) {
        return townName != null && allies.contains(townName.toLowerCase());
    }

    public void addInvite(String playerName, String inviter) {
        if (playerName != null && inviter != null) {
            String nameLower = playerName.toLowerCase();
            invites.put(nameLower, inviter.toLowerCase());
            joinRequests.remove(nameLower);
        }
    }

    public void removeInvite(String playerName) {
        if (playerName != null) {
            invites.remove(playerName.toLowerCase());
        }
    }

    public boolean isInvited(String playerName) {
        return playerName != null && invites.containsKey(playerName.toLowerCase());
    }

    public String getInviter(String playerName) {
        return playerName != null ? invites.get(playerName.toLowerCase()) : null;
    }

    public void addJoinRequest(String playerName) {
        if (playerName != null) {
            joinRequests.add(playerName.toLowerCase());
        }
    }

    public void removeJoinRequest(String playerName) {
        if (playerName != null) {
            joinRequests.remove(playerName.toLowerCase());
        }
    }

    public boolean hasJoinRequest(String playerName) {
        return playerName != null && joinRequests.contains(playerName.toLowerCase());
    }

    public void addBan(String playerName) {
        if (playerName != null) {
            String nameLower = playerName.toLowerCase();
            bannedPlayers.add(nameLower);

            // Remove from all other lists
            residents.remove(nameLower);
            assistants.remove(nameLower);
            deputies.remove(nameLower);
            invites.remove(nameLower);
            joinRequests.remove(nameLower);
        }
    }

    public void removeBan(String playerName) {
        if (playerName != null) {
            bannedPlayers.remove(playerName.toLowerCase());
        }
    }

    public boolean isBanned(String playerName) {
        return playerName != null && bannedPlayers.contains(playerName.toLowerCase());
    }

    public boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        return true;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        return true;
    }

    public Set<String> getAllMembers() {
        return new HashSet<>(residents);
    }

    public int getMemberCount() {
        return residents.size();
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public double getTaxAmount(double income) {
        return income * (taxRate / 100.0);
    }

    // Serialization
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("mayor", mayor);

        if (home != null && home.getLevel() != null) {
            map.put("home-world", home.getLevel().getName());
            map.put("home-x", home.getX());
            map.put("home-y", home.getY());
            map.put("home-z", home.getZ());
        }

        map.put("creation-time", creationTime);
        map.put("balance", balance);
        map.put("tax-rate", taxRate);
        map.put("description", description != null ? description : "");
        map.put("tag", tag != null ? tag : getTag());

        map.put("residents", new ArrayList<>(residents));
        map.put("assistants", new ArrayList<>(assistants));
        map.put("deputies", new ArrayList<>(deputies));
        map.put("chunks", new ArrayList<>(chunks));
        map.put("enemies", new ArrayList<>(enemies));
        map.put("allies", new ArrayList<>(allies));
        map.put("invites", new HashMap<>(invites));
        map.put("join-requests", new ArrayList<>(joinRequests));
        map.put("banned-players", new ArrayList<>(bannedPlayers));

        return map;
    }

    public static TownData fromMap(Map<String, Object> map) {
        TownData town = new TownData();

        town.name = (String) map.get("name");
        town.mayor = ((String) map.get("mayor")).toLowerCase();

        // Load position
        String worldName = (String) map.get("home-world");
        if (worldName != null) {
            Level level = Server.getInstance().getLevelByName(worldName);
            if (level != null) {
                double x = ((Number) map.get("home-x")).doubleValue();
                double y = ((Number) map.get("home-y")).doubleValue();
                double z = ((Number) map.get("home-z")).doubleValue();
                town.home = new Position(x, y, z, level);
            }
        }

        town.creationTime = ((Number) map.getOrDefault("creation-time", System.currentTimeMillis())).longValue();
        town.balance = ((Number) map.getOrDefault("balance", 0.0)).doubleValue();
        town.taxRate = ((Number) map.getOrDefault("tax-rate", 5.0)).doubleValue();
        town.description = (String) map.getOrDefault("description", "");
        town.tag = (String) map.getOrDefault("tag", town.getTag());

        // Load collections with null safety
        town.residents = new HashSet<>((List<String>) map.getOrDefault("residents", new ArrayList<>()));
        town.assistants = new HashSet<>((List<String>) map.getOrDefault("assistants", new ArrayList<>()));
        town.deputies = new HashSet<>((List<String>) map.getOrDefault("deputies", new ArrayList<>()));
        town.chunks = new HashSet<>((List<String>) map.getOrDefault("chunks", new ArrayList<>()));
        town.enemies = new HashSet<>((List<String>) map.getOrDefault("enemies", new ArrayList<>()));
        town.allies = new HashSet<>((List<String>) map.getOrDefault("allies", new ArrayList<>()));

        // Convert old invite format if needed
        Object invitesObj = map.get("invites");
        if (invitesObj instanceof Map) {
            Map<?, ?> invitesMap = (Map<?, ?>) invitesObj;
            for (Map.Entry<?, ?> entry : invitesMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    town.invites.put(
                            ((String) entry.getKey()).toLowerCase(),
                            ((String) entry.getValue()).toLowerCase()
                    );
                }
            }
        }

        town.joinRequests = new HashSet<>((List<String>) map.getOrDefault("join-requests", new ArrayList<>()));
        town.bannedPlayers = new HashSet<>((List<String>) map.getOrDefault("banned-players", new ArrayList<>()));

        return town;
    }
}