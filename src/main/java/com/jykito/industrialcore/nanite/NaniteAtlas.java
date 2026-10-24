package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.networking.AtlasSyncPacket;
import com.jykito.industrialcore.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Nanite species atlas (author, 2026-10-07: "a species atlas in the information panel"): what the player unlocked and with what record.
 * A species is a class plus a target ("miner:copper", "scientist:matter"), a rare species is a trait ("trait:cryophile").
 * It unlocks when a strain gets into the player inventory ({@link com.jykito.industrialcore.item.custom.NaniteStrainItem#inventoryTick}).
 * <p>
 * Stored in the player data under {@link Player#PERSISTED_NBT_TAG}, survives death. Sent to the client whole as a packet
 * ({@link AtlasSyncPacket}) on join and on every unlock. Record marks: bronze, silver, gold by the share of the class cap.
 */
public final class NaniteAtlas {

    private NaniteAtlas() {}

    public static final String TAG = "industrial_core_atlas";
    /** Shares of the class cap for bronze, silver, gold. */
    public static final float BRONZE = 0.25f, SILVER = 0.60f, GOLD = 0.95f;

    public static String speciesKey(NaniteGenome.Kind kind, String target) { return kind.key() + ":" + target; }

    public static final int UNKNOWN_COLOR = 0xFF2A3036;

    /** Strain for a species icon: class and target, so ItemColor tints it in the material or topic colour. */
    public static net.minecraft.world.item.ItemStack icon(NaniteGenome.Kind kind, String target) { return icon(kind, target, 1f); }

    /** The same with a given affinity (nanite lab JEI: a scientist exactly at the topic threshold). */
    public static net.minecraft.world.item.ItemStack icon(NaniteGenome.Kind kind, String target, float affinity) {
        net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(com.jykito.industrialcore.item.ModItems.NANITE_STRAIN.get());
        CompoundTag g = new CompoundTag(), aff = new CompoundTag();
        aff.putFloat(target, affinity);
        g.put("Aff", aff); g.putString("Kind", kind.key()); g.putInt("Gen", 1);
        st.set(com.jykito.industrialcore.item.ModDataComponents.NANITE_GENOME.get(), net.minecraft.world.item.component.CustomData.of(g));
        return st;
    }

    /** Strain for an icon, tinted with an explicitly set colour. */
    public static net.minecraft.world.item.ItemStack painted(int color) {
        net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(com.jykito.industrialcore.item.ModItems.NANITE_STRAIN.get());
        st.set(com.jykito.industrialcore.item.ModDataComponents.NANITE_COLOR.get(), color);
        return st;
    }
    public static String traitKey(NaniteTrait t) { return "trait:" + t.key(); }

    /** Record mark: 0 none, 1 bronze, 2 silver, 3 gold. */
    public static int medal(NaniteGenome.Kind kind, int bestPermille) {
        float share = bestPermille / 1000f / NaniteGenome.cap(kind);
        return share >= GOLD ? 3 : share >= SILVER ? 2 : share >= BRONZE ? 1 : 0;
    }

    /** The player's atlas (created on first access). */
    public static CompoundTag of(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag pers = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!pers.contains(TAG)) pers.put(TAG, new CompoundTag());
        return pers.getCompound(TAG);
    }

    /**
     * Record a strain into the player's atlas: new species, records, traits. If anything changed, send it to the client; the client
     * announces new ones itself with a toast in the top right, like an advancement (it compares with the previous copy).
     */
    public static void discover(ServerPlayer player, NaniteGenome g) {
        if (g.kind() == null || g.isEmpty()) return;
        CompoundTag atlas = of(player), best = atlas.getCompound("S"), icons = atlas.getCompound("I"), traits = atlas.getCompound("T");
        boolean changed = false;
        for (Map.Entry<String, Float> e : g.affinity().entrySet()) {
            if (e.getValue() < 0.005f) continue;
            String key = speciesKey(g.kind(), e.getKey());
            int now = Math.round(e.getValue() * 1000);
            if (best.contains(key) && now <= best.getInt(key)) continue;
            best.putInt(key, now);
            Item icon = g.icon(e.getKey());
            ResourceLocation id = icon == null ? null : BuiltInRegistries.ITEM.getKey(icon);
            if (id != null && !icons.contains(key)) icons.putString(key, id.toString());
            changed = true;
        }
        for (String key : g.allTraitKeys()) {
            if (traits.contains(key)) continue;
            traits.putBoolean(key, true);
            changed = true;
            com.jykito.industrialcore.advancement.VoltexEventTrigger.INSTANCE.trigger(player,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.NANITE_RARE);   // rare species = trait
        }
        if (!changed) return;
        atlas.put("S", best); atlas.put("I", icons); atlas.put("T", traits);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModMessages.sendToPlayer(new AtlasSyncPacket(of(player).copy()), player);
    }

    /** Species name: "Miner: copper ore", "Scientist: matter". */
    public static Component speciesName(NaniteGenome.Kind kind, String target, @Nullable Item icon) {
        Component named = kind.targetName(target);
        Component what = named != null ? named : icon != null ? icon.getDescription() : Component.literal(target);
        return Component.translatable(kind.lang("nanite_species.industrial_core"), what);
    }
}
