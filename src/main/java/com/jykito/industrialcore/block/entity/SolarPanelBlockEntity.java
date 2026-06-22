package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.BaseSolarPanel;
import com.jykito.industrialcore.block.custom.SolarPanelBlock;
import net.minecraft.world.level.block.Block;
import com.jykito.industrialcore.menu.SolarPanelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlockEntity extends BlockEntity implements MenuProvider {

    private static class SolarBuffer extends EnergyStorage {
        SolarBuffer(int capacity, int maxTransfer) { super(capacity, maxTransfer, maxTransfer); }
        @Override public boolean canReceive() { return false; }
        void addEnergy(int amount) { energy = Math.min(capacity, energy + amount); }
        void setEnergy(int amount) { energy = Math.max(0, Math.min(capacity, amount)); }
    }

    private final SolarBuffer buffer;
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int currentGeneration = 0;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 ->  buffer.getEnergyStored()    & 0xFFFF;
                case 1 -> (buffer.getEnergyStored()    >> 16) & 0xFFFF;
                case 2 ->  buffer.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (buffer.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 ->  currentGeneration;
                case 5 ->  currentGeneration > 0 ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) { if (i == 4) currentGeneration = v; }
        @Override public int getCount() { return 6; }
    };

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BaseSolarPanel block = (BaseSolarPanel) state.getBlock();

        this.buffer = new SolarBuffer(block.getBufferSize(), block.getBufferSize());
    }

    public int getTierLevel() {
        Block b = getBlockState().getBlock();
        return b instanceof com.jykito.industrialcore.block.custom.SolarPanelBlock spb ? spb.tier.tierNum : 1;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        BaseSolarPanel block = (BaseSolarPanel) state.getBlock();
        boolean changed = false;

        boolean canGenerate = !block.requiresSky() || level.canSeeSky(pos.above());
        if (canGenerate) {
            currentGeneration = level.isDay() ? block.getDayOutput() : block.getNightOutput();
            buffer.addEnergy(currentGeneration);
            changed = true;
        } else {
            currentGeneration = 0;
        }

        if (buffer.getEnergyStored() > 0) {
            int toGive = buffer.getEnergyStored();
            for (Direction dir : Direction.values()) {
                if (toGive <= 0) break;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;
                var cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (!cap.isPresent()) continue;
                IEnergyStorage target = cap.resolve().get();
                if (!target.canReceive()) continue;
                int accepted = target.receiveEnergy(toGive, false);
                if (accepted > 0) {
                    buffer.extractEnergy(accepted, false);
                    toGive -= accepted;
                    changed = true;
                }
            }
        }

        boolean lit = currentGeneration > 0;
        if (state.getValue(SolarPanelBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(SolarPanelBlock.LIT, lit), 3);
        }

        if (changed) setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SolarPanelMenu(id, inv, this, data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad()         { super.onLoad();        lazyEnergy = LazyOptional.of(() -> buffer); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("energy", buffer.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer.setEnergy(tag.getInt("energy"));
    }
}
