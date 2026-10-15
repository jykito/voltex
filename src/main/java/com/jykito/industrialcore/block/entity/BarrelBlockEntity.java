package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.BarrelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BarrelBlockEntity extends BlockEntity implements Caps.Provider  {

    private final int capacity;
    private final FluidTank tank;

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
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) tank;
        return null;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("fluid", tank.writeToNBT(registries, new CompoundTag()));
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("fluid")) tank.readFromNBT(registries, tag.getCompound("fluid"));
    }

    /** The fluid travels with the dropped barrel item (loot table copy_components) and comes back on placement. */
    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty())
            components.set(com.jykito.industrialcore.item.ModDataComponents.FLUID_CONTENT.get(), net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        var content = input.get(com.jykito.industrialcore.item.ModDataComponents.FLUID_CONTENT.get());
        if (content != null) tank.setFluid(content.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("fluid");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
