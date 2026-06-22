package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.OreWashingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

public class OreWasherJeiCategory implements IRecipeCategory<OreWashingRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_ore_washer.png");
    private static final int TEX_W = 154, TEX_H = 150, VIS_W = 154, VIS_H = 95;

    private static final int TANK_X = 28, TANK_Y = 17, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 125, GLASS_V = 100;
    private static final int IN_X = 52, IN_Y = 31;
    private static final int OUT1_X = 109, OUT1_Y = 11, OUT2_X = 109, OUT2_Y = 31;
    private static final int EBAR_X = 44, EBAR_Y = 75, EBAR_W = 66;
    private static final int PROG_GX = 71, PROG_GY = 31, PROG_W = 27, PROG_H = 14, PROG_U = 73, PROG_V = 121;
    private static final int INFO_X = 56, INFO_Y = 50;

    private final RecipeType<OreWashingRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public OreWasherJeiCategory(IGuiHelper helper, RecipeType<OreWashingRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<OreWashingRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OreWashingRecipe recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
               .setCustomRenderer(ForgeTypes.FLUID_STACK,
                       new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
               .addFluidStack(Fluids.WATER, recipe.getWaterAmount());

        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y)
               .addItemStacks(withCount(recipe.getInput().getItems(), recipe.getInputCount()));

        IRecipeSlotBuilder resultSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUT1_X, OUT1_Y).addItemStack(recipe.getResult());
        if (recipe.getResultChance() < 1.0f) {
            int resChance = Math.round(recipe.getResultChance() * 100);
            resultSlot.addTooltipCallback((v, tip) -> tip.add(
                    Component.translatable("jei.industrial_core.chance", resChance).withStyle(ChatFormatting.GRAY)));
        }

        if (recipe.hasByproduct()) {
            int chance = Math.round(recipe.getByproductChance() * 100);
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUT2_X, OUT2_Y)
                   .addItemStack(recipe.getByproduct())
                   .addTooltipCallback((v, tip) -> tip.add(
                           Component.translatable("jei.industrial_core.chance", chance).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(OreWashingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(OreWashingRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= EBAR_X && mx < EBAR_X + EBAR_W && my >= EBAR_Y && my < EBAR_Y + JeiGui.FILL_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }
}
