package ru.denis.forts3d.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.denis.forts3d.config.FortsConfig;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.MatchState;

/** NeoForge event bridge into the authoritative match simulation. */
public final class ServerEvents {
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        FortsMatchManager.get(event.getServer()).tick(event.getServer());
    }

    @SubscribeEvent
    public static void projectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level() instanceof ServerLevel level) {
            FortsMatchManager.get(level.getServer()).projectileImpact(
                level,
                event.getProjectile(),
                event.getRayTraceResult()
            );
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
            && event.getPlayer() instanceof ServerPlayer player
            && !FortsMatchManager.get(level.getServer()).canBreak(
                player,
                event.getPos(),
                true
            )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level
            && FortsMatchManager.get(level.getServer()).state() == MatchState.RUNNING
            && !FortsConfig.WORLD_DAMAGE.get()) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        FortsMatchManager.remove(event.getServer());
    }

    private ServerEvents() {}
}
