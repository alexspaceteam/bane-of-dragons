package com.baneodragons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BaneOfDragons extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BaneOfDragons enabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        getServer().broadcast(
            Component.text("[BaneOfDragons] ", NamedTextColor.GOLD)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text(" has joined the realm!", NamedTextColor.WHITE))
        );

        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.NETHERITE_SWORD) {
            event.getPlayer().getWorld().spawnEntity(event.getPlayer().getLocation(), EntityType.PIG);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        var item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item != null && item.getType() == Material.NETHERITE_SWORD) {
            event.getPlayer().getWorld().spawnEntity(event.getPlayer().getLocation(), EntityType.PIG);
        }
    }
}
