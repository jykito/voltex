package com.jykito.industrialcore.item.custom.weapon;

import com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import com.jykito.industrialcore.networking.GaussShotPacket;
import com.jykito.industrialcore.networking.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Gauss rifle (author, 2026-10-07: "let's do a gauss rifle"; damage tripled; pairs with Plasma armor).
 * <ul>
 * <li>Hold RMB: charges in stages of {@link #STAGE_TICKS} ticks, release: a beam shot without drop over {@link #RANGE}
 *     blocks: damage {@link #DAMAGE}, pierces {@link #PIERCE} targets, knocks back; costs {@link #COST} FE. If the charge
 *     is not enough for the reached stage, it fires one stage lower. After a shot, a {@link #COOLDOWN} tick cooldown.</li>
 * <li>Plasma: charge shared with the suit; a full set charges twice as fast; if the armor has a plasma blast ready,
 *     a third-stage shot spends it: an explosion at the hit point (does not break blocks) and sets targets on the line on fire.</li>
 * </ul>
 */
public class GaussRifleItem extends EnergyWeaponItem {

    public static final int CAPACITY = 200_000, RANGE = 64, COOLDOWN = 20, STAGE_TICKS = 20;
    public static final int[] COST = { 0, 2_000, 5_000, 10_000 };
    public static final float[] DAMAGE = { 0, 24, 48, 90 };
    public static final int[] PIERCE = { 0, 1, 3, Integer.MAX_VALUE };
    private static final float[] PUSH = { 0, 0.4f, 0.8f, 1.4f };
    public static final float BLAST_DAMAGE = 30f, BLAST_RADIUS = 4f;

    public GaussRifleItem(Properties properties) { super(CAPACITY, properties); }

    /** In hand, on the ground and in item frames a volumetric "Fork" (GaussRenderer); in the inventory the item model gives a flat icon. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.jykito.industrialcore.client.weapon.GaussRenderer.get();
            }

            /**
             * In third person the rifle is held forward with both hands, like a loaded crossbow (author, 2026-10-10: "it charges pointing up,
             * it should point forward"). The grip in the model (thirdperson [0,-90,135]) is made for a raised arm: that way the barrel goes
             * forward along the arm both while charging (bow pose from UseAnim.BOW) and at rest. With a lowered arm the barrel would point at the ground.
             */
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                    net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    // ───────── suit: Plasma ─────────

    @Override protected List<ItemStack> suit(Player player) { return pieces(player, s -> s.getItem() instanceof PlasmaArmorItem); }
    @Override protected int suitEnergy(ItemStack piece) { return PlasmaArmorItem.getEnergy(piece); }
    @Override protected void setSuitEnergy(ItemStack piece, int amount) { PlasmaArmorItem.setEnergy(piece, amount); }

    public static boolean fullPlasma(Player player) { return pieces(player, s -> s.getItem() instanceof PlasmaArmorItem).size() == 4; }

    /** Stage by hold time: 0 means not reached yet. With a full Plasma set it is twice as fast. */
    public static int stage(int ticks, boolean fast) { return Math.min(3, ticks / (fast ? STAGE_TICKS / 2 : STAGE_TICKS)); }

    // ───────── shot ─────────

    @Override public int getUseDuration(ItemStack stack) { return 72_000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getEnergy(stack) < COST[1]) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.4f);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Charge hum: each reached stage sounds higher. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        int ticks = getUseDuration(stack) - remaining, per = fullPlasma(player) ? STAGE_TICKS / 2 : STAGE_TICKS;
        if (ticks > 0 && ticks % per == 0 && ticks / per <= 3)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.8f + 0.3f * (ticks / per));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        int st = stage(getUseDuration(stack) - timeLeft, fullPlasma(player));
        while (st > 0 && getEnergy(stack) < COST[st]) st--;
        if (st == 0) return;
        drain(stack, COST[st]);
        List<ItemStack> plasma = pieces(player, s -> s.getItem() instanceof PlasmaArmorItem);
        boolean blast = st == 3 && plasma.size() == 4 && PlasmaArmorEventHandler.isBurstReady(plasma);
        if (blast) { for (ItemStack p : plasma) PlasmaArmorItem.setBurstCharge(p, 0); player.getInventory().setChanged(); }
        fire(player, (ServerLevel) level, st, blast, !plasma.isEmpty());
        player.getCooldowns().addCooldown(this, COOLDOWN);
    }

    private static void fire(Player player, ServerLevel level, int st, boolean blast, boolean plasmaTrace) {
        Vec3 start = player.getEyePosition(), dir = player.getLookAngle(), far = start.add(dir.scale(RANGE));
        HitResult block = level.clip(new ClipContext(start, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        // targets on the line, in order
        record Hit(LivingEntity e, Vec3 at, double d) {}
        List<Hit> hits = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, stop).inflate(1.0),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.isPickable() && !(e instanceof TamableAnimal pet && pet.isOwnedBy(player)))) {
            Optional<Vec3> at = e.getBoundingBox().inflate(0.3).clip(start, stop);
            at.ifPresent(v -> hits.add(new Hit(e, v, start.distanceToSqr(v))));
        }
        hits.sort(Comparator.comparingDouble(Hit::d));
        int n = Math.min(hits.size(), PIERCE[st]);
        for (int i = 0; i < n; i++) {
            LivingEntity e = hits.get(i).e();
            e.hurt(player.damageSources().playerAttack(player), DAMAGE[st]);
            if (st == 3 && e.isDeadOrDying() && player instanceof net.minecraft.server.level.ServerPlayer sp)
                com.jykito.industrialcore.advancement.VoltexEventTrigger.INSTANCE.trigger(sp,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.GAUSS_STAGE3_KILL);
            e.knockback(PUSH[st], -dir.x, -dir.z);
            if (blast) e.setSecondsOnFire(5);
        }
        // the beam ends at the last pierced target if there is nobody left to pierce
        Vec3 end = n > 0 && n == PIERCE[st] ? hits.get(n - 1).at() : stop;
        // clients draw the shot (beam, spiral, flashes in GaussBeams; author: "a proper shot instead of crappy particles"):
        // it starts at the muzzle, to the right and below the eyes, where the barrel of the rifle in hand is
        Vec3 right = Math.abs(dir.y) < 0.99 ? dir.cross(new Vec3(0, 1, 0)).normalize() : new Vec3(1, 0, 0), up = right.cross(dir);
        Vec3 muzzle = start.add(dir.scale(0.8)).add(right.scale(0.28)).add(up.scale(-0.22));
        ModMessages.sendToTrackingAndSelf(new GaussShotPacket(muzzle, end, st, plasmaTrace, blast, player.getId()), player);
        if (blast) blast(player, level, end);
        SoundEvent snd = st == 3 ? SoundEvents.WARDEN_SONIC_BOOM : SoundEvents.FIREWORK_ROCKET_BLAST;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), snd, SoundSource.PLAYERS, st == 3 ? 1.2f : 0.9f, st == 1 ? 1.5f : 1.0f);
    }

    /** Plasma shot: an explosion at the hit point, hitting living things around with falloff to the edge; blocks and the shooter are not harmed. */
    private static void blast(Player player, ServerLevel level, Vec3 at) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BLAST_RADIUS),
                e -> e != player && e.isAlive() && !(e instanceof TamableAnimal pet && pet.isOwnedBy(player)))) {
            double d = e.position().distanceTo(at);
            if (d > BLAST_RADIUS) continue;
            e.hurt(player.damageSources().playerAttack(player), (float) (BLAST_DAMAGE * (1 - d / BLAST_RADIUS * 0.6)));
            e.setSecondsOnFire(5);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.industrial_core.gauss_rifle.stages",
                (int) DAMAGE[1], (int) DAMAGE[2], (int) DAMAGE[3]).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.gauss_rifle.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.gauss_rifle.plasma").withStyle(ChatFormatting.GREEN));
    }
}
