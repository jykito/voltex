package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.recipe.MachineRecipe;
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

public class MachineCropCategory implements IRecipeCategory<MachineRecipe> {

    private static final int PROG_CYCLE_TICKS = 40;

    private final RecipeType<MachineRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int inX, inY, outX, outY, ebarX, ebarY, ebarW, progX, progY;
    private final int infoX, infoY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public MachineCropCategory(IGuiHelper helper, RecipeType<MachineRecipe> type, ItemStack iconStack,
                               Component title, ResourceLocation texture,
                               int texW, int texH, int visW, int visH,
                               int inX, int inY, int outX, int outY,
                               int ebarX, int ebarY, int ebarW,
                               int progX, int progY, int progW, int progH, int progSrcU, int progSrcV,
                               int infoX, int infoY) {
        this.recipeType = type; this.title = title;
        this.inX=inX; this.inY=inY; this.outX=outX; this.outY=outY;
        this.ebarX=ebarX; this.ebarY=ebarY; this.ebarW=ebarW;
        this.progX=progX; this.progY=progY; this.infoX=infoX; this.infoY=infoY;
        this.background = helper.drawableBuilder(texture, 0, 0, visW, visH).setTextureSize(texW, texH).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic progStatic = helper.drawableBuilder(texture, progSrcU, progSrcV, progW, progH)
                .setTextureSize(texW, texH).build();
        this.progress = helper.createAnimatedDrawable(progStatic, PROG_CYCLE_TICKS,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<MachineRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        Ingredient in = recipe.getIngredients().get(0);
        builder.addSlot(RecipeIngredientRole.INPUT, inX, inY)
               .addItemStacks(withCount(in.getItems(), recipe.getInputCount()));
        RegistryAccess ra = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        builder.addSlot(RecipeIngredientRole.OUTPUT, outX, outY)
               .addItemStack(recipe.getResultItem(ra));
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, ebarX, ebarY, ebarW, anim.frame());
        if (infoX >= 0) {
            var font = Minecraft.getInstance().font;
            JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCost()) + " FE/t", infoX, infoY, JeiGui.ENERGY);
            JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), infoX, infoY + JeiGui.SMALL_LINE, 0xB0B0B0);
        }
    }

    @Override
    public List<Component> getTooltipStrings(MachineRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= ebarX && mx < ebarX + ebarW && my >= ebarY && my < ebarY + JeiGui.FILL_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCost()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks)
                .map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; })
                .toList();
    }
}
