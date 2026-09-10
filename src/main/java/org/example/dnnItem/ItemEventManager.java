package org.example.dnnItem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemEventManager {




    public final Set<Block> visited = new HashSet<>();
    public final List<Block> logs = new ArrayList<>();


    private final Set<UUID> ProcessItem = new HashSet<>();

    private static final EnumSet<Material> ORES = EnumSet.of (


            Material.GOLD_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_COPPER_ORE


    );


    private static final EnumSet<Material> RAWS = EnumSet.of(

            Material.RAW_IRON,
            Material.RAW_GOLD,
            Material.RAW_COPPER


    );

    private static final EnumSet<Material> INGOT = EnumSet.of(

            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.COPPER_INGOT


    );




    public final Set<Location> placedLogs = new HashSet<>();


    public void dfs(Block block){

        if(visited.contains(block)) return;
        visited.add(block);

        if(!Tag.LOGS.isTagged(block.getType())) return;

        logs.add(block);
        if (placedLogs.contains(block.getLocation())) return;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) continue;

                    Block next = block.getRelative(x, y, z);
                    dfs(next);
                }
            }
        }







    }









    public void DNSphere(Block block, int radius, int height , int lenth){




        if (radius <= 0 || height <= 0 ) return;


        for(int y = - height ; y<= height; y++){


            for(int x =- radius ; x<= radius ; x++){

                for(int z =-radius ; z<= radius; z++){

                    if(x*x*lenth + y*y + z*z <= radius*lenth || z*z*lenth + y*y + x*x <= radius*lenth ){

                        Block b = block.getRelative(x,y,z);

                        if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK)
                            continue;

                        b.breakNaturally();



                    }
                }



            }
        }



    }

    private static final Map<Material, Material> AUTO_SMELT = new HashMap<>();

    static {
        AUTO_SMELT.put(Material.RAW_IRON, Material.IRON_INGOT);


        AUTO_SMELT.put(Material.RAW_GOLD, Material.GOLD_INGOT);


        AUTO_SMELT.put(Material.RAW_COPPER, Material.COPPER_INGOT);


        AUTO_SMELT.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);

        AUTO_SMELT.put(Material.SAND, Material.GLASS);
        AUTO_SMELT.put(Material.RED_SAND, Material.GLASS);

        AUTO_SMELT.put(Material.CLAY_BALL, Material.BRICK);
        AUTO_SMELT.put(Material.COBBLESTONE, Material.STONE);
    }


    public void autoSmelt(Player player, double radius){

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {

            if (!(entity instanceof Item item)) continue;

            entity.getWorld().spawnParticle(Particle.BLOCK,
                    entity.getLocation().add(0, 1, 0),
                    12,
                    0.3, 0.5, 0.3,
                    Material.FIRE.createBlockData());

            if (ProcessItem.contains(item.getUniqueId())) continue;


            ItemStack stack = item.getItemStack();

             Material result = AUTO_SMELT.get(stack.getType());

            if (result == null)
                continue;

            stack.setType(result);
            item.setItemStack(stack);

             ProcessItem.add(item.getUniqueId());
            }
        }


    }





