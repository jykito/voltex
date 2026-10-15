package com.jykito.industrialcore.advancement;

import com.google.gson.JsonObject;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Our single advancement criterion: "an event happened", with a name ({@code "trigger": "industrial_core:event",
 * "conditions": {"event": "cryo_formed"}}). Everything that is not "got an item": a structure formed, a stage,
 * resonance, quench, a rare species, a shot. Event names are the constants below; the advancement tree is tools/advancements/tree.js.
 */
public class VoltexEventTrigger extends SimpleCriterionTrigger<VoltexEventTrigger.Instance> {

    public static final ResourceLocation ID = new ResourceLocation(IndustrialCore.MODID, "event");
    public static final VoltexEventTrigger INSTANCE = new VoltexEventTrigger();

    public static final String CRYO_FORMED = "cryo_formed", CRYO_STAGE_3 = "cryo_stage_3",
            MAGNET_FORMED = "magnet_formed", MAGNET_RESONANCE = "magnet_resonance", MAGNET_QUENCH = "magnet_quench",
            LATE_FORMED = "late_formed", NANITE_RARE = "nanite_rare", GAUSS_STAGE3_KILL = "gauss_stage3_kill";

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, GsonHelper.getAsString(json, "event"));
    }

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

    public static class Instance extends AbstractCriterionTriggerInstance {
        private final String event;

        public Instance(ContextAwarePredicate player, String event) {
            super(ID, player);
            this.event = event;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("event", event);
            return json;
        }
    }
}
