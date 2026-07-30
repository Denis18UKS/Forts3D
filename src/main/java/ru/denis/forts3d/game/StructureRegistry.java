package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureRegistry {
    public record Node(BlockPos pos, FortsTeam team, int support, int weight, boolean anchor) {}
    private final Map<BlockPos, Node> nodes = new ConcurrentHashMap<>();
    public void add(BlockPos pos,FortsTeam team,int support,int weight,boolean anchor){nodes.put(pos.immutable(),new Node(pos.immutable(),team,support,weight,anchor));}
    public void remove(BlockPos pos){nodes.remove(pos);}
    public void clear(){nodes.clear();}
    public Collection<Node> nodes(){return List.copyOf(nodes.values());}
    public FortsTeam ownerAt(BlockPos pos){Node node=nodes.get(pos);return node==null?FortsTeam.SPECTATOR:node.team();}
    public List<BlockPos> unsupported(){
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();
        for (Node start : nodes.values()) {
            if (!visited.add(start.pos())) continue;
            ArrayDeque<Node> queue = new ArrayDeque<>();
            List<Node> component = new ArrayList<>();
            queue.add(start);
            boolean anchored = false;
            long support = 0;
            long weight = 0;
            while (!queue.isEmpty()) {
                Node current = queue.removeFirst();
                component.add(current);
                anchored |= current.anchor();
                support += current.support();
                weight += current.weight();
                for (var direction : net.minecraft.core.Direction.values()) {
                    Node neighbor = nodes.get(current.pos().relative(direction));
                    if (neighbor != null && neighbor.team() == current.team() && visited.add(neighbor.pos())) queue.add(neighbor);
                }
            }
            if (!anchored || support < weight) component.forEach(node -> result.add(node.pos()));
        }
        return result;
    }
}
