package com.jykito.industrialcore.item.upgrade;

import com.jykito.industrialcore.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.stream.Stream;

/**
 * Where each shared upgrade WORKS: a single source for the Shift tooltip and for the machines'
 * upgrade slots. A machine is on the list only if it actually reads this upgrade, so
 * useless combinations (a fluid pusher in a furnace) cannot be put into the slot.
 * <p>
 * ⚠ If you teach a machine a new upgrade or take one away, fix the list here: the upgrade
 * slots ({@link #works}) use it too, and without the fix the machine will not even accept the new upgrade.
 */
public final class UpgradeTargets {

    private UpgradeTargets() {}

    /** Simple machines with the common set: speed, parallel, items in and out. */
    private static final List<RegistryObject<Block>> BASIC = List.of(
            ModBlocks.CRUSHER, ModBlocks.COMPRESSOR, ModBlocks.EXTRACTOR, ModBlocks.METAL_FORMER,
            ModBlocks.ELECTRIC_FURNACE, ModBlocks.ORE_WASHER, ModBlocks.ROD_FABRICATOR, ModBlocks.PHASE_SPLITTER);

    public static final List<RegistryObject<Block>> ACCELERATOR = with(BASIC,
            ModBlocks.CAPSULE_FILLER, ModBlocks.CULTIVATOR, ModBlocks.CULTIVATOR_T2, ModBlocks.FLUID_ENRICHER, ModBlocks.LIQUEFIER,
            ModBlocks.CRYSTALLIZER, ModBlocks.STONE_GENERATOR, ModBlocks.LAVA_GENERATOR, ModBlocks.WATER_GENERATOR,
            ModBlocks.MINER_CORE, ModBlocks.BIOREACTOR);

    public static final List<RegistryObject<Block>> PARALLEL = with(BASIC,
            ModBlocks.CAPSULE_FILLER, ModBlocks.CULTIVATOR, ModBlocks.CULTIVATOR_T2, ModBlocks.FLUID_ENRICHER, ModBlocks.LIQUEFIER,
            ModBlocks.CRYSTALLIZER, ModBlocks.STONE_GENERATOR, ModBlocks.MINER_CORE);

    public static final List<RegistryObject<Block>> ITEM_PUSH = with(BASIC,
            ModBlocks.CAPSULE_FILLER, ModBlocks.CULTIVATOR, ModBlocks.CULTIVATOR_T2, ModBlocks.FLUID_ENRICHER, ModBlocks.CRYSTALLIZER,
            ModBlocks.STONE_GENERATOR, ModBlocks.BLAST_FURNACE, ModBlocks.RESTRUCTOR, ModBlocks.RESTRUCTOR_T2, ModBlocks.MATTER_FABRICATOR,
            ModBlocks.BIOREACTOR);

    public static final List<RegistryObject<Block>> ITEM_PULL = with(BASIC,
            ModBlocks.CAPSULE_FILLER, ModBlocks.FLUID_ENRICHER, ModBlocks.LIQUEFIER, ModBlocks.CRYSTALLIZER,
            ModBlocks.BLAST_FURNACE, ModBlocks.RESTRUCTOR, ModBlocks.RESTRUCTOR_T2, ModBlocks.BIOREACTOR);

    public static final List<RegistryObject<Block>> FLUID_PUSH = List.of(
            ModBlocks.CAPSULE_FILLER, ModBlocks.FLUID_ENRICHER, ModBlocks.LIQUEFIER,
            ModBlocks.LAVA_GENERATOR, ModBlocks.WATER_GENERATOR);

    public static final List<RegistryObject<Block>> FLUID_PULL = List.of(
            ModBlocks.ORE_WASHER, ModBlocks.CAPSULE_FILLER, ModBlocks.CULTIVATOR, ModBlocks.CULTIVATOR_T2, ModBlocks.FLUID_ENRICHER,
            ModBlocks.CRYSTALLIZER, ModBlocks.STONE_GENERATOR, ModBlocks.BIOREACTOR);

    /**
     * Whether a shared upgrade works in this machine. Machine upgrade slots check through this
     * method, so a slot accepts exactly what the tooltip says and nothing useless.
     * Private upgrades (blast furnace, cultivator) do not belong here and return false.
     */
    public static boolean works(Block machine, ItemStack stack) {
        Item item = stack.getItem();
        // processor strain: ore washer only (author, 2026-10-07); the multiblock ore washer keeps it in the core
        if (item instanceof com.jykito.industrialcore.item.custom.NaniteStrainItem)
            return machine == ModBlocks.ORE_WASHER.get()
                    && com.jykito.industrialcore.nanite.NaniteGenome.kindOf(stack) == com.jykito.industrialcore.nanite.NaniteGenome.Kind.PROCESSOR;
        List<RegistryObject<Block>> targets;
        if (item instanceof AcceleratorItem) targets = ACCELERATOR;
        else if (item instanceof ParallelModuleItem || item instanceof AdvancedParallelModuleItem) targets = PARALLEL;
        else if (item instanceof DirectionalUpgradeItem dir) targets = forMode(dir.getMode());
        else return false;
        for (RegistryObject<Block> b : targets)
            if (b.get() == machine) return true;
        return false;
    }

    public static List<RegistryObject<Block>> forMode(DirectionalUpgradeItem.Mode mode) {
        return switch (mode) {
            case ITEM_PUSH  -> ITEM_PUSH;
            case ITEM_PULL  -> ITEM_PULL;
            case FLUID_PUSH -> FLUID_PUSH;
            case FLUID_PULL -> FLUID_PULL;
        };
    }

    /**
     * Without Shift a single hint line, with Shift the list of machines. Names come from the block
     * names, so nothing needs separate translation.
     * <p>
     * Screen is safe here: appendHoverText is only called on the client.
     */
    @SafeVarargs
    public static void append(List<Component> tooltip, RegistryObject<Block>... targets) {
        append(tooltip, List.of(targets));
    }

    public static void append(List<Component> tooltip, List<RegistryObject<Block>> targets) {
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.industrial_core.upgrade.shift_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.industrial_core.upgrade.works_in")
                .withStyle(ChatFormatting.GRAY));
        for (RegistryObject<Block> b : targets)
            tooltip.add(Component.translatable("tooltip.industrial_core.upgrade.target",
                    Component.translatable(b.get().getDescriptionId())).withStyle(ChatFormatting.DARK_AQUA));
    }

    @SafeVarargs
    private static List<RegistryObject<Block>> with(List<RegistryObject<Block>> base, RegistryObject<Block>... more) {
        return Stream.concat(base.stream(), Stream.of(more)).toList();
    }
}
