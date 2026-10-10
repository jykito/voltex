package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import com.jykito.industrialcore.menu.EnergyPortMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Энергетический разъём мультиблока: только вход FE, наружу не отдаёт. Ёмкость задаёт
 * улучшение порта. Ядро забирает энергию напрямую через {@link #getEnergy()}.
 */
public class EnergyPortBlockEntity extends BlockEntity implements MenuProvider {

    /** Буфер с изменяемой ёмкостью: приём без лимита за вызов, отдачи наружу нет. */
    public static class PortEnergy extends MachineEnergy {
        PortEnergy() { super(PortUpgradeItem.energy(0), Integer.MAX_VALUE, 0); }
        void setCapacity(int cap) { capacity = cap; energy = Math.min(energy, cap); }
    }

    private final PortEnergy energy = new PortEnergy() {
        @Override public int receiveEnergy(int max, boolean sim) {
            int r = super.receiveEnergy(max, sim);
            if (r > 0 && !sim) setChanged();
            return r;
        }
    };
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int tier;

    private final ItemStackHandler upgrade = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return stack.getItem() instanceof PortUpgradeItem; }
        /** Снять улучшение можно, только если заряд влезет в буфер без него. */
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return energy.getEnergyStored() <= PortUpgradeItem.energy(0) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override protected void onContentsChanged(int slot) { applyTier(); setChanged(); }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int e = energy.getEnergyStored();
            return switch (i) {
                case 0 -> e & 0xFFFF;
                case 1 -> (e >>> 16) & 0xFFFF;
                case 2 -> tier;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 3; }
    };

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_PORT_BE.get(), pos, state);
    }

    public PortEnergy getEnergy()       { return energy; }
    public ItemStackHandler getUpgrade() { return upgrade; }

    private void applyTier() {
        tier = PortUpgradeItem.tierOf(upgrade.getStackInSlot(0));
        energy.setCapacity(PortUpgradeItem.energy(tier));
    }

    @Override public void onLoad() { super.onLoad(); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EnergyPortMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("upgrade", upgrade.serializeNBT());
        tag.putInt("energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        upgrade.deserializeNBT(tag.getCompound("upgrade"));
        applyTier();
        energy.setEnergy(tag.getInt("energy"));
    }
}
