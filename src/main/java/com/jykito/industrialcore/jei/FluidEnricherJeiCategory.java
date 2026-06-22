package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.FluidEnricherRecipe;
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
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

public class FluidEnricherJeiCategory implements IRecipeCategory<FluidEnricherRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_fluid_enricher.png");
    private static final int TEX_W = 154, TEX_H = 160, VIS_W = 154, VIS_H = 84;

    private static final int INTANK_X = 13, INTANK_Y = 10, OUTTANK_X = 125, OUTTANK_Y = 10;
    private static final int TANK_W = 16, TANK_H = 64, GLASS_U = 114, GLASS_V = 90;
    private static final int R1_X = 69, R1_Y = 9, R2_X = 69, R2_Y = 31;
    private static final int EBAR_X = 44, EBAR_Y = 64, EBAR_W = 66;
    private static final int PROG_GX = 30, PROG_GY = 17, PROG_W = 92, PROG_H = 22, PROG_U = 4, PROG_V = 127;
    private static final int INFO_X = 48, INFO_Y = 49;

    private final RecipeType<FluidEnricherRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public FluidEnricherJeiCategory(IGuiHelper helper, RecipeType<FluidEnricherRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 50, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<FluidEnricherRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidEnricherRecipe recipe, IFocusGroup focuses) {
        FluidStack in = recipe.makeFluidInput();
        IRecipeSlotBuilder inTank = builder.addSlot(RecipeIngredientRole.INPUT, INTANK_X, INTANK_Y);
        if (!in.isEmpty())
            inTank.setCustomRenderer(ForgeTypes.FLUID_STACK,
                       new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
                  .addFluidStack(in.getFluid(), in.getAmount());

        builder.addSlot(RecipeIngredientRole.INPUT, R1_X, R1_Y)
               .addItemStacks(withCount(recipe.getItemIngredient().getItems(), recipe.getItemCount()));
        if (recipe.hasSecondReagent())
            builder.addSlot(RecipeIngredientRole.INPUT, R2_X, R2_Y)
                   .addItemStacks(withCount(recipe.getItemIngredient2().getItems(), recipe.getItemCount2()));

        FluidStack out = recipe.makeFluidOutput();
        IRecipeSlotBuilder outTank = builder.addSlot(RecipeIngredientRole.OUTPUT, OUTTANK_X, OUTTANK_Y);
        if (!out.isEmpty())
            outTank.setCustomRenderer(ForgeTypes.FLUID_STACK,
                       new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
                   .addFluidStack(out.getFluid(), out.getAmount());
    }

    @Override
    public void draw(FluidEnricherRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(FluidEnricherRecipe recipe, IRecipeSlotsView view, double mx, double my) {
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
