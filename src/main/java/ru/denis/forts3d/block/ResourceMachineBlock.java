package ru.denis.forts3d.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;
import ru.denis.forts3d.game.MatchState;
import ru.denis.forts3d.game.Technology;

/** A structure machine which produces into its owning team's shared bank. */
public class ResourceMachineBlock extends FortsStructureBlock {
    public enum Kind {
        METAL,
        ENERGY,
        AMMO
    }

    private final Kind kind;
    private final int amount;

    public ResourceMachineBlock(
        Properties properties,
        Kind kind,
        int amount,
        int metalCost,
        Technology requiredTechnology
    ) {
        super(properties, 50, 30, false, metalCost, requiredTechnology);
        this.kind = kind;
        this.amount = Math.max(0, amount);
    }

    @Override
    protected void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !state.is(oldState.getBlock())) {
            serverLevel.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FortsMatchManager manager = FortsMatchManager.get(level.getServer());
        FortsTeam owner = manager.structures().ownerAt(pos);
        var bank = manager.bank(owner);
        if (manager.state() == MatchState.RUNNING
            && bank != null
            && manager.structures().isAnchored(pos)) {
            switch (kind) {
                case METAL -> bank.addMetal(amount);
                case ENERGY -> bank.addEnergy(amount);
                case AMMO -> bank.addAmmo(amount);
            }
        }
        level.scheduleTick(pos, this, 20);
    }
}
