package com.jykito.industrialcore.item.custom;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;

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

    /** Whether this suit takes part in the hit at all (see {@link PreArmorDamage}). */
    static boolean applies(LivingEntity target) {
        return target instanceof Player p && !getNexitePieces(p).isEmpty();
    }

    /** Pre-armor step, see {@link PreArmorDamage}: returns the damage left after the suit, ≤ 0 means nothing gets through. */
    static float absorb(LivingEntity target, float damage) {
        if (!(target instanceof Player player)) return damage;
        if (player.level().isClientSide()) return damage;

        List<ItemStack> pieces = getNexitePieces(player);
        if (pieces.isEmpty()) return damage;

        float absorptionRate = getAbsorptionRate(pieces.size());
        float wantAbsorb     = damage * absorptionRate;
        int   feCost         = (int) (wantAbsorb * NexiteArmorItem.FE_PER_DMG);

        int totalAvailable = pieces.stream().mapToInt(NexiteArmorItem::getEnergy).sum();
        if (totalAvailable <= 0) return damage;

        int   actualCost    = Math.min(feCost, totalAvailable);
        float actualAbsorb  = actualCost / (float) NexiteArmorItem.FE_PER_DMG;

        drainEvenly(pieces, actualCost);
        player.getInventory().setChanged();
        // energy blade counter-strike: damage absorbed by the armor accumulates and goes into the next blade hit
        com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem.addCounter(player, actualAbsorb);
        if (actualAbsorb > 0 && player.getMainHandItem().getItem() instanceof com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem)
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.industrial_core.energy_blade.counter_ready",
                    String.format(java.util.Locale.ROOT, "%.1f", com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem.counter(player))), true);

        return damage - actualAbsorb;   // ≤ 0: nothing gets through (the old cancel)
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
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;

        if (hasFullChargedSet(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, 0, false, false));
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
