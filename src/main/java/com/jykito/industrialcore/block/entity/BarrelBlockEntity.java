package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.BarrelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BarrelBlockEntity extends BlockEntity {

    private final int capacity;
    private final FluidTank tank;
    private LazyOptional<IFluidHandler> lazyTank = LazyOptional.empty();

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_BE.get(), pos, state);
        this.capacity = (state.getBlock() instanceof BarrelBlock b) ? b.capacity : 32000;
        this.tank = new FluidTank(capacity) {
            @Override protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide())
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        };
    }

    public int getCapacity()        { return capacity; }
    public FluidStack getFluidView() { return tank.getFluid(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyTank.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad()         { super.onLoad();         lazyTank = LazyOptional.of(() -> tank); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyTank.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("fluid", tank.writeToNBT(new CompoundTag()));
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("fluid")) tank.readFromNBT(tag.getCompound("fluid"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
