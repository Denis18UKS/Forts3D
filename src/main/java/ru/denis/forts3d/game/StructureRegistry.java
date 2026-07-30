package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight structural graph for one active match.
 *
 * <p>Nodes are joined through the six block faces. Every connected component must
 * reach an anchor and have enough total support for its weight. Integrity is kept
 * outside block states, allowing gradual artillery damage and repairs without
 * thousands of block-state variants.</p>
 */
public final class StructureRegistry {
    public record Node(
        BlockPos pos,
        FortsTeam team,
        int support,
        int weight,
        boolean anchor,
        int metalCost,
        int maxIntegrity,
        int damage
    ) {
        public int integrity() {
            return Math.max(0, maxIntegrity - damage);
        }

        public Node withDamage(int newDamage) {
            return new Node(
                pos,
                team,
                support,
                weight,
                anchor,
                metalCost,
                maxIntegrity,
                Math.max(0, Math.min(maxIntegrity, newDamage))
            );
        }
    }

    private final Map<BlockPos, Node> nodes = new ConcurrentHashMap<>();

    public void add(
        BlockPos pos,
        FortsTeam team,
        int support,
        int weight,
        boolean anchor,
        int metalCost,
        int maxIntegrity
    ) {
        BlockPos immutable = pos.immutable();
        nodes.put(immutable, new Node(
            immutable,
            team,
            Math.max(0, support),
            Math.max(0, weight),
            anchor,
            Math.max(0, metalCost),
            Math.max(1, maxIntegrity),
            0
        ));
    }

    public void remove(BlockPos pos) {
        nodes.remove(pos);
    }

    public void clear() {
        nodes.clear();
    }

    public Collection<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    public Node nodeAt(BlockPos pos) {
        return nodes.get(pos);
    }

    public FortsTeam ownerAt(BlockPos pos) {
        Node node = nodes.get(pos);
        return node == null ? FortsTeam.SPECTATOR : node.team();
    }

    public int damage(BlockPos pos, int amount) {
        if (amount <= 0) {
            Node current = nodes.get(pos);
            return current == null ? -1 : current.integrity();
        }
        Node updated = nodes.computeIfPresent(pos, (ignored, current) ->
            current.withDamage(current.damage() + amount));
        return updated == null ? -1 : updated.integrity();
    }

    public int repair(BlockPos pos, int amount) {
        if (amount <= 0) {
            Node current = nodes.get(pos);
            return current == null ? -1 : current.integrity();
        }
        Node updated = nodes.computeIfPresent(pos, (ignored, current) ->
            current.withDamage(current.damage() - amount));
        return updated == null ? -1 : updated.integrity();
    }

    public boolean isAnchored(BlockPos pos) {
        Node start = nodes.get(pos);
        if (start == null) {
            return false;
        }
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        visited.add(start.pos());
        queue.add(start);
        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (current.anchor()) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                Node neighbor = nodes.get(current.pos().relative(direction));
                if (neighbor != null
                    && neighbor.team() == start.team()
                    && visited.add(neighbor.pos())) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    public List<BlockPos> unsupported() {
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();
        for (Node start : nodes.values()) {
            if (!visited.add(start.pos())) {
                continue;
            }
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
                for (Direction direction : Direction.values()) {
                    Node neighbor = nodes.get(current.pos().relative(direction));
                    if (neighbor != null
                        && neighbor.team() == current.team()
                        && visited.add(neighbor.pos())) {
                        queue.add(neighbor);
                    }
                }
            }
            if (!anchored || support < weight) {
                component.forEach(node -> result.add(node.pos()));
            }
        }
        return result;
    }
}
