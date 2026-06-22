package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElectricWrenchItem extends Item {

    public static final int CAPACITY = 20_000;
    public static final int COST     = 500;

    public ElectricWrenchItem(Properties properties) {
        super(properties);
    }

    public static int getStoredEnergy(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("energy") : 0;
    }

    public static boolean drainEnergy(ItemStack stack, int amount) {
        int stored = getStoredEnergy(stack);
        if (stored < amount) return false;
        stack.getOrCreateTag().putInt("energy", stored - amount);
        return true;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getStoredEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getStoredEnergy(stack) + " / " + CAPACITY + " FE")
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        tooltip.add(Component.translatable("tooltip.industrial_core.electric_wrench")
                .withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final EnergyStorage storage = new EnergyStorage(CAPACITY, CAPACITY, CAPACITY) {

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(maxReceive, CAPACITY - stored);
                    if (!simulate && received > 0) stack.getOrCreateTag().putInt("energy", stored + received);
                    return received;
                }
                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(maxExtract, stored);
                    if (!simulate && extracted > 0) stack.getOrCreateTag().putInt("energy", stored - extracted);
                    return extracted;
                }
                @Override
                public int getEnergyStored() {
                    return stack.hasTag() ? stack.getTag().getInt("energy") : 0;
                }
            };
            private final LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> storage);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                               @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !IndustrialCore.MODID.equals(id.getNamespace())) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return InteractionResult.PASS;

        ItemStack wrench = context.getItemInHand();
        if (getStoredEnergy(wrench) < COST) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("msg.industrial_core.wrench_no_energy")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            drainEnergy(wrench, COST);

            net.minecraft.world.Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(block));

            level.removeBlock(pos, false);

            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.6f);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
