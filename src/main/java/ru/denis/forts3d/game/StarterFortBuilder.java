package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.registry.ModBlocks;

/** Builds the server-owned starter multiblock used when a match begins. */
public final class StarterFortBuilder {
    public static BlockPos build(ServerLevel level, BlockPos origin, FortsTeam team, StructureRegistry structures) {
        // 9x9 anchored foundation and a protected 5x5 reactor room.
        fill(level, structures, team, origin.offset(-4, 0, -4), origin.offset(4, 0, 4),
            ModBlocks.FOUNDATION.get(), 500, 100, true);
        fill(level, structures, team, origin.offset(-3, 1, -3), origin.offset(3, 1, 3),
            ModBlocks.METAL_BEAM.get(), 30, 8, false);

        for (int y = 2; y <= 5; y++) {
            for (int x = -3; x <= 3; x++) {
                place(level, structures, team, origin.offset(x, y, -3), ModBlocks.ARMOR_PLATE.get(), 70, 20, false);
                place(level, structures, team, origin.offset(x, y, 3), ModBlocks.ARMOR_PLATE.get(), 70, 20, false);
            }
            for (int z = -2; z <= 2; z++) {
                place(level, structures, team, origin.offset(-3, y, z), ModBlocks.ARMOR_PLATE.get(), 70, 20, false);
                place(level, structures, team, origin.offset(3, y, z), ModBlocks.ARMOR_PLATE.get(), 70, 20, false);
            }
        }
        fill(level, structures, team, origin.offset(-3, 6, -3), origin.offset(3, 6, 3),
            ModBlocks.METAL_BEAM.get(), 30, 8, false);

        BlockPos reactor = origin.offset(0, 2, 0);
        place(level, structures, team, reactor, ModBlocks.REACTOR_CORE.get(), 1000, 100, true);
        place(level, structures, team, origin.offset(-1, 2, 0), ModBlocks.BATTERY.get(), 50, 30, false);
        place(level, structures, team, origin.offset(1, 2, 0), ModBlocks.METAL_MINE.get(), 50, 30, false);
        place(level, structures, team, origin.offset(0, 2, 1), ModBlocks.MUNITIONS_PLANT.get(), 50, 30, false);
        place(level, structures, team, origin.offset(0, 2, -1), ModBlocks.WORKSHOP.get(), 50, 30, false);

        // Leave a two-block doorway on the side facing the battlefield.
        int doorX = team == FortsTeam.RED ? 3 : -3;
        level.removeBlock(origin.offset(doorX, 2, 0), false);
        level.removeBlock(origin.offset(doorX, 3, 0), false);
        structures.remove(origin.offset(doorX, 2, 0));
        structures.remove(origin.offset(doorX, 3, 0));
        return reactor;
    }

    private static void fill(ServerLevel level, StructureRegistry structures, FortsTeam team,
                             BlockPos from, BlockPos to, Block block, int support, int weight, boolean anchor) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            place(level, structures, team, pos, block, support, weight, anchor);
        }
    }

    private static void place(ServerLevel level, StructureRegistry structures, FortsTeam team,
                              BlockPos pos, Block block, int support, int weight, boolean anchor) {
        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        structures.add(pos, team, support, weight, anchor);
    }

    private StarterFortBuilder() {}
}
