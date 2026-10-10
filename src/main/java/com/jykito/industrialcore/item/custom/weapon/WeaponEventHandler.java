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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Бой энергоклинком: «Рез» добирает долю урона, который съела бы броня цели; к удару добавляется контрудар нексита;
 * с полным заряженным комплектом нексита у клинка в руке выше скорость атаки.
 */
public class WeaponEventHandler {

    private static final UUID NEXITE_SPEED = UUID.fromString("6f0f6c1e-3c2a-4f7a-9c55-4a1b2e7d9a11");
    private static final double NEXITE_SPEED_BONUS = 0.4;

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player player) || src.getDirectEntity() != player || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof EnergyBladeItem) || !EnergyBladeItem.charged(stack)) return;
        float amount = event.getAmount();
        if (EnergyBladeItem.mode(stack) == EnergyBladeItem.MODE_CUT) {
            LivingEntity target = event.getEntity();
            float after = CombatRules.getDamageAfterAbsorb(amount, target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            amount += (amount - after) * EnergyBladeItem.PIERCE;
        }
        float counter = EnergyBladeItem.takeCounter(player);
        amount += counter;
        event.setAmount(amount);
        if (counter >= 0.1f) {   // контрудар сработал — его должно быть видно и слышно (юзер: «контрудар не воркает»)
            LivingEntity target = event.getEntity();
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.6f);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel srv) {
                srv.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.4, 0.3, 0.3);
                srv.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.industrial_core.energy_blade.counter_hit",
                    String.format(java.util.Locale.ROOT, "%.1f", counter)), true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 10 != 0) return;
        Player player = event.player;
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        boolean want = player.getMainHandItem().getItem() instanceof EnergyBladeItem && EnergyBladeItem.fullNexite(player);
        boolean has = speed.getModifier(NEXITE_SPEED) != null;
        if (want && !has) speed.addTransientModifier(new AttributeModifier(NEXITE_SPEED, "Nexite blade", NEXITE_SPEED_BONUS, AttributeModifier.Operation.ADDITION));
        else if (!want && has) speed.removeModifier(NEXITE_SPEED);
    }
}
