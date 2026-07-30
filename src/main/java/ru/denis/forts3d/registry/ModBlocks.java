package ru.denis.forts3d.registry;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
import ru.denis.forts3d.Forts3D;
import ru.denis.forts3d.block.*;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Forts3D.MOD_ID);
    public static final DeferredBlock<Block> REACTOR_CORE=BLOCKS.register("reactor_core",()->new ReactorCoreBlock(BlockBehaviour.Properties.of().strength(20f,1200f).lightLevel(s->10).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> WOOD_BEAM=structure("wood_beam",4,8,8,2,SoundType.WOOD);
    public static final DeferredBlock<Block> METAL_BEAM=structure("metal_beam",12,40,30,8,SoundType.METAL);
    public static final DeferredBlock<Block> ARMOR_PLATE=structure("armor_plate",30,120,70,20,SoundType.METAL);
    public static final DeferredBlock<Block> REINFORCED_GLASS=structure("reinforced_glass",8,60,15,12,SoundType.GLASS);
    public static final DeferredBlock<Block> FOUNDATION=anchoredStructure("foundation",50,1200,500,100,SoundType.STONE);
    public static final DeferredBlock<Block> BLAST_DOOR=BLOCKS.registerSimpleBlock("blast_door",BlockBehaviour.Properties.of().strength(25,200).sound(SoundType.METAL));
    public static final DeferredBlock<Block> METAL_MINE=BLOCKS.register("metal_mine",()->new ResourceMachineBlock(BlockBehaviour.Properties.of().strength(12,40).sound(SoundType.METAL),ResourceMachineBlock.Kind.METAL,5));
    public static final DeferredBlock<Block> BATTERY=BLOCKS.register("battery",()->new ResourceMachineBlock(BlockBehaviour.Properties.of().strength(8,30).sound(SoundType.METAL),ResourceMachineBlock.Kind.ENERGY,12));
    public static final DeferredBlock<Block> MUNITIONS_PLANT=BLOCKS.register("munitions_plant",()->new ResourceMachineBlock(BlockBehaviour.Properties.of().strength(12,50).sound(SoundType.METAL),ResourceMachineBlock.Kind.AMMO,4));
    public static final DeferredBlock<Block> POWER_CABLE=structure("power_cable",3,10,5,1,SoundType.COPPER);
    public static final DeferredBlock<Block> REPAIR_STATION=simple("repair_station",12,50);
    public static final DeferredBlock<Block> WORKSHOP=simple("workshop",14,60);
    public static final DeferredBlock<Block> FACTORY=simple("factory",18,80);
    public static final DeferredBlock<Block> RADAR=simple("radar",10,40);
    public static final DeferredBlock<Block> SHIELD_EMITTER=simple("shield_emitter",20,100);
    private static DeferredBlock<Block> simple(String n,float s,float r){return BLOCKS.register(n,()->new FortsStructureBlock(BlockBehaviour.Properties.of().strength(s,r).sound(SoundType.METAL),50,30));}
    private static DeferredBlock<Block> structure(String n,float s,float r,int support,int weight,SoundType sound){return BLOCKS.register(n,()->new FortsStructureBlock(BlockBehaviour.Properties.of().strength(s,r).sound(sound),support,weight));}
    private static DeferredBlock<Block> anchoredStructure(String n,float s,float r,int support,int weight,SoundType sound){return BLOCKS.register(n,()->new FortsStructureBlock(BlockBehaviour.Properties.of().strength(s,r).sound(sound),support,weight,true));}
    private ModBlocks(){}
}
