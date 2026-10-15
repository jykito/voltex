package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Late multiblock heat port: takes heat from heat pipes and holds it, like the small liquefier.
 * While heat is supplied the level holds; without inflow it cools by 1 per tick.
 * The core only compares the level with the recipe threshold, it does not spend heat.
 */
public class HeatPortBlockEntity extends BlockEntity implements Caps.Provider  {

    public static final int MAX_HEAT = 10000;

    private int heat;
    private boolean receiving;

    private final IHeatHandler handler = new IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heat);
            if (!simulate && amount > 0) {
                receiving = true;   // a full port under a pipe does not cool either
                if (accepted > 0) { heat += accepted; setChanged(); }
            }
            return accepted;
        }
    };

    public HeatPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_PORT_BE.get(), pos, state);
    }

    public int getHeat() { return heat; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPortBlockEntity be) {
        if (!be.receiving && be.heat > 0) { be.heat--; be.setChanged(); }
        be.receiving = false;
    }


    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT) return (T) handler;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heat);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
    }
}
