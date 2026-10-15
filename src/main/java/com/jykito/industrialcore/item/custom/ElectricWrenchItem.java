package com.jykito.industrialcore.item.custom;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import com.jykito.industrialcore.item.EnergyItem;
import com.jykito.industrialcore.item.ModDataComponents;
import com.jykito.industrialcore.capability.Caps;

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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElectricWrenchItem extends Item implements EnergyItem {

    public static final int CAPACITY = 20_000;
    public static final int COST     = 500;

    public ElectricWrenchItem(Properties properties) {
        super(properties);
    }

    public static int getStoredEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static boolean drainEnergy(ItemStack stack, int amount) {
        int stored = getStoredEnergy(stack);
        if (stored < amount) return false;
        stack.set(ModDataComponents.ENERGY.get(), stored - amount);
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getStoredEnergy(stack) + " / " + CAPACITY + " FE")
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        tooltip.add(Component.translatable("tooltip.industrial_core.electric_wrench")
                .withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(), CAPACITY, CAPACITY, CAPACITY);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // plain RMB with the wrench on a heat pipe: toggle a side on/off
        if (!player.isShiftKeyDown()) {
            if (state.getBlock() instanceof com.jykito.industrialcore.block.custom.HeatPipeBlock) {
                if (!level.isClientSide()
                        && level.getBlockEntity(pos) instanceof com.jykito.industrialcore.block.entity.HeatPipeBlockEntity pipe) {
                    Direction dir = pickSide(context.getClickLocation(), pos);
                    boolean nowOff = pipe.toggleDisabled(dir);
                    com.jykito.industrialcore.block.custom.HeatPipeBlock.recomputeConnections(level, pos);
                    com.jykito.industrialcore.block.custom.HeatPipeBlock.recomputeConnections(level, pos.relative(dir));
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, nowOff ? 0.7f : 1.2f);
                    player.displayClientMessage(Component.translatable(nowOff
                            ? "msg.industrial_core.heat_pipe.side_off" : "msg.industrial_core.heat_pipe.side_on")
                            .withStyle(nowOff ? ChatFormatting.RED : ChatFormatting.GREEN), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        // shift -> remove the block
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
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

            // the item keeps what the block entity carries as components (barrel fluid), as with a pickaxe;
            // machine inventories drop on their own in onRemove
            ItemStack drop = new ItemStack(block);
            drop.applyComponents(be.collectComponents());
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);

            level.removeBlock(pos, false);

            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.6f);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Direction from the click point: the side the click is shifted furthest towards from the centre. */
    private static Direction pickSide(net.minecraft.world.phys.Vec3 loc, BlockPos pos) {
        double dx = loc.x - pos.getX() - 0.5;
        double dy = loc.y - pos.getY() - 0.5;
        double dz = loc.z - pos.getZ() - 0.5;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
