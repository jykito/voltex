package com.jykito.industrialcore.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.Nullable;

public class UniversalCapsuleItem extends Item {
    public UniversalCapsuleItem(Properties pProperties) {

        super(pProperties.stacksTo(64));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, 1000);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return pStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            FluidStack fluid = handler.getFluidInTank(0);
            if (!fluid.isEmpty()) {
                return Component.translatable(this.getDescriptionId(pStack))
                        .append(" (").append(fluid.getDisplayName()).append(")");
            }
            return super.getName(pStack);
        }).orElse(super.getName(pStack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        HitResult hitresult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
        if (hitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        }

        BlockHitResult blockhitresult = (BlockHitResult) hitresult;
        BlockPos blockpos = blockhitresult.getBlockPos();

        if (!pLevel.mayInteract(pPlayer, blockpos) || !pPlayer.mayUseItemAt(blockpos.relative(blockhitresult.getDirection()), blockhitresult.getDirection(), itemstack)) {
            return InteractionResultHolder.fail(itemstack);
        }

        return itemstack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            FluidStack currentFluid = handler.getFluidInTank(0);

            if (currentFluid.isEmpty()) {

                FluidActionResult result = FluidUtil.tryPickUpFluid(itemstack.copyWithCount(1), pPlayer, pLevel, blockpos, blockhitresult.getDirection());
                if (result.isSuccess()) {
                    ItemStack filledCapsule = result.getResult();
                    return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(itemstack, pPlayer, filledCapsule), pLevel.isClientSide());
                }
            } else {

                return InteractionResultHolder.pass(itemstack);
            }

            return InteractionResultHolder.pass(itemstack);
        }).orElse(InteractionResultHolder.pass(itemstack));
    }
}
