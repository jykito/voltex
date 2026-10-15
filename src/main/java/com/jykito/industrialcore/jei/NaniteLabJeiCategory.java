package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.NaniteLabBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.nanite.NaniteAtlas;
import com.jykito.industrialcore.nanite.NaniteColors;
import com.jykito.industrialcore.nanite.NaniteFeed;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteLab;
import com.jykito.industrialcore.nanite.NaniteTrait;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Nanite lab JEI window (mockup N3 "Topic ladder", the author's choice 2026-10-09: "show where to get the memory plates").
 * One recipe per topic. On the left all four topics in a column: sample, name, knowledge bar with the admission threshold;
 * this recipe's topic is highlighted, the others dimmed, so you see which step comes after which. On the right the recipe itself:
 * a scientist strain (not consumed, wears out) and a blank card -> the topic card, usage and writing time below
 * (at the threshold {@link NaniteLab#ticks} of the threshold, at full knowledge FAST_TICKS). The frame is the late frame, as in the
 * late machine windows ({@link LateJeiCategory.Frame}), the cells are the author's cell from the port window.
 */
public class NaniteLabJeiCategory implements IRecipeCategory<NaniteLabJeiCategory.Theme> {

    /** Topic and its knowledge threshold. */
    public record Theme(String key, float threshold) {}

    public static List<Theme> themes() {
        List<Theme> out = new ArrayList<>();
        for (Map.Entry<String, Float> e : NaniteLab.THRESHOLDS.entrySet()) out.add(new Theme(e.getKey(), e.getValue()));
        return out;
    }

    private static final ResourceLocation PROGRESS = new ResourceLocation(IndustrialCore.MODID, "textures/gui/progress_fill_6x_anim.png");
    private static final int W = 176, H = 104, CELL = 18;
    private static final int ROW_X = 7, ROW_Y = 7, ROW_STEP = 22, ROW_W = 120, ROW_H = 21;
    private static final int BAR_X = 31, BAR_W = 64, BAR_H = 5;   // line: sample, name, bar up to the threshold and "N%"; the topic card as the output on the right
    private static final int STRAIN_X = 131, BLANK_X = 151, IN_Y = 7, PROG_X = 132, PROG_Y = 31, PROG_W = 36, PROG_H = 8, OUT_X = 141, OUT_Y = 45;
    private static final int INFO_X = 131, INFO_Y = 70;
    private static final int C_DIM = 0xFF6A6A6A, C_TIME = 0xFFB0B0B0, PROG_CYCLE_MS = 2000;

    private final RecipeType<Theme> type;
    private final Component title;
    private final IDrawable icon, cell, background;
    private final JeiGui.Anim chevrons = new JeiGui.Anim();

    public NaniteLabJeiCategory(IGuiHelper helper, RecipeType<Theme> type, ItemStack lab, Component title) {
        this.type = type;
        this.title = title;
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, lab);
        this.cell = helper.drawableBuilder(LateJeiCategory.PARTS, 96, 0, CELL, CELL).setTextureSize(LateJeiCategory.PARTS_W, LateJeiCategory.PARTS_H).build();
        this.background = new LateJeiCategory.Frame(W, H);
    }

    @Override public RecipeType<Theme> getRecipeType() { return type; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    private static ItemStack[] samples(String theme) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(IndustrialCore.MODID, NaniteFeed.RESEARCH_PREFIX + theme));
        return Ingredient.of(tag).getItems();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Theme t, IFocusGroup focuses) {
        int pct = Math.round(t.threshold() * 100);
        // the sample of its own topic sits in its line of the ladder
        int row = rowOf(t.key());
        builder.addSlot(RecipeIngredientRole.INPUT, ROW_X + 3, ROW_Y + ROW_STEP * row + 2)
               .setBackground(cell, -1, -1)
               .addItemStacks(Arrays.asList(samples(t.key())))
               .addTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.industrial_core.nanite_lab.keeps").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, STRAIN_X + 1, IN_Y + 1)
               .setBackground(cell, -1, -1)
               .addItemStack(NaniteAtlas.icon(NaniteGenome.Kind.SCIENTIST, t.key(), t.threshold()))
               .addTooltipCallback((v, tip) -> {
                   tip.add(Component.translatable("jei.industrial_core.nanite_lab.knowledge", pct).withStyle(ChatFormatting.AQUA));
                   tip.add(Component.translatable("jei.industrial_core.nanite_lab.wears", Math.round(NaniteLabBlockEntity.WEAR_CHANCE * 100)).withStyle(ChatFormatting.GRAY));
               });
        builder.addSlot(RecipeIngredientRole.INPUT, BLANK_X + 1, IN_Y + 1)
               .setBackground(cell, -1, -1)
               .addItemStack(new ItemStack(ModItems.DATA_CARD_BLANK.get()));
        Item card = NaniteLab.card(t.key());
        if (card != null) builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1, OUT_Y + 1)
               .setBackground(cell, -1, -1)
               .addItemStack(new ItemStack(card))
               .addTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.industrial_core.nanite_lab.resonator",
                       Math.round(NaniteTrait.RESONATOR_CHANCE * 100)).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static int rowOf(String theme) {
        int i = 0;
        for (String k : NaniteLab.THRESHOLDS.keySet()) { if (k.equals(theme)) return i; i++; }
        return 0;
    }

    @Override
    public void draw(Theme t, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        int row = 0;
        for (Map.Entry<String, Float> e : NaniteLab.THRESHOLDS.entrySet()) {
            String key = e.getKey();
            boolean on = key.equals(t.key());
            int y = ROW_Y + ROW_STEP * row++, color = 0xFF000000 | NaniteColors.theme(key);
            box(g, ROW_X, y, ROW_W, ROW_H);
            if (!on) {                                                   // a sample of another topic is a picture, not a slot
                cell.draw(g, ROW_X + 2, y + 1);
                ItemStack[] s = samples(key);
                if (s.length > 0) g.renderItem(s[0], ROW_X + 3, y + 2);
            }
            g.drawString(font, Component.translatable("nanite_theme.industrial_core." + key), BAR_X, y + 3, on ? color : C_DIM, false);
            // knowledge bar: filled up to the threshold, a tick at the threshold
            int fill = Math.round(BAR_W * e.getValue());
            g.fill(BAR_X, y + 13, BAR_X + BAR_W, y + 13 + BAR_H, 0xFF0A0A0A);
            g.fill(BAR_X, y + 13, BAR_X + fill, y + 13 + BAR_H, on ? color : dim(color));
            g.fill(BAR_X + fill, y + 12, BAR_X + fill + 1, y + 14 + BAR_H, 0xFFE0E0E0);
            g.drawString(font, Math.round(e.getValue() * 100) + "%", BAR_X + BAR_W + 4, y + 12, on ? color : C_DIM, false);
            if (!on) g.fill(ROW_X + 1, y + 1, ROW_X + ROW_W - 1, y + ROW_H - 1, 0x80171717);
        }
        // progress as in the late machine windows: a recess, a chevron strip in a loop
        box(g, PROG_X, PROG_Y, PROG_W, PROG_H);
        chevrons.advance();
        int filled = (int) (System.currentTimeMillis() % PROG_CYCLE_MS * (PROG_W - 2) / PROG_CYCLE_MS);
        if (filled > 0) JeiGui.tileFill(g, PROGRESS, PROG_X + 1, PROG_Y + 1, filled, chevrons.frame());
        // usage and writing time: at the threshold -> at full knowledge
        JeiGui.smallText(g, font, JeiGui.fe(NaniteLabBlockEntity.ENERGY_PER_TICK) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(NaniteLab.ticks(t.threshold())) + " → " + JeiGui.secs(NaniteLab.FAST_TICKS), INFO_X, INFO_Y + JeiGui.SMALL_LINE, C_TIME);
    }

    /** Recess like the core display: dark top-left, light bottom-right. */
    private static void box(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF0F0F0F);
        g.fill(x, y, x + w, y + 1, 0xFF1B1B1B); g.fill(x, y, x + 1, y + h, 0xFF1B1B1B);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF6E6E6E); g.fill(x + w - 1, y, x + w, y + h, 0xFF6E6E6E);
    }

    private static int dim(int argb) {
        int r = (argb >> 16 & 255) * 2 / 5, gr = (argb >> 8 & 255) * 2 / 5, b = (argb & 255) * 2 / 5;
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }
}
