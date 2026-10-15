package com.jykito.industrialcore.client;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Looping electromagnet hum: volume and pitch follow the BE spin-up level (spin01).
 * Stops itself when the structure is unformed or removed.
 */
@OnlyIn(Dist.CLIENT)
public class MagnetHumSound extends AbstractTickableSoundInstance {

    private final ElectromagnetCoreBlockEntity be;

    public MagnetHumSound(ElectromagnetCoreBlockEntity be) {
        super(ModSounds.MAGNET_HUM.get(), SoundSource.BLOCKS, RandomSource.create());
        this.be = be;
        BlockPos p = be.getBlockPos();
        this.x = p.getX() + 0.5;
        this.y = p.getY() + 0.5;
        this.z = p.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4f;
        this.pitch = 0.8f;
    }

    @Override
    public void tick() {
        if (be.isRemoved() || !be.getBlockState().getValue(ElectromagnetCoreBlock.FORMED)) {
            stop();
            return;
        }
        float s = be.getSpinLevel();
        this.volume = (0.40f + 0.60f * s) * (float) Config.soundVolume; // louder while spinning up × config
        this.pitch  = 0.75f + 0.50f * s;                                // higher while spinning up
    }
}
