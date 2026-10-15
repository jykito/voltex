package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.RestructorBlockEntity;
import com.jykito.industrialcore.menu.RestructorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Restructor T2, the "Shared catalyst" window: each line has its own charge, the vein lights up with a catalyst. Art: tools/gui_gen/t2_gui.js. */
public class RestructorT2Screen extends MachineScreen<RestructorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/restructor_t2_gui.png");

    /** Line charge: (PROGRESS_X, PROGRESS_Y + step * line), filled from bottom to top; the sprite is the author's, from the T1 window. */
    private static final int PROGRESS_X = 122, PROGRESS_Y = RestructorMenu.T2_ROW_Y, PROGRESS_W = 12, PROGRESS_H = 17;
    private static final int PROGRESS_SRC_U = 166, PROGRESS_SRC_V = 260;

    /** The lit vein from the catalyst to the inputs. */
    public static final int PIPES_X = 76, PIPES_Y = 43, PIPES_W = 22, PIPES_H = 50;
    private static final int PIPES_SRC_U = 10, PIPES_SRC_V = 240;

    public RestructorT2Screen(RestructorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private static int laneY(int lane) { return PROGRESS_Y + RestructorMenu.T2_ROW_STEP * lane; }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        if (menu.getSlot(RestructorBlockEntity.SLOT_CATALYST).hasItem())
            g.blit(TEXTURE, x + PIPES_X, y + PIPES_Y, PIPES_SRC_U, PIPES_SRC_V, PIPES_W, PIPES_H, TEX_W, TEX_H);

        for (int lane = 0; lane < menu.getLanes(); lane++) {
            int sp = menu.getScaledProgress(lane);
            if (sp > 0)
                g.blit(TEXTURE, x + PROGRESS_X, y + laneY(lane) + PROGRESS_H - sp,
                        PROGRESS_SRC_U, PROGRESS_SRC_V + (PROGRESS_H - sp), PROGRESS_W, sp, TEX_W, TEX_H);
        }
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        for (int lane = 0; lane < menu.getLanes(); lane++) {
            if (!isHovering(PROGRESS_X, laneY(lane), PROGRESS_W, PROGRESS_H, mx, my)) continue;
            int cost = menu.getMaxRecipeCost(lane);
            String costStr = cost > 0 ? RestructorScreen.formatFE(cost) : "—";
            g.renderTooltip(font, Component.translatable("screen.industrial_core.restructor.charge",
                    RestructorScreen.formatFE(menu.getEnergyAccumulated(lane)), costStr), mx, my);
        }
    }
}
