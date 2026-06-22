package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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

import java.util.List;

public class ReactorSchemeCategory implements IRecipeCategory<ReactorSchemeRecipe> {

    private static final ResourceLocation SLOT_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    private static final int GRID_X  = 2;
    private static final int GRID_Y  = 2;
    private static final int STEP    = 18;
    private static final int W       = 194;
    private static final int H       = 94;

    private final RecipeType<ReactorSchemeRecipe> type;
    private final IDrawable background;
    private final IDrawable icon;

    public ReactorSchemeCategory(IGuiHelper gui,
                                  RecipeType<ReactorSchemeRecipe> type,
                                  ItemStack iconStack,
                                  Component title) {
        this.type       = type;
        this.background = gui.createBlankDrawable(W, H);
        this.icon       = gui.createDrawableItemStack(iconStack);
    }

    @Override public RecipeType<ReactorSchemeRecipe> getRecipeType() { return type; }

    @Override public Component getTitle() {
        return Component.translatable("jei.industrial_core.reactor_scheme");
    }

    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          ReactorSchemeRecipe recipe,
                          IFocusGroup focuses) {
        String layout = recipe.getLayout();
        for (int i = 0; i < 25; i++) {
            ItemStack stack = ReactorSchemeRecipe.charToStack(layout.charAt(i));
            if (stack.isEmpty()) continue;
            int col = i % 5, row = i / 5;
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                            GRID_X + col * STEP + 1,
                            GRID_Y + row * STEP + 1)
                   .addItemStack(stack);
        }
    }

    @Override
    public void draw(ReactorSchemeRecipe recipe,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     GuiGraphics graphics,
                     double mouseX, double mouseY) {

        String layout = recipe.getLayout();

        for (int i = 0; i < 25; i++) {
            int col = i % 5, row = i / 5;
            int sx = GRID_X + col * STEP;
            int sy = GRID_Y + row * STEP;

            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            graphics.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF373737);

            if (layout.charAt(i) == '.') {
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x44000000);
            }
        }

        int textX = GRID_X + 5 * STEP + 6;
        int textY = GRID_Y;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(0.9f, 0.9f, 1f);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable(recipe.getNameKey()),
                0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();

        String ept = formatFE(recipe.getEnergyPerTick()) + " FE/t";
        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY + 14, 0);
        graphics.pose().scale(0.85f, 0.85f, 1f);
        graphics.drawString(Minecraft.getInstance().font,
                ept, 0, 0, 0xFFCC00, false);
        graphics.pose().popPose();

    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000) return String.format("%.0fM", fe / 1_000_000.0);
        if (fe >= 1_000)     return String.format("%.0fk", fe / 1_000.0);
        return String.valueOf(fe);
    }
}
