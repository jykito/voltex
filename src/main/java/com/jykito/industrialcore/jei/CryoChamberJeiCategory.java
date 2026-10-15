package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.recipe.CryoRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

/**
 * Cryo chamber in JEI.
 * <p>
 * The main thing the card must say is at which STAGE the recipe works. A stage is not
 * crafting difficulty but a state of the installation the player pays for with minutes of heat supply,
 * so the depth gauge is filled exactly to the required threshold, not animated.
 */
public class CryoChamberJeiCategory implements IRecipeCategory<CryoRecipe> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/jei/jei_cryo_chamber.png");

    private static final int TEX_W = 154, TEX_H = 220, VIS_W = 154, VIS_H = 122;

    private static final int TANK_W = 16, TANK_H = 64, TANK_Y = 25;
    private static final int COOLANT_X = 9, HOT_X = 30, PRODUCT_X = 130;
    private static final int GLASS_U = 8, GLASS_V = 145;

    private static final int IN_X = 50, OUT_X = 106, SLOT_Y = 25;

    private static final int ROD_Y = 65;
    private static final int[] ROD_X = {55, 78, 101};
    private static final int FRAME_Y = 63, FRAME_W = 20, FRAME_H = 20;
    private static final int[] FRAME_X = {53, 76, 99};
    private static final int FRAME_SRC_U = 129, FRAME_SRC_V = 171;

    private static final int PROG_X = 70, PROG_Y = 27, PROG_W = 32, PROG_H = 12;
    private static final int PROG_SRC_U = 76, PROG_SRC_V = 163;
    private static final int PROG_CYCLE_TICKS = 40;

    private static final int BAR_X = 22, BAR_W = 110, BAR_H = 6;
    private static final int DEPTH_Y = 94, ENERGY_Y = 109;

    /** The gauge segment borders match the ticks on the texture, as in the chamber's own panel. */
    private static final int[] SEG_PX    = {0, 33, 67, BAR_W};
    private static final int[] SEG_DEPTH = {0, 200, 800, CryoCoreBlockEntity.MAX_DEPTH};
    private static final int DEPTH_COLOR = 0xFF40BEC7;

    private static final int INFO_X = 23, INFO_Y = 7;   // a text line in the recessed panel between the corner plates (window K4)

    private final RecipeType<CryoRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public CryoChamberJeiCategory(IGuiHelper helper, RecipeType<CryoRecipe> type,
                                  ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H)
                .setTextureSize(TEX_W, TEX_H)
                .build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);

        IDrawableStatic progStatic = helper.drawableBuilder(TEXTURE, PROG_SRC_U, PROG_SRC_V, PROG_W, PROG_H)
                .setTextureSize(TEX_W, TEX_H)
                .build();
        this.progress = helper.createAnimatedDrawable(progStatic, PROG_CYCLE_TICKS,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<CryoRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CryoRecipe recipe, IFocusGroup focuses) {
        int stage = recipe.getRequiredStage();
        int coolant = CryoCoreBlockEntity.coolantPerTick(stage) * recipe.getProcessingTime();

        // coolant for the whole run, not per tick: the player cares what one batch costs
        fluidSlot(builder, RecipeIngredientRole.INPUT, COOLANT_X,
                new FluidStack(ModFluids.SOURCE_COOLANT.get(), coolant));
        fluidSlot(builder, RecipeIngredientRole.OUTPUT, HOT_X,
                new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), coolant));
        if (recipe.hasFluidResult())
            fluidSlot(builder, RecipeIngredientRole.OUTPUT, PRODUCT_X, recipe.makeFluidResult());

        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, SLOT_Y)
               .addItemStacks(withCount(recipe.getIngredient().getItems(), recipe.getIngredientCount()));

        if (recipe.hasItemResult())
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, SLOT_Y)
                   .addItemStack(recipe.getResult().copy());

        // Rods are not an ingredient but an accelerator, hence a catalyst rather than an input.
        for (int r = 0; r < ROD_X.length; r++)
            builder.addSlot(RecipeIngredientRole.CATALYST, ROD_X[r], ROD_Y)
                   .addItemStack(new ItemStack(ModItems.CRYO_ROD.get()))
                   .addItemStack(new ItemStack(ModItems.CRYO_ROD_T2.get()))
                   .addItemStack(new ItemStack(ModItems.CRYO_ROD_T3.get()));
    }

    private void fluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, FluidStack fs) {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, TANK_Y);
        if (fs.isEmpty()) return;
        slot.setCustomRenderer(NeoForgeTypes.FLUID_STACK,
                    new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
            .addFluidStack(fs.getFluid(), fs.getAmount());
    }

    @Override
    public void draw(CryoRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        progress.draw(g, PROG_X, PROG_Y);

        // rod frames are always lit: on the card the slots are never empty
        for (int r = 0; r < FRAME_X.length; r++)
            g.blit(TEXTURE, FRAME_X[r], FRAME_Y, FRAME_SRC_U, FRAME_SRC_V, FRAME_W, FRAME_H, TEX_W, TEX_H);

        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, BAR_X, ENERGY_Y, BAR_W, anim.frame());

        int px = barPx(CryoCoreBlockEntity.depthForStage(recipe.getRequiredStage()));
        if (px > 0) g.fill(BAR_X, DEPTH_Y, BAR_X + px, DEPTH_Y + BAR_H, DEPTH_COLOR);

        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X + 60, INFO_Y, 0xB0B0B0);
        JeiGui.smallText(g, font,
                Component.translatable("jei.industrial_core.cryo.stage", recipe.getRequiredStage()),
                INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0x40BEC7);
    }

    @Override
    public List<Component> getTooltipStrings(CryoRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (inRect(mx, my, BAR_X, DEPTH_Y, BAR_W, BAR_H)) {
            return List.of(
                    Component.translatable("jei.industrial_core.cryo.stage", recipe.getRequiredStage())
                            .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.translatable("jei.industrial_core.cryo.depth_needed",
                            CryoCoreBlockEntity.depthForStage(recipe.getRequiredStage())).withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.cryo.heat_needed",
                            CryoCoreBlockEntity.depthForStage(recipe.getRequiredStage())).withStyle(ChatFormatting.GOLD));
        }
        if (inRect(mx, my, BAR_X, ENERGY_Y, BAR_W, BAR_H)) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int barPx(int depth) {
        if (depth <= 0) return 0;
        for (int i = 1; i < SEG_DEPTH.length; i++) {
            if (depth >= SEG_DEPTH[i]) continue;
            int d0 = SEG_DEPTH[i - 1], d1 = SEG_DEPTH[i];
            int p0 = SEG_PX[i - 1],    p1 = SEG_PX[i];
            return p0 + (depth - d0) * (p1 - p0) / Math.max(1, d1 - d0);
        }
        return BAR_W;
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks)
                .map(s -> { ItemStack c = s.copy(); c.setCount(count); return c; })
                .toList();
    }
}
