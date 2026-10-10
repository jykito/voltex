package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import com.jykito.industrialcore.menu.CryoChamberMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class CryoChamberScreen extends MachineScreen<CryoChamberMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/cryo_chamber_gui.png");

    private static final int TANK_W = 16, TANK_H = 64, TANK_Y = 32;
    private static final int COOLANT_X = 46, HOT_X = 67, PRODUCT_X = 173;
    private static final int GLASS_U = 34, GLASS_V = 242;

    private static final int PROG_X = 112, PROG_Y = 37, PROG_W = 32, PROG_H = 12;
    private static final int PROG_SRC_U = 102, PROG_SRC_V = 260;

    /** Стержень в слоте: поверх серых уголков слота ложатся бирюзовые (спрайт 20×20, клетка в нём с 1, 1). */
    private static final int FRAME_W = 20, FRAME_H = 20, FRAME_Y = 73;
    private static final int[] FRAME_X = {95, 118, 141};
    private static final int FRAME_SRC_U = 155, FRAME_SRC_V = 268;

    /**
     * Внизу окна две полосы: глубина, под ней энергия с молнией слева, как у остальных машин
     * (tools/gui_gen/mag_cryo/build.js, там те же координаты).
     */
    private static final int BAR_X = 72, BAR_W = 110, BAR_H = 6;
    private static final int DEPTH_Y = 106, ENERGY_Y = 120;

    /**
     * Шкала глубины кусочная: каждая ступень занимает свой отрезок, а не долю от абсолютного
     * максимума. Линейная растяжка на 2500 давила первые две ступени в край, хотя игрок
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
        this.imageHeight = 228;
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
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

        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + ENERGY_Y, menu.getScaledEnergy(BAR_W));

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
        FluidGauge.draw(g, t.getFluid(), t.getCapacity(), x, y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x, y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
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
