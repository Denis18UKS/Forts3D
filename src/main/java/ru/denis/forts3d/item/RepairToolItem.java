package ru.denis.forts3d.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import ru.denis.forts3d.game.FortsMatchManager;

public final class RepairToolItem extends Item {
    public RepairToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level
            && context.getPlayer() instanceof ServerPlayer player) {
            BlockPos pos = context.getClickedPos();
            int integrity = FortsMatchManager.get(level.getServer()).repair(
                player,
                pos,
                100
            );
            if (integrity >= 0) {
                level.levelEvent(2005, pos, 0);
                context.getItemInHand().hurtAndBreak(
                    1,
                    player,
                    context.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND
                );
                player.sendSystemMessage(Component.literal(
                    "§aКонструкция отремонтирована. Прочность: " + integrity));
            } else if (integrity == -2) {
                player.sendSystemMessage(Component.literal(
                    "§cНедостаточно металла для ремонта"));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§cМожно ремонтировать только конструкции своей команды"));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
