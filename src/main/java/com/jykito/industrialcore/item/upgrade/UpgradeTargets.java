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
 * Где какой общий апгрейд РАБОТАЕТ: единый источник для тултипа под Shift и для слотов
 * апгрейдов машин. Машина в списке только если она этот апгрейд реально читает, поэтому
 * бесполезное (выталкиватель жидкости в печь) в слот не положить.
 * <p>
 * ⚠ Научил машину новому апгрейду или отнял — поправь список здесь: по нему же работают
 * слоты апгрейдов ({@link #works}), без правки машина новый апгрейд даже не примет.
 */
public final class UpgradeTargets {

    private UpgradeTargets() {}

    /** Простые машины с общим набором: скорость, параллель, предметы туда-обратно. */
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
     * Работает ли общий апгрейд в этой машине. Слоты апгрейдов машин проверяют через этот
     * метод, поэтому слот принимает ровно то, что указано в тултипе, и ничего бесполезного.
     * Частные апгрейды (доменки, взрастителя) сюда не относятся и дают false.
     */
    public static boolean works(Block machine, ItemStack stack) {
        Item item = stack.getItem();
        // штамм-переработчик: только промывщик (юзер 2026-10-07); мультиблочный промывщик держит его в ядре
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
     * Без Shift одна строка-подсказка, с Shift список машин. Имена берутся из названий
     * блоков, так что переводить отдельно ничего не надо.
     * <p>
     * Screen здесь безопасен: appendHoverText вызывается только на клиенте.
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
