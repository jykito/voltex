package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.block.entity.StoneGeneratorMode;
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
import net.minecraft.world.level.material.Fluids;

public class StoneGenJeiCategory implements IRecipeCategory<StoneGeneratorMode> {

    private static final int FE_PER_TICK = 200;
    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;
    private static final int WATER_X = 30, LAVA_X = 108, TANK_Y = 10, TANK_W = 16, TANK_H = 48, GLASS_U = 115, GLASS_V = 91;
    private static final int OUT_X = 69, OUT_Y = 27;
    private static final int EBAR_X = 49, EBAR_Y = 65, EBAR_W = 56;
    private static final int PROG_GX = 74, PROG_GY = 44, PROG_W = 6, PROG_H = 9, PROG_U = 28, PROG_V = 115;
    private static final int INFO_X = 49, INFO_Y = 9;

    private final RecipeType<StoneGeneratorMode> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final ResourceLocation texture;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public StoneGenJeiCategory(IGuiHelper helper, RecipeType<StoneGeneratorMode> type, ItemStack iconStack,
                               Component title, ResourceLocation texture) {
        this.recipeType = type;
        this.title = title;
        this.texture = texture;
        this.background = helper.drawableBuilder(texture, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(texture, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override public RecipeType<StoneGeneratorMode> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StoneGeneratorMode mode, IFocusGroup focuses) {
        if (mode.waterCost > 0)
            builder.addSlot(RecipeIngredientRole.CATALYST, WATER_X, TANK_Y)
                   .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W, TANK_H, texture, GLASS_U, GLASS_V, TEX_W, TEX_H))
                   .addFluidStack(Fluids.WATER, mode.waterCost);
        if (mode.lavaCost > 0)
            builder.addSlot(RecipeIngredientRole.CATALYST, LAVA_X, TANK_Y)
                   .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W, TANK_H, texture, GLASS_U, GLASS_V, TEX_W, TEX_H))
                   .addFluidStack(Fluids.LAVA, mode.lavaCost);

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(new ItemStack(mode.result));
    }

    @Override
    public void draw(StoneGeneratorMode mode, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        progress.draw(g, PROG_GX, PROG_GY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, new ItemStack(mode.result).getHoverName(), INFO_X, INFO_Y, 0xFFFFFF);
        JeiGui.smallText(g, font, FE_PER_TICK + " FE/t", INFO_X, INFO_Y + JeiGui.SMALL_LINE, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(mode.processTicks), INFO_X, INFO_Y + JeiGui.SMALL_LINE * 2, 0xB0B0B0);
    }
}
