package ru.denis.forts3d.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import ru.denis.forts3d.game.FortsMatchManager;

/** Inspector and construction help tool. Actual blocks remain placeable normally. */
public final class BuildToolItem extends Item {
    public BuildToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level
            && context.getPlayer() instanceof ServerPlayer player) {
            var node = FortsMatchManager.get(level.getServer())
                .structures()
                .nodeAt(context.getClickedPos());
            if (node == null) {
                player.sendSystemMessage(Component.literal(
                    "§7Этот блок не входит в конструкцию Forts"));
            } else {
                player.sendSystemMessage(Component.literal(
                    node.team().color()
                        + "Владелец: " + node.team().id()
                        + " §f| Прочность: " + node.integrity() + "/" + node.maxIntegrity()
                        + " | Опора: " + node.support()
                        + " | Вес: " + node.weight()
                ));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        net.minecraft.world.entity.player.Player player,
        InteractionHand hand
    ) {
        if (!level.isClientSide) {
            player.sendSystemMessage(Component.literal(
                "§6Forts 3D: §fставьте блоки из вкладки мода рядом со своей базой; "
                    + "инструмент показывает их прочность"));
        }
        return InteractionResultHolder.sidedSuccess(
            player.getItemInHand(hand),
            level.isClientSide
        );
    }
}
