package com.jykito.industrialcore.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Энергомот дезинтеграции: кружит вокруг ВЕРТИКАЛЬНОЙ оси катушки (cx,cz), спиралью
 * стягивается на неё (в линию), медленно, цвет бирюза→фиолет, затухает. Свечение (full bright).
 * Центр орбиты приходит в конструктор через dx,dy,dz (передан velocity-слотами sendParticles, speed=1).
 */
@OnlyIn(Dist.CLIENT)
public class EnergyMoteParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double cx, cy, cz;
    private double angle, radius;
    private final double angularSpeed;

    EnergyMoteParticle(ClientLevel level, double x, double y, double z,
                       double cx, double cy, double cz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.cx = cx; this.cy = cy; this.cz = cz;
        double ddx = x - cx, ddz = z - cz;
        this.radius = Math.sqrt(ddx * ddx + ddz * ddz);
        this.angle = Math.atan2(ddz, ddx);
        this.angularSpeed = 0.09 + this.random.nextDouble() * 0.04; // медленно
        this.lifetime = 60 + this.random.nextInt(45);
        this.quadSize = 0.10f + this.random.nextFloat() * 0.09f;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.friction = 1f;
        this.setColor(0f, 1f, 0.8f);
        this.alpha = 0.95f;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }

        this.angle += this.angularSpeed; // орбита вокруг оси
        this.radius *= 0.965;            // спираль внутрь → в линию
        this.y += (this.cy - this.y) * 0.03; // лёгкое стягивание к центру по высоте
        this.x = this.cx + this.radius * Math.cos(this.angle);
        this.z = this.cz + this.radius * Math.sin(this.angle);
        this.setBoundingBox(new AABB(this.x - 0.05, this.y - 0.05, this.z - 0.05,
                                     this.x + 0.05, this.y + 0.05, this.z + 0.05));

        float t = (float) this.age / (float) this.lifetime;
        this.setColor(Mth.lerp(t, 0f, 0.75f), Mth.lerp(t, 1f, 0.25f), Mth.lerp(t, 0.8f, 1f)); // бирюза→фиолет
        this.alpha = (1f - t) * 0.95f;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; } // full bright — свечение

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            // dx,dy,dz несут ЦЕНТР орбиты (velocity-слоты sendParticles, speed=1)
            return new EnergyMoteParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
