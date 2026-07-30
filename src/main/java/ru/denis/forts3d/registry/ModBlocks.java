package ru.denis.forts3d.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.denis.forts3d.Forts3D;
import ru.denis.forts3d.block.BlastDoorBlock;
import ru.denis.forts3d.block.FortsStructureBlock;
import ru.denis.forts3d.block.ReactorCoreBlock;
import ru.denis.forts3d.block.ResearchBlock;
import ru.denis.forts3d.block.ResearchConsoleBlock;
import ru.denis.forts3d.block.ResourceMachineBlock;
import ru.denis.forts3d.game.Technology;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(Forts3D.MOD_ID);

    public static final DeferredBlock<Block> REACTOR_CORE = BLOCKS.register(
        "reactor_core",
        () -> new ReactorCoreBlock(
            BlockBehaviour.Properties.of()
                .strength(20.0F, 1200.0F)
                .lightLevel(state -> 10)
                .sound(SoundType.METAL)
        )
    );
    public static final DeferredBlock<Block> WOOD_BEAM = structure(
        "wood_beam", 4, 8, 8, 2, 5, null, SoundType.WOOD
    );
    public static final DeferredBlock<Block> METAL_BEAM = structure(
        "metal_beam", 12, 40, 30, 8, 15, null, SoundType.METAL
    );
    public static final DeferredBlock<Block> ARMOR_PLATE = structure(
        "armor_plate", 30, 120, 70, 20, 35, null, SoundType.METAL
    );
    public static final DeferredBlock<Block> REINFORCED_GLASS = structure(
        "reinforced_glass", 8, 60, 15, 12, 20, Technology.WORKSHOP, SoundType.GLASS
    );
    public static final DeferredBlock<Block> FOUNDATION = anchoredStructure(
        "foundation", 50, 1200, 500, 100, 50, SoundType.STONE
    );
    public static final DeferredBlock<Block> BLAST_DOOR = BLOCKS.register(
        "blast_door",
        () -> new BlastDoorBlock(
            BlockBehaviour.Properties.of()
                .strength(25, 200)
                .sound(SoundType.METAL)
                .noOcclusion()
        )
    );

    public static final DeferredBlock<Block> METAL_MINE = BLOCKS.register(
        "metal_mine",
        () -> new ResourceMachineBlock(
            BlockBehaviour.Properties.of()
                .strength(12, 40)
                .sound(SoundType.METAL),
            ResourceMachineBlock.Kind.METAL,
            5,
            120,
            Technology.WORKSHOP
        )
    );
    public static final DeferredBlock<Block> BATTERY = BLOCKS.register(
        "battery",
        () -> new ResourceMachineBlock(
            BlockBehaviour.Properties.of()
                .strength(8, 30)
                .sound(SoundType.METAL),
            ResourceMachineBlock.Kind.ENERGY,
            12,
            80,
            null
        )
    );
    public static final DeferredBlock<Block> MUNITIONS_PLANT = BLOCKS.register(
        "munitions_plant",
        () -> new ResourceMachineBlock(
            BlockBehaviour.Properties.of()
                .strength(12, 50)
                .sound(SoundType.METAL),
            ResourceMachineBlock.Kind.AMMO,
            4,
            220,
            Technology.WORKSHOP
        )
    );
    public static final DeferredBlock<Block> POWER_CABLE = structure(
        "power_cable", 3, 10, 5, 1, 3, null, SoundType.COPPER
    );
    public static final DeferredBlock<Block> REPAIR_STATION = simple(
        "repair_station", 12, 50, 100, Technology.WORKSHOP
    );
    public static final DeferredBlock<Block> RESEARCH_CONSOLE = BLOCKS.register(
        "research_console",
        () -> new ResearchConsoleBlock(
            BlockBehaviour.Properties.of()
                .strength(12, 50)
                .lightLevel(state -> 5)
                .sound(SoundType.METAL)
        )
    );
    public static final DeferredBlock<Block> WORKSHOP = research(
        "workshop", 14, 60, 150, Technology.WORKSHOP
    );
    public static final DeferredBlock<Block> FACTORY = research(
        "factory", 18, 80, 300, Technology.FACTORY
    );
    public static final DeferredBlock<Block> RADAR = research(
        "radar", 10, 40, 240, Technology.RADAR
    );
    public static final DeferredBlock<Block> SHIELD_EMITTER = simple(
        "shield_emitter", 20, 100, 500, Technology.SHIELDS
    );

    private static DeferredBlock<Block> simple(
        String name,
        float strength,
        float resistance,
        int metalCost,
        Technology technology
    ) {
        return BLOCKS.register(
            name,
            () -> new FortsStructureBlock(
                BlockBehaviour.Properties.of()
                    .strength(strength, resistance)
                    .sound(SoundType.METAL),
                50,
                30,
                false,
                metalCost,
                technology
            )
        );
    }

    private static DeferredBlock<Block> research(
        String name,
        float strength,
        float resistance,
        int metalCost,
        Technology technology
    ) {
        return BLOCKS.register(
            name,
            () -> new ResearchBlock(
                BlockBehaviour.Properties.of()
                    .strength(strength, resistance)
                    .sound(SoundType.METAL),
                metalCost,
                technology
            )
        );
    }

    private static DeferredBlock<Block> structure(
        String name,
        float strength,
        float resistance,
        int support,
        int weight,
        int metalCost,
        Technology technology,
        SoundType sound
    ) {
        return BLOCKS.register(
            name,
            () -> new FortsStructureBlock(
                BlockBehaviour.Properties.of()
                    .strength(strength, resistance)
                    .sound(sound),
                support,
                weight,
                false,
                metalCost,
                technology
            )
        );
    }

    private static DeferredBlock<Block> anchoredStructure(
        String name,
        float strength,
        float resistance,
        int support,
        int weight,
        int metalCost,
        SoundType sound
    ) {
        return BLOCKS.register(
            name,
            () -> new FortsStructureBlock(
                BlockBehaviour.Properties.of()
                    .strength(strength, resistance)
                    .sound(sound),
                support,
                weight,
                true,
                metalCost,
                null
            )
        );
    }

    private ModBlocks() {}
}
