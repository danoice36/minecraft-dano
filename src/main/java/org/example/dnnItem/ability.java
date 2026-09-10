package org.example.dnnItem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.*;

public class ability {


    public void BoxMine(Block block, int radiusX, int radiusY, int radiusZ) {


        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {

                for (int z = -radiusZ; z <= radiusZ; z++) {


                    Block b = block.getRelative(x, y, z);

                    if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK)
                        continue;

                    b.breakNaturally();

                }
            }
        }


    }


    public void Sphere(Block block, int radius) {

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {

                for (int z = -radius; z <= radius; z++) {

                    if (x * x + y * y + z * z <= radius * radius) {

                        Block b = block.getRelative(x, y, z);

                        if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK)
                            continue;

                        b.breakNaturally();

                    }


                }
            }
        }

    }

    // public ek acces modifire hai
    // void --
    public void firmArea(Block block, int radiusX, int radiusY, int radiusZ) {


        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {

                for (int z = -radiusZ; z <= radiusZ; z++) {


                    Block b = block.getRelative(x, y, z);

                    if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK || b.getType() == Material.SHORT_GRASS || b.getType() == Material.TALL_GRASS)
                        continue;

                    b.setType(Material.FARMLAND);

                }
            }
        }

    }


    public void slam(Player player) {

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                player.getLocation(),
                1
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                1,
                1
        );

        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {

            if (!(entity instanceof LivingEntity living)) continue;

            if (entity == player) continue;

            living.damage(10, player);

            Vector kb = living.getLocation()
                    .toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(1.8);

            kb.setY(0.5);

            living.setVelocity(kb);
        }
    }

    public void processWave(Block center, int blockRadius) {

        for (int x = -blockRadius; x <= blockRadius; x++) {
            for (int z = -blockRadius; z <= blockRadius; z++) {

                if (x * x + z * z > blockRadius * blockRadius)
                    continue;

                Block ground = findGround(center.getRelative(x, 0, z));

                animateBlock(ground);
            }
        }
    }

    private void animateBlock(Block block) {

        if (!block.getType().isSolid())
            return;

        Location loc = block.getLocation().add(0.5, 0, 0.5);

        BlockDisplay display = block.getWorld().spawn(loc, BlockDisplay.class);

        display.setBlock(block.getBlockData());

        display.setInterpolationDuration(2);

        new BukkitRunnable() {

            int tick = 0;

            @Override
            public void run() {

                if (!display.isValid()) {
                    cancel();
                    return;
                }

                Transformation t = display.getTransformation();

                Vector3f translation = new Vector3f(0, 0, 0);

                if (tick <= 3) {
                    // Up
                    translation.y = tick * 0.25f;
                } else {
                    // Down
                    translation.y = (6 - tick) * 0.25f;
                }

                t.getTranslation().set(translation);
                display.setTransformation(t);

                tick++;

                if (tick > 6) {
                    display.remove();
                    cancel();
                }
            }

        }.runTaskTimer(DnnItem.getInstance(), 0L, 1L);
    }

    public Block findGround(Block block) {

        while (block.getY() < block.getWorld().getMaxHeight() - 1
                && block.getType().isSolid()) {
            block = block.getRelative(BlockFace.UP);
        }

        while (block.getY() > block.getWorld().getMinHeight()
                && !block.getRelative(BlockFace.DOWN).getType().isSolid()) {
            block = block.getRelative(BlockFace.DOWN);
        }

        return block.getRelative(BlockFace.DOWN);
    }


    public void BlockWave(Player player, int radius) {


        Block start = player.getLocation().subtract(0, 1, 0).getBlock();

        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        new BukkitRunnable() {

            @Override
            public void run() {

                if (queue.isEmpty()) {
                    cancel();
                    return;
                }

                int size = queue.size();

                for (int i = 0; i < size; i++) {

                    Block block = queue.poll();

                    if (block == null)
                        continue;

                    // Effect
                    block.getWorld().spawnParticle(
                            Particle.BLOCK,
                            block.getLocation().add(0.5, 1, 0.5),
                            20,
                            block.getBlockData()
                    );

                    block.getWorld().playSound(
                            block.getLocation(),
                            Sound.BLOCK_STONE_BREAK,
                            1,
                            1
                    );

                    // Damage nearby entities
                    for (Entity entity : block.getWorld().getNearbyEntities(
                            block.getLocation().add(0.5, 1, 0.5),
                            1.5, 2, 1.5)) {

                        if (!(entity instanceof LivingEntity living))
                            continue;

                        if (living == player)
                            continue;

                        living.damage(5, player);

                        Vector knock = living.getLocation().toVector()
                                .subtract(player.getLocation().toVector())
                                .normalize()
                                .multiply(1.2)
                                .setY(0.5);

                        living.setVelocity(knock);
                    }

                    // BFS neighbours
                    Block[] neighbours = {
                            block.getRelative(BlockFace.NORTH),
                            block.getRelative(BlockFace.SOUTH),
                            block.getRelative(BlockFace.EAST),
                            block.getRelative(BlockFace.WEST)
                    };

                    for (Block next : neighbours) {

                        if (visited.contains(next))
                            continue;

                        if (next.getLocation().distanceSquared(start.getLocation()) > radius * radius)
                            continue;

                        visited.add(next);
                        queue.add(next);
                    }
                }

            }

        }.runTaskTimer(DnnItem.getInstance(), 0L, 2L);


    }

    public void rockSpike(Player player, int length, double damage) {

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        World world = player.getWorld();

        new BukkitRunnable() {

            int step = 1;

            @Override
            public void run() {

                if (step > length) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().clone().add(dir.clone().multiply(step));

                // Find ground
                while (!loc.getBlock().getType().isSolid() && loc.getY() > world.getMinHeight()) {
                    loc.subtract(0, 1, 0);
                }

                Block ground = loc.getBlock();

                FallingBlock spike = world.spawnFallingBlock(
                        ground.getLocation().add(0.5, 1, 0.5),
                        ground.getBlockData()
                );

                spike.setGravity(false);
                spike.setDropItem(false);
                spike.setHurtEntities(false);

                spike.setVelocity(new Vector(0, 0.7, 0));

                world.spawnParticle(
                        Particle.BLOCK,
                        ground.getLocation().add(0.5, 1, 0.5),
                        30,
                        ground.getBlockData()
                );

                world.playSound(
                        ground.getLocation(),
                        Sound.BLOCK_STONE_BREAK,
                        1f,
                        0.8f
                );

                for (Entity e : world.getNearbyEntities(
                        ground.getLocation().add(0.5, 1, 0.5),
                        1.5, 2, 1.5)) {

                    if (!(e instanceof LivingEntity living))
                        continue;

                    if (living == player)
                        continue;

                    living.damage(damage, player);

                    Vector kb = living.getLocation().toVector()
                            .subtract(player.getLocation().toVector())
                            .normalize()
                            .multiply(1.2)
                            .setY(0.5);

                    living.setVelocity(kb);
                }

                Bukkit.getScheduler().runTaskLater(DnnItem.getInstance(),
                        spike::remove, 10L);

                step++;
            }

        }.runTaskTimer(DnnItem.getInstance(), 0L, 2L);
    }


    private boolean isFullyGrown(Block block) {

        if (!(block.getBlockData() instanceof org.bukkit.block.data.Ageable age))
            return false;

        return switch (block.getType()) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS , PUMPKIN_STEM , MELON_STEM-> age.getAge() == 7;
            case NETHER_WART -> age.getAge() == 3;
            case COCOA -> age.getAge() == 2;
            default -> false;
        };
    }

    public void CropBreak(Player player,Block center, int radius) {


        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                if (x * x + z * z > radius * radius)
                    continue;

                Block block = center.getRelative(x, 1, z);

                BlockData data = block.getBlockData();

               // player.sendMessage(block.getType().name());



                if (!(block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable)) {
                    //player.sendMessage("Not ageable");
                    continue;
                }

                // Fully grown?
                if (!isFullyGrown(block)) {
                  //  player.sendMessage("Not grown: " + block.getType());
                    continue;
                }



                BlockData particleData = block.getBlockData();
                Material type = block.getType();



                Collection<ItemStack> drops =
                        new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand(), player));

              //  player.sendMessage("Drops: " + drops.size());

                switch (type) {

                    case WHEAT -> removeOne(drops, Material.WHEAT_SEEDS);

                    case CARROTS -> removeOne(drops, Material.CARROT);

                    case POTATOES -> removeOne(drops, Material.POTATO);

                    case BEETROOTS -> removeOne(drops, Material.BEETROOT_SEEDS);

                    case NETHER_WART -> removeOne(drops, Material.NETHER_WART);

                    default -> {
                    }
                }
                // Give drops
                for (ItemStack item : drops) {

                    HashMap<Integer, ItemStack> left =
                            player.getInventory().addItem(item);

                    for (ItemStack remain : left.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), remain);
                    }
                }

                // Replant
                ageable.setAge(0);
                block.setBlockData((BlockData) ageable);

// Effects
                World world = block.getWorld();

                world.playSound(
                        block.getLocation(),
                        Sound.BLOCK_GRASS_BREAK,
                        1f,
                        1f
                );

                world.spawnParticle(
                        Particle.BLOCK,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        12,
                        particleData
                );

                world.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        block.getLocation().add(0.5, 1, 0.5),
                        5
                );
            }

        }


    }
    private static final EnumSet<Material> CROPS = EnumSet.of (


            Material.CARROT,
            Material.CARROTS,
            Material.PUMPKIN,
            Material.WHEAT,
            Material.BEETROOTS,
            Material.MELON,
            Material.POTATO,
            Material.POTATOES


    );

    private void removeOne(Collection<ItemStack> drops, Material material) {

        Iterator<ItemStack> iterator = drops.iterator();

        while (iterator.hasNext()) {

            ItemStack item = iterator.next();

            if (item.getType() != material)
                continue;

            if (item.getAmount() <= 1)
                iterator.remove();
            else
                item.setAmount(item.getAmount() - 1);

            return;
        }
    }

    //------------------------------------------------------------------

    private final Map<UUID, Entity> heldEntities = new HashMap<>();
    private final Map<UUID, BlockState> heldBlocks = new HashMap<>();
    private final Map<UUID, Long> holdStartTime = new HashMap<>();
    private final Map<UUID, Material> heldBlockType = new HashMap<>();
    private final Map<UUID, BlockData> heldBlockData = new HashMap<>();
    // ==================== GRAVITY GUN ====================
    public void handleGravityGun(Player p, ConfigurationSection actionSec) {
        UUID uuid = p.getUniqueId();
        int range = actionSec.getInt("gravity-range", 8);
        double throwPower = actionSec.getDouble("gravity-throw-power", 2.2);
        boolean allowBlocks = actionSec.getBoolean("gravity-pickup-blocks", true);
        boolean allowPlayers = actionSec.getBoolean("gravity-pickup-players", false);

        // ========== 1. PEHLE CHECK KARO: Kuch hold hai kya? ==========
        boolean holdingEntity = heldEntities.containsKey(uuid) && heldEntities.get(uuid) != null;
        boolean holdingBlock = heldBlockType.containsKey(uuid) && heldBlockType.get(uuid) != null;



        if (holdingEntity || holdingBlock) {
            throwHeld(p, throwPower);
            return;
        }

        // ========== 2. Naya target dhundo ==========
        RayTraceResult result = p.getWorld().rayTrace(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                range,
                FluidCollisionMode.NEVER,
                true,
                0.4,
                entity -> entity != p && !(entity instanceof org.bukkit.entity.Item)
        );

        if (result == null) {
            p.sendMessage("§cKoi target nahi mila!");
            return;
        }

        // ========== 3. Entity Pickup ==========
        if (result.getHitEntity() != null) {
            Entity target = result.getHitEntity();



            if (target instanceof Player targetPlayer) {
                // Permission check
                if (!allowPlayers && !p.hasPermission("dnnitem.gravity.players")) {
                    p.sendMessage("§cPlayers ko nahi utha sakte!");
                    return;
                }

                // Khud ko mat uthao
                if (targetPlayer.equals(p)) {
                    return;
                }
                // Optional: target ke paas bypass permission ho to mat uthao
                if (targetPlayer.hasPermission("dnnitem.gravity.bypass")) {
                    p.sendMessage("§cYeh player protected hai!");
                    return;
                }

            }
            heldEntities.put(uuid, target);
            holdStartTime.put(uuid, System.currentTimeMillis());
           p.sendMessage("§b§lEntity: §f" + target.getType().name());
            return;
        }

        // ========== 4. Block Pickup ==========
        if (allowBlocks && result.getHitBlock() != null) {
            Block block = result.getHitBlock();
            Material type = block.getType();

            if (type == Material.AIR || type == Material.BEDROCK || type == Material.BARRIER
                    || !type.isSolid()) {
                p.sendMessage("§cYeh block is not pickable!");
                return;
            }

            if (!DnnItem.getInstance().canBuildArea(p, p.getLocation(),15)) {
                p.sendMessage("§cProtected area!");
                return;
            }

            // Save
            heldBlockType.put(uuid, type);
            heldBlockData.put(uuid, block.getBlockData().clone());
            holdStartTime.put(uuid, System.currentTimeMillis());

            // Remove block
            block.setType(Material.AIR, false);

            //p.sendMessage("§b§lBlock uthaya: §f" + type.name());
          //  p.sendMessage("§7Phir se Right Click karke phenko!");
            return;
        }

        p.sendMessage("§cKoi target nahi mila!");
    }

    private void throwHeld(Player p, double power) {

        UUID uuid = p.getUniqueId();
        Vector dir = p.getEyeLocation().getDirection().normalize().multiply(power);
        dir.setY(Math.max(0.35, dir.getY()));

        // Entity
        if (heldEntities.containsKey(uuid)) {
            Entity entity = heldEntities.remove(uuid);
            holdStartTime.remove(uuid);
            if (entity != null && !entity.isDead()) {

                if (entity instanceof Player targetPlayer) {
                    targetPlayer.setFlying(false);
                    targetPlayer.setAllowFlight(false);
                }
                entity.setVelocity(dir);
                entity.setFallDistance(0);
                //p.sendMessage("§a§lEntity phenk diya!");
            }
            return;
        }

        // Block
        if (heldBlockType.containsKey(uuid)) {
            Material type = heldBlockType.remove(uuid);
            BlockData data = heldBlockData.remove(uuid);
            holdStartTime.remove(uuid);

            if (type == null || data == null) {
                p.sendMessage("§cHold data missing!");
                return;
            }

            Location spawnLoc = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(2.0));
            if (spawnLoc.getBlock().getType().isSolid()) {
                spawnLoc.add(0, 1.2, 0);
            }

            FallingBlock fb = p.getWorld().spawnFallingBlock(spawnLoc, data);
            fb.setVelocity(dir);
            fb.setDropItem(true);
            fb.setHurtEntities(true);
            fb.setFallDistance(0);

            p.sendMessage("§a§lBlock : §f" + type.name());
            return;
        }

        p.sendMessage("§c!");
    }

    public void HoldEnitity(){

        Bukkit.getScheduler().runTaskTimer(DnnItem.getInstance(), () -> {
            for (Map.Entry<UUID, Entity> entry : new HashMap<>(heldEntities).entrySet()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                Entity entity = entry.getValue();

                if (p == null || !p.isOnline() || entity == null || entity.isDead()) {
                    heldEntities.remove(entry.getKey());
                    holdStartTime.remove(entry.getKey());
                    continue;
                }

                // Max hold time check
                Long start = holdStartTime.get(entry.getKey());
                if (start != null && System.currentTimeMillis() - start > 15000) {
                    throwHeld(p, 1.0);
                    p.sendMessage("§cHold time khatam! Automatically phenk diya.");
                    continue;
                }

                if (entity instanceof Player targetPlayer) {
                    // Flight temporarily enable (smooth hold)
                    targetPlayer.setAllowFlight(true);
                    targetPlayer.setFlying(true);
                }

                Location targetLoc = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(2.5));
                entity.teleport(targetLoc);
                entity.setVelocity(new Vector(0, 0, 0));
                entity.setFallDistance(0);
            }
        }, 1L, 1L);
    }

    public void startTornado(Player p, ConfigurationSection sec) {
        int radius = sec.getInt("tornado-radius", 6);
        int height = sec.getInt("tornado-height", 8);
        double power = sec.getDouble("tornado-power", 1.4);
        int durationSec = sec.getInt("tornado-duration", 6);
        double damage = sec.getDouble("tornado-damage", 2.0);

        Location center = p.getLocation();

        // WorldGuard check
        if (!DnnItem.getInstance().canBuildArea(p, center, radius)) {
            p.sendMessage("§cYahan tornado allowed nahi hai!");
            return;
        }

        p.sendMessage("§f§lTornado release!");
        p.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.6f);

        // Tornado task
        final int[] ticks = {0};
        int maxTicks = durationSec * 20;

        Bukkit.getScheduler().runTaskTimer(DnnItem.getInstance(), task -> {
            if (ticks[0] >= maxTicks) {
                task.cancel();
                return;
            }

            Location base = center.clone();

            // Particles (tornado shape)
            for (double y = 0; y < height; y += 0.4) {
                double currentRadius = radius * (1.0 - (y / height) * 0.7); // upar patla
                double angle = (ticks[0] * 0.3) + (y * 0.8);

                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;

                Location particleLoc = base.clone().add(x, y, z);
                p.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
            }

            // Entities ko kheench + ghuma
            for (Entity entity : base.getWorld().getNearbyEntities(base, radius, height, radius)) {
                if (entity == p || entity.isDead()) continue;
                if (!(entity instanceof LivingEntity living)) continue;

                Location eLoc = entity.getLocation();
                Vector toCenter = base.toVector().subtract(eLoc.toVector());
                double distance = toCenter.length();

                if (distance < 0.5) distance = 0.5;

                // Spiral force
                Vector pull = toCenter.normalize().multiply(0.15 * power);
                Vector up = new Vector(0, 0.12 * power, 0);

                // Sideways spin
                Vector spin = new Vector(-toCenter.getZ(), 0, toCenter.getX()).normalize().multiply(0.18 * power);

                Vector finalVel = pull.add(up).add(spin);
                entity.setVelocity(entity.getVelocity().multiply(0.3).add(finalVel));
                entity.setFallDistance(0);

                // Thoda damage har second
                if (ticks[0] % 20 == 0 && damage > 0) {
                    living.damage(damage, p);
                }
            }

            ticks[0]++;
        }, 1L, 1L);
    }

    public void startEarthquake(Player p, ConfigurationSection sec) {
        int radius = sec.getInt("earthquake-radius", 8);
        double damage = sec.getDouble("earthquake-damage", 4.0);
        double power = sec.getDouble("earthquake-power", 1.6);
        int durationSec = sec.getInt("earthquake-duration", 3);

        Location center = p.getLocation();

        if (!DnnItem.getInstance().canBuildArea(p, center, radius)) {
            p.sendMessage("§cYahan earthquake allowed nahi hai!");
            return;
        }

        p.sendMessage("§6§lEARTHQUAKE!");
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        p.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.4f);

        final int[] ticks = {0};
        int maxTicks = durationSec * 20;

        Bukkit.getScheduler().runTaskTimer(DnnItem.getInstance(), task -> {
            if (ticks[0] >= maxTicks) {
                task.cancel();
                return;
            }

            // Screen shake feel (particles + sound)
            if (ticks[0] % 4 == 0) {
                p.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.6f);
                p.getWorld().spawnParticle(Particle.BLOCK, center, 40, radius * 0.6, 0.3, radius * 0.6, 0.1,
                        Material.DIRT.createBlockData());
                p.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 0.2, 0), 15, radius * 0.5, 0.1, radius * 0.5, 0.02);
            }

            // Entities ko hilao + damage
            for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 4, radius)) {
                if (entity == p || entity.isDead()) continue;
                if (!(entity instanceof LivingEntity living)) continue;

                double distance = entity.getLocation().distance(center);
                if (distance > radius) continue;

                // Center se bahar ki taraf knockback
                Vector knock = entity.getLocation().toVector().subtract(center.toVector());
                if (knock.lengthSquared() < 0.01) {
                    knock = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                }
                knock.normalize().multiply(power * (1.2 - (distance / radius)));
                knock.setY(0.35 + (Math.random() * 0.25));

                entity.setVelocity(knock);
                entity.setFallDistance(0);

                // Damage har 10 ticks
                if (ticks[0] % 10 == 0) {
                    double dmg = damage * (1.0 - (distance / radius) * 0.6);
                    living.damage(Math.max(1.0, dmg), p);
                }
            }

            // Random small block crack effect (sirf visual)
            if (ticks[0] % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    Location bLoc = center.clone().add(ox, 0, oz);
                    Block b = bLoc.getBlock();
                    if (b.getType().isSolid()) {
                        p.getWorld().spawnParticle(Particle.BLOCK, bLoc.add(0.5, 1, 0.5), 8, 0.3, 0.2, 0.3, 0.05, b.getBlockData());
                    }
                }
            }

            ticks[0]++;
        }, 1L, 1L);
    }


}
