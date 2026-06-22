package com.jykito.industrialcore.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MinerLensCategory implements IRecipeCategory<MinerLensRecipe> {

    private static final int LENS_X = 2,  LENS_Y = 2;
    private static final int ARROW_X = 20, ARROW_Y = 2;
    private static final int GRID_X = 46, GRID_Y = 2, STEP = 18, COLS = 7;

    public static final int WIDTH  = GRID_X + COLS * STEP;
    public static final int HEIGHT = 88;

    private final RecipeType<MinerLensRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;

    public MinerLensCategory(IGuiHelper helper, RecipeType<MinerLensRecipe> type,
                              ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title      = title;
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon       = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        this.slot       = JeiGui.slot(helper);
    }

    @Override public RecipeType<MinerLensRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()    { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()     { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MinerLensRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, LENS_X, LENS_Y)
               .setBackground(slot, -1, -1)
               .addItemStack(recipe.lens());

        var ores = recipe.ores();
        for (int i = 0; i < ores.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            double chance = recipe.chancePercent(i);
            String chanceText = String.format("%.1f%%", chance);

            builder.addSlot(RecipeIngredientRole.OUTPUT, GRID_X + col * STEP, GRID_Y + row * STEP)
                   .setBackground(slot, -1, -1)
                   .addItemStack(new ItemStack(ores.get(i)))
                   .addTooltipCallback((view, tooltip) ->
                       tooltip.add(Component.literal(chanceText)
                           .withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(MinerLensRecipe recipe, IRecipeSlotsView view, GuiGraphics g,
                     double mx, double my) {
        JeiGui.arrow(g, ARROW_X, ARROW_Y);
    }
}
