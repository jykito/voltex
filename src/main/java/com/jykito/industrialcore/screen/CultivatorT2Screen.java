package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.CultivatorBlockEntity;
import com.jykito.industrialcore.menu.CultivatorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Взраститель T2, окно «Ряды»: три линии со своими стрелками, общие бак, энергия и модуль. Арт — tools/gui_gen/t2_gui.js. */
public class CultivatorT2Screen extends MachineScreen<CultivatorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/cultivator_t2_gui.png");

    private static final int ENERGY_X = 72, ENERGY_Y = 103;

    /** Стрелка линии: слева сверху (PROG_X, PROG_Y + PROG_STEP * линия); спрайт заливки — его, из окна T1. */
    public static final int PROG_X = 120, PROG_Y = 41, PROG_STEP = CultivatorMenu.T2_ROW_STEP, PROG_W = 22, PROG_H = 8;
    private static final int PROG_SRC_U = 120, PROG_SRC_V = 255;

    private static final int TANK_X = 71, TANK_Y = 45, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 167, GLASS_V = 244;

    private static final int SPEC_OV_X = CultivatorMenu.T2_MODULE_X - 1, SPEC_OV_Y = CultivatorMenu.T2_MODULE_Y - 1,
            SPEC_OV_U = 77, SPEC_OV_V = 241, SPEC_OV_SZ = 18;

    public CultivatorT2Screen(CultivatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        FluidGauge.tooltip(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.blockEntity.getFluidInTank(), menu.blockEntity.getTankCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());

        for (int lane = 0; lane < menu.getLanes(); lane++) {
            int pw = menu.getScaledProgress(lane);
            if (pw > 0)
                g.blit(TEXTURE, x + PROG_X, y + PROG_Y + PROG_STEP * lane, PROG_SRC_U, PROG_SRC_V, pw, PROG_H, TEX_W, TEX_H);
        }

        FluidGauge.draw(g, menu.blockEntity.getFluidInTank(), menu.blockEntity.getTankCapacity(),
                x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);

        if (!menu.blockEntity.getItemHandler().getStackInSlot(CultivatorBlockEntity.SLOT_MODULE).isEmpty())
            g.blit(TEXTURE, x + SPEC_OV_X, y + SPEC_OV_Y, SPEC_OV_U, SPEC_OV_V, SPEC_OV_SZ, SPEC_OV_SZ, TEX_W, TEX_H);
    }
}
