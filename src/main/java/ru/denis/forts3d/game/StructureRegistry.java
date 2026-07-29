package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureRegistry {
    public record Node(BlockPos pos, FortsTeam team, int support, int weight) {}
    private final Map<BlockPos, Node> nodes = new ConcurrentHashMap<>();
    public void add(BlockPos pos,FortsTeam team,int support,int weight){nodes.put(pos.immutable(),new Node(pos.immutable(),team,support,weight));}
    public void remove(BlockPos pos){nodes.remove(pos);}
    public Collection<Node> nodes(){return List.copyOf(nodes.values());}
    public List<BlockPos> unsupported(){
        List<BlockPos> result=new ArrayList<>();
        for(Node n:nodes.values()){
            int supports=0;
            for(var d:net.minecraft.core.Direction.values()) if(nodes.containsKey(n.pos.relative(d))) supports++;
            if(n.pos.getY()>-64 && supports==0) result.add(n.pos);
        }
        return result;
    }
}
