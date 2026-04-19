package com.baneodragons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.DragonFireball;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BaneOfDragons extends JavaPlugin implements Listener {

    private static final Set<String> ALLOWED = Set.of(".Fireburner3309", "_Abraxis");


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
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.NETHERITE_SWORD) return;
        if (!ALLOWED.contains(event.getPlayer().getName())) return;

        var player = event.getPlayer();
        var direction = player.getEyeLocation().getDirection();
        var spawnLoc = player.getEyeLocation().add(direction);

        var fireball = player.getWorld().spawn(spawnLoc, DragonFireball.class);
        fireball.setDirection(direction);
    }
}
