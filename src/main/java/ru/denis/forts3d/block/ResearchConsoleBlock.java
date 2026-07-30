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

/** Gives players an in-world way to progress through the complete research tree. */
public final class ResearchConsoleBlock extends FortsStructureBlock {
    public ResearchConsoleBlock(Properties properties) {
        super(properties, 60, 25, false, 120, null);
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
                player.sendSystemMessage(Component.literal(
                    "§cСначала вступите в команду"));
                return InteractionResult.FAIL;
            }

            Technology candidate = null;
            for (Technology technology : Technology.values()) {
                if (bank.has(technology)) {
                    continue;
                }
                Technology prerequisite = technology.prerequisite();
                if (prerequisite == null || bank.has(prerequisite)) {
                    candidate = technology;
                    break;
                }
            }
            if (candidate == null) {
                player.sendSystemMessage(Component.literal(
                    "§aВсе технологии уже изучены"));
            } else if (bank.unlock(candidate)) {
                player.sendSystemMessage(Component.literal(
                    "§aИзучена технология: " + candidate.title()));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§cДля «" + candidate.title() + "» требуется "
                        + candidate.metal() + " металла и "
                        + candidate.energy() + " энергии"));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
