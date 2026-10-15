package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.CrystallizerRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

/**
 * Crystallization in JEI: a fluid tank and the seed slot on the left, the item on the right.
 * The seed is optional: a recipe without it shows only the tank.
 */
public class CrystallizerJeiCategory implements IRecipeCategory<CrystallizerRecipe> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/jei/jei_crystallizer.png");

    private static final int TEX_W = 154, TEX_H = 160, VIS_W = 154, VIS_H = 91;

    private static final int TANK_X = 13, TANK_Y = 13, TANK_W = 16, TANK_H = 64;
    private static final int GLASS_U = 124, GLASS_V = 94;

    private static final int SEED_X = 50, SEED_Y = 28;
    private static final int OUT_X  = 95, OUT_Y  = 28;

    private static final int PROG_X = 69, PROG_Y = 28, PROG_W = 23, PROG_H = 16;
    private static final int PROG_SRC_U = 49, PROG_SRC_V = 124;
    private static final int PROG_CYCLE_TICKS = 40;

    private static final int BAR_X = 39, BAR_Y = 62, BAR_W = 81, BAR_H = 6;
    private static final int INFO_X = 55, INFO_Y = 48;

    private final RecipeType<CrystallizerRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public CrystallizerJeiCategory(IGuiHelper helper, RecipeType<CrystallizerRecipe> type,
                                   ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H)
                .setTextureSize(TEX_W, TEX_H)
                .build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);

        IDrawableStatic progStatic = helper.drawableBuilder(TEXTURE, PROG_SRC_U, PROG_SRC_V, PROG_W, PROG_H)
                .setTextureSize(TEX_W, TEX_H)
                .build();
        this.progress = helper.createAnimatedDrawable(progStatic, PROG_CYCLE_TICKS,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<CrystallizerRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystallizerRecipe recipe, IFocusGroup focuses) {
        FluidStack in = recipe.makeFluidInput();
        IRecipeSlotBuilder tank = builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y);
        if (!in.isEmpty()) {
            tank.setCustomRenderer(NeoForgeTypes.FLUID_STACK,
                        new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
                .addFluidStack(in.getFluid(), in.getAmount());
        }

        if (recipe.hasSeed()) {
            builder.addSlot(RecipeIngredientRole.INPUT, SEED_X, SEED_Y)
                   .addItemStacks(withCount(recipe.getIngredient().getItems(), recipe.getIngredientCount()));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y)
               .addItemStack(recipe.getResult().copy());
    }

    @Override
    public void draw(CrystallizerRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        progress.draw(g, PROG_X, PROG_Y);

        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, BAR_X, BAR_Y, BAR_W, anim.frame());

        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(CrystallizerRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= BAR_X && mx < BAR_X + BAR_W && my >= BAR_Y && my < BAR_Y + BAR_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks)
                .map(s -> { ItemStack c = s.copy(); c.setCount(count); return c; })
                .toList();
    }
}
