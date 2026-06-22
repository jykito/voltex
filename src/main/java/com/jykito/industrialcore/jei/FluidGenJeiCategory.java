package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.block.entity.FluidGeneratorType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FluidGenJeiCategory implements IRecipeCategory<FluidGeneratorType> {

    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;
    private static final int TANK_X = 69, TANK_Y = 11, TANK_W = 16, TANK_H = 48, GLASS_U = 115, GLASS_V = 91;
    private static final int EBAR_X = 49, EBAR_Y = 65, EBAR_W = 56;
    private static final int INFO_X = 88, INFO_Y = 31;

    private final RecipeType<FluidGeneratorType> recipeType;
    private final IDrawable background, icon;
    private final Component title;
    private final ResourceLocation texture;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public FluidGenJeiCategory(IGuiHelper helper, RecipeType<FluidGeneratorType> type, ItemStack iconStack,
                               Component title, ResourceLocation texture) {
        this.recipeType = type;
        this.title = title;
        this.texture = texture;
        this.background = helper.drawableBuilder(texture, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override public RecipeType<FluidGeneratorType> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidGeneratorType type, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, TANK_X, TANK_Y)
               .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W, TANK_H, texture, GLASS_U, GLASS_V, TEX_W, TEX_H))
               .addFluidStack(type.fluid, type.mbPerCycle);
    }

    @Override
    public void draw(FluidGeneratorType type, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(type.fePerTick) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(type.processTicks), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
        JeiGui.smallText(g, font, type.mbPerCycle + " mB", INFO_X, INFO_Y + JeiGui.SMALL_LINE * 2, 0x55AAFF);
    }
}
