package ru.denis.forts3d.registry;

import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import ru.denis.forts3d.Forts3D;
import ru.denis.forts3d.game.Technology;
import ru.denis.forts3d.item.*;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Forts3D.MOD_ID);
    public static final DeferredItem<Item> ADMIN_TABLET=ITEMS.register("admin_tablet",()->new AdminTabletItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TEAM_SELECTOR=ITEMS.register("team_selector",()->new TeamSelectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BUILD_TOOL=ITEMS.registerSimpleItem("build_tool",new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> REPAIR_TOOL=ITEMS.register("repair_tool",()->new RepairToolItem(new Item.Properties().durability(512)));
    public static final DeferredItem<Item> MACHINE_GUN=weapon("machine_gun",1,5,1,1.8f,Technology.WORKSHOP);
    public static final DeferredItem<Item> CANNON=weapon("cannon",6,40,3,1.25f,Technology.FACTORY);
    public static final DeferredItem<Item> MORTAR=weapon("mortar",4,25,2,0.75f,Technology.MUNITIONS);
    public static final DeferredItem<Item> ROCKET_LAUNCHER=weapon("rocket_launcher",10,80,5,1.0f,Technology.ROCKETRY);
    public static final DeferredItem<Item> LASER=weapon("laser",0,180,4,2.5f,Technology.LASERS);
    public static final DeferredItem<Item> FLAK=weapon("flak",3,30,2,1.6f,Technology.MUNITIONS);
    public static final DeferredItem<BlockItem> REACTOR_CORE=block(ModBlocks.REACTOR_CORE); public static final DeferredItem<BlockItem> WOOD_BEAM=block(ModBlocks.WOOD_BEAM);
    public static final DeferredItem<BlockItem> METAL_BEAM=block(ModBlocks.METAL_BEAM); public static final DeferredItem<BlockItem> ARMOR_PLATE=block(ModBlocks.ARMOR_PLATE);
    public static final DeferredItem<BlockItem> REINFORCED_GLASS=block(ModBlocks.REINFORCED_GLASS); public static final DeferredItem<BlockItem> FOUNDATION=block(ModBlocks.FOUNDATION);
    public static final DeferredItem<BlockItem> BLAST_DOOR=block(ModBlocks.BLAST_DOOR); public static final DeferredItem<BlockItem> METAL_MINE=block(ModBlocks.METAL_MINE);
    public static final DeferredItem<BlockItem> BATTERY=block(ModBlocks.BATTERY); public static final DeferredItem<BlockItem> MUNITIONS_PLANT=block(ModBlocks.MUNITIONS_PLANT);
    public static final DeferredItem<BlockItem> POWER_CABLE=block(ModBlocks.POWER_CABLE); public static final DeferredItem<BlockItem> REPAIR_STATION=block(ModBlocks.REPAIR_STATION);
    public static final DeferredItem<BlockItem> WORKSHOP=block(ModBlocks.WORKSHOP); public static final DeferredItem<BlockItem> FACTORY=block(ModBlocks.FACTORY);
    public static final DeferredItem<BlockItem> RADAR=block(ModBlocks.RADAR); public static final DeferredItem<BlockItem> SHIELD_EMITTER=block(ModBlocks.SHIELD_EMITTER);
    private static DeferredItem<Item> weapon(String n,int a,int e,int x,float v,Technology t){return ITEMS.register(n,()->new WeaponItem(new Item.Properties().stacksTo(1).durability(2048),a,e,x,v,t));}
    private static DeferredItem<BlockItem> block(DeferredBlock<? extends net.minecraft.world.level.block.Block> b){return ITEMS.registerSimpleBlockItem(b);}
    private ModItems(){}
}
