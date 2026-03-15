package frost.towny;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FrostTowny extends PluginBase {

    private static FrostTowny instance;
    private EconomyAPI economyAPI;
    private TownManager townManager;
    private Config config;
    private Config messages;
    private Map<String, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // Load EconomyAPI
        if (getServer().getPluginManager().getPlugin("EconomyAPI") == null) {
            getLogger().error("EconomyAPI not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economyAPI = EconomyAPI.getInstance();

        // Create data folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load configs
        saveResource("config.yml", false);
        saveResource("messages.yml", false);

        config = new Config(new File(getDataFolder(), "config.yml"), Config.YAML);
        messages = new Config(new File(getDataFolder(), "messages.yml"), Config.YAML);

        // Initialize manager
        townManager = new TownManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(new TownEventListener(this), this);

        // Start scheduled tasks
        startScheduledTasks();

        getLogger().info(TextFormat.GREEN + "FrostTowny v1.0.0 enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Просто передаємо виконання команди класу TownCommand
        return new TownCommand(this).execute(sender, label, args);
    }

    @Override
    public void onDisable() {
        townManager.saveAllData();
        getLogger().info(TextFormat.RED + "FrostTowny disabled!");
    }

    private void startScheduledTasks() {
        // Hourly tasks
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            townManager.processTaxes();
            townManager.processWars();
        }, 20 * 60 * 60);

        // Daily tasks (every 24 hours)
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            townManager.processSalaries();
            townManager.cleanupInactiveTowns();
        }, 20 * 60 * 60 * 24);

        // Save data every 5 minutes
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            townManager.saveAllData();
        }, 20 * 60 * 5);
    }

    // Getters
    public static FrostTowny getInstance() {
        return instance;
    }

    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }

    public TownManager getTownManager() {
        return townManager;
    }

    public Config getPluginConfig() {
        return config;
    }

    public Config getMessages() {
        return messages;
    }

    // Cooldown management
    public void setCooldown(String key, long duration) {
        cooldowns.put(key, System.currentTimeMillis() + duration);
    }

    public boolean hasCooldown(String key) {
        if (!cooldowns.containsKey(key)) return false;
        return System.currentTimeMillis() < cooldowns.get(key);
    }

    public long getRemainingCooldown(String key) {
        if (!cooldowns.containsKey(key)) return 0;
        long remaining = cooldowns.get(key) - System.currentTimeMillis();
        return Math.max(0, remaining) / 1000;
    }

    // Utility methods
    public String getMessage(String key, String defaultValue, Object... args) {
        String message = messages.getString(key, defaultValue);
        message = TextFormat.colorize(message);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return message;
    }

    public String getMessage(String key, String defaultValue, String... args) {
        String message = messages.getString(key, defaultValue);
        message = TextFormat.colorize(message);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }

        return message;
    }

    public void broadcastToTown(TownData town, String messageKey, Object... args) {
        String message = getMessage(messageKey, messageKey, args);
        for (String resident : town.getAllMembers()) {
            Player residentPlayer = getServer().getPlayerExact(resident);
            if (residentPlayer != null) {
                residentPlayer.sendMessage(message);
            }
        }
    }
}