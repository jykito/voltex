package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.NuclearReactorBlockEntity;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.custom.ReactorSchemeItem;
import com.jykito.industrialcore.jei.ReactorSchemeRecipe;
import com.jykito.industrialcore.menu.NuclearReactorMenu;
import com.jykito.industrialcore.reactor.ReactorSchemes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class NuclearReactorScreen extends AbstractContainerScreen<NuclearReactorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/nuclear_reactor_gui.png");
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");
    private static final ResourceLocation HEAT_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/heat_fill_6x_anim.png");

    private static final int TEX_W = 256, TEX_H = 315;

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private int animTick = 0, animFrame = 0;

    private static final int GRID_X0 = 79, GRID_Y0 = 10, GRID_STEP = 18;

    private static final int ENERGY_X = 79, ENERGY_Y = 100, BAR_W = 88, BAR_H = 6;
    private static final int HEAT_X = 79, HEAT_Y = 109;

    private static final int COOLANT_X = 175, COOLANT_Y = 34, TANK_W = 16, TANK_H = 64;
    private static final int HOT_X = 199, HOT_Y = 34;

    private static final int GLASS_U = 192, GLASS_V = 237;

    private static final int INFO_X = 17, INFO_Y = 10, INFO_W = 57, INFO_H = 88;

    public NuclearReactorScreen(NuclearReactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 256;
        this.imageHeight = 224;
    }

    @Override protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
    }

    @Override
    protected java.util.List<Component> getTooltipFromContainerItem(ItemStack stack) {
        java.util.List<Component> tip = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && hoveredSlot.index < NuclearReactorBlockEntity.GRID_SLOTS) {
            int heat = menu.getCellHeat(hoveredSlot.index);
            if (heat > 0) {
                tip = new java.util.ArrayList<>(tip);
                tip.add(Component.translatable("screen.industrial_core.reactor.heat", heat,
                        NuclearReactorBlockEntity.MAX_CELL_HEAT)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
        return tip;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FRAMES; }
        super.render(g, mouseX, mouseY, partial);
        renderSchemeOverlays(g);
        this.renderTooltip(g, mouseX, mouseY);
        renderBarTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        tileBar(g, ENERGY_FILL, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());
        tileBar(g, HEAT_FILL,   x + HEAT_X,   y + HEAT_Y,   menu.getScaledHullHeat());

        renderTank(g, x + COOLANT_X, y + COOLANT_Y,
                new FluidStack(ModFluids.SOURCE_COOLANT.get(), menu.getCoolantAmount()));
        renderTank(g, x + HOT_X, y + HOT_Y,
                new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), menu.getHotCoolantAmount()));

        for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
            int heat = menu.getCellHeat(i);
            if (heat <= 0) continue;
            int col = i % NuclearReactorBlockEntity.GRID_COLS;
            int row = i / NuclearReactorBlockEntity.GRID_COLS;
            int sx = x + GRID_X0 + col * GRID_STEP;
            int sy = y + GRID_Y0 + row * GRID_STEP;
            float ratio = Math.min(1f, (float) heat / NuclearReactorBlockEntity.MAX_CELL_HEAT);
            int alpha = (int)(ratio * 0xA0);
            g.fill(sx, sy, sx + 16, sy + 16, (alpha << 24) | 0xFF4400);
        }

        int schemeIdx = menu.getSchemeIndex();
        if (schemeIdx >= 0) {
            String blockedLayout = ReactorSchemes.SCHEMES[schemeIdx].layout();
            for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
                if (blockedLayout.charAt(i) != '.') continue;
                if (!menu.slots.get(i).getItem().isEmpty()) continue;
                int col = i % NuclearReactorBlockEntity.GRID_COLS;
                int row = i / NuclearReactorBlockEntity.GRID_COLS;
                int sx = x + GRID_X0 + col * GRID_STEP + 1;
                int sy = y + GRID_Y0 + row * GRID_STEP + 1;
                g.fill(sx, sy, sx + 15, sy + 15, 0x99000000);
            }
        }

        if (schemeIdx >= 0 && !menu.isSchemeMatch()) {
            String layout = ReactorSchemes.SCHEMES[schemeIdx].layout();
            for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
                char c = layout.charAt(i);
                if (c == '.') continue;
                ItemStack actual = menu.slots.get(i).getItem();
                if (!actual.isEmpty()) continue;
                ItemStack ghost = ReactorSchemeRecipe.charToStack(c);
                if (ghost.isEmpty()) continue;
                int col = i % NuclearReactorBlockEntity.GRID_COLS;
                int row = i / NuclearReactorBlockEntity.GRID_COLS;
                int sx = x + GRID_X0 + col * GRID_STEP;
                int sy = y + GRID_Y0 + row * GRID_STEP;
                g.renderItem(ghost, sx, sy);
                g.fill(sx, sy, sx + 16, sy + 16, 0xAAFFFFFF);
            }
        }
    }

    private void renderTank(GuiGraphics g, int x, int y, FluidStack fluid) {
        renderFluid(g, fluid, NuclearReactorBlockEntity.TANK_CAPACITY, x, y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x, y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void renderSchemeOverlays(GuiGraphics g) {
        int schemeIdx = menu.getSchemeIndex();
        if (schemeIdx < 0 || menu.isSchemeMatch()) return;
        String layout = ReactorSchemes.SCHEMES[schemeIdx].layout();
        for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
            ItemStack actual = menu.slots.get(i).getItem();
            if (actual.isEmpty()) continue;
            char c = layout.charAt(i);
            if (ReactorSchemeItem.charMatchesStack(c, actual)) continue;
            int col = i % NuclearReactorBlockEntity.GRID_COLS;
            int row = i / NuclearReactorBlockEntity.GRID_COLS;
            int sx = this.leftPos + GRID_X0 + col * GRID_STEP + 1;
            int sy = this.topPos  + GRID_Y0 + row * GRID_STEP + 1;
            g.fill(sx, sy, sx + 16, sy + 16, 0x66FF0000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        boolean active = menu.isActive();
        boolean hot    = menu.isDangerous();
        int ept        = menu.getEnergyPerTick();
        int hull       = menu.getHullHeat();
        int tx = INFO_X + 2, ty = INFO_Y + 2;

        line(g, tx, ty,      active ? formatFE(ept) + "/t" : Component.translatable("screen.industrial_core.reactor.idle").getString(), active ? com.jykito.industrialcore.ModStyle.ENERGY : 0xAAAAAA);
        line(g, tx, ty + 11, Component.translatable("screen.industrial_core.reactor.hull", hull).getString(), hot ? 0xFF4444 : 0xDDDDDD);
        line(g, tx, ty + 22, formatFE(menu.getEnergyStored()) + " FE", com.jykito.industrialcore.ModStyle.ENERGY);
        line(g, tx, ty + 33, Component.translatable("screen.industrial_core.reactor.coolant_short", menu.getCoolantAmount()).getString(), 0x44AAFF);
        if (hot) line(g, tx, ty + 44, Component.translatable("screen.industrial_core.reactor.overheat").getString(), 0xFF2222);

        int schemeIdx = menu.getSchemeIndex();
        if (schemeIdx >= 0) {
            String name = Component.translatable(ReactorSchemes.SCHEMES[schemeIdx].nameKey()).getString();
            line(g, tx, ty + 60, name, menu.isSchemeMatch() ? 0x55FF55 : 0xFF5555);
        } else {
            line(g, tx, ty + 60, Component.translatable("screen.industrial_core.reactor.no_scheme").getString(), 0x888888);
        }
    }

    private void line(GuiGraphics g, int x, int y, String text, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
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
        if (stack.isEmpty()) return;
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

    private void renderBarTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (isOver(ENERGY_X, ENERGY_Y, BAR_W, BAR_H, mouseX, mouseY))
            g.renderTooltip(font, Component.translatable("screen.industrial_core.reactor.energy",
                    formatFE(menu.getEnergyStored()),
                    formatFE(NuclearReactorBlockEntity.MAX_ENERGY)), mouseX, mouseY);

        if (isOver(HEAT_X, HEAT_Y, BAR_W, BAR_H, mouseX, mouseY))
            g.renderTooltip(font, Component.translatable("screen.industrial_core.reactor.hull_heat",
                    menu.getHullHeat(),
                    NuclearReactorBlockEntity.MAX_HULL_HEAT), mouseX, mouseY);

        if (isOver(COOLANT_X, COOLANT_Y, TANK_W, TANK_H, mouseX, mouseY))
            g.renderTooltip(font, Component.translatable("screen.industrial_core.reactor.coolant",
                    menu.getCoolantAmount(),
                    NuclearReactorBlockEntity.TANK_CAPACITY), mouseX, mouseY);

        if (isOver(HOT_X, HOT_Y, TANK_W, TANK_H, mouseX, mouseY))
            g.renderTooltip(font, Component.translatable("screen.industrial_core.reactor.hot_coolant",
                    menu.getHotCoolantAmount(),
                    NuclearReactorBlockEntity.TANK_CAPACITY), mouseX, mouseY);

        int gx = leftPos + GRID_X0, gy = topPos + GRID_Y0;
        for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
            int heat = menu.getCellHeat(i);
            if (heat <= 0) continue;
            if (!menu.slots.get(i).getItem().isEmpty()) continue;
            int col = i % NuclearReactorBlockEntity.GRID_COLS;
            int row = i / NuclearReactorBlockEntity.GRID_COLS;
            int sx = gx + col * GRID_STEP, sy = gy + row * GRID_STEP;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16)
                g.renderTooltip(font, Component.translatable("screen.industrial_core.reactor.cell",
                        row, col, heat,
                        NuclearReactorBlockEntity.MAX_CELL_HEAT), mouseX, mouseY);
        }
    }

    private boolean isOver(int rx, int ry, int w, int h, int mx, int my) {
        int ax = leftPos + rx, ay = topPos + ry;
        return mx >= ax && mx < ax + w && my >= ay && my < ay + h;
    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000)     return String.format("%.1fk", fe / 1_000.0);
        return String.valueOf(fe);
    }
}
