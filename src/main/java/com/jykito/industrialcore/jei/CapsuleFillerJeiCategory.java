package com.jykito.industrialcore.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CapsuleFillerJeiCategory implements IRecipeCategory<CapsuleFillerJeiRecipe> {

    private static final int FE_PER_TICK = 80, TICKS = 40;
    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;
    private static final int FLUID_X = 59, FLUID_Y = 11, TANK_W = 16, TANK_H = 48, GLASS_U = 123, GLASS_V = 91;
    private static final int IN_X = 80, IN_Y = 11, OUT_X = 80, OUT_Y = 44;
    private static final int EBAR_X = 49, EBAR_Y = 65, EBAR_W = 56;
    private static final int PROG_GX = 84, PROG_GY = 29, PROG_W = 8, PROG_H = 13, PROG_U = 92, PROG_V = 115;
    private static final int INFO_X = 98, INFO_Y = 10;

    private final RecipeType<CapsuleFillerJeiRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final ResourceLocation texture;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public CapsuleFillerJeiCategory(IGuiHelper helper, RecipeType<CapsuleFillerJeiRecipe> type, ItemStack iconStack,
                                    Component title, ResourceLocation texture) {
        this.recipeType = type;
        this.title = title;
        this.texture = texture;
        this.background = helper.drawableBuilder(texture, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(texture, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.TOP, false);
    }

    @Override public RecipeType<CapsuleFillerJeiRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CapsuleFillerJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, FLUID_X, FLUID_Y)
               .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W, TANK_H, texture, GLASS_U, GLASS_V, TEX_W, TEX_H))
               .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount());

        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y).addItemStack(recipe.emptyContainer());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.filledContainer());
    }

    @Override
    public void draw(CapsuleFillerJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        progress.draw(g, PROG_GX, PROG_GY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, FE_PER_TICK + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(TICKS), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }
}
