package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Тепловой порт лейт-мультиблока: принимает тепло от теплотруб и держит его, как малый сжижитель.
 * Пока тепло подводится, уровень стоит; без притока остывает на 1 за тик.
 * Ядро только сравнивает уровень с порогом рецепта, тепло не тратит.
 */
public class HeatPortBlockEntity extends BlockEntity {

    public static final int MAX_HEAT = 10000;

    private int heat;
    private boolean receiving;

    private final IHeatHandler handler = new IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heat);
            if (!simulate && amount > 0) {
                receiving = true;   // полный порт под трубой тоже не остывает
                if (accepted > 0) { heat += accepted; setChanged(); }
            }
            return accepted;
        }
    };
    private LazyOptional<IHeatHandler> lazyHeat = LazyOptional.empty();

    public HeatPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_PORT_BE.get(), pos, state);
    }

    public int getHeat() { return heat; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPortBlockEntity be) {
        if (!be.receiving && be.heat > 0) { be.heat--; be.setChanged(); }
        be.receiving = false;
    }

    @Override public void onLoad() { super.onLoad(); lazyHeat = LazyOptional.of(() -> handler); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyHeat.invalidate(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT) return lazyHeat.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", heat);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getInt("heat");
    }
}
