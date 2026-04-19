package com.baneodragons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.DragonFireball;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BaneOfDragons extends JavaPlugin implements Listener {

    private static final Set<String> ALLOWED = Set.of(".Fireburner3309", "_Abraxis");
    private static final long DOUBLE_CLICK_MS = 350;
    private static final int STARFALL_DURATION_TICKS = 100; // 5 seconds
    private static final int STARFALL_INTERVAL_TICKS = 10;  // strike every 0.5s

    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, BukkitTask> starfallTasks = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BaneOfDragons enabled!");
    }

    @EventHandler
    public void onAreaEffectCloud(AreaEffectCloudApplyEvent event) {
        event.getAffectedEntities().removeIf(
            entity -> entity instanceof Player p && ALLOWED.contains(p.getName())
        );
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
        var uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastClickTime.getOrDefault(uuid, 0L);
        lastClickTime.put(uuid, now);

        if (now - last < DOUBLE_CLICK_MS) {
            lastClickTime.put(uuid, 0L); // reset so triple-click doesn't re-trigger
            teleportToLook(player);
        } else {
            toggleStarfall(player);
        }
    }

    private void teleportToLook(org.bukkit.entity.Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(), player.getEyeLocation().getDirection(), 50
        );
        Location target;
        if (result != null && result.getHitBlock() != null) {
            target = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
        } else {
            target = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(50));
        }
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.teleport(target);
    }

    private void toggleStarfall(org.bukkit.entity.Player player) {
        var uuid = player.getUniqueId();

        if (starfallTasks.containsKey(uuid)) {
            starfallTasks.remove(uuid).cancel();
            return;
        }

        var runnable = new BukkitRunnable() {
            int ticks = STARFALL_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline() || ticks <= 0) {
                    starfallTasks.remove(uuid);
                    cancel();
                    return;
                }
                ticks -= STARFALL_INTERVAL_TICKS;

                RayTraceResult result = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(), player.getEyeLocation().getDirection(), 50
                );
                Location target = result != null && result.getHitBlock() != null
                    ? result.getHitBlock().getLocation().add(0.5, 0, 0.5)
                    : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(50));

                var spawnLoc = target.clone().add(0, 15, 0);
                var fireball = player.getWorld().spawn(spawnLoc, DragonFireball.class);
                fireball.setDirection(new Vector(0, -1, 0));
            }
        };

        starfallTasks.put(uuid, runnable.runTaskTimer(this, 0L, STARFALL_INTERVAL_TICKS));
    }
}
