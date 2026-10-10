package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.BioreactorRecipe;
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

import java.util.Arrays;
import java.util.List;

/** Биореактор в JEI: раскладка промывателя (бак, вход, выход) плюс добавка над входом и порог тепла. */
public class BioreactorJeiCategory implements IRecipeCategory<BioreactorRecipe> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_bioreactor.png");
    private static final int TEX_W = 154, TEX_H = 150, VIS_W = 154, VIS_H = 95;
    private static final int TANK_X = 28, TANK_Y = 17, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 125, GLASS_V = 100;
    private static final int IN_X = 52, IN_Y = 31, ADD_X = 52, ADD_Y = 11;
    private static final int OUT_X = 109, OUT_Y = 31;
    private static final int EBAR_X = 44, EBAR_Y = 75, EBAR_W = 66;
    private static final int PROG_GX = 78, PROG_GY = 33, PROG_W = 16, PROG_H = 12, PROG_U = 42, PROG_V = 124;
    private static final int INFO_X = 56, INFO_Y = 50;

    private final RecipeType<BioreactorRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public BioreactorJeiCategory(IGuiHelper helper, RecipeType<BioreactorRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<BioreactorRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BioreactorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
               .setCustomRenderer(ForgeTypes.FLUID_STACK, new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
               .addFluidStack(Fluids.WATER, recipe.getWaterAmount());
        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y).addItemStacks(withCount(recipe.getInput().getItems(), recipe.getInputCount()));
        if (recipe.hasAdditive())
            builder.addSlot(RecipeIngredientRole.INPUT, ADD_X, ADD_Y).addItemStacks(withCount(recipe.getAdditive().getItems(), recipe.getAdditiveCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResult());
    }

    @Override
    public void draw(BioreactorRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, PROG_GX, PROG_GY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
        JeiGui.smallText(g, font, Component.translatable("jei.industrial_core.bioreactor.heat", recipe.getRequiredHeat()).getString(),
                INFO_X, INFO_Y + 2 * JeiGui.SMALL_LINE, 0xFF8A3D);
    }

    @Override
    public List<Component> getTooltipStrings(BioreactorRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= EBAR_X && mx < EBAR_X + EBAR_W && my >= EBAR_Y && my < EBAR_Y + JeiGui.FILL_H)
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20).withStyle(ChatFormatting.DARK_GRAY));
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }
}
