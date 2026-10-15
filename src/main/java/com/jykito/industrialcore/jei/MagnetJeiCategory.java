package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.recipe.AcceleratorRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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

/** JEI category of the magnet-accelerator: 8 inputs in a ring -> output, + parameters (coil/energy/time/gate/x2). */
public class MagnetJeiCategory implements IRecipeCategory<AcceleratorRecipe> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/jei_magnet.png");
    private static final ResourceLocation PROG_ANIM =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/progress_bar_magnet_anim.png");
    private static final int TEX_W = 154, TEX_H = 220, VIS_W = 154, VIS_H = 146;

    // 8 inputs in a ring (blue 0-3, red 4-7) + the output in the centre
    private static final int[][] IN = {
            {69,18},{46,27},{33,48},{46,69},
            {92,27},{105,48},{92,69},{69,78} };
    private static final int OUT_X = 69, OUT_Y = 48;
    private static final int EBAR_X = 22, EBAR_Y = 104, EBAR_W = 110;
    private static final int PROG_X = 46, PROG_Y = 25, PROG_FRAME = 64, PROG_FRAMES = 18; // 64x64 shape, centred on the output (+1,+1)
    private static final int INFO_X = 27, INFO_Y = 113;

    private final RecipeType<AcceleratorRecipe> recipeType;
    private final IDrawable background, icon;
    private final Component title;
    private final JeiGui.Anim eAnim = new JeiGui.Anim();
    private int pTick = 0, pFrame = 0;

    public MagnetJeiCategory(IGuiHelper helper, RecipeType<AcceleratorRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, VIS_W, VIS_H).setTextureSize(TEX_W, TEX_H).build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override public RecipeType<AcceleratorRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle()      { return title; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AcceleratorRecipe recipe, IFocusGroup focuses) {
        var ings = recipe.getIngredients();
        int[] counts = recipe.getCounts();
        for (int i = 0; i < ings.size() && i < IN.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, IN[i][0], IN[i][1])
                   .addItemStacks(withCount(ings.get(i).getItems(), i < counts.length ? counts[i] : 1));
        }
        RegistryAccess ra = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).addItemStack(recipe.getResultItem(ra));
    }

    @Override
    public void draw(AcceleratorRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        if (++pTick >= 4) { pTick = 0; pFrame = (pFrame + 1) % PROG_FRAMES; }           // progress shape (animation frames)
        g.blit(PROG_ANIM, PROG_X, PROG_Y, 0, pFrame * PROG_FRAME, PROG_FRAME, PROG_FRAME, PROG_FRAME, PROG_FRAME * PROG_FRAMES);

        eAnim.advance();                                                                // energy bar (animated fill)
        JeiGui.tileFill(g, JeiGui.ENERGY_FILL, EBAR_X, EBAR_Y, EBAR_W, eAnim.frame());

        var font = Minecraft.getInstance().font;                                        // parameters (3 lines)
        int tier = Math.max(1, Math.min(3, recipe.getMinTier()));
        JeiGui.smallText(g, font, Component.translatable("jei.industrial_core.magnet.coil." + tier), INFO_X, INFO_Y, 0xC0C0C0);
        if (recipe.getBandHi() > recipe.getBandLo())
            JeiGui.smallText(g, font, Component.translatable("jei.industrial_core.magnet.x2", (int) recipe.getBandLo(), (int) recipe.getBandHi()), INFO_X + 64, INFO_Y, 0x55FF7A);
        JeiGui.smallText(g, font, JeiGui.fe(recipe.getEnergyPerTick()) + " FE/t", INFO_X, INFO_Y + JeiGui.SMALL_LINE, JeiGui.ENERGY);
        JeiGui.smallText(g, font, JeiGui.secs(recipe.getProcessingTime()), INFO_X + 40, INFO_Y + JeiGui.SMALL_LINE, 0xB0B0B0);
        JeiGui.smallText(g, font, Component.translatable("jei.industrial_core.magnet.gate", (int) recipe.getMinInstab()), INFO_X, INFO_Y + 2 * JeiGui.SMALL_LINE, 0xFFD24A);
    }

    @Override
    public List<Component> getTooltipStrings(AcceleratorRecipe recipe, IRecipeSlotsView view, double mx, double my) {
        if (mx >= EBAR_X && mx < EBAR_X + EBAR_W && my >= EBAR_Y && my < EBAR_Y + JeiGui.FILL_H) {
            return List.of(Component.literal(JeiGui.fe(recipe.getEnergyPerTick()) + " FE/t").withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks).map(s -> { ItemStack c = s.copy(); c.setCount(Math.max(1, count)); return c; }).toList();
    }
}
