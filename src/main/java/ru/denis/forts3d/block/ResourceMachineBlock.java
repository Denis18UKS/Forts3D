package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.game.*;

public class ResourceMachineBlock extends FortsStructureBlock {
    public enum Kind { METAL, ENERGY, AMMO }
    private final Kind kind; private final int amount;
    public ResourceMachineBlock(Properties p,Kind kind,int amount){super(p,50,30);this.kind=kind;this.amount=amount;}
    @Override public void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState old,boolean moved){super.onPlace(state,level,pos,old,moved);if(level instanceof ServerLevel sl)sl.scheduleTick(pos,this,20);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){var m=FortsMatchManager.get(level.getServer());FortsTeam owner=nearestOwner(m,pos);var b=m.bank(owner);if(b!=null)switch(kind){case METAL->b.addMetal(amount);case ENERGY->b.addEnergy(amount);case AMMO->b.addAmmo(amount);}level.scheduleTick(pos,this,20);}
    private FortsTeam nearestOwner(FortsMatchManager m,BlockPos pos){return m.structures().ownerAt(pos);}
}
