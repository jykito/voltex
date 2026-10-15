package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.BaseSolarPanel;
import com.jykito.industrialcore.block.custom.SolarPanelBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private final MachineEnergy buffer;
    private int currentGeneration = 0;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 ->  buffer.getEnergyStored()    & 0xFFFF;
                case 1 -> (buffer.getEnergyStored()    >> 16) & 0xFFFF;
                case 2 ->  buffer.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (buffer.getMaxEnergyStored() >> 16) & 0xFFFF;
                // a ContainerData cell is 16-bit, and T5+ gives more than 32767 -> two halves
                case 4 ->  currentGeneration           & 0xFFFF;
                case 5 -> (currentGeneration           >> 16) & 0xFFFF;
                case 6 ->  currentGeneration > 0 ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 7; }
    };

    public SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BaseSolarPanel block = (BaseSolarPanel) state.getBlock();

        this.buffer = MachineEnergy.generator(block.getBufferSize(), block.getBufferSize());
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
                var cap = Caps.get(neighbor, Caps.ENERGY, dir.getOpposite());
                if (!cap.isPresent()) continue;
                IEnergyStorage target = cap.get();
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
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK) return (T) buffer;
        return null;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("energy", buffer.getEnergyStored());
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.setEnergy(tag.getInt("energy"));
    }
}
