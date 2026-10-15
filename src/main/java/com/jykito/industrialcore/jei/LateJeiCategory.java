package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.multiblock.LateRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Late machine JEI window (mockup L1 "Ports", the author's choice 2026-10-09): a mode line centred at the top (if the core has
 * several), a tank on the input, inputs in a grid up to 6x3 (the Heart of Voltex has 17 inputs and essence, the assembler up to 15), progress
 * as in the core window (a chevron strip), the output (by-product below it, a tank on the output), at the bottom usage, time and heat
 * threshold. The frame is the author's late frame, assembled to the window size from a piece of late_parts.png (tools/gui_gen/jei_late/parts.js);
 * the cells are the author's cell from the port window. There is no core icon in the header: the machine name is already in the JEI title (author: "the icon
 * either needs text or should go"). The layout is computed once over all of the core's recipes: there are as many cells as
 * its recipes ever have inputs, the tank and the by-product cell only if at least one recipe has them;
 * the content is centred.
 */
public class LateJeiCategory implements IRecipeCategory<LateJeiCategory.Entry> {

    /** Core recipe and mode label (null: the core has a single mode, nothing to label). */
    public record Entry(LateRecipe recipe, Component mode) {}

    static final ResourceLocation PARTS = new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/late_parts.png");
    private static final ResourceLocation PROGRESS = new ResourceLocation(IndustrialCore.MODID, "textures/gui/progress_fill_6x_anim.png");
    static final int PARTS_W = 128, PARTS_H = 64;
    private static final int CELL = 18, TANK_W = 12, PROG_W = 24, PROG_H = 8, GAP = 4, COLS = 6;
    /** The window is at least 184 wide with 14 px margins (frame corners): the magnet and assembler content is 174, and at 184 the tank and text sat on the frame. */
    private static final int MIN_WIDTH = 184, MARGIN = 14, MODE_Y = 10, LINE = 11, PROG_CYCLE_MS = 2000;
    private static final int C_MODE = 0xFFE0C060, C_TIME = 0xFFB0B0B0, C_HEAT = 0xFFFFA347;

    private final RecipeType<Entry> recipeType;
    private final Component title;
    private final IDrawable icon, cell, background;
    private final JeiGui.Anim chevrons = new JeiGui.Anim();

    // layout: x is counted from the left edge of the window, already centred
    private final int width, bodyY, cols, rows, bodyH, height;
    private final int tankInX, gridX, gridY, progX, progY, outX, outY, byX, byY, tankOutX;
    private final boolean fluidIn, itemOut, byproduct, fluidOut, heat;

    public LateJeiCategory(IGuiHelper helper, RecipeType<Entry> type, ItemStack core, Component title, List<Entry> recipes) {
        this.recipeType = type;
        this.title = title;
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, core);
        this.cell = helper.drawableBuilder(PARTS, 96, 0, CELL, CELL).setTextureSize(PARTS_W, PARTS_H).build();

        int maxIn = 1;
        boolean fIn = false, iOut = false, by = false, fOut = false, ht = false, modes = false;
        for (Entry e : recipes) {
            LateRecipe r = e.recipe();
            maxIn = Math.max(maxIn, r.items().length);
            fIn |= !r.fluid().isEmpty();
            iOut |= !r.result().isEmpty();
            by |= !r.byproduct().isEmpty();
            fOut |= !r.fluidResult().isEmpty();
            ht |= r.heat() > 0;
            modes |= e.mode() != null;
        }
        fluidIn = fIn; itemOut = iOut || !fOut; byproduct = by; fluidOut = fOut; heat = ht;
        bodyY = modes ? 24 : 12;                                       // without the mode line the body goes right under the frame
        cols = Math.min(COLS, maxIn);
        rows = Math.min(3, (maxIn + COLS - 1) / COLS);
        bodyH = Math.max(rows * CELL, Math.max(fluidIn || fluidOut ? 2 * CELL : CELL, byproduct ? 2 * CELL + 2 : 0));

        // content width and centring
        int w = (fluidIn ? TANK_W + GAP : 0) + cols * CELL + GAP + PROG_W + GAP
                + (itemOut ? CELL : 0) + (itemOut && fluidOut ? GAP : 0) + (fluidOut ? TANK_W : 0);
        width = Math.max(MIN_WIDTH, w + 2 * MARGIN);
        int x = (width - w) / 2;
        tankInX = x;                    if (fluidIn) x += TANK_W + GAP;
        gridX = x;                      x += cols * CELL + GAP;
        gridY = bodyY + (bodyH - rows * CELL) / 2;
        progX = x;                      x += PROG_W + GAP;
        progY = bodyY + (bodyH - PROG_H) / 2;
        outX = x;                       if (itemOut) x += CELL + (fluidOut ? GAP : 0);
        outY = byproduct ? bodyY + bodyH / 2 - CELL - 1 : bodyY + (bodyH - CELL) / 2;
        byX = outX; byY = bodyY + bodyH / 2 + 1;
        tankOutX = x;
        height = bodyY + bodyH + 6 + LINE + (heat ? LINE : 0) + 6;
        this.background = new Frame(width, height);
    }

    @Override public RecipeType<Entry> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        LateRecipe r = entry.recipe();
        for (int i = 0; i < r.items().length && i < cols * rows; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, gridX + CELL * (i % cols) + 1, gridY + CELL * (i / cols) + 1)
                   .setBackground(cell, -1, -1)
                   .addItemStacks(withCount(r.items()[i].getItems(), r.counts()[i]));
        }
        if (!r.fluid().isEmpty()) tank(builder, RecipeIngredientRole.INPUT, tankInX, r.fluid());
        if (!r.result().isEmpty()) chance(builder.addSlot(RecipeIngredientRole.OUTPUT, outX + 1, outY + 1)
                .setBackground(cell, -1, -1).addItemStack(r.result()), r.resultChance());
        if (!r.byproduct().isEmpty()) chance(builder.addSlot(RecipeIngredientRole.OUTPUT, byX + 1, byY + 1)
                .setBackground(cell, -1, -1).addItemStack(r.byproduct()), r.byproductChance());
        if (!r.fluidResult().isEmpty()) tank(builder, RecipeIngredientRole.OUTPUT, tankOutX, r.fluidResult());
    }

    private void tank(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, FluidStack fluid) {
        builder.addSlot(role, x + 1, bodyY + 1)
               .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W - 2, bodyH - 2))
               .addFluidStack(fluid.getFluid(), fluid.getAmount());
    }

    private static void chance(IRecipeSlotBuilder slot, float chance) {
        if (chance >= 1f) return;
        int pct = Math.round(chance * 100);
        slot.addTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.industrial_core.chance", pct).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(Entry entry, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        LateRecipe r = entry.recipe();
        var font = Minecraft.getInstance().font;
        int left = fluidIn ? tankInX : gridX;                 // left edge of the content: the bottom lines align to it
        if (entry.mode() != null) g.drawString(font, entry.mode(), (width - font.width(entry.mode())) / 2, MODE_Y, C_MODE, false);
        // unused grid cells (the recipe has fewer inputs than the core's longest recipe) are dimmed
        for (int i = r.items().length; i < cols * rows; i++) {
            int cx = gridX + CELL * (i % cols), cy = gridY + CELL * (i / cols);
            cell.draw(g, cx, cy);
            g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x99000000);
        }
        // tank frames (like the author's display: dark top-left, light bottom-right): always present on a core with a tank, even if the recipe has none
        if (fluidIn) gauge(g, tankInX);
        if (fluidOut) gauge(g, tankOutX);
        if (itemOut && r.result().isEmpty() && fluidOut) {   // a core can have both an item and a fluid: an empty cell is dimmed
            cell.draw(g, outX, outY);
            g.fill(outX + 1, outY + 1, outX + CELL - 1, outY + CELL - 1, 0x99000000);
        }
        // progress as in the core window: the author's display recess, a chevron strip inside fills in a loop
        g.fill(progX, progY, progX + PROG_W, progY + PROG_H, 0xFF0F0F0F);
        g.fill(progX, progY, progX + PROG_W, progY + 1, 0xFF1B1B1B); g.fill(progX, progY, progX + 1, progY + PROG_H, 0xFF1B1B1B);
        g.fill(progX, progY + PROG_H - 1, progX + PROG_W, progY + PROG_H, 0xFF6E6E6E); g.fill(progX + PROG_W - 1, progY, progX + PROG_W, progY + PROG_H, 0xFF6E6E6E);
        chevrons.advance();
        int filled = (int) (System.currentTimeMillis() % PROG_CYCLE_MS * (PROG_W - 2) / PROG_CYCLE_MS);
        if (filled > 0) JeiGui.tileFill(g, PROGRESS, progX + 1, progY + 1, filled, chevrons.frame());
        // usage and time, the heat threshold below
        int y = bodyY + bodyH + 6;
        String fe = JeiGui.fe(r.energyPerTick()) + " FE/t";
        g.drawString(font, fe, left, y, JeiGui.ENERGY, false);
        g.drawString(font, JeiGui.secs(r.time()), left + font.width(fe) + 8, y, C_TIME, false);
        if (r.heat() > 0) g.drawString(font, Component.translatable("gui.industrial_core.late_core.heat",
                String.format(Locale.ROOT, "%,d", r.heat()).replace(',', ' ')), left, y + LINE, C_HEAT, false);
    }

    private void gauge(GuiGraphics g, int x) {
        int y = bodyY, h = bodyH;
        g.fill(x, y, x + TANK_W, y + h, 0xFF0F0F0F);
        g.fill(x, y, x + TANK_W, y + 1, 0xFF1B1B1B); g.fill(x, y, x + 1, y + h, 0xFF1B1B1B);
        g.fill(x, y + h - 1, x + TANK_W, y + h, 0xFF6E6E6E); g.fill(x + TANK_W - 1, y, x + TANK_W, y + h, 0xFF6E6E6E);
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }

    /**
     * The author's late frame of the needed size from a 96x64 piece of late_parts.png: 14x14 corners as is, 6 px edges stretch
     * from a single column/row (they are uniform along their length), 40x7 brackets centred at the top and bottom, background #171717 inside.
     */
    static final class Frame implements IDrawable {   // and the nanite lab window (NaniteLabJeiCategory)
        private static final int SRC = 96, SRC_H = 64, C = 14, E = 6, BR_W = 40, BR_H = 7;
        private final int w, h;
        Frame(int w, int h) { this.w = w; this.h = h; }
        @Override public int getWidth()  { return w; }
        @Override public int getHeight() { return h; }

        @Override
        public void draw(GuiGraphics g, int x, int y) {
            g.fill(x + E, y + E, x + w - E, y + h - E, 0xFF171717);
            // edges: source column 20 (between the corner and the bracket) and row 32
            g.blit(PARTS, x + C, y, w - 2 * C, E, 20, 0, 1, E, PARTS_W, PARTS_H);
            g.blit(PARTS, x + C, y + h - E, w - 2 * C, E, 20, SRC_H - E, 1, E, PARTS_W, PARTS_H);
            g.blit(PARTS, x, y + C, E, h - 2 * C, 0, 32, E, 1, PARTS_W, PARTS_H);
            g.blit(PARTS, x + w - E, y + C, E, h - 2 * C, SRC - E, 32, E, 1, PARTS_W, PARTS_H);
            // brackets centred
            int bx = x + w / 2 - BR_W / 2, sx = SRC / 2 - BR_W / 2;
            g.blit(PARTS, bx, y, sx, 0, BR_W, BR_H, PARTS_W, PARTS_H);
            g.blit(PARTS, bx, y + h - BR_H, sx, SRC_H - BR_H, BR_W, BR_H, PARTS_W, PARTS_H);
            // corners
            g.blit(PARTS, x, y, 0, 0, C, C, PARTS_W, PARTS_H);
            g.blit(PARTS, x + w - C, y, SRC - C, 0, C, C, PARTS_W, PARTS_H);
            g.blit(PARTS, x, y + h - C, 0, SRC_H - C, C, C, PARTS_W, PARTS_H);
            g.blit(PARTS, x + w - C, y + h - C, SRC - C, SRC_H - C, C, C, PARTS_W, PARTS_H);
        }
    }
}
