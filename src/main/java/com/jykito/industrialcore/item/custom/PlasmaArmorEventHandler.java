package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.entity.ModEntities;
import com.jykito.industrialcore.entity.PlasmaWaveEntity;
import com.jykito.industrialcore.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class PlasmaArmorEventHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static float getAbsorptionRate(int chargedPieces) {
        return switch (chargedPieces) {
            case 1 -> 0.25f;
            case 2 -> 0.50f;
            case 3 -> 0.75f;
            case 4 -> 1.00f;
            default -> 0.0f;
        };
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        List<ItemStack> pieces  = getPlasmaPieces(player);
        List<ItemStack> charged = getChargedPieces(pieces);
        if (charged.isEmpty()) return;

        float damage         = event.getAmount();
        float absorptionRate = getAbsorptionRate(charged.size());
        float wantAbsorb     = damage * absorptionRate;
        int   feCost         = (int) (wantAbsorb * PlasmaArmorItem.FE_PER_DMG);

        int totalAvailable = charged.stream().mapToInt(PlasmaArmorItem::getEnergy).sum();
        int actualCost     = Math.min(feCost, totalAvailable);
        float actualAbsorb = actualCost / (float) PlasmaArmorItem.FE_PER_DMG;

        drainEvenly(charged, actualCost);

        int burstGain    = (int) actualAbsorb;
        int perPieceBurst = burstGain / pieces.size();
        int burstRemainder = burstGain % pieces.size();
        for (int i = 0; i < pieces.size(); i++) {
            int gain = perPieceBurst + (i == 0 ? burstRemainder : 0);
            PlasmaArmorItem.setBurstCharge(pieces.get(i),
                    PlasmaArmorItem.getBurstCharge(pieces.get(i)) + gain);
        }

        player.getInventory().setChanged();

        if (actualAbsorb > 0) {
            playShieldEffect(player);
        }

        float remaining = damage - actualAbsorb;
        if (remaining <= 0.0f) {
            event.setCanceled(true);
        } else {
            event.setAmount(remaining);
        }

    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (event.player.tickCount % 20 != 0) return;

        List<ItemStack> pieces = getPlasmaPieces(event.player);
        if (pieces.size() == 4 && !getChargedPieces(pieces).isEmpty()) {
            event.player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false));
        }
    }

    public static void triggerPlasmaBurst(Player player, List<ItemStack> pieces) {

        player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8.0),
                e -> e != player && !player.isAlliedTo(e))
            .forEach(e -> e.hurt(player.damageSources().playerAttack(player), 100.0f));

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                ModSounds.PLASMA_BURST.get(), SoundSource.PLAYERS, 3.0f, 1.0f);

        PlasmaWaveEntity wave = new PlasmaWaveEntity(ModEntities.PLASMA_WAVE.get(), player.level());
        wave.setPos(player.getX(), player.getY(), player.getZ());
        player.level().addFreshEntity(wave);

        for (ItemStack piece : pieces) {
            PlasmaArmorItem.setBurstCharge(piece, 0);
        }
        player.getInventory().setChanged();
    }

    private static void playShieldEffect(Player player) {
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel srv) {
            srv.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1, player.getZ(),
                    12, 0.5, 0.8, 0.5, 0.05);
        }
    }

    private static void drainEvenly(List<ItemStack> pieces, int totalCost) {
        if (totalCost <= 0 || pieces.isEmpty()) return;
        int perPiece  = totalCost / pieces.size();
        int remainder = totalCost % pieces.size();
        for (int i = 0; i < pieces.size(); i++) {
            int cost   = perPiece + (i == 0 ? remainder : 0);
            int stored = PlasmaArmorItem.getEnergy(pieces.get(i));
            PlasmaArmorItem.setEnergy(pieces.get(i), stored - Math.min(cost, stored));
        }
    }

    private static List<ItemStack> getPlasmaPieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof PlasmaArmorItem) pieces.add(stack);
        }
        return pieces;
    }

    private static List<ItemStack> getChargedPieces(List<ItemStack> pieces) {
        List<ItemStack> charged = new ArrayList<>();
        for (ItemStack piece : pieces) {
            if (PlasmaArmorItem.getEnergy(piece) > 0) charged.add(piece);
        }
        return charged;
    }

    public static boolean isBurstReady(List<ItemStack> pieces) {
        for (ItemStack piece : pieces) {
            if (PlasmaArmorItem.getBurstCharge(piece) < PlasmaArmorItem.BURST_MAX) return false;
        }
        return true;
    }
}
