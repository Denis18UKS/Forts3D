package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;

public final class ReactorCoreBlock extends FortsStructureBlock {
    public ReactorCoreBlock(Properties properties) {
        super(properties, 1000, 100, true, 1000, null);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer != null) {
            FortsMatchManager manager = FortsMatchManager.get(serverLevel.getServer());
            FortsTeam team = manager.teamOf(placer.getUUID());
            if (manager.structures().nodeAt(pos) != null) {
                manager.registerReactor(team, pos);
            }
        }
    }

    @Override
    protected void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState nextState,
        boolean movedByPiston
    ) {
        if (level instanceof ServerLevel serverLevel && !state.is(nextState.getBlock())) {
            FortsMatchManager.get(serverLevel.getServer()).reactorRemoved(
                serverLevel.getServer(),
                pos
            );
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
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
            player.sendSystemMessage(Component.literal(
                "§6Реактор " + owner.id() + ": §f"
                    + manager.reactorHealth(owner) + " HP"
            ));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
