package ru.denis.forts3d.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;

/**
 * One-block blast-door segment. Adjacent vertical segments open together so a
 * two-block doorway behaves as one Forts door.
 */
public final class BlastDoorBlock extends FortsStructureBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final MapCodec<BlastDoorBlock> CODEC = simpleCodec(BlastDoorBlock::new);

    public BlastDoorBlock(Properties properties) {
        super(properties, 80, 25, false, 40, null);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (level instanceof ServerLevel serverLevel) {
            FortsMatchManager manager = FortsMatchManager.get(serverLevel.getServer());
            FortsTeam owner = manager.structures().ownerAt(pos);
            FortsTeam playerTeam = manager.teamOf(player.getUUID());
            if (owner != playerTeam && !player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("§cЭто дверь другой команды"));
                return InteractionResult.FAIL;
            }

            boolean open = !state.getValue(OPEN);
            setDoorState(level, pos, state, open);
            for (BlockPos neighbor : new BlockPos[]{pos.above(), pos.below()}) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(this)) {
                    setDoorState(level, neighbor, neighborState, open);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void setDoorState(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
        level.levelEvent(null, open ? 1005 : 1011, pos, 0);
    }

    @Override
    protected VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return state.getValue(OPEN) ? box(0, 8, 0, 16, 16, 16) : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
