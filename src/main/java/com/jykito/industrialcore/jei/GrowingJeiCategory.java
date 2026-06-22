package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.recipe.GrowingRecipe;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

public class GrowingJeiCategory implements IRecipeCategory<GrowingRecipe> {

    private static final int TEX_W = 154, TEX_H = 150, VIS_W = 154, VIS_H = 84;
    private static final int TANK_X = 21, TANK_Y = 17, TANK_W = 16, TANK_H = 48, GLASS_U = 88, GLASS_V = 100;
    private static final int UP_X = 70, UP_Y = 42, IN_X = 49, IN_Y = 23, OUT_X = 91, OUT_Y = 23;
    private static final int EBAR_X = 46, EBAR_Y = 66, EBAR_W = 66;
    private static final int PROG_GX = 67, PROG_GY = 27, PROG_W = 22, PROG_H = 8, PROG_U = 41, PROG_V = 111;
    private static final int INFO_X = 111, INFO_Y = 9;
    private static final int WATER_PER_RECIPE = 1000;

    private final RecipeType<GrowingRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final ItemStack upgrade;
    private final ResourceLocation texture;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public GrowingJeiCategory(IGuiHelper helper, RecipeType<GrowingRecipe> type, ItemStack iconStack,
                              Component title, ItemStack upgrade, ResourceLocation texture) {
        this.recipeType = type;
        this.title = title;
        this.upgrade = upgrade;
        this.texture = texture;
        this.background = helper.drawableBuilder(texture, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(texture, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<GrowingRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrowingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, TANK_X, TANK_Y)
               .setCustomRenderer(ForgeTypes.FLUID_STACK,
                       new FluidTankRenderer(TANK_W, TANK_H, texture, GLASS_U, GLASS_V, TEX_W, TEX_H))
               .addFluidStack(Fluids.WATER, WATER_PER_RECIPE);

        builder.addSlot(RecipeIngredientRole.CATALYST, UP_X, UP_Y).addItemStack(upgrade);

        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y)
               .addItemStacks(withCount(recipe.getInput().getItems(), recipe.getInputCount()));

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(GrowingRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        progress.draw(g, PROG_GX, PROG_GY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergy()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
        if (recipe.keepInput())
            JeiGui.smallText(g, font, "keep", INFO_X, INFO_Y + JeiGui.SMALL_LINE * 2, 0x70C070);
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : stacks) { ItemStack c = s.copy(); c.setCount(count); out.add(c); }
        return out;
    }
}
