package ru.denis.forts3d.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;

public final class TeamSelectorItem extends Item {
    public TeamSelectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        net.minecraft.world.entity.player.Player player,
        InteractionHand hand
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            FortsMatchManager manager = FortsMatchManager.get(serverPlayer.server);
            FortsTeam next = manager.teamOf(serverPlayer.getUUID()) == FortsTeam.RED
                ? FortsTeam.BLUE
                : FortsTeam.RED;
            manager.join(serverPlayer, next);
        }
        return InteractionResultHolder.sidedSuccess(
            player.getItemInHand(hand),
            level.isClientSide
        );
    }
}
