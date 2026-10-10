package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.networking.AtlasSyncPacket;
import com.jykito.industrialcore.networking.ModMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Атлас видов нанитов (юзер 2026-10-07: «Атлас видов в информационной панели»): что игрок открыл и с каким рекордом.
 * Вид — класс плюс цель («miner:copper», «scientist:matter»), редкий вид — черта («trait:cryophile»).
 * Открывается, когда штамм попадает игроку в инвентарь ({@link com.jykito.industrialcore.item.custom.NaniteStrainItem#inventoryTick}).
 * <p>
 * Хранится в данных игрока под {@link Player#PERSISTED_NBT_TAG} — переживает смерть. Клиенту уходит целиком пакетом
 * ({@link AtlasSyncPacket}) при входе и при каждом открытии. Отметки за рекорд: бронза, серебро, золото по доле потолка класса.
 */
public final class NaniteAtlas {

    private NaniteAtlas() {}

    public static final String TAG = "industrial_core_atlas";
    /** Доли потолка класса для бронзы, серебра, золота. */
    public static final float BRONZE = 0.25f, SILVER = 0.60f, GOLD = 0.95f;

    public static String speciesKey(NaniteGenome.Kind kind, String target) { return kind.key() + ":" + target; }

    /** Ключ NBT, которым иконка штамма просит у ItemColor прямо заданный цвет (силуэт, цвет черты). */
    public static final String COLOR_TAG = "AtlasColor";
    public static final int UNKNOWN_COLOR = 0xFF2A3036;

    /** Штамм для иконки вида: класс и цель, чтобы ItemColor окрасил его в цвет материала или темы. */
    public static net.minecraft.world.item.ItemStack icon(NaniteGenome.Kind kind, String target) { return icon(kind, target, 1f); }

    /** То же с заданным сродством (JEI нанолаборатории: учёный ровно на пороге темы). */
    public static net.minecraft.world.item.ItemStack icon(NaniteGenome.Kind kind, String target, float affinity) {
        net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(com.jykito.industrialcore.item.ModItems.NANITE_STRAIN.get());
        CompoundTag g = new CompoundTag(), aff = new CompoundTag();
        aff.putFloat(target, affinity);
        g.put("Aff", aff); g.putString("Kind", kind.key()); g.putInt("Gen", 1);
        st.addTagElement(NaniteGenome.TAG, g);
        return st;
    }

    /** Штамм для иконки, окрашенный прямо заданным цветом. */
    public static net.minecraft.world.item.ItemStack painted(int color) {
        net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(com.jykito.industrialcore.item.ModItems.NANITE_STRAIN.get());
        st.getOrCreateTag().putInt(COLOR_TAG, color);
        return st;
    }
    public static String traitKey(NaniteTrait t) { return "trait:" + t.key(); }

    /** Отметка за рекорд: 0 нет, 1 бронза, 2 серебро, 3 золото. */
    public static int medal(NaniteGenome.Kind kind, int bestPermille) {
        float share = bestPermille / 1000f / NaniteGenome.cap(kind);
        return share >= GOLD ? 3 : share >= SILVER ? 2 : share >= BRONZE ? 1 : 0;
    }

    /** Атлас игрока (создаётся при первом обращении). */
    public static CompoundTag of(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag pers = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!pers.contains(TAG)) pers.put(TAG, new CompoundTag());
        return pers.getCompound(TAG);
    }

    /**
     * Записать штамм в атлас игрока: новые виды, рекорды, черты. Если что-то изменилось — отправить клиенту; о новом
     * скажет сам клиент всплывашкой справа сверху, как достижение (сравнивает с прежней копией).
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
            ResourceLocation id = icon == null ? null : ForgeRegistries.ITEMS.getKey(icon);
            if (id != null && !icons.contains(key)) icons.putString(key, id.toString());
            changed = true;
        }
        for (NaniteTrait t : g.traits()) {
            if (traits.contains(t.key())) continue;
            traits.putBoolean(t.key(), true);
            changed = true;
            com.jykito.industrialcore.advancement.VoltexEventTrigger.INSTANCE.trigger(player,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.NANITE_RARE);   // редкий вид = черта
        }
        if (!changed) return;
        atlas.put("S", best); atlas.put("I", icons); atlas.put("T", traits);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModMessages.sendToPlayer(new AtlasSyncPacket(of(player).copy()), player);
    }

    /** Имя вида: «Добытчик: медная руда», «Учёный: материя». */
    public static Component speciesName(NaniteGenome.Kind kind, String target, @Nullable Item icon) {
        Component what = kind == NaniteGenome.Kind.SCIENTIST ? Component.translatable("nanite_theme.industrial_core." + target)
                : icon != null ? icon.getDescription() : Component.literal(target);
        return Component.translatable("nanite_species.industrial_core." + kind.key(), what);
    }
}
