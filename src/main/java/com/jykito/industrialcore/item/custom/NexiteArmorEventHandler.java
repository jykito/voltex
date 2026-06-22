package com.jykito.industrialcore.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class NexiteArmorEventHandler {

    private static float getAbsorptionRate(int pieceCount) {
        return switch (pieceCount) {
            case 1 -> 0.50f;
            case 2 -> 0.60f;
            case 3 -> 0.80f;
            case 4 -> 0.90f;
            default -> 0.0f;
        };
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        List<ItemStack> pieces = getNexitePieces(player);
        if (pieces.isEmpty()) return;

        float damage         = event.getAmount();
        float absorptionRate = getAbsorptionRate(pieces.size());
        float wantAbsorb     = damage * absorptionRate;
        int   feCost         = (int) (wantAbsorb * NexiteArmorItem.FE_PER_DMG);

        int totalAvailable = pieces.stream().mapToInt(NexiteArmorItem::getEnergy).sum();
        if (totalAvailable <= 0) return;

        int   actualCost    = Math.min(feCost, totalAvailable);
        float actualAbsorb  = actualCost / (float) NexiteArmorItem.FE_PER_DMG;

        drainEvenly(pieces, actualCost);
        player.getInventory().setChanged();

        float remaining = damage - actualAbsorb;
        if (remaining <= 0.0f) {
            event.setCanceled(true);
        } else {
            event.setAmount(remaining);
        }
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof NexiteArmorItem)) return;

        float damagingDistance = Math.max(0, event.getDistance() - 4.0f);
        if (damagingDistance <= 0) return;

        int cost   = (int) (damagingDistance * NexiteArmorItem.FE_PER_DMG);
        int stored = NexiteArmorItem.getEnergy(boots);
        if (stored <= 0) return;

        if (stored >= cost) {
            NexiteArmorItem.setEnergy(boots, stored - cost);
            player.getInventory().setChanged();
            event.setCanceled(true);
        } else {
            float absorbed = stored / (float) NexiteArmorItem.FE_PER_DMG;
            NexiteArmorItem.setEnergy(boots, 0);
            player.getInventory().setChanged();
            event.setDistance(Math.max(0, event.getDistance() - absorbed));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (event.player.tickCount % 20 != 0) return;

        if (hasFullChargedSet(event.player)) {
            event.player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, 0, false, false));
        }
    }

    private static void drainEvenly(List<ItemStack> pieces, int totalCost) {
        if (totalCost <= 0 || pieces.isEmpty()) return;
        int perPiece  = totalCost / pieces.size();
        int remainder = totalCost % pieces.size();
        for (int i = 0; i < pieces.size(); i++) {
            int cost   = perPiece + (i == 0 ? remainder : 0);
            int stored = NexiteArmorItem.getEnergy(pieces.get(i));
            NexiteArmorItem.setEnergy(pieces.get(i), stored - Math.min(cost, stored));
        }
    }

    private static List<ItemStack> getNexitePieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof NexiteArmorItem) pieces.add(stack);
        }
        return pieces;
    }

    private static boolean hasFullChargedSet(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof NexiteArmorItem)) return false;
            if (NexiteArmorItem.getEnergy(stack) <= 0) return false;
        }
        return true;
    }
}
