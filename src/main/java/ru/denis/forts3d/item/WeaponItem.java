package ru.denis.forts3d.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.denis.forts3d.game.*;

public class WeaponItem extends Item {
    private final int ammo, energy, explosion; private final float velocity; private final Technology technology;
    public WeaponItem(Properties p,int ammo,int energy,int explosion,float velocity,Technology technology){super(p);this.ammo=ammo;this.energy=energy;this.explosion=explosion;this.velocity=velocity;this.technology=technology;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){ItemStack stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);if(!(player instanceof ServerPlayer sp))return InteractionResultHolder.fail(stack);var m=FortsMatchManager.get(sp.server);if(m.state()!=MatchState.RUNNING){sp.sendSystemMessage(Component.literal("§cОружие доступно только во время матча"));return InteractionResultHolder.fail(stack);}var team=m.teamOf(sp.getUUID());var bank=m.bank(team);if(bank==null){sp.sendSystemMessage(Component.literal("§cСначала войдите в команду"));return InteractionResultHolder.fail(stack);}if(technology!=null&&!bank.has(technology)){sp.sendSystemMessage(Component.literal("§cНе изучена технология: "+technology));return InteractionResultHolder.fail(stack);}if(!bank.spend(0,energy,ammo)){sp.sendSystemMessage(Component.literal("§cНедостаточно энергии или боеприпасов"));return InteractionResultHolder.fail(stack);}Vec3 look=sp.getLookAngle();LargeFireball shot=new LargeFireball(level,sp,look.normalize(),explosion);shot.setPos(sp.getX()+look.x*1.5,sp.getEyeY()-0.1+look.y*1.5,sp.getZ()+look.z*1.5);shot.setDeltaMovement(look.scale(velocity));level.addFreshEntity(shot);sp.getCooldowns().addCooldown(this,Math.max(4,explosion*3));return InteractionResultHolder.consume(stack);}
}
