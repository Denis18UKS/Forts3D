package ru.denis.forts3d.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.denis.forts3d.config.FortsConfig;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;
import ru.denis.forts3d.game.MatchState;

/** Hand-operated 3D weapon controller backed by the team's economy. */
public final class WeaponItem extends Item {
    private final WeaponKind kind;

    public WeaponItem(Properties properties, WeaponKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        net.minecraft.world.entity.player.Player player,
        InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        FortsMatchManager manager = FortsMatchManager.get(serverPlayer.server);
        if (manager.state() != MatchState.RUNNING) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cОружие доступно только во время матча"));
            return InteractionResultHolder.fail(stack);
        }

        FortsTeam team = manager.teamOf(serverPlayer.getUUID());
        var bank = manager.bank(team);
        if (bank == null) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cСначала вступите в команду"));
            return InteractionResultHolder.fail(stack);
        }
        if (FortsConfig.REQUIRE_TECH.get() && !bank.has(kind.technology())) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cНе изучена технология: " + kind.technology().title()));
            return InteractionResultHolder.fail(stack);
        }
        if (!manager.canRegisterProjectiles(kind.projectiles())) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cДостигнут серверный лимит активных снарядов"));
            return InteractionResultHolder.fail(stack);
        }
        if (!bank.spend(0, kind.energyCost(), kind.ammoCost())) {
            serverPlayer.sendSystemMessage(Component.literal(
                "§cНедостаточно энергии или боеприпасов"));
            return InteractionResultHolder.fail(stack);
        }

        Vec3 look = serverPlayer.getLookAngle().normalize();
        for (int index = 0; index < kind.projectiles(); index++) {
            Vec3 direction = spread(look, serverPlayer, kind.spread());
            if (kind == WeaponKind.MORTAR) {
                direction = direction.add(0, 0.42, 0).normalize();
            }
            LargeFireball projectile = new LargeFireball(
                level,
                serverPlayer,
                direction,
                kind.explosionPower()
            );
            projectile.setPos(
                serverPlayer.getX() + direction.x * 1.5,
                serverPlayer.getEyeY() - 0.1 + direction.y * 1.5,
                serverPlayer.getZ() + direction.z * 1.5
            );
            projectile.setDeltaMovement(direction.scale(kind.velocity()));
            if (!manager.registerProjectile(
                projectile,
                team,
                kind.structureDamage(),
                kind.reactorDamage(),
                kind.damageRadius(),
                kind.gravity()
            )) {
                projectile.discard();
                continue;
            }
            level.addFreshEntity(projectile);
        }

        serverPlayer.getCooldowns().addCooldown(this, kind.cooldownTicks());
        stack.hurtAndBreak(
            1,
            serverPlayer,
            hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND
        );
        return InteractionResultHolder.consume(stack);
    }

    private static Vec3 spread(
        Vec3 direction,
        ServerPlayer player,
        double amount
    ) {
        if (amount <= 0) {
            return direction;
        }
        return direction.add(
            (player.getRandom().nextDouble() - 0.5) * amount,
            (player.getRandom().nextDouble() - 0.5) * amount,
            (player.getRandom().nextDouble() - 0.5) * amount
        ).normalize();
    }
}
