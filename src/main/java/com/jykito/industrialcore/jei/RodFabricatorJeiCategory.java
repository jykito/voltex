package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.RodFabricatorRecipe;
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

import java.util.Arrays;
import java.util.List;

public class RodFabricatorJeiCategory implements IRecipeCategory<RodFabricatorRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_rod_fabricator.png");
    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;

    private static final int IN1_X = 44, IN1_Y = 14, IN2_X = 44, IN2_Y = 36, OUT_X = 93, OUT_Y = 25;
    private static final int EBAR_X = 44, EBAR_Y = 64, EBAR_W = 66;
    private static final int PROG_GX = 62, PROG_GY = 21, PROG_W = 29, PROG_H = 25, PROG_U = 80, PROG_V = 90;
    private static final int INFO_X = 64, INFO_Y = 46;

    private final RecipeType<RodFabricatorRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public RodFabricatorJeiCategory(IGuiHelper helper, RecipeType<RodFabricatorRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<RodFabricatorRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RodFabricatorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, IN1_X, IN1_Y)
               .addItemStacks(withCount(recipe.getIngredient1().getItems(), recipe.getCount1()));
        builder.addSlot(RecipeIngredientRole.INPUT, IN2_X, IN2_Y)
               .addItemStacks(withCount(recipe.getIngredient2().getItems(), recipe.getCount2()));

        RegistryAccess ra = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResultItem(ra));
    }

    @Override
    public void draw(RodFabricatorRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(RodFabricatorRecipe recipe, IRecipeSlotsView view, double mx, double my) {
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
