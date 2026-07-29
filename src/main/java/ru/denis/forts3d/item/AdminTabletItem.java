package ru.denis.forts3d.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import ru.denis.forts3d.game.*;

public class AdminTabletItem extends Item {
    public AdminTabletItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer sp){if(!sp.hasPermissions(2))sp.sendSystemMessage(Component.literal("§cНужен уровень оператора 2"));else{var m=FortsMatchManager.get(sp.server);sp.sendSystemMessage(Component.literal("§6═══ Forts 3D Admin GUI ═══"));sp.sendSystemMessage(Component.literal("§eСостояние: §f"+m.state()+"  §eВремя: §f"+m.remainingTicks()/20+"с"));for(FortsTeam t:new FortsTeam[]{FortsTeam.RED,FortsTeam.BLUE}){var b=m.bank(t);if(b!=null)sp.sendSystemMessage(Component.literal(t.color()+t.id()+" §fM:"+b.metal()+" E:"+b.energy()+" A:"+b.ammo()+" Reactor:"+m.reactorHealth(t)));}sp.sendSystemMessage(Component.literal("§a/forts match start  §e/forts match pause  §c/forts match stop"));sp.sendSystemMessage(Component.literal("§b/forts config list  §d/forts resource  §6/forts tech"));}}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
