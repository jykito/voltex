package com.jykito.industrialcore.capability;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Capabilities after the port to NeoForge 1.21.
 * <ul>
 *   <li>Block entities expose them through {@link Provider#getCap}; there is one registration for all of the mod's BE types ({@link #register}).</li>
 *   <li>Consumers ask through {@link #get}: it returns an {@link Optional}, so the old LazyOptional chains
 *       {@code .ifPresent / .map / .orElse} stayed as they were.</li>
 * </ul>
 */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Caps {
    private Caps() {}

    /** Capability kind: block and item halves (either can be absent). */
    public record Kind<T>(@Nullable BlockCapability<T, Direction> block, @Nullable ItemCapability<T, Void> item) {}

    public static final Kind<IEnergyStorage> ENERGY = new Kind<>(Capabilities.EnergyStorage.BLOCK, Capabilities.EnergyStorage.ITEM);
    public static final Kind<IItemHandler> ITEM_HANDLER = new Kind<>(Capabilities.ItemHandler.BLOCK, Capabilities.ItemHandler.ITEM);
    public static final Kind<IFluidHandler> FLUID_HANDLER = new Kind<>(Capabilities.FluidHandler.BLOCK, null);
    public static final Kind<IFluidHandlerItem> FLUID_HANDLER_ITEM = new Kind<>(null, Capabilities.FluidHandler.ITEM);
    public static final Kind<IHeatHandler> HEAT = new Kind<>(HeatCapability.HEAT, null);

    private static final List<BlockCapability<?, Direction>> BLOCK_CAPS = List.of(
            Capabilities.EnergyStorage.BLOCK, Capabilities.ItemHandler.BLOCK, Capabilities.FluidHandler.BLOCK, HeatCapability.HEAT);

    /** A block entity that decides itself what to expose on which side (formerly getCapability). */
    public interface Provider {
        @Nullable <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side);
    }

    // ── consumers ──

    public static <T> Optional<T> get(@Nullable BlockEntity be, Kind<T> kind, @Nullable Direction side) {
        if (be == null || kind.block() == null) return Optional.empty();
        Level level = be.getLevel();
        if (level != null)
            return Optional.ofNullable(level.getCapability(kind.block(), be.getBlockPos(), be.getBlockState(), be, side));
        return be instanceof Provider p ? Optional.ofNullable(p.getCap(kind.block(), side)) : Optional.empty();
    }

    public static <T> Optional<T> get(@Nullable BlockEntity be, Kind<T> kind) {
        return get(be, kind, null);
    }

    public static <T> Optional<T> get(ItemStack stack, Kind<T> kind) {
        if (stack.isEmpty() || kind.item() == null) return Optional.empty();
        return Optional.ofNullable(stack.getCapability(kind.item()));
    }

    /** Items have no sides: the form with a side is kept for old {@code getCapability(cap, null)} calls. */
    public static <T> Optional<T> get(ItemStack stack, Kind<T> kind, @Nullable Direction ignored) {
        return get(stack, kind);
    }

    // ── registration ──

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        for (var holder : ModBlockEntities.BLOCK_ENTITIES.getEntries())
            for (BlockCapability<?, Direction> cap : BLOCK_CAPS)
                registerBlockEntity(event, cap, holder.get());
        ItemCaps.register(event);
        com.jykito.industrialcore.compat.ae2.MePorts.registerCaps(event);
    }

    private static <T, BE extends BlockEntity> void registerBlockEntity(RegisterCapabilitiesEvent event,
            BlockCapability<T, Direction> cap, BlockEntityType<BE> type) {
        event.registerBlockEntity(cap, type, (be, side) -> be instanceof Provider p ? p.getCap(cap, side) : null);
    }
}
