package ru.denis.forts3d;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import ru.denis.forts3d.command.FortsCommands;
import ru.denis.forts3d.config.FortsConfig;
import ru.denis.forts3d.event.ServerEvents;
import ru.denis.forts3d.registry.*;

@Mod(Forts3D.MOD_ID)
public final class Forts3D {
    public static final String MOD_ID = "forts3d";
    public Forts3D(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FortsConfig.SPEC, "forts3d-server.toml");
        NeoForge.EVENT_BUS.addListener(FortsCommands::register);
        NeoForge.EVENT_BUS.register(ServerEvents.class);
    }
}
