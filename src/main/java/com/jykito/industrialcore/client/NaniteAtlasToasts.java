package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.nanite.NaniteAtlas;
import com.jykito.industrialcore.nanite.NaniteAtlasClient;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteTrait;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * New atlas from the server against the previous copy: a toast for every new species, new mark and rare species
 * ({@link NaniteAtlasToast}). The first sync after joining the world just loads the atlas.
 */
@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NaniteAtlasToasts {

    private NaniteAtlasToasts() {}

    private static final int C_SPECIES = 0xFF7FF4FF, C_RARE = 0xFFC98BFF;
    private static final int[] C_MEDAL = {0xFF8B949E, 0xFFD08A4F, 0xFFC3CCD1, 0xFFFFCF33};

    public static void onSync(CompoundTag next) {
        CompoundTag prev = NaniteAtlasClient.data();
        boolean first = !NaniteAtlasClient.synced();
        NaniteAtlasClient.set(next);
        if (first) return;
        var toasts = Minecraft.getInstance().getToasts();
        CompoundTag ps = prev.getCompound("S"), ns = next.getCompound("S"), icons = next.getCompound("I");
        for (String key : ns.getAllKeys()) {
            int cut = key.lastIndexOf(':');   // class keys of add-ons contain a colon themselves: modid:name:target
            String[] kt = cut < 0 ? new String[]{key} : new String[]{key.substring(0, cut), key.substring(cut + 1)};
            NaniteGenome.Kind kind = kt.length == 2 ? NaniteGenome.Kind.byKey(kt[0]) : null;
            if (kind == null) continue;
            Component name = NaniteAtlas.speciesName(kind, kt[1], item(icons.getString(key)));
            if (!ps.contains(key)) {
                toasts.addToast(new NaniteAtlasToast(NaniteAtlas.icon(kind, kt[1]), Component.translatable("toast.industrial_core.atlas.species"), name, C_SPECIES));
                continue;
            }
            int was = NaniteAtlas.medal(kind, ps.getInt(key)), now = NaniteAtlas.medal(kind, ns.getInt(key));
            if (now > was) toasts.addToast(new NaniteAtlasToast(NaniteAtlas.icon(kind, kt[1]),
                    Component.translatable("toast.industrial_core.atlas.medal", Component.translatable("gui.industrial_core.atlas.medal." + now)), name, C_MEDAL[now]));
        }
        CompoundTag pt = prev.getCompound("T"), nt = next.getCompound("T");
        for (com.jykito.industrialcore.nanite.NaniteTraitRegistry.TraitInfo t : com.jykito.industrialcore.nanite.NaniteTraitRegistry.all())
            if (nt.getBoolean(t.key()) && !pt.getBoolean(t.key()))
                toasts.addToast(new NaniteAtlasToast(NaniteAtlas.painted(t.color()), Component.translatable("toast.industrial_core.atlas.rare"),
                        Component.translatable(t.langKey()), C_RARE));
    }

    private static Item item(String id) {
        ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        Item it = rl == null ? null : BuiltInRegistries.ITEM.get(rl);
        return it == Items.AIR ? null : it;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { NaniteAtlasClient.reset(); }
}
