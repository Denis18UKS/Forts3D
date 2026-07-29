package ru.denis.forts3d.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.denis.forts3d.Forts3D;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Forts3D.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.forts3d.main"))
        .icon(() -> ModItems.REACTOR_CORE.toStack())
        .displayItems((parameters, output) -> {
            // Construction
            output.accept(ModItems.WOOD_BEAM); output.accept(ModItems.METAL_BEAM); output.accept(ModItems.ARMOR_PLATE);
            output.accept(ModItems.REINFORCED_GLASS); output.accept(ModItems.BLAST_DOOR); output.accept(ModItems.FOUNDATION);
            // Economy and power
            output.accept(ModItems.REACTOR_CORE); output.accept(ModItems.METAL_MINE); output.accept(ModItems.BATTERY);
            output.accept(ModItems.POWER_CABLE); output.accept(ModItems.REPAIR_STATION);
            // Technology
            output.accept(ModItems.WORKSHOP); output.accept(ModItems.FACTORY); output.accept(ModItems.MUNITIONS_PLANT);
            output.accept(ModItems.RADAR); output.accept(ModItems.SHIELD_EMITTER);
            // Weapons
            output.accept(ModItems.MACHINE_GUN); output.accept(ModItems.CANNON); output.accept(ModItems.MORTAR);
            output.accept(ModItems.ROCKET_LAUNCHER); output.accept(ModItems.LASER); output.accept(ModItems.FLAK);
            // Tools and administration
            output.accept(ModItems.BUILD_TOOL); output.accept(ModItems.REPAIR_TOOL); output.accept(ModItems.TEAM_SELECTOR);
            output.accept(ModItems.ADMIN_TABLET);
        }).build());
    private ModCreativeTabs() {}
}
