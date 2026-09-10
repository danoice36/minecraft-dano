package org.example.dnnItem;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.dnnItem.AutoResoucce.betterResouceGenerator;
import org.example.dnnItem.AutoResoucce.resoucePackGen;
import org.example.dnnItem.Resource.ResourcePackGenerator;

import java.io.File;
import java.util.*;



public final class DnnItem extends JavaPlugin implements Listener {

    private ItemEventManager abilityManager;

    private BlockPlacementChecker placementChecker;
    private ResourcePackGenerator resourcePackGenerator;

    private resoucePackGen enerateResource;

    private ability abili;


    private betterResouceGenerator betterGen;

    private static DnnItem instance;
    private File itemsFile;
    private FileConfiguration itemsConfig;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private final NamespacedKey ITEM_ID_KEY = new NamespacedKey(this, "custom_item_id");
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {

        instance = this;
        abilityManager = new ItemEventManager();
        placementChecker = new BlockPlacementChecker();
        abili = new ability();
        saveDefaultResource("items.yml");   // Fixed line
        loadItemsConfig();
        loadCustomItems();
        loadArmorRendering();

        enerateResource = new resoucePackGen();
        betterGen = new betterResouceGenerator();

        //resourcePackGenerator = new ResourcePackGenerator(this);

        DnnItem.getInstance().getAbility().HoldEnitity();
        getServer().getPluginManager().registerEvents(new BlockPlacementChecker(), this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("§aCustomItemPlugin loaded successfully!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public betterResouceGenerator getBetterGen(){

        return betterGen;
    }

    public static DnnItem getInstance() {
        return instance;
    }

    public ItemEventManager getAbilityManager() {
        return abilityManager;
    }

    public BlockPlacementChecker getChecker() {
        return placementChecker;
    }

    public ability getAbility() {return abili;}

    public ResourcePackGenerator getResourcePackGenerator() {
        return resourcePackGenerator;
    }

    public resoucePackGen getResoucePack() {
        return enerateResource;
    }

    public enum ActionType {

        ON_MINE("on-mine"),
        ON_INTERACT("on-interact"),
        ON_HIT("on-hit"),
        ON_RIGHT_CLICK("on-right-click"),
        ON_RIGHT_AIR("on-right-air"),
        ON_LEFT_CLICK("on-left-click"),
        ON_PLAYER_SNEAK("on-sneak");

        private final String configName;

        ActionType(String configName) {
            this.configName = configName;
        }

        public String getConfigName() {
            return configName;
        }
    }

    private void saveDefaultResource(String resource) {
        File file = new File(getDataFolder(), resource);
        if (!file.exists()) {
            saveResource(resource, false);
        }
    }

    private void loadItemsConfig() {
        itemsFile = new File(getDataFolder(), "items.yml");
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }


    private void loadArmorRendering() {
        ConfigurationSection armorSection = itemsConfig.getConfigurationSection("armors_rendering");
        if (armorSection == null) return;

        getLogger().info("Loading armor rendering configurations...");

        for (String armorId : armorSection.getKeys(false)) {
            ConfigurationSection sec = armorSection.getConfigurationSection(armorId);
            if (sec == null) continue;

            String color = sec.getString("color", "#ffffff");
            String layer1 = sec.getString("layer_1", "");
            String layer2 = sec.getString("layer_2", "");
            boolean useColor = sec.getBoolean("use_color", false);

            getLogger().info("Loaded armor rendering: " + armorId + " | Layer1: " + layer1);

            // Yahan future mein koi logic add kar sakte ho (jaise packet sending etc.)
            // Abhi ke liye sirf load ho raha hai
        }
    }

    private void loadCustomItems() {

        customItems.clear();
        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            getLogger().warning("No 'items' section found in items.yml!");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection itemSec = itemsSection.getConfigurationSection(id);
            if (itemSec == null) continue;

            try {
                Material mat = Material.valueOf(itemSec.getString("material", "STONE").toUpperCase());
                ItemStack item = new ItemStack(mat, itemSec.getInt("amount", 1));

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {

                    // Display Name
                    if (itemSec.contains("name")) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemSec.getString("name")));
                    }

                    String texture = itemSec.getString("texture");
                    if (texture != null && !texture.isEmpty()) {
                        // Custom Model Data already set hai upar
                        // Texture path ko note kar sakte ho future packet modification ke liye
                        getLogger().info("Loaded custom texture for " + id + ": " + texture);
                    }

                    // Leather Color (ItemAdder style)
                    if (meta instanceof LeatherArmorMeta leatherMeta) {
                        String colorStr = itemSec.getString("color");
                        if (colorStr != null && !colorStr.isEmpty()) {
                            try {
                                java.awt.Color awt = java.awt.Color.decode(colorStr);
                                leatherMeta.setColor(org.bukkit.Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue()));
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    // Lore
                    if (itemSec.contains("lore")) {
                        List<String> lore = itemSec.getStringList("lore");
                        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s));
                        meta.setLore(lore);
                    }

                    // Custom Model Data
                    if (itemSec.contains("custom-model-data")) {
                        meta.setCustomModelData(itemSec.getInt("custom-model-data"));
                    }

                    // ==================== ATTRIBUTES (Fixed for 1.21+) ====================
                    if (itemSec.contains("attributes")) {
                        ConfigurationSection attrSec = itemSec.getConfigurationSection("attributes");
                        if (attrSec != null) {
                            for (String attrName : attrSec.getKeys(false)) {
                                ConfigurationSection attr = attrSec.getConfigurationSection(attrName);
                                if (attr == null) continue;

                                try {
                                    // New Recommended Way
                                    Attribute attribute = switch (attrName.toUpperCase()) {
                                        case "ATTACK_DAMAGE" -> Attribute.ATTACK_DAMAGE;
                                        case "ATTACK_SPEED" -> Attribute.ATTACK_SPEED;
                                        case "MAX_HEALTH" -> Attribute.MAX_HEALTH;
                                        case "MOVEMENT_SPEED" -> Attribute.MOVEMENT_SPEED;
                                        case "ARMOR" -> Attribute.ARMOR;
                                        case "ARMOR_TOUGHNESS" -> Attribute.ARMOR_TOUGHNESS;
                                        case "KNOCKBACK_RESISTANCE" -> Attribute.KNOCKBACK_RESISTANCE;
                                        case "LUCK" -> Attribute.LUCK;
                                        case "BLOCK_BREAK_SPEED" -> Attribute.BLOCK_BREAK_SPEED;
                                        default -> null;
                                    };

                                    if (attribute == null) {
                                        getLogger().warning("Unknown attribute: " + attrName + " in item " + id);
                                        continue;
                                    }

                                    double amount = attr.getDouble("amount", 0.0);
                                    String opStr = attr.getString("operation", "ADD_NUMBER").toUpperCase();
                                    String slotStr = attr.getString("slot", "MAINHAND").toUpperCase();

                                    AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(opStr);
                                    EquipmentSlot slot = EquipmentSlot.valueOf(slotStr);


                                    EquipmentSlotGroup group = switch (slot) {
                                        case HEAD -> EquipmentSlotGroup.HEAD;
                                        case CHEST -> EquipmentSlotGroup.CHEST;
                                        case LEGS -> EquipmentSlotGroup.LEGS;
                                        case FEET -> EquipmentSlotGroup.FEET;
                                        case HAND -> EquipmentSlotGroup.HAND;
                                        case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
                                        default -> EquipmentSlotGroup.ANY;
                                    };


                                    NamespacedKey modifierKey = new NamespacedKey(this, "custom_" + id + "_" + attrName.toLowerCase());

                                    AttributeModifier modifier = new AttributeModifier(
                                            modifierKey,
                                            amount,
                                            operation,
                                            group
                                    );

                                    meta.addAttributeModifier(attribute, modifier);
                                    getLogger().info("Added attribute: " + attrName + " to " + id);

                                } catch (Exception ex) {
                                    getLogger().warning("Failed to add attribute '" + attrName + "' in item " + id + " → " + ex.getMessage());
                                }
                            }
                        }
                    }

                    // Enchantments
                    if (itemSec.contains("enchantments")) {
                        ConfigurationSection enchSec = itemSec.getConfigurationSection("enchantments");
                        for (String ench : enchSec.getKeys(false)) {
                            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(ench.toLowerCase()));
                            if (enchantment != null) {
                                meta.addEnchant(enchantment, enchSec.getInt(ench), true);
                            }
                        }
                    }

                    // Unbreakable
                    if (itemSec.getBoolean("unbreakable", false)) {
                        meta.setUnbreakable(true);
                    }

                    // Item Flags
                    if (itemSec.contains("flags")) {
                        for (String flag : itemSec.getStringList("flags")) {
                            try {
                                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    // Persistent Data (Item ID)
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, id);

                    item.setItemMeta(meta);
                }

                customItems.put(id.toLowerCase(), item);
                getLogger().info("Loaded custom item: " + id);

            } catch (Exception e) {
                getLogger().severe("Failed to load item: " + id);
                e.printStackTrace();
            }
        }
    }

    public boolean checkCooldown(Player p, String itemId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;

        UUID uuid = p.getUniqueId();
        cooldowns.putIfAbsent(uuid, new HashMap<>());
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        long currentTime = System.currentTimeMillis();
        long lastUsed = playerCooldowns.getOrDefault(itemId, 0L);

        if (currentTime - lastUsed < cooldownSeconds * 1000L) {
            long remaining = (lastUsed + cooldownSeconds * 1000L - currentTime) / 1000L;
            p.sendMessage("§cWait " + remaining + " seconds before using again!");
            return false;
        }

        playerCooldowns.put(itemId, currentTime);
        return true;
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent e) {


        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null) return;

        Action action = e.getAction();
        Block center = e.getClickedBlock();

        // Air click pe target block try karo (optional)
        if (center == null) {
            center = p.getTargetBlockExact(6);
        }


        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {

            // Right click AIR
            if (action == Action.RIGHT_CLICK_AIR) {
                ExecuteAction(p, item, ActionType.ON_RIGHT_AIR);
            }

            // Right click (Air + Block dono)
            executeItemAction(p, item, ActionType.ON_RIGHT_CLICK, center);

            // General interact
            executeItemAction(p, item, ActionType.ON_INTERACT, center);
        }

        // ========== LEFT CLICK ==========
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            executeItemAction(p, item, ActionType.ON_LEFT_CLICK, center);
        }


        // this is  item for equip
        if (e.getHand() != EquipmentSlot.HAND)
            return;


        if (item == null)
            return;

        String itemId = getItemId(item);
        if (itemId == null)
            return;

        ConfigurationSection sec = itemsConfig.getConfigurationSection("items." + itemId);
        if (sec == null)
            return;

        String slotName = sec.getString("equip-slot");
        if (slotName == null)
            return;


        EquipmentSlot slot = EquipmentSlot.valueOf(slotName.toUpperCase());

        ItemStack old;

        switch (slot) {
            case HEAD -> {
                old = p.getInventory().getHelmet();
                p.getInventory().setHelmet(item);
            }

            case CHEST -> {
                old = p.getInventory().getChestplate();
                p.getInventory().setChestplate(item);
            }

            case LEGS -> {
                old = p.getInventory().getLeggings();
                p.getInventory().setLeggings(item);
            }

            case FEET -> {
                old = p.getInventory().getBoots();
                p.getInventory().setBoots(item);
            }

            default -> {
                return;
            }
        }

        item.setAmount(0);

        if (old != null && old.getType() != Material.AIR) {
            p.getInventory().addItem(old);
        }

        e.setCancelled(true);

    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {

        Player p = e.getPlayer();


        Block block = e.getBlock();


//  thus is event trigger
      /*  if (!Tag.LOGS.isTagged(block.getType())) return;

        DnnItem.getInstance().abilityManager.visited.clear();
        DnnItem.getInstance().abilityManager.logs.clear();



        DnnItem.getInstance().abilityManager.dfs(block);

        for (Block b : DnnItem.getInstance().abilityManager.logs) {
            b.breakNaturally(e.getPlayer().getInventory().getItemInMainHand());
        }*/


        executeItemAction(
                p,
                p.getInventory().getItemInMainHand(),
                ActionType.ON_MINE,
                block
        );


    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {


        if (!(e.getDamager() instanceof Player p)) {
            return;
        }

        EntityAction(
                p,
                p.getInventory().getItemInMainHand(),
                ActionType.ON_HIT

        );
    }


    private void EntityAction(Player p, ItemStack item, ActionType actionType) {

        String itemId = getItemId(item);
        if (itemId == null) return;

        ConfigurationSection itemSec = itemsConfig.getConfigurationSection("items." + itemId);
        if (itemSec == null) return;

        int cooldown = itemSec.getInt("cooldown", 0);
        if (!checkCooldown(p, itemId + "_" + actionType.getConfigName(), cooldown)) {
            return;
        }

        ConfigurationSection actionSec = itemSec.getConfigurationSection(actionType.getConfigName());
        if (actionSec == null) return;

        ConfigurationSection ability = actionSec.getConfigurationSection("ability");
        if (ability == null) return;

        if (ability.contains("bleed")) {

            int duration = ability.getInt("bleed");
            int damage = ability.getInt("damage");
            EntityBleed(p, duration, damage);


        }


    }


    private void ExecuteAction(Player p , ItemStack item , ActionType action){




        String itemId = getItemId(item);
        if (itemId == null) return;

        ConfigurationSection itemSec = itemsConfig.getConfigurationSection("items." + itemId);
        if (itemSec == null) return;

        int cooldown = itemSec.getInt("cooldown", 0);
        if (!checkCooldown(p, itemId + "_" + action.getConfigName(), cooldown)) {
            return;
        }


        ConfigurationSection actionSec = itemSec.getConfigurationSection(action.getConfigName());
        if (actionSec == null) return;

        ConfigurationSection ability = actionSec.getConfigurationSection("ability");
        if (ability == null) return;






    }

    private void executeItemAction(Player p, ItemStack item, ActionType action, Block block) {


        String itemId = getItemId(item);
        if (itemId == null) return;

        ConfigurationSection itemSec = itemsConfig.getConfigurationSection("items." + itemId);
        if (itemSec == null) return;

        int cooldown = itemSec.getInt("cooldown", 0);
        if (!checkCooldown(p, itemId + "_" + action.getConfigName(), cooldown)) {
            return;
        }


        ConfigurationSection actionSec = itemSec.getConfigurationSection(action.getConfigName());
        if (actionSec == null) return;

        ConfigurationSection ability = actionSec.getConfigurationSection("ability");
        if (ability == null) return;


        // Tree Chop


        if (ability.getBoolean("tree-chop", false)
                && block != null
                && isLog(block.getType())) {

            int treeRadius = ability.getInt("radius", 3);
            int treeHeight = ability.getInt("height", 20);


            if (!canBuildArea(p, p.getLocation(), treeRadius)) {
                p.sendMessage("§c§lProtected Area! Tree chop is not allowed.");
                return;
            }

            chopTree(
                    block,
                    p,
                    treeRadius, treeHeight);

        }


        // this is earthquake
        if (ability.getBoolean("earthquake", false)) {
            DnnItem.getInstance().getAbility().startEarthquake(p, ability);
            return;
        }


        // this is turnedo code
        if (ability.getBoolean("tornado", false)) {
            DnnItem.getInstance().getAbility().startTornado(p, ability);
            return;
        }

        // Gravity Gun
        if (ability.getBoolean("gravity-gun", false)) {
            DnnItem.getInstance().getAbility().handleGravityGun(p, ability);
            return;
        }

        // FARM ABILITY

        if(ability.getBoolean("isFarmLand",false)){

            if (block == null) {
                block = p.getTargetBlockExact(5);
                if (block == null) return;
            }

            int AxisX = ability.getInt("axisX");
            int AxisY = ability.getInt("axisY");
            int AxisZ = ability.getInt("axisZ");

            if (!canBuildArea(p, p.getLocation(), AxisX*AxisY*AxisZ)) {
                p.sendMessage("§c§lProtected Area! FARM is not allowed.");
                return;
            }

            DnnItem.getInstance().getAbility().firmArea(block,AxisX,AxisY,AxisZ);

        }


        if(ability.getBoolean("autoSmelt",false)){

            int smeltRadius = ability.getInt("smeltRadius");

            if (!canBuildArea(p, p.getLocation(), smeltRadius)) {
                p.sendMessage("don't smelt here  ?");
                return;
            }

            DnnItem.getInstance().getAbilityManager().autoSmelt(p,smeltRadius);


        }

        //
        if(ability.getBoolean("isHarvest",false)){

            if (block == null) {
                block = p.getTargetBlockExact(5);
                if (block == null) return;
            }

            int harvestRadius = ability.getInt("harvestRadius");

            if (!canBuildArea(p, p.getLocation(), harvestRadius)) {
                p.sendMessage("don't harvest here  ?");
                return;
            }


            DnnItem.getInstance().getAbility().CropBreak(p,block,harvestRadius);



         }


        // this is throw player


        if (ability.getBoolean("throw",false)) {
            int throwRadius = ability.getInt("throw-radius", 5);
            double throwPower = ability.getDouble("throw-power", 1.8);
            double height = ability.getDouble("height", 6.0);
            String throwType = ability.getString("throw-type", "player").toLowerCase();

            if (!canBuildArea(p, p.getLocation(), throwRadius)) {
                p.sendMessage("§c§lThrow karna is area mein allowed nahi hai!");
                return;
            }

            throwEntities(p, throwRadius, throwPower, throwType, height);

        }

        if(ability.getBoolean("blockWave",false)){

            int waveRadiusx = ability.getInt("radius");

            DnnItem.getInstance().getAbility().processWave(block,waveRadiusx);

        }


        // this is rock spike
        if (ability.getBoolean("isRockSpike",false)){

            int length = ability.getInt("spikeLength");
            int spikeRadius = ability.getInt("spikeRadius");

            if (!canBuildArea(p, p.getLocation(), spikeRadius)) {
                p.sendMessage("noo  ?");
                return;
            }

            DnnItem.getInstance().getAbility().rockSpike(p,length,spikeRadius);

        }

        // this is we
        if(ability.getBoolean("wave",false)){

            int waveRadius  = ability.getInt("waveRadius");

            if (!canBuildArea(p, p.getLocation(), waveRadius)) {
                p.sendMessage("ye tere baap ka area hai ?");
                return;
            }

            DnnItem.getInstance().getAbility().BlockWave(p,waveRadius);


        }


        if (ability.getBoolean("isDamage",false)) {

            int damageAmmout = ability.getInt("damage", 5);
            int damageRadius = ability.getInt("damageRadius", 5);
            String DameType = ability.getString("damageType", "all".toLowerCase());

            if (!canBuildArea(p, p.getLocation(), damageRadius)) {
                p.sendMessage("ye tere baap ka area hai ?");
                return;
            }

            EntityDamage(p, damageAmmout, damageRadius, DameType);


        }




        ConfigurationSection BlockBreak = ability.getConfigurationSection("blockBreak");

        if (BlockBreak == null) return;

        if (BlockBreak.getBoolean("cylinderShape", false)) {

            int cylinderShape = BlockBreak.getInt("radius");
            int cylinderHeight = BlockBreak.getInt("Height");

            if (!canBuildArea(p, p.getLocation(), cylinderShape)) {

                p.sendMessage("You nigga");
                return;
            }

            DnnItem.getInstance().abilityManager.DNSphere(block, cylinderShape, cylinderHeight, 5);



        }

        if(BlockBreak.getBoolean("cubeShape",false)){

            int radiusX = BlockBreak.getInt("radiusX");
            int radiusY = BlockBreak.getInt("radiusY");
            int radiusZ = BlockBreak.getInt("radiusZ");

            if (!canBuildArea(p, p.getLocation(), radiusX*radiusY*radiusZ)) {

                p.sendMessage("You nigga");
                return;
            }

            DnnItem.getInstance().getAbility().BoxMine(block,radiusX,radiusY,radiusZ);




        }


        if(BlockBreak.getBoolean("sphereShape",false)){

            int Sphereradius = BlockBreak.getInt("radius");


            if (!canBuildArea(p, p.getLocation(), Sphereradius)) {

                p.sendMessage("You nigga");
                return;
            }

            DnnItem.getInstance().getAbility().Sphere(block,Sphereradius);




        }


        //--------------------------------------------------------------------






        if (ability.contains("silePower")) {

            double slidePower = ability.getDouble("silePower", 1.5);

            PlayerSlide(p, slidePower);

        }


        // Dig Area
        String shape = ability.getString("dig-shape", "none");
        int radius = ability.getInt("dig-radius", 0);
        int xRadius = ability.getInt("x-radius", 0);
        int zRadius = ability.getInt("z-radius", 0);
        int Up = ability.getInt("Up", 0);
        int Down = ability.getInt("Down", 0);

        if (!shape.equalsIgnoreCase("none")
                && (xRadius > 0 || zRadius > 0 || Up > 0 || Down > 0 || radius > 0)) {

            if (xRadius <= 0) xRadius = radius;
            if (zRadius <= 0) zRadius = radius;
            if (Up <= 0) Up = radius;
            if (Down <= 0) Down = radius;

            digArea(block, p, shape, radius, xRadius, zRadius, Up, Down);
            return;
        }

        // Function
        String function = ability.getString("function");
        if (function != null && !function.isEmpty()) {

            int fillRadius = ability.getInt("fill-radius", 3);

            if (!canBuildArea(p, block.getLocation(), fillRadius))
                return;

            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "execute as " + p.getName() +
                            " at " + p.getName() +
                            " run function " + function
            );
        }
    }


    public boolean canBuildArea(Player p, Location center, int radius) {

        RegionQuery query = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .createQuery();

        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

        World world = center.getWorld();
        Block block = center.getBlock();
        boolean gpEnabled = Bukkit.getPluginManager().getPlugin("GriefPrevention") != null;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Location loc = new Location(
                            world,
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z
                    );

                    if (!query.testState(
                            BukkitAdapter.adapt(loc),
                            lp,
                            Flags.BUILD
                    )) {
                        return false; // Ek bhi protected block mila
                    }

                    // GriefPrevention
                    Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, true, null);

                    if (claim == null) {
                        //Bukkit.getLogger().info("No claim: " + loc);
                    } else {
                       // Bukkit.getLogger().info("Claim found: " + loc);

                        String reason = claim.allowBuild(p, loc.getBlock().getType());
                     //   Bukkit.getLogger().info("allowBuild = " + reason);

                        if (reason != null) {
                            return false;
                        }
                    }


                }
            }
        }


        return true;
    }


    private boolean canBuild(Player p, Block block) {
        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer();

            RegionQuery query = container.createQuery();

            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);

            return query.testState(
                    BukkitAdapter.adapt(block.getLocation()),
                    localPlayer,
                    Flags.BUILD
            );

        } catch (Exception ex) {
            getLogger().warning("WorldGuard check failed: " + ex.getMessage());
            return true;
        }
    }

    public String getItemId(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        if (!command.getName().equalsIgnoreCase("dnnItem")) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadItemsConfig();
            loadCustomItems();
            sender.sendMessage("§aCustom items reloaded!");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("get")) {
            String id = args[1].toLowerCase();
            ItemStack item = customItems.get(id);
            if (item != null && sender instanceof Player p) {
                p.getInventory().addItem(item.clone());
                p.sendMessage("§a§lle tera item: §r" + id);
            } else {
                sender.sendMessage("§cItem not mila!");
            }
            return true;
        }


        if (args.length == 1 && args[0].equalsIgnoreCase("generatepack")) {
            DnnItem.getInstance().getResoucePack().generateResourcePackX();
            DnnItem.getInstance().getResoucePack().generateArmorPack();
            sender.sendMessage("§a§lResource Pack generate ho gaya!");
            sender.sendMessage("§eFolder: plugins/CustomItemPlugin/generated_pack");
            return true;
        }

       /* if (args.length == 1 && args[0].equalsIgnoreCase("generatepack")) {

            sender.sendMessage("1");

            try {

                sender.sendMessage("2");

                resourcePackGenerator.generate();

                sender.sendMessage("3");

            } catch (Exception e) {

                sender.sendMessage("ERROR: " + e.getClass().getSimpleName());
                sender.sendMessage(e.getMessage());

                e.printStackTrace();

            }

        return true;
    }*/




        sender.sendMessage("§e/customitem reload | get <id>");
        return true;



    }


    // ==================== TREE CHOPPER ====================

    private void chopTree(Block startBlock, Player p, int radius, int maxHeight) {

        if (!isLog(startBlock.getType())) return;

        Set<Block> toBreak = new HashSet<>();
        findTreeBlocksImproved(startBlock, toBreak, 0, radius, maxHeight);

        for (Block b : toBreak) {
            if (!canBuild(p, b)) {
                p.sendMessage("§c§lProtected Area! Tree chop cancelled.");
                return;
            }
        }

        // Sab allowed hain, ab tree
        int broken = 0;
        ItemStack tool = p.getInventory().getItemInMainHand();

        for (Block b : toBreak) {
            if (b.getType() != Material.AIR) {
                b.breakNaturally(tool);
                broken++;
            }
        }



        p.sendMessage("§2🌲 " + broken + " le kat gaya tera!");
    }


    private void findTreeBlocksImproved(Block block, Set<Block> toBreak, int depth, int radius, int maxHeight) {
        if (depth > maxHeight || toBreak.size() > 800) return;
        if (toBreak.contains(block)) return;

        Material type = block.getType();

        if (isLog(type) || isLeaf(type)) {
            toBreak.add(block);

            // Horizontal radius check
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 2; y++) {   // thoda vertical flexibility
                        Block next = block.getRelative(x, y, z);
                        if (!toBreak.contains(next)) {
                            findTreeBlocksImproved(next, toBreak, depth + 1, radius, maxHeight);
                        }
                    }
                }
            }
        }
    }

    private boolean isLog(Material m) {
        String name = m.name();
        return name.contains("LOG") || name.contains("WOOD") || name.contains("STEM");
    }

    private boolean isLeaf(Material m) {
        String name = m.name();
        return name.contains("LEAVES") || name.contains("LEAF");
    }

    private void digArea(Block centerBlock,
                         Player p,
                         String shape,
                         int radius,
                         int xRadius,
                         int zRadius,
                         int up,
                         int down) {



        Location center;

        if (centerBlock != null) {
            center = centerBlock.getLocation();
        } else {
            // Right click in air → Player ke looking direction mein dig
            center = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(4));
        }



        if (radius > 255) {
            p.sendMessage("§c§lWarning: §r255 radius bahut dangerous hai! Lag ho sakta hai.");
        }

        Set<Block> toBreak = new HashSet<>();
        int maxBlocks = 80000; // safety limit (adjust kar sakte ho)
        int broken = 0;
        WorldGuardPlugin wg = WorldGuardPlugin.inst();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -down; y <= up; y++) {
                for (int z = -zRadius; z <= zRadius; z++) {

                    // Sphere
                    if (shape.equalsIgnoreCase("sphere")) {
                        double nx = (double) x / Math.max(xRadius, 1);
                        double ny = y >= 0
                                ? (double) y / Math.max(up, 1)
                                : (double) y / Math.max(down, 1);
                        double nz = (double) z / Math.max(zRadius, 1);

                        if (nx * nx + ny * ny + nz * nz > 1.0) {
                            continue;
                        }
                    }

                    Block b = center.getBlock().getRelative(x, y, z);

                    if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK)
                        continue;

                    if (!canBuild(p, b))
                        continue;

                    toBreak.add(b);
                }
            }
        }

        ItemStack tool = p.getInventory().getItemInMainHand();

        for (Block b : toBreak) {
            b.breakNaturally(tool);
            broken++;
        }

        p.sendMessage("§b§l" + broken + " blocks mined!");








    }



    private void throwEntities(Player thrower, int radius, double power, String type,double maxY) {

        if (radius <= 0 || power <= 0 || maxY <=0) return;

        Location center = thrower.getLocation();
        int count = 0;
        // max upward velocity


        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, radius, radius * 0.8, radius);

        for (Entity entity : nearby) {
            if (entity == thrower || entity.isDead()) continue;

            // Type Filter
            boolean shouldThrow = switch (type.toLowerCase()) {
                case "player" -> entity instanceof Player;
                case "entity", "mob" -> !(entity instanceof Player);
                case "all" -> true;
                default -> false;
            };

            if (!shouldThrow) continue;

            // Optimized Vector Calculation
            org.bukkit.util.Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize();

            // Add some randomness for natural feel
            direction.add(new Vector((Math.random() - 0.5) * 0.3, 0, (Math.random() - 0.5) * 0.3)).normalize();

            double finalPower = power * (1 - (center.distance(entity.getLocation()) / (radius * 1.2)));

            Vector velocity = direction.multiply(finalPower);
            velocity.setY(Math.max(maxY, velocity.getY() + 0.9)); // Good upward throw

            entity.setVelocity(velocity);
            count++;

            // Visual + Sound Effect
            if (count % 5 == 0) {  // Performance ke liye har entity pe nahi
                entity.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, entity.getLocation(), 8, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Feedback
        if (count > 0) {
            thrower.sendMessage("§b§l" + count + " entities! (Power: " + power + ")");
            thrower.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
            thrower.playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        } else {
            thrower.sendMessage("§c target not found in radius .");
        }

    }

    private void EntityDamage(Player p ,int damageAmount , int radius , String type ) {

        if (radius <= 0 || damageAmount <= 0) return;


        Location center = p.getLocation();

        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, radius, radius * 0.8, radius);


        for(Entity entity : nearby){

            if(entity == p || entity.isDead()) continue;

            boolean shouldDamage = switch (type.toLowerCase()) {
                case "player" -> entity instanceof Player;
                case "entity", "mob" -> !(entity instanceof Player);
                case "all" -> true;
                default -> false;
            };

            if (!shouldDamage) continue;

            if (!(entity instanceof LivingEntity living)) continue;

            double distance = center.distance(entity.getLocation());

            if (distance > radius) continue;

            // Distance ke hisab se damage kam hoga
            double finalDamage = damageAmount * (1.0 - (distance / radius));

            // Minimum damage
            if (finalDamage < 1.0) finalDamage = 1.0;

            living.damage(finalDamage, p); // p damage dealer hoga



        }



    }


    private void PlayerSlide(Player p, double slidePower){

        if (slidePower <= 0) return;

        Vector direction = p.getLocation().getDirection().normalize();

        // Y ko 0 kar do agar sirf ground slide chahiye
        direction.setY(0).normalize();

        p.setVelocity(direction.multiply(slidePower));



    }


    private void EntityBleed(Player p , int bleedDuration , int damage) {


        if (bleedDuration <= 0 || damage <= 0) return;

        final int[] ticks = {0};

        Location center = p.getLocation();

        new BukkitRunnable() {
            @Override
            public void run() {

                if (!p.isOnline() || p.isDead()) {
                    cancel();
                    return;
                }

                for (Entity entity : p.getNearbyEntities(5, 5, 5)) {

                    if (!(entity instanceof LivingEntity living)) continue;
                    if (living == p) continue; // player ko damage mat do

                    living.damage(damage, p); // attacker = player



                    living.getWorld().spawnParticle(
                            Particle.BLOCK,
                            living.getLocation().add(0, 1, 0),
                            12,
                            0.3, 0.5, 0.3,
                            Material.REDSTONE_BLOCK.createBlockData()
                    );

                    living.getWorld().playSound(
                            living.getLocation(),
                            Sound.ENTITY_PLAYER_HURT,
                            0.6f,
                            0.8f
                    );
                }




                // Damage
              //  p.damage(damage);

                // Blood particles


                ticks[0]++;

                // 1 second = 20 ticks
                if (ticks[0] >= bleedDuration) {
                    cancel();
                }
            }

        }.runTaskTimer(DnnItem.getInstance(), 0L, 20L);

    }
}
