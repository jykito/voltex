package com.jykito.industrialcore.item.custom.weapon;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;


/**
 * Energy blade combat: "Cut" adds a share of the damage the target's armor would have eaten; Nexite counter-strike is added to the hit;
 * with a full charged Nexite set and the blade in hand, attack speed is higher.
 */
public class WeaponEventHandler {

    private static final ResourceLocation NEXITE_SPEED = ResourceLocation.fromNamespaceAndPath(com.jykito.industrialcore.IndustrialCore.MODID, "nexite_blade_speed");
    private static final double NEXITE_SPEED_BONUS = 0.4;

    /** A melee hit of a charged energy blade, so the pre-armor step applies (see PreArmorDamage). */
    public static boolean applies(DamageSource src) {
        if (!(src.getEntity() instanceof Player player) || src.getDirectEntity() != player || !src.is(DamageTypes.PLAYER_ATTACK)) return false;
        if (player.level().isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof EnergyBladeItem && EnergyBladeItem.charged(stack);
    }

    /** Pre-armor step: "Cut" adds part of what the armor would eat, plus the stored Nexite counter-strike. */
    public static float boost(LivingEntity target, DamageSource src, float amount) {
        Player player = (Player) src.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (EnergyBladeItem.mode(stack) == EnergyBladeItem.MODE_CUT) {
            float after = CombatRules.getDamageAfterAbsorb(target, amount, src, target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            amount += (amount - after) * EnergyBladeItem.PIERCE;
        }
        float counter = EnergyBladeItem.takeCounter(player);
        amount += counter;
        if (counter >= 0.1f) {   // the counter-strike fired, it must be visible and audible (author: "the counter-strike does not work")
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.6f);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel srv) {
                srv.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.4, 0.3, 0.3);
                srv.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.industrial_core.energy_blade.counter_hit",
                    String.format(java.util.Locale.ROOT, "%.1f", counter)), true);
        }
        return amount;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) return;
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        boolean want = player.getMainHandItem().getItem() instanceof EnergyBladeItem && EnergyBladeItem.fullNexite(player);
        boolean has = speed.getModifier(NEXITE_SPEED) != null;
        if (want && !has) speed.addTransientModifier(new AttributeModifier(NEXITE_SPEED, NEXITE_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE));
        else if (!want && has) speed.removeModifier(NEXITE_SPEED);
    }
}
