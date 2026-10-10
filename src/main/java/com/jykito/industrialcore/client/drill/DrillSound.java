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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Звук работы бура (юзер 2026-10-10: «бур — добавить звук»): петля drill_run, пока свой игрок копает буром.
 * Нарастает за пару тиков с лёгким разгоном тона и так же затихает, когда копать перестал; у промышленного бура тон ниже.
 * Запускается из ClientForgeEvents.onClientTick.
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

    /** Из клиентского тика: копаешь буром, а звука нет — запустить (один на игрока). */
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
