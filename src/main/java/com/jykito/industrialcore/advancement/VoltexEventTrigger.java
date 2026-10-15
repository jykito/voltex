package com.jykito.industrialcore.advancement;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Our single advancement criterion: "an event happened", with a name ({@code "trigger": "industrial_core:event",
 * "conditions": {"event": "cryo_formed"}}). Everything that is not "got an item": a structure formed, a stage,
 * resonance, quench, a rare species, a shot. Event names are the constants below; the advancement tree is tools/advancements/tree.js.
 */
public class VoltexEventTrigger extends SimpleCriterionTrigger<VoltexEventTrigger.Instance> {

    public static final VoltexEventTrigger INSTANCE = new VoltexEventTrigger();

    /** In 1.21 triggers are registry entries; we register the same singleton, so calls through INSTANCE do not change. */
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, IndustrialCore.MODID);
    static { TRIGGERS.register("event", () -> INSTANCE); }

    public static final String CRYO_FORMED = "cryo_formed", CRYO_STAGE_3 = "cryo_stage_3",
            MAGNET_FORMED = "magnet_formed", MAGNET_RESONANCE = "magnet_resonance", MAGNET_QUENCH = "magnet_quench",
            LATE_FORMED = "late_formed", NANITE_RARE = "nanite_rare", GAUSS_STAGE3_KILL = "gauss_stage3_kill";

    @Override
    public Codec<Instance> codec() { return Instance.CODEC; }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event.equals(event));
    }

    /** To every player within radius blocks of the point: this is how machine events are credited. */
    public static void award(Level level, Vec3 at, double radius, String event) {
        if (!(level instanceof ServerLevel server)) return;
        double r2 = radius * radius;
        for (ServerPlayer p : server.players())
            if (p.distanceToSqr(at) <= r2) INSTANCE.trigger(p, event);
    }

    public static void award(Level level, BlockPos pos, double radius, String event) {
        award(level, Vec3.atCenterOf(pos), radius, event);
    }

    public record Instance(Optional<ContextAwarePredicate> player, String event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)
        ).apply(i, Instance::new));
    }
}
