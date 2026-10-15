package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.item.custom.weapon.WeaponEventHandler;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Damage steps of the power suits and the energy blade that work on the amount BEFORE armor.
 * <p>
 * In 1.20.1 they were LivingHurtEvent listeners: that event fired inside actuallyHurt, after the shield, the helmet
 * and freezing multipliers and the invulnerability frames, with the pre-armor amount. 1.21 has no event at that point
 * (LivingIncomingDamageEvent fires before all of those), so the steps run in the ARMOR reduction of the damage container,
 * which is evaluated exactly there, and the armor result is recomputed for the changed amount. The steps keep the old
 * listener order: Nexite, Plasma, energy blade.
 */
public final class PreArmorDamage {

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        DamageSource source = event.getSource();
        boolean nexite = NexiteArmorEventHandler.applies(target);
        boolean plasma = PlasmaArmorEventHandler.applies(target);
        boolean blade = WeaponEventHandler.applies(source);
        if (!nexite && !plasma && !blade) return;

        event.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, armorReduction) -> {
            float pre = container.getNewDamage();
            float amount = pre;
            if (nexite) amount = NexiteArmorEventHandler.absorb(target, amount);
            if (amount > 0 && plasma) amount = PlasmaArmorEventHandler.absorb(target, amount);
            if (amount > 0 && blade) amount = WeaponEventHandler.boost(target, source, amount);
            if (amount <= 0) return pre;                         // the old cancel: nothing gets through
            if (amount == pre) return armorReduction;            // untouched hit: vanilla armor as is
            float after = source.is(DamageTypeTags.BYPASSES_ARMOR) ? amount
                    : CombatRules.getDamageAfterAbsorb(target, amount, source, target.getArmorValue(),
                            (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            return pre - after;
        });
    }
}
