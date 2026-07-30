package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.Technology;

/** Research station which unlocks its technology when an owner interacts with it. */
public final class ResearchBlock extends FortsStructureBlock {
    private final Technology technology;

    public ResearchBlock(Properties properties, int metalCost, Technology technology) {
        super(properties, 70, 35, false, metalCost, technology.prerequisite());
        this.technology = technology;
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
            var bank = manager.bank(manager.teamOf(player.getUUID()));
            if (bank == null) {
                player.sendSystemMessage(Component.literal("§cСначала вступите в команду"));
            } else if (bank.has(technology)) {
                player.sendSystemMessage(Component.literal("§eТехнология уже изучена: " + technology.title()));
            } else if (!bank.canUnlock(technology)) {
                player.sendSystemMessage(Component.literal(
                    "§cНужно: " + technology.metal() + " металла, " + technology.energy()
                        + " энергии и предыдущая технология"));
            } else if (bank.unlock(technology)) {
                player.sendSystemMessage(Component.literal("§aИзучена технология: " + technology.title()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
