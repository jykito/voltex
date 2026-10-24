package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.ChargingPlateBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Charging plate: takes power from cables into a small buffer and spends up to {@link #RATE} FE per tick on whoever
 * stands on top. Order: armour (the backpack too), main hand, off hand, then the rest of the inventory.
 */
public class ChargingPlateBlockEntity extends BlockEntity {

    public static final int CAPACITY = 1_000_000;
    public static final int RATE = 100_000;
    /** Ticks the plate stays lit after the last charge, so the light does not flicker between items. */
    private static final int LINGER = 10;

    private final MachineEnergy energy = MachineEnergy.consumer(CAPACITY, RATE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int litTicks = 0;

    public ChargingPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_PLATE_BE.get(), pos, state);
    }

    @Override public void onLoad() { super.onLoad(); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != Direction.UP) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPlateBlockEntity be) {
        int budget = Math.min(RATE, be.energy.getEnergyStored()), used = 0;
        if (budget > 0) {
            AABB zone = new AABB(pos.getX() + 0.05, pos.getY() + 0.2, pos.getZ() + 0.05, pos.getX() + 0.95, pos.getY() + 1.2, pos.getZ() + 0.95);
            for (Player player : level.getEntitiesOfClass(Player.class, zone)) {
                used += charge(player, budget - used);
                if (used >= budget) break;
            }
        }
        if (used > 0) { be.energy.consumeEnergy(used); be.litTicks = LINGER; be.setChanged(); }
        else if (be.litTicks > 0) be.litTicks--;

        boolean on = be.litTicks > 0;
        if (state.getValue(ChargingPlateBlock.CHARGING) != on) level.setBlock(pos, state.setValue(ChargingPlateBlock.CHARGING, on), 3);
    }

    /** Pushes up to {@code budget} FE into the player's items; returns how much was taken. */
    private static int charge(Player player, int budget) {
        Inventory inv = player.getInventory();
        int left = budget;
        left = chargeList(inv.armor, left);
        left = chargeOne(player.getMainHandItem(), left);
        left = chargeOne(player.getOffhandItem(), left);
        for (int i = 0; i < inv.items.size() && left > 0; i++) if (i != inv.selected) left = chargeOne(inv.items.get(i), left);
        return budget - left;
    }

    private static int chargeList(java.util.List<ItemStack> stacks, int left) {
        for (ItemStack s : stacks) { if (left <= 0) break; left = chargeOne(s, left); }
        return left;
    }

    private static int chargeOne(ItemStack stack, int left) {
        if (left <= 0 || stack.isEmpty()) return left;
        IEnergyStorage e = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (e == null || !e.canReceive()) return left;
        return left - e.receiveEnergy(left, false);
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("energy", energy.getEnergyStored()); }
    @Override public void load(CompoundTag tag) { super.load(tag); energy.setEnergy(tag.getInt("energy")); }
}
