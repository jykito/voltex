package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.NaniteLabBlockEntity;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.menu.NaniteLabMenu;
import com.jykito.industrialcore.nanite.NaniteFeed;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Окно нанолаборатории 256×228: табло (статус и порог темы, тема и изученность, стабильность), слева учёный и образец,
 * справа пустая карта, полоса записи и записанная карта, внизу энергия.
 */
public class NaniteLabScreen extends MachineScreen<NaniteLabMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/nanite_lab_gui.png");

    private static final int WINDOW_H = 228;
    private static final int TEXT_X = 20, LINE1_Y = 16, LINE2_Y = 27, RIGHT_X = 236;
    private static final int PROG_X = 121, PROG_Y = 67, PROG_W = 68, BAR_H = 6, ENERGY_X = 23, ENERGY_Y = 107, ENERGY_W = 218;

    private static final int C_OK = 0xFF6BE06B, C_BAD = 0xFFFF7A6B, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ACCENT = 0xFF7FF4FF;

    private final FillBar chevrons = new FillBar(6);

    public NaniteLabScreen(NaniteLabMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        chevrons.advance();
        chevrons.draw(g, FillBar.PROGRESS, x + PROG_X, y + PROG_Y, menu.scaledProgress(PROG_W), BAR_H);
        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.scaledEnergy(ENERGY_W), BAR_H);
        drawBoard(g, x, y);
    }

    private void drawBoard(GuiGraphics g, int x, int y) {
        NaniteLabBlockEntity.Status s = menu.status();
        text(g, tr("status." + s.name().toLowerCase(Locale.ROOT)), x + TEXT_X, y + LINE1_Y, s == NaniteLabBlockEntity.Status.WORKING ? C_OK : C_BAD);
        ItemStack strain = menu.lab.getItems().getStackInSlot(NaniteLabBlockEntity.SLOT_STRAIN);
        ItemStack sample = menu.lab.getItems().getStackInSlot(NaniteLabBlockEntity.SLOT_SAMPLE);
        if (strain.isEmpty()) return;
        NaniteGenome genome = NaniteGenome.read(strain);
        String stab = tr("stability", NaniteStrainItem.percent(genome.stability()));
        right(g, stab, x + RIGHT_X, y + LINE2_Y, C_LABEL);
        int room = RIGHT_X - TEXT_X - Math.round(font.width(stab) * 0.9f) - 6;   // левый текст не заходит на стабильность
        String theme = sample.isEmpty() ? genome.dominant() : NaniteFeed.theme(sample.getItem());
        if (theme == null) return;
        text(g, clip(NaniteStrainItem.targetName(genome, theme, null).getString() + "  " + NaniteStrainItem.effectShort(genome, theme).getString(), room), x + TEXT_X, y + LINE2_Y, C_ACCENT);
        if (!sample.isEmpty())   // порог темы: зелёный, если учёный его прошёл
            right(g, tr("threshold", NaniteStrainItem.percent(menu.threshold())), x + RIGHT_X, y + LINE1_Y,
                    genome.bonus(theme) >= menu.threshold() ? C_OK : C_BAD);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, BAR_H, menu.energy(), menu.energyCap());
        if (in(mx, my, PROG_X, PROG_Y, PROG_W, BAR_H))
            g.renderTooltip(font, Component.literal(tr("progress", String.format(Locale.ROOT, "%.1f", menu.maxProgress() / 20f))), mx, my);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 4) {
            String key = switch (hoveredSlot.index) { case 0 -> "slot_strain"; case 1 -> "slot_sample"; case 2 -> "slot_blank"; default -> "slot_output"; };
            g.renderComponentTooltip(font, List.of(Component.literal(tr(key)), Component.literal(tr(key + "_hint"))), mx, my);
        }
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

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.industrial_core.nanite_lab." + key, args).getString();
    }
}
