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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class CryoChamberScreen extends MachineScreen<CryoChamberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/cryo_chamber_gui.png");

    private static final int TANK_W = 16, TANK_H = 64, TANK_Y = 32;
    private static final int COOLANT_X = 46, HOT_X = 67, PRODUCT_X = 173;
    private static final int GLASS_U = 34, GLASS_V = 242;

    private static final int PROG_X = 112, PROG_Y = 37, PROG_W = 32, PROG_H = 12;
    private static final int PROG_SRC_U = 102, PROG_SRC_V = 260;

    /** A rod in the slot: teal corners go over the grey slot corners (sprite 20x20, the cell in it starts at 1, 1). */
    private static final int FRAME_W = 20, FRAME_H = 20, FRAME_Y = 73;
    private static final int[] FRAME_X = {95, 118, 141};
    private static final int FRAME_SRC_U = 155, FRAME_SRC_V = 268;

    /**
     * Two bars at the bottom of the window: depth, and below it energy with a bolt on the left, like the other machines
     * (tools/gui_gen/mag_cryo/build.js has the same coordinates).
     */
    private static final int BAR_X = 72, BAR_W = 110, BAR_H = 6;
    private static final int DEPTH_Y = 106, ENERGY_Y = 120;

    /**
     * The depth gauge is piecewise: each stage takes its own segment rather than a share of the absolute
     * maximum. A linear stretch to 2500 squeezed the first two stages into the edge, although the player
     * spends almost all the time exactly there. Segment borders match the ticks on the texture.
     */
    private static final int[] SEG_PX    = {0, 33, 67, BAR_W};
    private static final int[] SEG_DEPTH = {0, 200, 800, CryoCoreBlockEntity.MAX_DEPTH};

    private static final int DEPTH_COLOR  = 0xFF40BEC7;
    private static final int INFLOW_COLOR = 0xFFFFC24A;

    /** Panel with numbers: depth, stage, inflow. */
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

        // the frame lights up when a rod is in the slot
        var h = menu.blockEntity.getItemHandler();
        for (int r = 0; r < FRAME_X.length; r++)
            if (!h.getStackInSlot(2 + r).isEmpty())
                g.blit(TEXTURE, x + FRAME_X[r], y + FRAME_Y, FRAME_SRC_U, FRAME_SRC_V,
                        FRAME_W, FRAME_H, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + ENERGY_Y, menu.getScaledEnergy(BAR_W));

        int depthPx = barPx(menu.getDepth());
        if (depthPx > 0)
            g.fill(x + BAR_X, y + DEPTH_Y, x + BAR_X + depthPx, y + DEPTH_Y + BAR_H, DEPTH_COLOR);

        // Inflow marker: depth always moves towards it and settles exactly on it. The tick to the right of
        // the fill means still going deeper, to the left means losing depth right now.
        int inflowPx = barPx(menu.getHeatInflow());
        if (inflowPx > 0)
            g.fill(x + BAR_X + Math.min(inflowPx, BAR_W - 1), y + DEPTH_Y - 1,
                   x + BAR_X + Math.min(inflowPx, BAR_W - 1) + 1, y + DEPTH_Y + BAR_H + 1, INFLOW_COLOR);

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);

        infoPanel(g, x + INFO_X, y + INFO_Y);
    }

    /** Depth to pixels on the piecewise scale: within a stage segment the stretch is linear. */
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

    /** The panel is narrow (36 px), so the labels are half size; the numbers stay readable. */
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
     * The tank role goes first. Three identical glass tanks in a row without labels read as three
     * inputs, and the first thing the player does is try to fill the second one, although it is an output.
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

    /** Depth is the chamber's main indicator, so the tooltip shows both the stage and the heat inflow. */
    private void depthTooltip(GuiGraphics g, int mx, int my) {
        int x = leftPos + BAR_X, y = topPos + DEPTH_Y;
        if (mx < x || mx >= x + BAR_W || my < y || my >= y + BAR_H) return;

        Component head = Component.translatable("gui.industrial_core.cryo.depth",
                menu.getDepth(), CryoCoreBlockEntity.MAX_DEPTH).withStyle(ModStyle.ENERGY_STYLE);
        Component stageLine = Component.translatable("gui.industrial_core.cryo.stage", menu.getStage())
                .withStyle(ChatFormatting.GRAY);
        Component flow = Component.translatable("gui.industrial_core.cryo.inflow", menu.getHeatInflow())
                .withStyle(ChatFormatting.GOLD);

        // Hit the module cap: say it directly, otherwise the player will keep pouring heat into a wall and not understand why.
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>(
                List.of(head.getVisualOrderText(), stageLine.getVisualOrderText(), flow.getVisualOrderText()));
        int allowed = menu.getAllowedStage();
        if (allowed < 3)
            lines.add(Component.translatable("gui.industrial_core.cryo.module_cap", allowed)
                    .withStyle(ChatFormatting.RED).getVisualOrderText());
        g.renderTooltip(font, lines, mx, my);
    }
}
