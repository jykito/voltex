package com.jykito.industrialcore.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FluidTankRenderer implements IIngredientRenderer<FluidStack> {

    private final int width, height;
    private final ResourceLocation overlay;
    private final int overlayU, overlayV, texW, texH;

    public FluidTankRenderer(int width, int height) {
        this(width, height, null, 0, 0, 256, 256);
    }

    public FluidTankRenderer(int width, int height,
                             ResourceLocation overlay, int overlayU, int overlayV, int texW, int texH) {
        this.width = width;
        this.height = height;
        this.overlay = overlay;
        this.overlayU = overlayU;
        this.overlayV = overlayV;
        this.texW = texW;
        this.texH = texH;
    }

    @Override
    public void render(GuiGraphics g, FluidStack fluid) {
        if (fluid != null && !fluid.isEmpty()) {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ext.getStillTexture(fluid));
            int color = ext.getTintColor(fluid);
            if (fluid.getFluid() == Fluids.WATER || fluid.getFluid() == Fluids.FLOWING_WATER)
                color = 0xFF3F76E4;
            else if (fluid.getFluid() == Fluids.LAVA || fluid.getFluid() == Fluids.FLOWING_LAVA)
                color = 0xFFFF7A1E;
            float r = ((color >> 16) & 0xFF) / 255f, gg = ((color >> 8) & 0xFF) / 255f,
                  b = (color & 0xFF) / 255f, a = ((color >> 24) & 0xFF) / 255f;
            if (a == 0f) a = 1f;

            RenderSystem.enableBlend();
            g.setColor(r, gg, b, a);
            for (int i = 0; i < width; i += 16) {
                for (int j = 0; j < height; j += 16) {
                    int dw = Math.min(width - i, 16), dh = Math.min(height - j, 16);
                    g.blit(i, height - j - dh, 0, dw, dh, sprite);
                }
            }
            g.setColor(1f, 1f, 1f, 1f);
        }

        if (overlay != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(overlay, 0, 0, overlayU, overlayV, width, height, texW, texH);
        }
    }

    @Override
    public List<Component> getTooltip(FluidStack fluid, TooltipFlag flag) {
        List<Component> tip = new ArrayList<>();
        if (fluid == null || fluid.isEmpty()) return tip;
        tip.add(fluid.getDisplayName());
        tip.add(Component.literal(fluid.getAmount() + " mB").withStyle(ChatFormatting.GRAY));
        return tip;
    }

    @Override public int getWidth()  { return width; }
    @Override public int getHeight() { return height; }
}
