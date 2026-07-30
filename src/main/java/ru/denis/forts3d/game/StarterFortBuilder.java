package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.block.FortsStructureBlock;
import ru.denis.forts3d.registry.ModBlocks;

/** Builds a compact, playable starter fortress for a team. */
public final class StarterFortBuilder {
    public static BlockPos build(
        ServerLevel level,
        BlockPos origin,
        FortsTeam team,
        StructureRegistry structures
    ) {
        fill(
            level,
            structures,
            team,
            origin.offset(-4, 0, -4),
            origin.offset(4, 0, 4),
            ModBlocks.FOUNDATION.get()
        );
        fill(
            level,
            structures,
            team,
            origin.offset(-3, 1, -3),
            origin.offset(3, 1, 3),
            ModBlocks.METAL_BEAM.get()
        );

        for (int y = 2; y <= 5; y++) {
            for (int x = -3; x <= 3; x++) {
                place(
                    level,
                    structures,
                    team,
                    origin.offset(x, y, -3),
                    ModBlocks.ARMOR_PLATE.get()
                );
                place(
                    level,
                    structures,
                    team,
                    origin.offset(x, y, 3),
                    ModBlocks.ARMOR_PLATE.get()
                );
            }
            for (int z = -2; z <= 2; z++) {
                place(
                    level,
                    structures,
                    team,
                    origin.offset(-3, y, z),
                    ModBlocks.ARMOR_PLATE.get()
                );
                place(
                    level,
                    structures,
                    team,
                    origin.offset(3, y, z),
                    ModBlocks.ARMOR_PLATE.get()
                );
            }
        }
        fill(
            level,
            structures,
            team,
            origin.offset(-3, 6, -3),
            origin.offset(3, 6, 3),
            ModBlocks.METAL_BEAM.get()
        );

        BlockPos reactor = origin.offset(0, 2, 0);
        place(level, structures, team, reactor, ModBlocks.REACTOR_CORE.get());
        place(
            level,
            structures,
            team,
            origin.offset(-1, 2, 0),
            ModBlocks.BATTERY.get()
        );
        place(
            level,
            structures,
            team,
            origin.offset(1, 2, 0),
            ModBlocks.METAL_MINE.get()
        );
        place(
            level,
            structures,
            team,
            origin.offset(0, 2, 1),
            ModBlocks.MUNITIONS_PLANT.get()
        );
        place(
            level,
            structures,
            team,
            origin.offset(0, 2, -1),
            ModBlocks.WORKSHOP.get()
        );

        // A paired blast door faces the opposing base.
        int doorX = team == FortsTeam.RED ? 3 : -3;
        place(
            level,
            structures,
            team,
            origin.offset(doorX, 2, 0),
            ModBlocks.BLAST_DOOR.get()
        );
        place(
            level,
            structures,
            team,
            origin.offset(doorX, 3, 0),
            ModBlocks.BLAST_DOOR.get()
        );
        return reactor;
    }

    private static void fill(
        ServerLevel level,
        StructureRegistry structures,
        FortsTeam team,
        BlockPos from,
        BlockPos to,
        Block block
    ) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            place(level, structures, team, pos, block);
        }
    }

    private static void place(
        ServerLevel level,
        StructureRegistry structures,
        FortsTeam team,
        BlockPos pos,
        Block block
    ) {
        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (block instanceof FortsStructureBlock structureBlock) {
            structures.add(
                pos,
                team,
                structureBlock.support(),
                structureBlock.weight(),
                structureBlock.anchor(),
                structureBlock.metalCost(),
                structureBlock.maxIntegrity()
            );
        }
    }

    private StarterFortBuilder() {}
}
