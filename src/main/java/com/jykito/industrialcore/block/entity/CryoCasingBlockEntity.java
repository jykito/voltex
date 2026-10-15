package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
public class CryoCasingBlockEntity extends BlockEntity implements Caps.Provider  {

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
        invalidateCapabilities();
    }

    public void clearCore() {
        if (corePos == null) return;
        corePos = null;
        setChanged();
        invalidateCapabilities();
    }

    @Nullable
    private BlockEntity core() {
        if (level == null || corePos == null) return null;
        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CryoCoreBlockEntity) || be.isRemoved()) { corePos = null; return null; }
        return be;
    }

    /**
     * We expose exactly what the core exposes. Neighbours' capability caches (BlockCapabilityCache) learn about a change
     * through {@link #setCore}/{@link #clearCore}; a broken core unlinks the casings itself (CryoCoreBlock.onRemove).
     */
    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (isRemoved()) return null;
        if (cap == HeatCapability.HEAT
                || cap == Capabilities.EnergyStorage.BLOCK
                || cap == Capabilities.ItemHandler.BLOCK
                || cap == Capabilities.FluidHandler.BLOCK) {
            BlockEntity core = core();
            if (core != null) return core instanceof Caps.Provider p ? p.getCap(cap, side) : null;
            return null;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (corePos != null) tag.putLong("core", corePos.asLong());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
