package ru.denis.forts3d.event;
import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.neoforge.event.server.ServerStoppedEvent;import net.neoforged.neoforge.event.tick.ServerTickEvent;import ru.denis.forts3d.game.FortsMatchManager;
public final class ServerEvents {@SubscribeEvent public static void tick(ServerTickEvent.Post e){FortsMatchManager.get(e.getServer()).tick(e.getServer());}@SubscribeEvent public static void stop(ServerStoppedEvent e){FortsMatchManager.remove(e.getServer());}}
