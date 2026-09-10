package org.example.dnnItem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BlockPlacementChecker implements Listener {


    public final Set<Location> placedlog = new HashSet<>();

    @EventHandler
    private void onPlace(BlockPlaceEvent e){

        Block block = e.getBlock();

        if (Tag.LOGS.isTagged(block.getType())) {
            placedlog.add(block.getLocation());
        }


    }

    @EventHandler
    private void onPlayerCrouch(PlayerToggleSneakEvent event){







    }

    private void executeItemAction(Player p, ItemStack item, DnnItem.ActionType action, Block block) {


        String itemId = DnnItem.getInstance().getItemId(item);
        if (itemId == null) return;

        ConfigurationSection itemSec = DnnItem.getInstance().getItemsConfig().getConfigurationSection("items." + itemId);
        if (itemSec == null) return;

        int cooldown = itemSec.getInt("cooldown", 0);
        if (!DnnItem.getInstance().checkCooldown(p, itemId + "_" + action.getConfigName(), cooldown)) {
            return;
        }


        ConfigurationSection actionSec = itemSec.getConfigurationSection(action.getConfigName());
        if (actionSec == null) return;

        ConfigurationSection ability = actionSec.getConfigurationSection("ability");
        if (ability == null) return;











    }




}
