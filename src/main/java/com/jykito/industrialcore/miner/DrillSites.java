package com.jykito.industrialcore.miner;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of the world's drill sites: what lies under each drill beacon in the selected layer.
 * <p>
 * Stored in the Overworld data, so the industrial miner reads a site's composition without loading the beacon chunk,
 * even if the beacon is in another dimension. The beacon writes here after a scan or a layer or lens change and removes the entry when it
 * is broken or the lens no longer fits the dimension.
 */
public final class DrillSites extends SavedData {

    private static final String NAME = "industrial_core_drill_sites";

    /** Site composition: ore block count per item, average ore height and total blocks in the layer. */
    public record Site(Map<Item, Long> weights, int avgY, long total) {}

    private final Map<String, Site> sites = new HashMap<>();

    public static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "|" + pos.asLong();
    }

    public static DrillSites get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DrillSites::load, DrillSites::new, NAME);
    }

    public @Nullable Site get(ResourceKey<Level> dim, BlockPos pos) {
        return sites.get(key(dim, pos));
    }

    public void put(ResourceKey<Level> dim, BlockPos pos, Site site) {
        sites.put(key(dim, pos), site);
        setDirty();
    }

    public void remove(ResourceKey<Level> dim, BlockPos pos) {
        if (sites.remove(key(dim, pos)) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        sites.forEach((k, s) -> {
            CompoundTag e = new CompoundTag();
            e.putString("k", k);
            e.putInt("y", s.avgY());
            e.putLong("t", s.total());
            ListTag w = new ListTag();
            s.weights().forEach((item, n) -> {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null) return;
                CompoundTag o = new CompoundTag();
                o.putString("i", id.toString());
                o.putLong("n", n);
                w.add(o);
            });
            e.put("w", w);
            list.add(e);
        });
        tag.put("sites", list);
        return tag;
    }

    private static DrillSites load(CompoundTag tag) {
        DrillSites d = new DrillSites();
        for (Tag t : tag.getList("sites", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            Map<Item, Long> w = new LinkedHashMap<>();
            for (Tag o : e.getList("w", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) o;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(c.getString("i")));
                if (item != null && item != Items.AIR) w.put(item, c.getLong("n"));   // ore of a removed mod simply drops out of the composition
            }
            d.sites.put(e.getString("k"), new Site(w, e.getInt("y"), e.getLong("t")));
        }
        return d;
    }
}
