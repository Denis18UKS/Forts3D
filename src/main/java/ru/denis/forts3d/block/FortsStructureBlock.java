package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.game.*;

public class FortsStructureBlock extends Block {
    private final int support, weight;
    public FortsStructureBlock(Properties p,int support,int weight){super(p);this.support=support;this.weight=weight;}
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(level instanceof ServerLevel sl){FortsTeam t=placer==null?FortsTeam.SPECTATOR:FortsMatchManager.get(sl.getServer()).teamOf(placer.getUUID());FortsMatchManager.get(sl.getServer()).structures().add(pos,t,support,weight);}}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){if(level instanceof ServerLevel sl&&!state.is(next.getBlock()))FortsMatchManager.get(sl.getServer()).structures().remove(pos);super.onRemove(state,level,pos,next,moved);}
}
