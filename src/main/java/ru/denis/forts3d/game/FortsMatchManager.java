package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.denis.forts3d.config.FortsConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FortsMatchManager {
    private static final Map<MinecraftServer,FortsMatchManager> INSTANCES=new ConcurrentHashMap<>();
    public static FortsMatchManager get(MinecraftServer server){return INSTANCES.computeIfAbsent(server,s->new FortsMatchManager());}
    public static void remove(MinecraftServer server){INSTANCES.remove(server);}
    private MatchState state=MatchState.IDLE; private int remainingTicks; private long ticks;
    private final EnumMap<FortsTeam,TeamResourceBank> banks=new EnumMap<>(FortsTeam.class);
    private final Map<UUID,FortsTeam> players=new ConcurrentHashMap<>();
    private final EnumMap<FortsTeam,BlockPos> reactors=new EnumMap<>(FortsTeam.class);
    private final EnumMap<FortsTeam,Integer> reactorHealth=new EnumMap<>(FortsTeam.class);
    private final StructureRegistry structures=new StructureRegistry();
    public MatchState state(){return state;} public int remainingTicks(){return remainingTicks;} public long ticks(){return ticks;}
    public Map<FortsTeam,TeamResourceBank> banks(){return Collections.unmodifiableMap(banks);} public StructureRegistry structures(){return structures;}
    public FortsTeam teamOf(UUID id){return players.getOrDefault(id,FortsTeam.SPECTATOR);} public TeamResourceBank bank(FortsTeam t){return banks.get(t);}
    public void create(){state=MatchState.PREPARING;ticks=0;players.clear();reactors.clear();reactorHealth.clear();banks.clear();
        banks.put(FortsTeam.RED,new TeamResourceBank(FortsConfig.START_METAL.get(),FortsConfig.START_ENERGY.get(),10000));
        banks.put(FortsTeam.BLUE,new TeamResourceBank(FortsConfig.START_METAL.get(),FortsConfig.START_ENERGY.get(),10000));}
    public boolean join(ServerPlayer p,FortsTeam t){if(t==FortsTeam.SPECTATOR){players.put(p.getUUID(),t);return true;} long count=players.values().stream().filter(v->v==t).count(); if(count>=FortsConfig.MAX_TEAM_SIZE.get())return false;players.put(p.getUUID(),t);return true;}
    public void registerReactor(FortsTeam team,BlockPos pos){reactors.put(team,pos.immutable());reactorHealth.put(team,FortsConfig.REACTOR_HEALTH.get());}
    public void damageReactor(MinecraftServer server,FortsTeam team,int damage){int hp=Math.max(0,reactorHealth.getOrDefault(team,FortsConfig.REACTOR_HEALTH.get())-damage);reactorHealth.put(team,hp);if(hp==0)win(server,team==FortsTeam.RED?FortsTeam.BLUE:FortsTeam.RED);}
    public int reactorHealth(FortsTeam t){return reactorHealth.getOrDefault(t,0);}
    public void start(MinecraftServer server){if(state==MatchState.IDLE||state==MatchState.FINISHED)create();state=MatchState.RUNNING;remainingTicks=FortsConfig.MATCH_TIME_SECONDS.get()*20;broadcast(server,"§6[Forts 3D] Матч начался");}
    public void stop(MinecraftServer server){state=MatchState.FINISHED;broadcast(server,"§6[Forts 3D] Матч завершён");}
    public void pause(){state=state==MatchState.PAUSED?MatchState.RUNNING:MatchState.PAUSED;}
    public void win(MinecraftServer server,FortsTeam team){state=MatchState.FINISHED;broadcast(server,team.color()+"Команда "+team.id()+" победила!");}
    public void tick(MinecraftServer server){if(state!=MatchState.RUNNING)return;ticks++;if(--remainingTicks<=0){stop(server);return;}
        if(ticks%FortsConfig.RESOURCE_TICK_RATE.get()==0){double m=FortsConfig.RESOURCE_MULTIPLIER.get();for(var b:banks.values()){b.addEnergy(Math.round(5*m));}}
        if(FortsConfig.STRUCTURAL_COLLAPSE.get() && ticks%FortsConfig.STRUCTURE_TICK_RATE.get()==0){for(BlockPos pos:structures.unsupported()){var level=server.overworld();if(level.isLoaded(pos)){level.destroyBlock(pos,true);structures.remove(pos);}}}
    }
    private void broadcast(MinecraftServer s,String msg){s.getPlayerList().broadcastSystemMessage(Component.literal(msg),false);}
}
