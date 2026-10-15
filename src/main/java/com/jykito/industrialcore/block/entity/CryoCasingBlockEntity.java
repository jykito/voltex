package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cryo chamber casing as a conductor for the core.
 * <p>
 * The core needs heat, coolant, spent coolant, product, energy and items; six
 * faces are not enough, and the third stage alone needs five pipe lines for heat.
 * So every casing block exposes the core's capability outside: the floor and ceiling give about fifty
 * faces, connect to any of them.
 * <p>
 * The entity deliberately <b>does not tick</b> and stores nothing; it only forwards. The core
 * writes its position itself while checking the structure: it visits every cell anyway, a second search
 * is not needed. While the chamber is not formed, {@code corePos} is empty and the casing exposes nothing, so a pipe
 * or cable will not connect to an unfinished box, and you can see right away that the structure
 * has not come together yet.
 */
public class CryoCasingBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos corePos;

    public CryoCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYO_CASING_BE.get(), pos, state);
    }

    /** Called by the core after a successful structure check. */
    public void setCore(BlockPos pos) {
        if (pos.equals(corePos)) return;
        corePos = pos.immutable();
        setChanged();
    }

    public void clearCore() {
        if (corePos == null) return;
        corePos = null;
        setChanged();
    }

    @Nullable
    private BlockEntity core() {
        if (level == null || corePos == null) return null;
        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CryoCoreBlockEntity) || be.isRemoved()) { corePos = null; return null; }
        return be;
    }

    /**
     * We expose exactly what the core exposes. Its own {@code LazyOptional} is returned, so when
     * the core is removed the subscribers find out by themselves, without a second invalidation mechanism.
     */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (isRemoved()) return LazyOptional.empty();
        if (cap == HeatCapability.HEAT
                || cap == ForgeCapabilities.ENERGY
                || cap == ForgeCapabilities.ITEM_HANDLER
                || cap == ForgeCapabilities.FLUID_HANDLER) {
            BlockEntity core = core();
            if (core != null) return core.getCapability(cap, side);
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (corePos != null) tag.putLong("core", corePos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
