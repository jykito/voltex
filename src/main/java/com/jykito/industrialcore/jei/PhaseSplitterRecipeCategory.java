package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.PhaseSplitterRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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

import java.util.List;

public class PhaseSplitterRecipeCategory implements IRecipeCategory<PhaseSplitterRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_phase_splitter.png");
    private static final int TEX_W = 154, TEX_H = 150, VIS_W = 154, VIS_H = 95;

    private static final int IN_X = 41, IN_Y = 32;
    private static final int O1_X = 99, O1_Y = 13, O2_X = 99, O2_Y = 32, O3_X = 99, O3_Y = 51;
    private static final int EBAR_X = 44, EBAR_Y = 75, EBAR_W = 66;
    private static final int PROG_GX = 63, PROG_GY = 17, PROG_W = 32, PROG_H = 46, PROG_U = 43, PROG_V = 101;
    private static final int INFO_X = 43, INFO_Y = 50;

    private final RecipeType<PhaseSplitterRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public PhaseSplitterRecipeCategory(IGuiHelper helper, RecipeType<PhaseSplitterRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<PhaseSplitterRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PhaseSplitterRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y).addIngredients(recipe.getIngredients().get(0));
        if (!recipe.getOutput1().isEmpty())
            builder.addSlot(RecipeIngredientRole.OUTPUT, O1_X, O1_Y).addItemStack(recipe.getOutput1());
        if (!recipe.getOutput2().isEmpty())
            builder.addSlot(RecipeIngredientRole.OUTPUT, O2_X, O2_Y).addItemStack(recipe.getOutput2());
        if (!recipe.getOutput3().isEmpty())
            builder.addSlot(RecipeIngredientRole.OUTPUT, O3_X, O3_Y).addItemStack(recipe.getOutput3());
    }

    @Override
    public void draw(PhaseSplitterRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCost()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(PhaseSplitterRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= EBAR_X && mx < EBAR_X + EBAR_W && my >= EBAR_Y && my < EBAR_Y + JeiGui.FILL_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCost()) + " FE").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }
}
