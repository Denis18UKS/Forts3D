package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;
import ru.denis.forts3d.game.Technology;

/**
 * Common base for every block which participates in a Forts structure.
 *
 * <p>The block owns the authoritative build price and structural values. Placement
 * validation happens on the server after vanilla selected the final position. If
 * placement is rejected the consumed stack is restored and the block is removed.
 * Starter forts are placed directly by the server and therefore bypass the economy.</p>
 */
public class FortsStructureBlock extends Block {
    private final int support;
    private final int weight;
    private final int metalCost;
    private final int maxIntegrity;
    private final boolean anchor;
    private final Technology requiredTechnology;

    public FortsStructureBlock(Properties properties, int support, int weight) {
        this(properties, support, weight, false, Math.max(1, weight), null);
    }

    public FortsStructureBlock(Properties properties, int support, int weight, boolean anchor) {
        this(properties, support, weight, anchor, Math.max(1, weight), null);
    }

    public FortsStructureBlock(
        Properties properties,
        int support,
        int weight,
        boolean anchor,
        int metalCost,
        Technology requiredTechnology
    ) {
        super(properties);
        this.support = Math.max(0, support);
        this.weight = Math.max(0, weight);
        this.anchor = anchor;
        this.metalCost = Math.max(0, metalCost);
        this.requiredTechnology = requiredTechnology;
        this.maxIntegrity = Math.max(20, support * 4 + weight * 6);
    }

    public int support() {
        return support;
    }

    public int weight() {
        return weight;
    }

    public int metalCost() {
        return metalCost;
    }

    public int maxIntegrity() {
        return maxIntegrity;
    }

    public boolean anchor() {
        return anchor;
    }

    public Technology requiredTechnology() {
        return requiredTechnology;
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        FortsMatchManager manager = FortsMatchManager.get(serverLevel.getServer());
        FortsTeam team = placer == null
            ? FortsTeam.SPECTATOR
            : manager.teamOf(placer.getUUID());

        if (placer instanceof ServerPlayer player) {
            Component rejection = manager.validateAndPayForPlacement(player, pos, this);
            if (rejection != null) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.grow(1);
                }
                player.sendSystemMessage(rejection);
                return;
            }
        }

        manager.structures().add(
            pos,
            team,
            support,
            weight,
            anchor,
            metalCost,
            maxIntegrity
        );
    }

    @Override
    protected void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState nextState,
        boolean movedByPiston
    ) {
        if (level instanceof ServerLevel serverLevel && !state.is(nextState.getBlock())) {
            FortsMatchManager.get(serverLevel.getServer()).structures().remove(pos);
        }
        super.onRemove(state, level, pos, nextState, movedByPiston);
    }
}
