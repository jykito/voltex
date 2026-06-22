package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;

public class RestructorJeiCategory implements IRecipeCategory<MachineRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_restructor.png");
    private static final int TEX_W = 154, TEX_H = 140, VIS_W = 154, VIS_H = 84;

    private static final int IN_X = 44, IN_Y = 21, OUT_X = 94, OUT_Y = 21, CAT_X = 69, CAT_Y = 42;
    private static final int EBAR_X = 44, EBAR_Y = 64, EBAR_W = 66;
    private static final int PROG_GX = 71, PROG_GY = 21, PROG_W = 12, PROG_H = 17, PROG_U = 73, PROG_V = 99;
    private static final int INFO_X = 60, INFO_Y = 7;

    private final RecipeType<MachineRecipe> recipeType;
    private final IDrawable background, icon;
    private final IDrawableAnimated progress;
    private final Component title;
    private final int progX = PROG_GX, progY = PROG_GY;
    private final JeiGui.Anim anim = new JeiGui.Anim();

    public RestructorJeiCategory(IGuiHelper helper, RecipeType<MachineRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
        IDrawableStatic ps = helper.drawableBuilder(TEXTURE, PROG_U, PROG_V, PROG_W, PROG_H).setTextureSize(TEX_W, TEX_H).build();
        this.progress = helper.createAnimatedDrawable(ps, 40, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override public RecipeType<MachineRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        Ingredient in = recipe.getIngredients().get(0);
        builder.addSlot(RecipeIngredientRole.INPUT, IN_X, IN_Y)
               .addItemStacks(withCount(in.getItems(), recipe.getInputCount()));

        RegistryAccess ra = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResultItem(ra));

        String catalystId = recipe.getCatalystId();
        if (catalystId != null) {
            var catItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(catalystId));
            if (catItem != null)
                builder.addSlot(RecipeIngredientRole.CATALYST, CAT_X, CAT_Y).addItemStack(new ItemStack(catItem));
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        progress.draw(g, progX, progY);
        anim.advance();
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, anim.frame());
        var font = Minecraft.getInstance().font;
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyCost()) + " FE/t", INFO_X, INFO_Y, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
    }

    @Override
    public List<Component> getTooltipStrings(MachineRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= EBAR_X && mx < EBAR_X + EBAR_W && my >= EBAR_Y && my < EBAR_Y + JeiGui.FILL_H) {
            return List.of(
                    Component.translatable("jei.industrial_core.energy").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE),
                    Component.literal(JeiGui.fe(recipe.getEnergyCost()) + " FE/t").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.industrial_core.time", recipe.getProcessingTime() / 20)
                            .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }
}
