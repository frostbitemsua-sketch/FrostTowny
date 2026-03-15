package frost.towny;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.entity.EntityInteractEvent;
import cn.nukkit.entity.item.EntityArmorStand;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.block.*;
import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import java.util.HashSet;
import java.util.Set;

public class TownEventListener implements Listener {

    private FrostTowny plugin;
    private TownManager townManager;

    // Множина всіх типів скринь та контейнерів
    private static final Set<Integer> CHEST_TYPES = new HashSet<>();
    private static final Set<Integer> DOOR_TYPES = new HashSet<>();
    private static final Set<Integer> TRAPDOOR_TYPES = new HashSet<>();
    private static final Set<Integer> FENCE_GATE_TYPES = new HashSet<>();
    private static final Set<Integer> INTERACTIVE_BLOCKS = new HashSet<>();

    static {
        // Скрині та контейнери
        CHEST_TYPES.add(Block.CHEST);
        CHEST_TYPES.add(Block.TRAPPED_CHEST);
        CHEST_TYPES.add(Block.ENDER_CHEST);
        CHEST_TYPES.add(Block.BARREL);
        CHEST_TYPES.add(Block.SHULKER_BOX);
        CHEST_TYPES.add(Block.DISPENSER);
        CHEST_TYPES.add(Block.DROPPER);
        CHEST_TYPES.add(Block.HOPPER_BLOCK);
        CHEST_TYPES.add(Block.FURNACE);
        CHEST_TYPES.add(Block.BLAST_FURNACE);
        CHEST_TYPES.add(Block.SMOKER);
        CHEST_TYPES.add(Block.CARTOGRAPHY_TABLE);
        CHEST_TYPES.add(Block.FLETCHING_TABLE);
        CHEST_TYPES.add(Block.SMITHING_TABLE);
        CHEST_TYPES.add(Block.GRINDSTONE);
        CHEST_TYPES.add(Block.STONECUTTER);
        CHEST_TYPES.add(Block.LOOM);
        CHEST_TYPES.add(Block.BEEHIVE);
        CHEST_TYPES.add(Block.BEE_NEST);
        CHEST_TYPES.add(Block.COMPOSTER);
        CHEST_TYPES.add(Block.LECTERN);
        CHEST_TYPES.add(Block.ANVIL);
        CHEST_TYPES.add(Block.ENCHANTING_TABLE);

        // Двері
        DOOR_TYPES.add(Block.DOOR_BLOCK);
        DOOR_TYPES.add(Block.IRON_DOOR_BLOCK);
        DOOR_TYPES.add(Block.SPRUCE_DOOR_BLOCK);
        DOOR_TYPES.add(Block.BIRCH_DOOR_BLOCK);
        DOOR_TYPES.add(Block.JUNGLE_DOOR_BLOCK);
        DOOR_TYPES.add(Block.ACACIA_DOOR_BLOCK);
        DOOR_TYPES.add(Block.DARK_OAK_DOOR_BLOCK);
        DOOR_TYPES.add(Block.CRIMSON_DOOR_BLOCK);
        DOOR_TYPES.add(Block.WARPED_DOOR_BLOCK);
        DOOR_TYPES.add(Block.MANGROVE_DOOR_BLOCK);
        DOOR_TYPES.add(Block.BAMBOO_DOOR_BLOCK);
        DOOR_TYPES.add(Block.CHERRY_DOOR_BLOCK);

        // Люки
        TRAPDOOR_TYPES.add(Block.TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.IRON_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.SPRUCE_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.BIRCH_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.JUNGLE_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.ACACIA_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.DARK_OAK_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.CRIMSON_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.WARPED_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.MANGROVE_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.BAMBOO_TRAPDOOR);
        TRAPDOOR_TYPES.add(Block.CHERRY_TRAPDOOR);

        // Хвіртки
        FENCE_GATE_TYPES.add(Block.FENCE_GATE);

        FENCE_GATE_TYPES.add(Block.CRIMSON_FENCE_GATE);
        FENCE_GATE_TYPES.add(Block.WARPED_FENCE_GATE);
        FENCE_GATE_TYPES.add(Block.MANGROVE_FENCE_GATE);
        FENCE_GATE_TYPES.add(Block.BAMBOO_FENCE_GATE);
        FENCE_GATE_TYPES.add(Block.CHERRY_FENCE_GATE);

        // Інтерактивні блоки
        INTERACTIVE_BLOCKS.add(Block.LEVER);
        INTERACTIVE_BLOCKS.add(Block.STONE_BUTTON);
        INTERACTIVE_BLOCKS.add(Block.WOODEN_BUTTON);
        INTERACTIVE_BLOCKS.add(Block.POLISHED_BLACKSTONE_BUTTON);
        INTERACTIVE_BLOCKS.add(Block.REDSTONE_TORCH);
        INTERACTIVE_BLOCKS.add(Block.UNLIT_REDSTONE_TORCH);
        INTERACTIVE_BLOCKS.add(Block.UNPOWERED_REPEATER);
        INTERACTIVE_BLOCKS.add(Block.POWERED_REPEATER);
        INTERACTIVE_BLOCKS.add(Block.UNPOWERED_COMPARATOR);
        INTERACTIVE_BLOCKS.add(Block.POWERED_COMPARATOR);
        INTERACTIVE_BLOCKS.add(Block.DAYLIGHT_DETECTOR);
        INTERACTIVE_BLOCKS.add(Block.DAYLIGHT_DETECTOR_INVERTED);
        INTERACTIVE_BLOCKS.add(Block.TRIPWIRE_HOOK);
        INTERACTIVE_BLOCKS.add(Block.JUKEBOX);
        INTERACTIVE_BLOCKS.add(Block.CAKE_BLOCK);
        INTERACTIVE_BLOCKS.add(Block.BED_BLOCK);
        INTERACTIVE_BLOCKS.add(Block.BELL);

        // Примітка: Block.ARMOR_STAND не існує в Nukkit
        // Арморстенд завжди є EntityArmorStand (сутність)
    }

    public TownEventListener(FrostTowny plugin) {
        this.plugin = plugin;
        this.townManager = plugin.getTownManager();
    }

    // ============ БЛОКИ ============

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check bypass permission
        if (player.hasPermission("frosttowny.bypass.build")) {
            return;
        }

        String chunkKey = townManager.getChunkKey(block);
        String townName = townManager.getTownAtChunk(
                block.getLevel().getName(),
                block.getChunkX(),
                block.getChunkZ()
        );

        if (townName != null) {
            TownData town = townManager.getTown(townName);
            if (town == null) return;

            // Check if player can build here
            if (!townManager.canBuild(player, chunkKey)) {
                event.setCancelled();
                player.sendMessage(plugin.getMessage("event.block-break.denied", "&cYou cannot break blocks here!", ""));
                return;
            }

            // Check if it's enemy territory
            TownData playerTown = townManager.getPlayerTown(player.getName());
            if (playerTown != null && playerTown.isEnemy(townName)) {
                // Check if block breaking is allowed in enemy territory during war
                boolean allowBreak = plugin.getPluginConfig().getBoolean("war.allow-block-break-in-enemy-territory", false);
                if (!allowBreak) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.block-break.enemy-territory", "&cYou cannot break blocks in enemy territory!", ""));
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Check bypass permission
        if (player.hasPermission("frosttowny.bypass.build")) {
            return;
        }

        String chunkKey = townManager.getChunkKey(block);
        String townName = townManager.getTownAtChunk(
                block.getLevel().getName(),
                block.getChunkX(),
                block.getChunkZ()
        );

        if (townName != null) {
            TownData town = townManager.getTown(townName);
            if (town == null) return;

            // Check if player can build here
            if (!townManager.canBuild(player, chunkKey)) {
                event.setCancelled();
                player.sendMessage(plugin.getMessage("event.block-place.denied", "&cYou cannot place blocks here!", ""));
                return;
            }

            // Check if it's enemy territory
            TownData playerTown = townManager.getPlayerTown(player.getName());
            if (playerTown != null && playerTown.isEnemy(townName)) {
                // Check if block placing is allowed in enemy territory during war
                boolean allowPlace = plugin.getPluginConfig().getBoolean("war.allow-block-place-in-enemy-territory", false);
                if (!allowPlace) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.block-place.enemy-territory", "&cYou cannot place blocks in enemy territory!", ""));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (block == null) {
            return;
        }

        // Check bypass permission
        if (player.hasPermission("frosttowny.bypass.chest") ||
                player.hasPermission("frosttowny.bypass.door") ||
                player.hasPermission("frosttowny.bypass.interact")) {
            return;
        }

        String chunkKey = townManager.getChunkKey(block);
        String townName = townManager.getTownAtChunk(
                block.getLevel().getName(),
                block.getChunkX(),
                block.getChunkZ()
        );

        if (townName != null) {
            TownData town = townManager.getTown(townName);
            if (town == null) return;

            int blockId = block.getId();

            // Перевірка доступу до скринь та контейнерів
            if (CHEST_TYPES.contains(blockId)) {
                if (!canAccessContainers(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.chest-access.denied", "&cYou cannot access containers here!", ""));
                    return;
                }
            }

            // Перевірка доступу до дверей
            if (DOOR_TYPES.contains(blockId)) {
                if (!canUseDoors(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.door-access.denied", "&cYou cannot use doors here!", ""));
                    return;
                }
            }

            // Перевірка доступу до люків
            if (TRAPDOOR_TYPES.contains(blockId)) {
                if (!canUseTrapdoors(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.trapdoor-access.denied", "&cYou cannot use trapdoors here!", ""));
                    return;
                }
            }

            // Перевірка доступу до хвірток
            if (FENCE_GATE_TYPES.contains(blockId)) {
                if (!canUseFenceGates(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.fence-gate-access.denied", "&cYou cannot use fence gates here!", ""));
                    return;
                }
            }

            // Перевірка доступу до інтерактивних блоків
            if (INTERACTIVE_BLOCKS.contains(blockId)) {
                if (!canUseInteractiveBlocks(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.interact.denied", "&cYou cannot interact with this block here!", ""));
                    return;
                }
            }

            // Спеціальні перевірки для конкретних блоків
            switch (blockId) {
                case Block.CRAFTING_TABLE:
                    if (!canUseCraftingTable(player, town, chunkKey)) {
                        event.setCancelled();
                        player.sendMessage(plugin.getMessage("event.crafting-table.denied", "&cYou cannot use crafting table here!", ""));
                    }
                    break;

                case Block.BEACON:
                    if (!canUseBeacon(player, town, chunkKey)) {
                        event.setCancelled();
                        player.sendMessage(plugin.getMessage("event.beacon.denied", "&cYou cannot use beacon here!", ""));
                    }
                    break;

                case Block.FLOWER_POT_BLOCK:
                    if (!canUseFlowerPot(player, town, chunkKey)) {
                        event.setCancelled();
                        player.sendMessage(plugin.getMessage("event.flower-pot.denied", "&cYou cannot use flower pots here!", ""));
                    }
                    break;

                case Block.JUKEBOX:
                    if (!canUseJukebox(player, town, chunkKey)) {
                        event.setCancelled();
                        player.sendMessage(plugin.getMessage("event.jukebox.denied", "&cYou cannot use jukebox here!", ""));
                    }
                    break;


                case Block.ITEM_FRAME_BLOCK:

                    if (!canUseItemFrame(player, town, chunkKey)) {
                        event.setCancelled();
                        player.sendMessage(plugin.getMessage("event.item-frame.denied", "&cYou cannot use item frames here!", ""));
                    }
                    break;
            }
        }
    }

    // ============ СУТНОСТІ ============

    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Check bypass permission
        if (player.hasPermission("frosttowny.bypass.interact")) {
            return;
        }

        // Отримуємо блок, з яким взаємодіють (у Nukkit EntityInteractEvent має поле block)
        Block targetBlock = event.getBlock();
        if (targetBlock == null) {
            return;
        }

        String chunkKey = townManager.getChunkKey(targetBlock);
        String townName = townManager.getTownAtChunk(
                targetBlock.getLevel().getName(),
                targetBlock.getChunkX(),
                targetBlock.getChunkZ()
        );

        if (townName != null) {
            TownData town = townManager.getTown(townName);
            if (town == null) return;

            int blockId = targetBlock.getId();

            if (INTERACTIVE_BLOCKS.contains(blockId)) {
                if (!canUseInteractiveBlocks(player, town, chunkKey)) {
                    event.setCancelled();
                    player.sendMessage(plugin.getMessage("event.interact.denied", "&cYou cannot interact with this block here!", ""));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Захист EntityArmorStand від пошкодження
        if (entity instanceof EntityArmorStand) {
            EntityArmorStand armorStand = (EntityArmorStand) entity;

            String chunkKey = getChunkKey(armorStand);
            String townName = townManager.getTownAtChunk(
                    armorStand.getLevel().getName(),
                    armorStand.getChunkX(),
                    armorStand.getChunkZ()
            );

            if (townName != null) {
                TownData town = townManager.getTown(townName);
                if (town == null) return;

                // Перевірка, чи це пошкодження від гравця
                Player damager = null;
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
                    Entity damagerEntity = damageEvent.getDamager();

                    // Отримуємо справжнього гравця, якщо це снаряд
                    if (damagerEntity instanceof EntityProjectile) {
                        EntityProjectile projectile = (EntityProjectile) damagerEntity;
                        if (projectile.shootingEntity != null && projectile.shootingEntity instanceof Player) {
                            damager = (Player) projectile.shootingEntity;
                        }
                    } else if (damagerEntity instanceof Player) {
                        damager = (Player) damagerEntity;
                    }
                }

                if (damager != null) {
                    // Check bypass permission
                    if (damager.hasPermission("frosttowny.bypass.interact")) {
                        return;
                    }

                    // Перевіряємо, чи може гравець взаємодіяти з арморстендом
                    if (!canUseArmorStand(damager, town, chunkKey)) {
                        event.setCancelled();
                        damager.sendMessage(plugin.getMessage("event.armor-stand.denied", "&cYou cannot interact with armor stand here!", ""));
                        return;
                    }
                } else {
                    // Заборона будь-якого пошкодження арморстенда (від монстрів, падіння тощо)
                    event.setCancelled();
                }
            }
        }

        // Handle PvP
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            Entity damager = damageEvent.getDamager();

            // Get actual player if damager is a projectile
            if (damager instanceof EntityProjectile) {
                EntityProjectile projectile = (EntityProjectile) damager;
                if (projectile.shootingEntity != null) {
                    damager = projectile.shootingEntity;
                }
            }

            if (entity instanceof Player && damager instanceof Player) {
                Player victim = (Player) entity;
                Player attacker = (Player) damager;

                // Check bypass permission
                if (attacker.hasPermission("frosttowny.bypass.pvp")) {
                    return;
                }

                handlePvP(victim, attacker, event);
            }
        }

        // Handle mob spawning prevention
        if (entity instanceof EntityMob) {
            String townName = townManager.getTownAtChunk(
                    entity.getLevel().getName(),
                    entity.getChunkX(),
                    entity.getChunkZ()
            );

            if (townName != null) {
                // Check if mob spawning is disabled in this town
                boolean mobSpawning = plugin.getPluginConfig().getBoolean("flags.mob-spawning", true);
                if (!mobSpawning) {
                    event.setCancelled();
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Prevent explosions in towns with explosion protection
        boolean explosionsAllowed = plugin.getPluginConfig().getBoolean("flags.explosions", false);
        if (!explosionsAllowed) {
            for (Block block : event.getBlockList()) {
                String townName = townManager.getTownAtChunk(
                        block.getLevel().getName(),
                        block.getChunkX(),
                        block.getChunkZ()
                );

                if (townName != null) {
                    event.setCancelled();
                    plugin.getLogger().info("Explosion prevented in town: " + townName);
                    break;
                }
            }
        }
    }

    // ============ РУХ ГРАВЦЯ ============

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Get chunk coordinates
        int fromChunkX = (int) Math.floor(event.getFrom().x) >> 4;
        int fromChunkZ = (int) Math.floor(event.getFrom().z) >> 4;
        int toChunkX = (int) Math.floor(event.getTo().x) >> 4;
        int toChunkZ = (int) Math.floor(event.getTo().z) >> 4;

        // Check if player moved to a different chunk
        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            String fromTown = townManager.getTownAtChunk(player.getLevel().getName(), fromChunkX, fromChunkZ);
            String toTown = townManager.getTownAtChunk(player.getLevel().getName(), toChunkX, toChunkZ);

            // Entering a new town
            if (toTown != null && !toTown.equals(fromTown)) {
                TownData town = townManager.getTown(toTown);
                if (town != null) {
                    // Get town tag for display
                    String townTag = town.getTag();

                    // Show welcome message based on visual settings
                    boolean showTitle = plugin.getPluginConfig().getBoolean("visual.show-entry-title", true);
                    boolean showChat = true;

                    if (showTitle) {
                        String title = plugin.getMessage("event.enter-town.title", "&6{0}", town.getName());
                        String subtitle = plugin.getMessage("event.enter-town.subtitle", "&eTag: &f[{0}]", townTag);
                        player.sendTitle(title, subtitle, 10, 40, 10);
                    }

                    if (showChat) {
                        String message = plugin.getMessage("event.enter-town.chat",
                                "&eEntering &6{0} &7[{1}] &eterritory",
                                town.getName(), townTag);
                        player.sendMessage(message);
                    }

                    // Check if it's enemy territory
                    TownData playerTown = townManager.getPlayerTown(player.getName());
                    if (playerTown != null && playerTown.isEnemy(toTown)) {
                        String warning = plugin.getMessage("event.enter-town.enemy",
                                "&cWarning: &4Enemy territory!", "");
                        player.sendMessage(warning);
                    }
                }
            }

            // Leaving a town
            if (fromTown != null && !fromTown.equals(toTown)) {
                TownData town = townManager.getTown(fromTown);
                if (town != null) {
                    String message = plugin.getMessage("event.leave-town.chat",
                            "&7Leaving &8{0} &7territory",
                            town.getName());
                    player.sendMessage(message);
                }
            }
        }
    }

    // ============ ДОПОМІЖНІ МЕТОДИ ============

    private String getChunkKey(Entity entity) {
        return entity.getLevel().getName() + ":" + entity.getChunkX() + ":" + entity.getChunkZ();
    }

    private boolean canBuild(Player player, TownData town, String chunkKey) {
        // Check if player is in this town
        if (town.isResident(player.getName())) {
            // Check for private plot
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                // Public town land - residents can build
                return true;
            } else {
                // Private plot - only owner can build
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Check if player is banned from this town
        if (town.isBanned(player.getName())) {
            return false;
        }

        // Non-residents cannot build
        return false;
    }

    private boolean canAccessContainers(Player player, TownData town, String chunkKey) {
        // Check if player is banned
        if (town.isBanned(player.getName())) {
            return false;
        }

        // Check if player is in this town
        if (town.isResident(player.getName())) {
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                // Check town flag for container access
                boolean chestAccess = plugin.getPluginConfig().getBoolean("flags.chest-access", false);
                return chestAccess || town.isResident(player.getName());
            } else {
                // In private plots, only owner can access containers
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Non-residents cannot access containers by default
        boolean chestAccess = plugin.getPluginConfig().getBoolean("flags.chest-access", false);
        return chestAccess;
    }

    private boolean canUseDoors(Player player, TownData town, String chunkKey) {
        // Check if player is banned
        if (town.isBanned(player.getName())) {
            return false;
        }

        if (town.isResident(player.getName())) {
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                // Check town flag for door access
                boolean doorAccess = plugin.getPluginConfig().getBoolean("flags.door-access", false);
                return doorAccess || town.isResident(player.getName());
            } else {
                // In private plots, only owner can use doors
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Non-residents cannot use doors by default
        boolean doorAccess = plugin.getPluginConfig().getBoolean("flags.door-access", false);
        return doorAccess;
    }

    private boolean canUseTrapdoors(Player player, TownData town, String chunkKey) {
        return canUseDoors(player, town, chunkKey);
    }

    private boolean canUseFenceGates(Player player, TownData town, String chunkKey) {
        return canUseDoors(player, town, chunkKey);
    }

    private boolean canUseInteractiveBlocks(Player player, TownData town, String chunkKey) {
        // Check if player is banned
        if (town.isBanned(player.getName())) {
            return false;
        }

        if (town.isResident(player.getName())) {
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                // Residents can use interactive blocks in public areas
                return true;
            } else {
                // In private plots, only owner can use interactive blocks
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Non-residents cannot use interactive blocks by default
        return false;
    }

    private boolean canUseCraftingTable(Player player, TownData town, String chunkKey) {
        return canUseInteractiveBlocks(player, town, chunkKey);
    }

    private boolean canUseBeacon(Player player, TownData town, String chunkKey) {
        return canUseInteractiveBlocks(player, town, chunkKey);
    }

    private boolean canUseFlowerPot(Player player, TownData town, String chunkKey) {
        return canUseInteractiveBlocks(player, town, chunkKey);
    }

    private boolean canUseJukebox(Player player, TownData town, String chunkKey) {
        return canUseInteractiveBlocks(player, town, chunkKey);
    }

    private boolean canUseNoteBlock(Player player, TownData town, String chunkKey) {
        return canUseInteractiveBlocks(player, town, chunkKey);
    }

    private boolean canUseItemFrame(Player player, TownData town, String chunkKey) {
        // Check if player is banned
        if (town.isBanned(player.getName())) {
            return false;
        }

        // Check town flag for item frame interaction
        boolean itemFrameAccess = plugin.getPluginConfig().getBoolean("flags.item-frames", false);

        if (town.isResident(player.getName())) {
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                return itemFrameAccess || town.isResident(player.getName());
            } else {
                // In private plots, only owner can use item frames
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Non-residents cannot use item frames by default
        return itemFrameAccess;
    }

    private boolean canUseArmorStand(Player player, TownData town, String chunkKey) {
        // Check if player is banned
        if (town.isBanned(player.getName())) {
            return false;
        }

        // Check town flag for armor stand interaction
        boolean armorStandAccess = plugin.getPluginConfig().getBoolean("flags.armor-stands", false);

        if (town.isResident(player.getName())) {
            String plotOwner = townManager.getPlotOwner(chunkKey);

            if (plotOwner == null) {
                return armorStandAccess || town.isResident(player.getName());
            } else {
                // In private plots, only owner can use armor stands
                return plotOwner.equalsIgnoreCase(player.getName());
            }
        }

        // Non-residents cannot use armor stands by default
        return armorStandAccess;
    }

    private void handlePvP(Player victim, Player attacker, EntityDamageEvent event) {
        String victimTownName = townManager.getPlayerTownName(victim.getName());
        String attackerTownName = townManager.getPlayerTownName(attacker.getName());

        // Check global PvP flag
        boolean globalPvP = plugin.getPluginConfig().getBoolean("flags.pvp", false);
        if (!globalPvP) {
            event.setCancelled();
            attacker.sendMessage(plugin.getMessage("event.pvp.denied", "&cPvP is disabled globally!", ""));
            return;
        }

        // Both players are in towns
        if (victimTownName != null && attackerTownName != null) {
            TownData victimTown = townManager.getTown(victimTownName);
            TownData attackerTown = townManager.getTown(attackerTownName);

            // Check if towns are at war
            if (victimTown != null && attackerTown != null) {
                if (victimTown.isEnemy(attackerTownName) || attackerTown.isEnemy(victimTownName)) {
                    // War - allow PvP
                    boolean allowPvPInEnemyTerritory = plugin.getPluginConfig().getBoolean("war.allow-pvp-in-enemy-territory", true);
                    if (allowPvPInEnemyTerritory) {
                        String message = plugin.getMessage("event.pvp.enemy-pvp",
                                "&cYou can attack enemies in their territory!", "");
                        attacker.sendMessage(message);
                        return;
                    }
                }

                // Check if towns are allies
                if (victimTown.isAlly(attackerTownName) || attackerTown.isAlly(victimTownName)) {
                    event.setCancelled();
                    attacker.sendMessage(plugin.getMessage("event.pvp.ally",
                            "&cYou cannot attack allies!", ""));
                    return;
                }
            }
        }

        // Check PvP flags in the victim's chunk
        String chunkTown = townManager.getTownAtChunk(
                victim.getLevel().getName(),
                victim.getChunkX(),
                victim.getChunkZ()
        );

        if (chunkTown != null) {
            TownData town = townManager.getTown(chunkTown);
            if (town != null) {
                // Check town-specific PvP flag
                boolean townPvP = plugin.getPluginConfig().getBoolean("flags.pvp", false);
                if (!townPvP) {
                    event.setCancelled();
                    attacker.sendMessage(plugin.getMessage("event.pvp.denied", "&cPvP is disabled in this town!", ""));
                    return;
                }

                // Check if attacker is resident
                if (!town.isResident(attacker.getName())) {
                    event.setCancelled();
                    attacker.sendMessage(plugin.getMessage("event.pvp.not-resident",
                            "&cOnly residents can PvP in this town!", ""));
                    return;
                }
            }
        }

        // Check if players are in the same town
        if (victimTownName != null && victimTownName.equals(attackerTownName)) {
            boolean sameTownPvP = plugin.getPluginConfig().getBoolean("flags.pvp-same-town", false);
            if (!sameTownPvP) {
                event.setCancelled();
                attacker.sendMessage(plugin.getMessage("event.pvp.same-town",
                        "&cYou cannot attack members of your own town!", ""));
            }
        }
    }
}