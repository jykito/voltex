package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Client helper for the electromagnet sound: keeps one looping hum per core position, recreates it if the SoundManager
 * unloaded it (you left the radius and came back). Called only from the BE client tick.
 */
@OnlyIn(Dist.CLIENT)
public class MagnetSoundClient {

    private static final Map<BlockPos, MagnetHumSound> HUMS = new HashMap<>();

    public static void ensureHum(ElectromagnetCoreBlockEntity be) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        BlockPos key = be.getBlockPos();
        MagnetHumSound s = HUMS.get(key);
        if (s == null || !sm.isActive(s)) {
            s = new MagnetHumSound(be);
            sm.play(s);
            HUMS.put(key.immutable(), s);
        }
    }
}
