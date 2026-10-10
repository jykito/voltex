package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.EvolutionChamberBlockEntity;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.menu.EvolutionChamberMenu;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteFeed;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Окно камеры эволюции 256×240: табло (статус, поколение, сродство к целевой руде, стабильность), слоты штамма и руды,
 * график сродства по поколениям (новые справа, по 2 px на поколение), полосы хода поколения, энергии и тепла.
 */
public class EvolutionChamberScreen extends MachineScreen<EvolutionChamberMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/evolution_chamber_gui.png");

    private static final int WINDOW_H = 240;
    private static final int TEXT_X = 20, LINE1_Y = 16, LINE2_Y = 27, RIGHT_X = 236;
    private static final int BAR_X = 23, BAR_W = 218, BAR_H = 6, PROG_Y = 107, ENERGY_Y = 118, HEAT_Y = 129;
    private static final int GRAPH_X = 48, GRAPH_Y = 47, GRAPH_W = 192, GRAPH_H = 52, STEP = 2;

    private static final int C_OK = 0xFF6BE06B, C_BAD = 0xFFFF7A6B, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0,
            C_ACCENT = 0xFF7FF4FF, C_LINE = 0xFF60CFE7, C_AREA = 0x4430A8C0, C_GRID = 0xFF1E2A2E;

    private final FillBar chevrons = new FillBar(6);

    public EvolutionChamberScreen(EvolutionChamberMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        chevrons.advance();
        chevrons.draw(g, FillBar.PROGRESS, x + BAR_X, y + PROG_Y, menu.scaledProgress(BAR_W), BAR_H);
        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + ENERGY_Y, menu.scaledEnergy(BAR_W), BAR_H);
        bars.draw(g, FillBar.HEAT, x + BAR_X, y + HEAT_Y, menu.scaledHeat(BAR_W), BAR_H);

        drawBoard(g, x, y);
        drawGraph(g, x, y);
    }

    private void drawBoard(GuiGraphics g, int x, int y) {
        EvolutionChamberBlockEntity.Status s = menu.status();
        text(g, tr("status." + s.name().toLowerCase(Locale.ROOT)), x + TEXT_X, y + LINE1_Y, s == EvolutionChamberBlockEntity.Status.WORKING ? C_OK : C_BAD);
        ItemStack strain = menu.strain();
        if (strain.isEmpty()) return;
        NaniteGenome genome = NaniteGenome.read(strain);
        right(g, tr("generation", genome.generation()), x + RIGHT_X, y + LINE1_Y, C_VALUE);
        String stab = tr("stability", NaniteStrainItem.percent(genome.stability()));
        right(g, stab, x + RIGHT_X, y + LINE2_Y, C_LABEL);
        int room = RIGHT_X - TEXT_X - Math.round(font.width(stab) * 0.9f) - 6;   // левый текст не заходит на стабильность

        // сродство к цели корма в слоте, а без корма — к главной цели штамма; как читать число, решает класс
        ItemStack feed = menu.chamber.getItems().getStackInSlot(EvolutionChamberBlockEntity.SLOT_FEED);
        NaniteFeed food = NaniteFeed.of(feed);
        String target = food == null ? genome.dominant() : food.target();
        if (target == null) return;
        Component name = NaniteStrainItem.targetName(genome, target, food == null || food.kind() == NaniteGenome.Kind.SCIENTIST ? null : feed);
        text(g, clip(name.getString() + "  " + NaniteStrainItem.effectShort(genome, target).getString(), room), x + TEXT_X, y + LINE2_Y, C_ACCENT);
    }

    private void drawGraph(GuiGraphics g, int x, int y) {
        int gx = x + GRAPH_X, gy = y + GRAPH_Y, bottom = gy + GRAPH_H;
        ItemStack strain = menu.strain();
        float top = NaniteGenome.cap(strain.isEmpty() ? null : NaniteGenome.kindOf(strain)), cap = top * 1000;
        for (int k = 1; k < 3; k++) {   // сетка на трети шкалы
            int ly = bottom - 1 - Math.round((GRAPH_H - 1) * k / 3f);
            for (int px = gx; px < gx + GRAPH_W; px += 4) g.fill(px, ly, px + 2, ly + 1, C_GRID);
        }
        boolean scientist = !strain.isEmpty() && NaniteGenome.kindOf(strain) == NaniteGenome.Kind.SCIENTIST;
        small(g, (scientist ? "" : "+") + Math.round(top * 100) + "%", gx + 2, gy + 1, C_LABEL);

        List<Integer> h = menu.chamber.getHistory();
        if (h.isEmpty()) {
            // переносится по ширине графика: английская подсказка в одну строку вылезала за окно (юзер 10-10)
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(tr("graph_empty")), Math.round((GRAPH_W - 4) / 0.7f));
            for (int i = 0; i < lines.size(); i++) {
                g.pose().pushPose(); g.pose().translate(gx + 2, bottom - 7 * (lines.size() - i), 0); g.pose().scale(0.7f, 0.7f, 1f);
                g.drawString(font, lines.get(i), 0, 0, C_LABEL, false);
                g.pose().popPose();
            }
            return;
        }
        int n = h.size(), x0 = gx + GRAPH_W - n * STEP, prev = 0;
        for (int i = 0; i < n; i++) {
            int px = x0 + i * STEP;
            int py = bottom - 1 - Math.round((GRAPH_H - 1) * Math.min(1f, h.get(i) / cap));
            g.fill(px, py, px + STEP, bottom, C_AREA);
            if (i > 0) g.fill(px, Math.min(prev, py), px + 1, Math.max(prev, py) + 1, C_LINE);
            g.fill(px, py, px + STEP, py + 1, C_LINE);
            prev = py;
        }
        Item icon = menu.chamber.getHistoryIcon();
        if (icon != null) {
            g.pose().pushPose(); g.pose().translate(gx + GRAPH_W - 10, gy + 1, 0); g.pose().scale(0.5f, 0.5f, 1f);
            g.renderItem(new ItemStack(icon), 0, 0);
            g.pose().popPose();
        }
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + ENERGY_Y, BAR_W, BAR_H, menu.energy(), menu.energyCap());
        Bars.heat(g, font, mx, my, leftPos + BAR_X, topPos + HEAT_Y, BAR_W, BAR_H, menu.heat(), EvolutionChamberBlockEntity.MAX_HEAT);
        if (in(mx, my, BAR_X, PROG_Y, BAR_W, BAR_H))
            g.renderTooltip(font, Component.literal(tr("progress", String.format(Locale.ROOT, "%.1f", menu.ticksPerGeneration() / 20f))), mx, my);
        if (in(mx, my, GRAPH_X, GRAPH_Y, GRAPH_W, GRAPH_H))
            g.renderComponentTooltip(font, List.of(Component.literal(tr("graph")), Component.literal(tr("graph_heat"))), mx, my);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 2)
            g.renderTooltip(font, Component.literal(tr(hoveredSlot.index == 0 ? "slot_strain" : "slot_feed")), mx, my);
    }

    private boolean in(int mx, int my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w && my >= topPos + ry && my < topPos + ry + h;
    }

    /** Обрезать строку под ширину {@code px} при масштабе текста 0.9. */
    private String clip(String s, int px) {
        int w = Math.round(px / 0.9f);
        return font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("…")) + "…";
    }

    private void text(GuiGraphics g, String s, int x, int y, int col) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(0.9f, 0.9f, 1f);
        g.drawString(font, s, 0, 0, col, false);
        g.pose().popPose();
    }

    private void right(GuiGraphics g, String s, int x, int y, int col) {
        text(g, s, x - Math.round(font.width(s) * 0.9f), y, col);
    }

    private void small(GuiGraphics g, String s, int x, int y, int col) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(0.7f, 0.7f, 1f);
        g.drawString(font, s, 0, 0, col, false);
        g.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.industrial_core.evolution_chamber." + key, args).getString();
    }
}
