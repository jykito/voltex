package com.jykito.industrialcore.client.drill;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.item.custom.EnergyDrillItem;
import com.jykito.industrialcore.item.custom.IndustrialDrillItem;
import com.jykito.industrialcore.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Drill working sound (author, 2026-10-10: "drill: add a sound"): the drill_run loop while your player digs with a drill.
 * Fades in over a couple of ticks with a slight pitch rise and fades out the same way when you stop digging; the industrial drill has a lower pitch.
 * Started from ClientForgeEvents.onClientTick.
 */
@OnlyIn(Dist.CLIENT)
public class DrillSound extends AbstractTickableSoundInstance {

    private static DrillSound current;

    private final Player player;
    private final float basePitch;
    private float level = 0f;

    private DrillSound(Player player, boolean industrial) {
        super(ModSounds.DRILL_RUN.get(), SoundSource.PLAYERS, RandomSource.create());
        this.player = player;
        this.basePitch = industrial ? 0.85f : 1.0f;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.pitch = basePitch * 0.92f;
        this.x = player.getX(); this.y = player.getY() + 1; this.z = player.getZ();
    }

    private static boolean drilling(Minecraft mc) {
        return mc.player != null && mc.gameMode != null && mc.gameMode.isDestroying()
                && mc.player.getMainHandItem().getItem() instanceof EnergyDrillItem;
    }

    /** From the client tick: digging with a drill but no sound, so start it (one per player). */
    public static void clientTick(Minecraft mc) {
        if (current != null && !current.isStopped()) return;
        if (!drilling(mc)) return;
        current = new DrillSound(mc.player, mc.player.getMainHandItem().getItem() instanceof IndustrialDrillItem);
        mc.getSoundManager().play(current);
    }

    @Override
    public boolean canStartSilent() { return true; }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (player.isRemoved() || mc.player != player) { stop(); return; }
        level = drilling(mc) ? Math.min(1f, level + 0.3f) : level - 0.25f;
        if (level <= 0f) { stop(); return; }
        this.x = player.getX(); this.y = player.getY() + 1; this.z = player.getZ();
        this.volume = 0.55f * level * (float) Config.soundVolume;
        this.pitch = basePitch * (0.92f + 0.08f * level);
    }
}
