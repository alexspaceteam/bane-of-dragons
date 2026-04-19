package com.baneodragons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BaneOfDragons extends JavaPlugin implements Listener {

    private static final Set<String> ALLOWED = Set.of(".Fireburner3309", "_Abraxis");

    private static final long CLICK_WINDOW_MS  = 400;
    private static final int  PENDING_DELAY    = 8;   // ticks to wait before firing (allows double-click)
    private static final int  STARFALL_DURATION = 100;
    private static final int  STARFALL_INTERVAL = 10;

    private final Map<UUID, Integer>    clickCount    = new HashMap<>();
    private final Map<UUID, Long>       lastClickTime = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingTask   = new HashMap<>();
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

    // Left-click → Shadow Slash
    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.NETHERITE_SWORD) return;
        if (!ALLOWED.contains(event.getPlayer().getName())) return;

        shadowSlash(event.getPlayer());
    }

    // Right-click: single → Starfall, double → Teleport
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.NETHERITE_SWORD) return;
        if (!ALLOWED.contains(event.getPlayer().getName())) return;

        var player = event.getPlayer();
        var uuid   = player.getUniqueId();
        long now   = System.currentTimeMillis();
        long last  = lastClickTime.getOrDefault(uuid, 0L);

        if (now - last > CLICK_WINDOW_MS) {
            clickCount.put(uuid, 0);
        }
        lastClickTime.put(uuid, now);
        int count = clickCount.merge(uuid, 1, Integer::sum);

        if (count == 1) {
            BukkitTask task = getServer().getScheduler().runTaskLater(this, () -> {
                pendingTask.remove(uuid);
                clickCount.remove(uuid);
                toggleStarfall(player);
            }, PENDING_DELAY);
            pendingTask.put(uuid, task);

        } else if (count >= 2) {
            cancelPending(uuid);
            clickCount.remove(uuid);
            teleportToLook(player);
        }
    }

    // ── Starfall ──────────────────────────────────────────────────────────────

    private void toggleStarfall(Player player) {
        var uuid = player.getUniqueId();

        if (starfallTasks.containsKey(uuid)) {
            starfallTasks.remove(uuid).cancel();
            return;
        }

        var runnable = new BukkitRunnable() {
            int ticks = STARFALL_DURATION;

            @Override
            public void run() {
                if (!player.isOnline() || ticks <= 0) {
                    starfallTasks.remove(uuid);
                    cancel();
                    return;
                }
                ticks -= STARFALL_INTERVAL;

                RayTraceResult result = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(), player.getEyeLocation().getDirection(), 50
                );
                Location target = result != null && result.getHitBlock() != null
                    ? result.getHitBlock().getLocation().add(0.5, 0, 0.5)
                    : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(50));

                var fireball = player.getWorld().spawn(target.clone().add(0, 15, 0), DragonFireball.class);
                fireball.setDirection(new Vector(0, -1, 0));
            }
        };

        starfallTasks.put(uuid, runnable.runTaskTimer(this, 0L, STARFALL_INTERVAL));
    }

    // ── Teleport ──────────────────────────────────────────────────────────────

    private void teleportToLook(Player player) {
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

    // ── Shadow Slash ──────────────────────────────────────────────────────────

    private void shadowSlash(Player player) {
        var loc = player.getLocation();
        var dir = loc.getDirection().normalize();

        player.getNearbyEntities(4, 4, 4).stream()
            .filter(e -> e instanceof LivingEntity && e != player)
            .filter(e -> {
                var toTarget = e.getLocation().subtract(loc).toVector();
                if (toTarget.lengthSquared() == 0) return false;
                return toTarget.normalize().dot(dir) > 0.5;
            })
            .forEach(e -> {
                ((LivingEntity) e).damage(20, player);
                ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            });

        player.setVelocity(dir.clone().multiply(2.0).setY(0.3));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cancelPending(UUID uuid) {
        BukkitTask t = pendingTask.remove(uuid);
        if (t != null) t.cancel();
    }
}
