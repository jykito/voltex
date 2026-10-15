package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.DrillBeaconBlockEntity;
import com.jykito.industrialcore.menu.DrillBeaconMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Drill beacon window 256x228: display, scan progress and lens slot, depth layer (arrows at each bound, a rescan
 * button), the layer's top five ores with their shares.
 */
public class DrillBeaconScreen extends MachineScreen<DrillBeaconMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/drill_beacon_gui.png");

    private static final int WINDOW_H = 228;
    private static final int STATUS_X = 20, STATUS_Y = 16, BAR_X = 23, BAR_Y = 47, BAR_W = 190, BAR_H = 6;
    private static final int BAND_Y = 59, LIST_Y = 74, ROW = 11, PCT_X = 140, PCT_W = 64;
    /** Layer buttons: x of the left edge, width, id on the server. */
    private static final int[][] BTN = {{78, 9, 0}, {112, 9, 1}, {146, 9, 2}, {180, 9, 3}, {200, 41, 4}};

    private static final int C_OK = 0xFF6BE06B, C_BAD = 0xFFFF7A6B, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_GREEN = 0xFF05FF00;

    public DrillBeaconScreen(DrillBeaconMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        bars.draw(g, FillBar.PROGRESS, x + BAR_X, y + BAR_Y, menu.state() == DrillBeaconBlockEntity.State.READY ? BAR_W : menu.scaledScan(BAR_W), BAR_H);
        drawStatus(g, x, y);
        drawBand(g, x, y, mx, my);
        drawList(g, x, y);
    }

    private void drawStatus(GuiGraphics g, int x, int y) {
        var s = menu.state();
        String line1 = switch (s) {
            case NO_LENS -> tr("need_lens", lensName());
            case SCANNING -> tr("scanning", menu.total() == 0 ? 0 : 100 * menu.step() / menu.total());
            case WAITING -> tr("waiting");
            case READY -> tr("ready", IndustrialMinerScreen.num((int) Math.min(Integer.MAX_VALUE, menu.beacon.getClientTotal())));
        };
        text(g, line1, x + STATUS_X, y + STATUS_Y, s == DrillBeaconBlockEntity.State.READY ? C_OK : s == DrillBeaconBlockEntity.State.NO_LENS ? C_BAD : C_VALUE);
        text(g, tr("area"), x + STATUS_X, y + STATUS_Y + 10, C_LABEL);
    }

    private void drawBand(GuiGraphics g, int x, int y, int mx, int my) {
        text(g, tr("band"), x + 18, y + BAND_Y + 1, C_LABEL);
        text(g, String.valueOf(menu.yFrom()), x + 90, y + BAND_Y + 1, C_VALUE);
        text(g, tr("band_to"), x + 124, y + BAND_Y + 1, C_LABEL);
        text(g, String.valueOf(menu.yTo()), x + 158, y + BAND_Y + 1, C_VALUE);
        for (int[] b : BTN) {
            boolean hover = in(mx, my, b[0], BAND_Y, b[1], 10);
            g.fill(x + b[0], y + BAND_Y, x + b[0] + b[1], y + BAND_Y + 10, 0xFF1B1B1B);
            g.fill(x + b[0] + 1, y + BAND_Y + 1, x + b[0] + b[1] - 1, y + BAND_Y + 9, hover ? 0xFF4A4A4A : 0xFF2E2E2E);
            // glyph width without the trailing space (font.width includes it); "+" and "−" are 5x5, unscaled, exactly centred in the button
            String label = b[2] == 4 ? tr("rescan") : b[2] % 2 == 0 ? "−" : "+";
            int ink = font.width(label) - 1;
            if (b[2] == 4) small(g, label, x + b[0] + (b[1] - (int) (ink * 0.7f)) / 2, y + BAND_Y + 2, hover ? 0xFFFFFFFF : C_VALUE);
            else g.drawString(font, label, x + b[0] + (b[1] - ink) / 2, y + BAND_Y + 1, hover ? 0xFFFFFFFF : C_VALUE, false);
        }
    }

    private void drawList(GuiGraphics g, int x, int y) {
        List<Item> items = menu.beacon.getClientItems();
        List<Long> counts = menu.beacon.getClientCounts();
        long total = menu.beacon.getClientTotal();
        if (menu.state() != DrillBeaconBlockEntity.State.READY) return;
        if (items.isEmpty()) { text(g, tr("empty"), x + 18, y + LIST_Y, C_BAD); return; }
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            int ry = y + LIST_Y + i * ROW;
            ItemStack st = new ItemStack(items.get(i));
            g.pose().pushPose(); g.pose().translate(x + 18, ry, 0); g.pose().scale(0.5f, 0.5f, 1f);
            g.renderItem(st, 0, 0);
            g.pose().popPose();
            text(g, font.plainSubstrByWidth(st.getHoverName().getString(), 110), x + 30, ry, C_VALUE);
            double share = total > 0 ? (double) counts.get(i) / total : 0;
            g.fill(x + PCT_X, ry + 1, x + PCT_X + PCT_W, ry + 7, 0xFF1B1B1B);
            g.fill(x + PCT_X + 1, ry + 2, x + PCT_X + 1 + (int) Math.round((PCT_W - 2) * share), ry + 6, C_GREEN);
            text(g, String.format(java.util.Locale.ROOT, "%.1f%%", share * 100), x + PCT_X + PCT_W + 4, ry, C_LABEL);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) for (int[] b : BTN)
            if (in((int) mx, (int) my, b[0], BAND_Y, b[1], 10)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b[2]);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        if (in(mx, my, 78, BAND_Y, 112, 10)) g.renderTooltip(font, font.split(Component.literal(tr("band_hint")), 200), mx, my);
        else if (menu.state() == DrillBeaconBlockEntity.State.NO_LENS && in(mx, my, 15, 11, 226, 28))
            g.renderTooltip(font, font.split(Component.literal(tr("lens_hint")), 220), mx, my);
    }

    /** Which lens the beacon's dimension needs. */
    private String lensName() {
        var dim = minecraft.level == null ? Level.OVERWORLD : minecraft.level.dimension();
        String key = dim == Level.NETHER ? "miner_lens_2" : dim == Level.END ? "miner_lens_3" : dim == Level.OVERWORLD ? "miner_lens_1" : "miner_lens_4";
        return Component.translatable("item.industrial_core." + key).getString();
    }

    private boolean in(int mx, int my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w && my >= topPos + ry && my < topPos + ry + h;
    }

    private void text(GuiGraphics g, String s, int x, int y, int col) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(0.9f, 0.9f, 1f);
        g.drawString(font, s, 0, 0, col, false);
        g.pose().popPose();
    }

    private void small(GuiGraphics g, String s, int x, int y, int col) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(0.7f, 0.7f, 1f);
        g.drawString(font, s, 0, 0, col, false);
        g.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.industrial_core.drill_beacon." + key, args).getString();
    }
}
