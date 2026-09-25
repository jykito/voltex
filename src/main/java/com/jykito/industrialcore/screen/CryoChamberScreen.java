package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import com.jykito.industrialcore.menu.CryoChamberMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
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
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class CryoChamberScreen extends AbstractContainerScreen<CryoChamberMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/cryo_chamber_gui.png");
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int TEX_W = 256, TEX_H = 315;

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private int animTick = 0, animFrame = 0;

    private static final int TANK_W = 16, TANK_H = 64, TANK_Y = 32;
    private static final int COOLANT_X = 46, HOT_X = 67, PRODUCT_X = 173;
    private static final int GLASS_U = 34, GLASS_V = 242;

    private static final int PROG_X = 112, PROG_Y = 37, PROG_W = 32, PROG_H = 12;
    private static final int PROG_SRC_U = 102, PROG_SRC_V = 260;

    /** Рамки вокруг слотов стержней: пустая нарисована в фоне, горящая накладывается сверху. */
    private static final int FRAME_W = 20, FRAME_H = 20, FRAME_Y = 73;
    private static final int[] FRAME_X = {95, 118, 141};
    private static final int FRAME_SRC_U = 155, FRAME_SRC_V = 268;

    private static final int BAR_X = 73, BAR_W = 110, BAR_H = 6;
    private static final int DEPTH_Y = 112, ENERGY_Y = 127;

    /**
     * Шкала глубины кусочная: каждая ступень занимает свой отрезок, а не долю от абсолютного
     * максимума. Линейная растяжка на 2500 давила первые две ступени в левый край, хотя игрок
     * почти всё время сидит именно там. Границы отрезков совпадают с рисками на текстуре.
     */
    private static final int[] SEG_PX    = {0, 33, 67, BAR_W};
    private static final int[] SEG_DEPTH = {0, 200, 800, CryoCoreBlockEntity.MAX_DEPTH};

    private static final int DEPTH_COLOR  = 0xFF40BEC7;
    private static final int INFLOW_COLOR = 0xFFFFC24A;

    /** Панель с числами: глубина, ступень, приток. */
    private static final int INFO_X = 192, INFO_Y = 41;

    public CryoChamberScreen(CryoChamberMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 228;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FRAMES; }
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);

        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + ENERGY_Y, BAR_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
        depthTooltip(g, mx, my);

        tankTooltip(g, mx, my, COOLANT_X, menu.blockEntity.coolantTank, "gui.industrial_core.cryo.tank.coolant");
        tankTooltip(g, mx, my, HOT_X,     menu.blockEntity.hotTank,     "gui.industrial_core.cryo.tank.hot");
        tankTooltip(g, mx, my, PRODUCT_X, menu.blockEntity.productTank, "gui.industrial_core.cryo.tank.product");
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        tank(g, x + COOLANT_X, y + TANK_Y, menu.blockEntity.coolantTank);
        tank(g, x + HOT_X,     y + TANK_Y, menu.blockEntity.hotTank);
        tank(g, x + PRODUCT_X, y + TANK_Y, menu.blockEntity.productTank);

        // рамка загорается, когда в слоте стоит стержень
        var h = menu.blockEntity.getItemHandler();
        for (int r = 0; r < FRAME_X.length; r++)
            if (!h.getStackInSlot(2 + r).isEmpty())
                g.blit(TEXTURE, x + FRAME_X[r], y + FRAME_Y, FRAME_SRC_U, FRAME_SRC_V,
                        FRAME_W, FRAME_H, TEX_W, TEX_H);

        tileBar(g, ENERGY_FILL, x + BAR_X, y + ENERGY_Y, menu.getScaledEnergy(BAR_W));

        int depthPx = barPx(menu.getDepth());
        if (depthPx > 0)
            g.fill(x + BAR_X, y + DEPTH_Y, x + BAR_X + depthPx, y + DEPTH_Y + BAR_H, DEPTH_COLOR);

        // Метка притока: глубина всегда идёт к нему и садится ровно на него. Риска правее
        // заливки — ещё погружаемся, левее — теряем глубину прямо сейчас.
        int inflowPx = barPx(menu.getHeatInflow());
        if (inflowPx > 0)
            g.fill(x + BAR_X + Math.min(inflowPx, BAR_W - 1), y + DEPTH_Y - 1,
                   x + BAR_X + Math.min(inflowPx, BAR_W - 1) + 1, y + DEPTH_Y + BAR_H + 1, INFLOW_COLOR);

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);

        infoPanel(g, x + INFO_X, y + INFO_Y);
    }

    /** Глубина в пиксели по кусочной шкале: внутри отрезка ступени растяжка линейная. */
    private static int barPx(int depth) {
        if (depth <= 0) return 0;
        for (int i = 1; i < SEG_DEPTH.length; i++) {
            if (depth >= SEG_DEPTH[i]) continue;
            int d0 = SEG_DEPTH[i - 1], d1 = SEG_DEPTH[i];
            int p0 = SEG_PX[i - 1],    p1 = SEG_PX[i];
            return p0 + (depth - d0) * (p1 - p0) / Math.max(1, d1 - d0);
        }
        return BAR_W;
    }

    private void infoPanel(GuiGraphics g, int x, int y) {
        small(g, Component.translatable("gui.industrial_core.cryo.label.depth"), x, y, 0x6E8A96);
        g.drawString(font, String.valueOf(menu.getDepth()), x, y + 6, DEPTH_COLOR, false);

        small(g, Component.translatable("gui.industrial_core.cryo.label.stage"), x, y + 20, 0x6E8A96);
        g.drawString(font, String.valueOf(menu.getStage()), x, y + 26, 0xC8D6DE, false);

        small(g, Component.translatable("gui.industrial_core.cryo.label.inflow"), x, y + 40, 0x6E8A96);
        g.drawString(font, menu.getHeatInflow() + "/t", x, y + 46, INFLOW_COLOR, false);
    }

    /** Панель узкая (36 px), поэтому подписи идут вполовину размера — числа остаются читаемыми. */
    private void small(GuiGraphics g, Component text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void tank(GuiGraphics g, int x, int y, FluidTank t) {
        renderFluid(g, t.getFluid(), t.getCapacity(), x, y, TANK_W, TANK_H);
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
        int fluidHeight = Math.min(height, (int)((float) stack.getAmount() / capacity * height));
        if (fluidHeight <= 0) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int color = ext.getTintColor(stack);
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

    /**
     * Роль бака идёт первой строкой. Три одинаковых стекляшки подряд без подписи читаются как три
     * входа, и первое, что делает игрок, — пытается залить во второй, хотя это выход.
     */
    private void tankTooltip(GuiGraphics g, int mx, int my, int tankX, FluidTank t, String roleKey) {
        int x = leftPos + tankX, y = topPos + TANK_Y;
        if (mx < x || mx >= x + TANK_W || my < y || my >= y + TANK_H) return;
        FluidStack fluid = t.getFluid();
        Component role = Component.translatable(roleKey).withStyle(ModStyle.ENERGY_STYLE);
        Component body = fluid.isEmpty()
                ? Component.translatable("gui.industrial_core.empty").withStyle(ChatFormatting.GRAY)
                : Component.literal("").append(fluid.getDisplayName())
                        .append(": " + fluid.getAmount() + " / " + t.getCapacity() + " mB")
                        .withStyle(ChatFormatting.GRAY);
        g.renderTooltip(font, List.of(role.getVisualOrderText(), body.getVisualOrderText()), mx, my);
    }

    /** Глубина — главный показатель камеры, поэтому в подсказке и ступень, и приток тепла. */
    private void depthTooltip(GuiGraphics g, int mx, int my) {
        int x = leftPos + BAR_X, y = topPos + DEPTH_Y;
        if (mx < x || mx >= x + BAR_W || my < y || my >= y + BAR_H) return;

        Component head = Component.translatable("gui.industrial_core.cryo.depth",
                menu.getDepth(), CryoCoreBlockEntity.MAX_DEPTH).withStyle(ModStyle.ENERGY_STYLE);
        Component stageLine = Component.translatable("gui.industrial_core.cryo.stage", menu.getStage())
                .withStyle(ChatFormatting.GRAY);
        Component flow = Component.translatable("gui.industrial_core.cryo.inflow", menu.getHeatInflow())
                .withStyle(ChatFormatting.GOLD);

        // Упёрлись в модуль — говорим прямо, иначе игрок будет лить тепло в стену и не поймёт.
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>(
                List.of(head.getVisualOrderText(), stageLine.getVisualOrderText(), flow.getVisualOrderText()));
        int allowed = menu.getAllowedStage();
        if (allowed < 3)
            lines.add(Component.translatable("gui.industrial_core.cryo.module_cap", allowed)
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
        g.renderTooltip(font, lines, mx, my);
    }
}
