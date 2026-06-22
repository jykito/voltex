package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.StoneGeneratorMode;
import com.jykito.industrialcore.menu.StoneGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class StoneGeneratorScreen extends AbstractContainerScreen<StoneGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/stone_generator_gui.png");
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int TEX_W = 256, TEX_H = 315;

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private static final int ENERGY_X = 72, ENERGY_Y = 97, BAR_H = 6;
    private int animTick = 0, animFrame = 0;

    private static final int LAVA_X = 81, WATER_X = 159, TANK_Y = 40, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 167, GLASS_V = 244;

    private static final int PROG_X = 125, PROG_Y = 66, PROG_W = 6, PROG_H = 9, PROG_U = 117, PROG_V = 270;

    private static final int MODE_X = 123, MODE_Y = 76, MODE_SZ = 10, ICON_V = 232;

    private static final int PWR_X = 125, PWR_Y = 87, PWR_SZ = 6, PWR_ON_U = 101, PWR_OFF_U = 107, PWR_V = 250;

    public StoneGeneratorScreen(StoneGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    private Component modeLabel() {
        StoneGeneratorMode m = StoneGeneratorMode.byId(menu.getMode());
        return Component.translatable("screen.industrial_core.stone_generator.mode")
                .append(": ").append(new ItemStack(m.result).getHoverName());
    }

    private Component powerLabel() {
        return Component.translatable(menu.isRunning()
                ? "screen.industrial_core.stone_generator.on"
                : "screen.industrial_core.stone_generator.off");
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (inRect(mx, my, leftPos + MODE_X, topPos + MODE_Y, MODE_SZ, MODE_SZ)) { click(0); return true; }
            if (inRect(mx, my, leftPos + PWR_X,  topPos + PWR_Y,  PWR_SZ,  PWR_SZ))  { click(1); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void click(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FRAMES; }
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);

        renderTankTooltip(g, mx, my, WATER_X, menu.blockEntity.waterTank.getFluid(), menu.blockEntity.waterTank.getCapacity());
        renderTankTooltip(g, mx, my, LAVA_X,  menu.blockEntity.lavaTank.getFluid(),  menu.blockEntity.lavaTank.getCapacity());

        if (inRect(mx, my, leftPos + MODE_X, topPos + MODE_Y, MODE_SZ, MODE_SZ))
            g.renderTooltip(font, modeLabel(), mx, my);
        if (inRect(mx, my, leftPos + PWR_X, topPos + PWR_Y, PWR_SZ, PWR_SZ))
            g.renderTooltip(font, powerLabel(), mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        tileBar(g, ENERGY_FILL, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());

        drawTank(g, x + WATER_X, y + TANK_Y, menu.blockEntity.waterTank.getFluid(), menu.blockEntity.waterTank.getCapacity());
        drawTank(g, x + LAVA_X,  y + TANK_Y, menu.blockEntity.lavaTank.getFluid(),  menu.blockEntity.lavaTank.getCapacity());

        int ph = menu.getScaledProgress();
        if (ph > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y + (PROG_H - ph), PROG_U, PROG_V + (PROG_H - ph), PROG_W, ph, TEX_W, TEX_H);

        int mode = Math.floorMod(menu.getMode(), 4);
        g.blit(TEXTURE, x + MODE_X + 1, y + MODE_Y + 1, 72 + mode * 8, ICON_V, 8, 8, TEX_W, TEX_H);

        int pwrU = menu.isRunning() ? PWR_ON_U : PWR_OFF_U;
        g.blit(TEXTURE, x + PWR_X, y + PWR_Y, pwrU, PWR_V, PWR_SZ, PWR_SZ, TEX_W, TEX_H);
    }

    private void drawTank(GuiGraphics g, int x, int y, FluidStack fluid, int capacity) {
        renderFluid(g, fluid, capacity, x, y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x, y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void tileBar(GuiGraphics g, ResourceLocation tex, int barX, int barY, int filled) {
        if (filled <= 0) return;
        int vOffset = animFrame * FILL_H;
        int drawX = barX;
        while (drawX < barX + filled) {
            int tileW = Math.min(FILL_W, barX + filled - drawX);
            g.blit(tex, drawX, barY, 0, vOffset, tileW, BAR_H, FILL_W, ANIM_H);
            drawX += FILL_W;
        }
    }

    private void renderFluid(GuiGraphics g, FluidStack stack, int capacity, int x, int y, int width, int height) {
        if (stack.isEmpty() || capacity <= 0) return;
        int fluidHeight = (int) ((float) stack.getAmount() / capacity * height);
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

    private void renderTankTooltip(GuiGraphics g, int mx, int my, int tankX, FluidStack fluid, int capacity) {
        int x = leftPos + tankX, y = topPos + TANK_Y;
        if (mx >= x && mx < x + TANK_W && my >= y && my < y + TANK_H) {
            Component text = fluid.isEmpty()
                    ? Component.translatable("gui.industrial_core.empty")
                    : Component.literal("").append(fluid.getDisplayName()).append(": " + fluid.getAmount() + " / " + capacity + " mB");
            g.renderTooltip(font, text, mx, my);
        }
    }
}
