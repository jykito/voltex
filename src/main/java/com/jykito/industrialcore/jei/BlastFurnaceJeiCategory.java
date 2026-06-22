package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.BlastFurnaceRecipe;
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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

public class BlastFurnaceJeiCategory implements IRecipeCategory<BlastFurnaceRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_blast_furnace.png");
    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;

    private static final int IN1_X = 47, IN1_Y = 13, IN2_X = 47, IN2_Y = 41, OUT_X = 95, OUT_Y = 27;
    private static final int HBAR_X = 52, HBAR_Y = 66, HBAR_W = 58;
    private static final int PROG_GX = 65, PROG_GY = 25, PROG_W = 28, PROG_H = 20, PROG_U = 58, PROG_V = 100;
    private static final int INFO_X = 67, INFO_Y = 46;

    private final RecipeType<BlastFurnaceRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public BlastFurnaceJeiCategory(IGuiHelper helper, RecipeType<BlastFurnaceRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<BlastFurnaceRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlastFurnaceRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, IN1_X, IN1_Y)
               .addItemStacks(withCount(recipe.getIngredient().getItems(), recipe.getIngredientCount()));
        builder.addSlot(RecipeIngredientRole.INPUT, IN2_X, IN2_Y)
               .addItemStacks(withCount(recipe.getFuel().getItems(), recipe.getFuelCount()));

        RegistryAccess ra = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResultItem(ra));
    }

    @Override
    public void draw(BlastFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.HEAT_FILL, HBAR_X, HBAR_Y, HBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, "≥ " + recipe.getRequiredHeat() + "°", INFO_X, INFO_Y, 0xFF8030);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(BlastFurnaceRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= HBAR_X && mx < HBAR_X + HBAR_W && my >= HBAR_Y && my < HBAR_Y + JeiGui.FILL_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.heat").withStyle(ChatFormatting.GOLD),
                    Component.literal("≥ " + recipe.getRequiredHeat() + "°").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }
}
