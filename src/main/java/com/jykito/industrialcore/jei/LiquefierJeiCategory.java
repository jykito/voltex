package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.LiquefierRecipe;
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

public class LiquefierJeiCategory implements IRecipeCategory<LiquefierRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_liquefier.png");
    private static final ResourceLocation HEAT_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/heat_fill_6x_anim.png");
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");
    private static final int TEX_W = 154, TEX_H = 160, VIS_W = 154, VIS_H = 95;

    private static final int FILL_TEX_W = 96, FILL_TEX_H = 36, FILL_FRAMES = 6, FILL_H = 6, FILL_FRAME_TICKS = 16;

    private static final int IN1_X = 53, IN1_Y = 21;
    private static final int IN2_X = 53, IN2_Y = 43;
    private static final int TANK_X = 107, TANK_Y = 8, TANK_W = 16, TANK_H = 64;
    private static final int PROG_X = 70, PROG_Y = 26, PROG_W = 33, PROG_H = 28;
    private static final int PROG_SRC_U = 42, PROG_SRC_V = 119;
    private static final int GLASS_U = 120, GLASS_V = 96;

    private static final int BAR_X = 47, BAR_W = 56, BAR_H = 6;
    private static final int EBAR_Y = 70, HBAR_Y = 80;
    private static final int INFO_X = 18, INFO_Y = 20;

    private static final int PROG_CYCLE_TICKS = 40;

    private final RecipeType<LiquefierRecipe> recipeType;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;
    private final Component title;

    private int animTick = 0, animFrame = 0;

    public LiquefierJeiCategory(IGuiHelper helper, RecipeType<LiquefierRecipe> type, ItemStack iconStack, Component title) {
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

    @Override public RecipeType<LiquefierRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()  { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()   { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LiquefierRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, IN1_X, IN1_Y)
               .addItemStacks(withCount(recipe.getIngredient().getItems(), recipe.getIngredientCount()));
        if (recipe.hasSecondIngredient()) {
            builder.addSlot(RecipeIngredientRole.INPUT, IN2_X, IN2_Y)
                   .addItemStacks(withCount(recipe.getIngredient2().getItems(), recipe.getIngredientCount2()));
        }

        FluidStack out = recipe.makeFluidOutput();
        IRecipeSlotBuilder tank = builder.addSlot(RecipeIngredientRole.OUTPUT, TANK_X, TANK_Y);
        if (!out.isEmpty()) {
            tank.setCustomRenderer(ForgeTypes.FLUID_STACK,
                        new FluidTankRenderer(TANK_W, TANK_H, TEXTURE, GLASS_U, GLASS_V, TEX_W, TEX_H))
                .addFluidStack(out.getFluid(), out.getAmount());
        }
    }

    @Override
    public void draw(LiquefierRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {

        progress.draw(g, PROG_X, PROG_Y);

        if (++animTick >= FILL_FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FILL_FRAMES; }
        int frameV = animFrame * FILL_H;
        drawFill(g, ENERGY_FILL, BAR_X, EBAR_Y, BAR_W, frameV);
        drawFill(g, HEAT_FILL,   BAR_X, HBAR_Y, BAR_W, frameV);

        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCostPerTick()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
        JeiGui.smallText(g, font, "≥ " + recipe.getRequiredHeat() + "°", INFO_X, INFO_Y + JeiGui.SMALL_LINE * 2, 0xFF8030);
    }

    @Override
    public List<Component> getTooltipStrings(LiquefierRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (inRect(mx, my, BAR_X, EBAR_Y, BAR_W, BAR_H)) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(fmt(recipe.getEnergyCostPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (inRect(mx, my, BAR_X, HBAR_Y, BAR_W, BAR_H)) {
            return List.of(
                    Component.translatable("jei.industrial_core.heat").withStyle(ChatFormatting.GOLD),
                    Component.literal("≥ " + recipe.getRequiredHeat() + "°").withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void drawFill(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int frameV) {
        int drawn = 0;
        while (drawn < w) {
            int tileW = Math.min(FILL_TEX_W, w - drawn);
            g.blit(tex, x + drawn, y, 0, frameV, tileW, FILL_H, FILL_TEX_W, FILL_TEX_H);
            drawn += FILL_TEX_W;
        }
    }

    private static String fmt(long v) {
        if (v >= 1_000_000) return (v / 1_000_000) + "M";
        if (v >= 1_000)     return (v / 1_000) + "k";
        return String.valueOf(v);
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks)
                .map(s -> { ItemStack c = s.copy(); c.setCount(count); return c; })
                .toList();
    }
}
