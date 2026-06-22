package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.menu.MinerCoreMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class MinerCoreScreen extends AbstractContainerScreen<MinerCoreMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/miner_core_gui.png");
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int TEX_W = 256, TEX_H = 315;

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private static final int ENERGY_X = 93, ENERGY_Y = 105, ENERGY_W = 69, BAR_H = 6;
    private int animTick = 0, animFrame = 0;

    private static final int TANK_X = 64, TANK_Y = 43, TANK_W = 16, TANK_H = 64;
    private static final int GLASS_U = 192, GLASS_V = 243;

    private static final int PROG_X = 111, PROG_Y = 53, PROG_W = 30, PROG_H = 34;
    private static final int PROG_SRC_U = 106, PROG_SRC_V = 251;

    public MinerCoreScreen(MinerCoreMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 256;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FRAMES; }
        super.render(g, mx, my, partial);
        this.renderTooltip(g, mx, my);

        int ex = leftPos + ENERGY_X, ey = topPos + ENERGY_Y;
        if (mx >= ex && mx < ex + ENERGY_W && my >= ey && my < ey + BAR_H)
            g.renderTooltip(font, Component.literal(
                    menu.getTotalEnergy() + " / " + menu.getMaxTotalEnergy() + " FE")
                    .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE), mx, my);

        int fx = leftPos + TANK_X, fy = topPos + TANK_Y;
        if (mx >= fx && mx < fx + TANK_W && my >= fy && my < fy + TANK_H)
            g.renderTooltip(font, Component.literal(
                    menu.getTotalFluid() + " / " + menu.getMaxTotalFluid() + " mB"), mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy(ENERGY_W);
        if (filled > 0) {
            int vOffset = animFrame * FILL_H;
            int drawX = x + ENERGY_X;
            while (drawX < x + ENERGY_X + filled) {
                int tileW = Math.min(FILL_W, x + ENERGY_X + filled - drawX);
                g.blit(ENERGY_FILL, drawX, y + ENERGY_Y, 0, vOffset, tileW, BAR_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);

        renderFluid(g, new FluidStack(ModFluids.SOURCE_DRILL_FLUID.get(), menu.getTotalFluid()),
                menu.getMaxTotalFluid(), x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void renderFluid(GuiGraphics g, FluidStack stack, int capacity, int x, int y, int width, int height) {
        if (stack.isEmpty() || capacity <= 0) return;
        int fluidHeight = (int)((float) stack.getAmount() / capacity * height);
        if (fluidHeight <= 0) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int color = ext.getTintColor(stack);
        if (stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || stack.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER) color = 0xFF3F76E4;
        float r = ((color >> 16) & 0xFF) / 255f, gg = ((color >> 8) & 0xFF) / 255f,
              b = (color & 0xFF) / 255f, a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0f) a = 1f;
        RenderSystem.enableBlend();
        g.setColor(r, gg, b, a);
        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < fluidHeight; j += 16) {
                int dw = Math.min(width - i, 16), dh = Math.min(fluidHeight - j, 16);
                g.blit(x + i, y + height - j - dh, 0, dw, dh, sprite);
            }
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
