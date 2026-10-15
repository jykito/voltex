package com.jykito.industrialcore.item.custom.weapon;
import net.minecraft.world.item.Item;
import com.jykito.industrialcore.item.ModDataComponents;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jykito.industrialcore.item.custom.NexiteArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Energy blade (author, 2026-10-07: "let's do an energy blade"; damage 15; pairs with Nexite armor).
 * <ul>
 * <li>Charged it hits for {@link #DAMAGE}, a hit costs {@link #COST} FE; empty it is a bare hilt for {@link #DAMAGE_OFF}.</li>
 * <li>Modes (Shift + RMB): "Cut" hits one target and adds {@link #PIERCE} of the damage its armor would have eaten
 *     (computed in {@link WeaponEventHandler}); "Arc" also sweeps a fan in front of you of {@link #ARC_RADIUS} blocks,
 *     each target takes {@link #ARC_SHARE} of the damage, the hit costs twice as much.</li>
 * <li>Nexite: charge shared with the suit; counter-strike: damage absorbed by the armor accumulates on the player (up to {@link #COUNTER_MAX},
 *     burns out after {@link #COUNTER_TTL} ticks) and is added to the next hit; a full charged set gives higher attack
 *     speed and "Arc" at the price of a normal hit.</li>
 * </ul>
 */
public class EnergyBladeItem extends EnergyWeaponItem {

    public static final int CAPACITY = 100_000, COST = 400;
    public static final float DAMAGE = 15f, DAMAGE_OFF = 3f, ATTACK_SPEED = -2.4f;
    public static final float PIERCE = 0.3f, ARC_SHARE = 0.7f, ARC_RADIUS = 3f;
    public static final float COUNTER_MAX = 7.5f;
    public static final int COUNTER_TTL = 200;
    public static final int MODE_CUT = 0, MODE_ARC = 1;

    public EnergyBladeItem(Properties properties) { super(CAPACITY, properties); }

    public static boolean charged(ItemStack stack) { return getEnergy(stack) >= COST; }
    public static int mode(ItemStack stack) { return stack.getOrDefault(ModDataComponents.MODE.get(), MODE_CUT); }
    private static String modeKey(int mode) { return "tooltip.industrial_core.energy_blade.mode_" + (mode == MODE_ARC ? "arc" : "cut"); }

    // ───────── suit: Nexite ─────────

    @Override protected List<ItemStack> suit(Player player) { return pieces(player, s -> s.getItem() instanceof NexiteArmorItem); }
    @Override protected int suitEnergy(ItemStack piece) { return NexiteArmorItem.getEnergy(piece); }
    @Override protected void setSuitEnergy(ItemStack piece, int amount) { NexiteArmorItem.setEnergy(piece, amount); }

    /** A full Nexite set, and every piece has charge. */
    public static boolean fullNexite(Player player) {
        List<ItemStack> p = pieces(player, s -> s.getItem() instanceof NexiteArmorItem);
        return p.size() == 4 && p.stream().allMatch(s -> NexiteArmorItem.getEnergy(s) > 0);
    }

    /** Counter-strike: damage absorbed by Nexite accumulates on the player. Called from NexiteArmorEventHandler. */
    public static void addCounter(Player player, float absorbed) {
        if (absorbed <= 0) return;
        CompoundTag t = player.getPersistentData();
        long now = player.level().getGameTime();
        float cur = now - t.getLong("VoltexCounterT") > COUNTER_TTL ? 0 : t.getFloat("VoltexCounter");
        t.putFloat("VoltexCounter", Math.min(COUNTER_MAX, cur + absorbed));
        t.putLong("VoltexCounterT", now);
    }

    /** How much counter-strike is accumulated right now (without taking it). */
    public static float counter(Player player) {
        CompoundTag t = player.getPersistentData();
        return player.level().getGameTime() - t.getLong("VoltexCounterT") > COUNTER_TTL ? 0 : t.getFloat("VoltexCounter");
    }

    /** Take the accumulated counter-strike (and reset it); a burned-out one is zero. */
    public static float takeCounter(Player player) {
        CompoundTag t = player.getPersistentData();
        float v = player.level().getGameTime() - t.getLong("VoltexCounterT") > COUNTER_TTL ? 0 : t.getFloat("VoltexCounter");
        t.putFloat("VoltexCounter", 0);
        return v;
    }

    // ───────── combat ─────────

    /** Main hand only. Asked again whenever the stack changes, so the damage follows the charge. */
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        (charged(stack) ? DAMAGE : DAMAGE_OFF) - 1, AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            int m = mode(stack) == MODE_ARC ? MODE_CUT : MODE_ARC;
            stack.set(ModDataComponents.MODE.get(), m);
            player.displayClientMessage(Component.translatable("message.industrial_core.energy_blade.mode",
                    Component.translatable(modeKey(m))), true);
            level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4f, m == MODE_ARC ? 0.8f : 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The damage is already dealt ("Cut" and counter-strike bonuses are in WeaponEventHandler); here we spend charge and do "Arc". */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || player.level().isClientSide() || !charged(stack)) return true;
        drain(stack, COST);
        if (mode(stack) == MODE_ARC && (fullNexite(player) || drain(stack, COST))) arc(player, target);
        if (player.level() instanceof ServerLevel srv)
            srv.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 6, 0.2, 0.25, 0.2, 0.1);
        return true;
    }

    private static void arc(Player player, LivingEntity first) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        float dmg = DAMAGE * ARC_SHARE;
        List<LivingEntity> hit = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ARC_RADIUS, 1, ARC_RADIUS),
                e -> e != player && e != first && e.isAlive() && !(e instanceof ArmorStand) && !player.isAlliedTo(e)
                        && !(e instanceof TamableAnimal pet && pet.isOwnedBy(player)));
        for (LivingEntity e : hit) {
            Vec3 to = new Vec3(e.getX() - player.getX(), 0, e.getZ() - player.getZ());
            if (to.lengthSqr() > ARC_RADIUS * ARC_RADIUS || to.normalize().dot(look) < 0.3) continue;
            e.knockback(0.4, -look.x, -look.z);
            e.hurt(player.damageSources().playerAttack(player), dmg);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.3f);
        if (player.level() instanceof ServerLevel srv)
            srv.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 1.5, player.getY(0.55), player.getZ() + look.z * 1.5, 1, 0, 0, 0, 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        @Nullable Level level = context.level();
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.industrial_core.energy_blade.mode", Component.translatable(modeKey(mode(stack))))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.energy_blade.switch").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.energy_blade.nexite").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
