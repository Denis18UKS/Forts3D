package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.denis.forts3d.game.*;

public class ReactorCoreBlock extends FortsStructureBlock {
    public ReactorCoreBlock(Properties properties){super(properties,1000,100);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(level instanceof ServerLevel sl&&placer!=null){var m=FortsMatchManager.get(sl.getServer());m.registerReactor(m.teamOf(placer.getUUID()),pos);}}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide&&level instanceof ServerLevel sl){var m=FortsMatchManager.get(sl.getServer());var t=m.teamOf(player.getUUID());player.sendSystemMessage(Component.literal("§6Реактор "+t.id()+": §f"+m.reactorHealth(t)+" HP"));}return InteractionResult.sidedSuccess(level.isClientSide);}
}
