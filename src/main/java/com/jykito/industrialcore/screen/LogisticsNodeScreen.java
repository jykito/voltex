package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.logistics.HubFaceMode;
import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.client.LogisticsHighlightRenderer;
import com.jykito.industrialcore.menu.LogisticsNodeMenu;
import com.jykito.industrialcore.networking.ModMessages;
import com.jykito.industrialcore.networking.NodeReorderPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Окно узла логистики: слева связи списком (порядок = приоритет, перетаскивание за ручку), справа карта сверху вокруг
 * узла (1 пиксель = 1 блок, север вверху, дальние связи прижаты к краю), внизу подробности выбранной связи и подсветка
 * её блока в мире. Текстура logistics_node_gui из tools/gui_gen/build_hub_guis.ps1, в ней только неподвижное.
 */
@OnlyIn(Dist.CLIENT)
public class LogisticsNodeScreen extends AbstractContainerScreen<LogisticsNodeMenu> {

    private static final ResourceLocation TEX = new ResourceLocation(IndustrialCore.MODID, "textures/gui/logistics_node_gui.png");
    private static final int TEX_W = 256, TEX_H = 315;

    private static final int LIST_X = 15, LIST_Y = 28, ROW_W = 124, ROW_H = 19, ROW_STEP = 20, VISIBLE_ROWS = 6;
    private static final int HANDLE_X = LIST_X + ROW_W - 11, NAME_W = 76;
    private static final int MAP_X = 144, MAP_Y = 28, MAP_W = 97, MAP_H = 121, MAP_CX = MAP_X + 48, MAP_CY = MAP_Y + 60;
    private static final int MAP_RX = 44, MAP_RZ = 56;
    private static final int DET_X = 15, DET_Y = 151, DET_W = 226, BTN_W = 68, BTN_H = 15;
    private static final long HIGHLIGHT_MS = 5000L;

    private static final int COL_TEXT = 0xFFE8E8E8, COL_SUB = 0xFF9A9A9A, COL_DIM = 0xFF7A7A7A, COL_ERR = 0xFFFF7A6B;
    private static final int COL_ITEM = 0xFFFF7F1E, COL_FLUID = 0xFF2F6BFF, COL_NODE = 0xFF33D6F0, COL_BAD = 0xFFFF5555;
    private static final int COL_EDGE = 0xFF0B0B0B;

    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private boolean dragging = false;
    private int dragFromIndex = -1;
    private int dragCurrentY = 0;

    public LogisticsNodeScreen(LogisticsNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 196;
    }

    private LogisticsNodeBlockEntity getNode() {
        if (minecraft == null || minecraft.level == null) return null;
        BlockEntity be = minecraft.level.getBlockEntity(menu.nodePos);
        return be instanceof LogisticsNodeBlockEntity n ? n : null;
    }

    /** Что стоит на конце связи: цвет метки, иконка, имя, пояснение. */
    private record Link(int color, ItemStack icon, Component name, Component detail, boolean error) {}

    private Link link(BlockPos p) {
        BlockEntity be = minecraft == null || minecraft.level == null ? null : minecraft.level.getBlockEntity(p);
        if (be instanceof HubBlockEntity h)
            return new Link(h.fluid ? COL_FLUID : COL_ITEM, new ItemStack((h.fluid ? ModBlocks.FLUID_HUB : ModBlocks.ITEM_HUB).get()),
                    Component.translatable(h.fluid ? "block.industrial_core.fluid_hub" : "block.industrial_core.item_hub"),
                    Component.translatable("screen.industrial_core.node.faces", activeFaces(h)), false);
        if (be instanceof LogisticsNodeBlockEntity)
            return new Link(COL_NODE, new ItemStack(ModBlocks.LOGISTICS_NODE.get()), Component.translatable("block.industrial_core.logistics_node"),
                    Component.translatable("screen.industrial_core.node.relay"), false);
        Component n = Component.translatable(be != null ? "screen.industrial_core.node.unknown" : "screen.industrial_core.node.unloaded");
        return new Link(COL_BAD, ItemStack.EMPTY, n, Component.empty(), true);
    }

    private static int activeFaces(HubBlockEntity h) {
        int n = 0;
        for (Direction d : Direction.values()) if (h.getMode(d) != HubFaceMode.OFF) n++;
        return n;
    }

    private int distance(BlockPos p) { return (int) Math.round(Math.sqrt(p.distSqr(menu.nodePos))); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        LogisticsNodeBlockEntity node = getNode();
        if (node == null || dragging) return;
        int i = markerAt(node.linkedPositions, mx, my);
        if (i >= 0) {
            BlockPos p = node.linkedPositions.get(i);
            Link l = link(p);
            g.renderComponentTooltip(font, List.of(Component.literal("#" + (i + 1) + " ").append(l.name()),
                    Component.translatable("screen.industrial_core.node.distance", distance(p)).withStyle(s -> s.withColor(COL_SUB & 0xFFFFFF))), mx, my);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        g.drawString(font, title, x + 23, y + 13, 0xFFE0E0E0, false);

        LogisticsNodeBlockEntity node = getNode();
        if (node == null) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.not_found"), x + LIST_X + 4, y + LIST_Y + 6, COL_ERR, false);
            return;
        }
        List<BlockPos> links = node.linkedPositions;
        Component count = Component.translatable("screen.industrial_core.node.links_count", links.size());
        g.drawString(font, count, x + 226 - font.width(count), y + 13, COL_SUB, false);
        g.fill(x + 229, y + 14, x + 234, y + 19, COL_EDGE);   // в сети, если есть связи
        g.fill(x + 230, y + 15, x + 233, y + 18, links.isEmpty() ? COL_BAD : 0xFF44FF44);
        if (selectedIndex >= links.size()) selectedIndex = -1;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= links.size()) break;
            if (dragging && idx == dragFromIndex) continue;
            renderRow(g, y + LIST_Y + i * ROW_STEP, links.get(idx), idx, idx == selectedIndex, i % 2 == 1);
        }
        if (links.isEmpty()) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.no_links"), x + LIST_X + 4, y + LIST_Y + 6, COL_SUB, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.use_linker"), x + LIST_X + 4, y + LIST_Y + 18, COL_DIM, false);
        }
        renderMap(g, links);
        renderDetails(g, node, links);
        if (dragging && dragFromIndex >= 0 && dragFromIndex < links.size())
            renderRow(g, dragCurrentY - ROW_H / 2, links.get(dragFromIndex), dragFromIndex, true, false);
    }

    private void renderRow(GuiGraphics g, int ry, BlockPos p, int idx, boolean sel, boolean odd) {
        int x = leftPos + LIST_X;
        Link l = link(p);
        g.fill(x, ry, x + ROW_W, ry + ROW_H, sel ? 0xFFE8E8E8 : COL_EDGE);
        g.fill(x + 1, ry + 1, x + ROW_W - 1, ry + ROW_H - 1, sel ? 0xFF2C2C2C : odd ? 0xFF202020 : 0xFF232323);
        g.fill(x + 1, ry + 1, x + 3, ry + ROW_H - 1, l.color());
        g.drawString(font, "#" + (idx + 1), x + 5, ry + 6, COL_DIM, false);
        if (!l.icon().isEmpty()) g.renderItem(l.icon(), x + 15, ry + 1);
        g.drawString(font, truncate(l.name().getString(), NAME_W), x + 34, ry + 2, l.error() ? COL_ERR : COL_TEXT, false);
        g.drawString(font, Component.translatable("screen.industrial_core.node.distance", distance(p)), x + 34, ry + 10, COL_DIM, false);
        for (int k = 0; k < 3; k++) g.fill(leftPos + HANDLE_X, ry + 6 + k * 3, leftPos + HANDLE_X + 7, ry + 7 + k * 3, 0xFF666666);
    }

    /** Точка метки связи на карте; дальние прижаты к краю по направлению. */
    private int[] markerPos(BlockPos p) {
        int dx = p.getX() - menu.nodePos.getX(), dz = p.getZ() - menu.nodePos.getZ();
        boolean far = Math.abs(dx) > MAP_RX || Math.abs(dz) > MAP_RZ;
        if (far) {
            double k = Math.min(dx == 0 ? Double.MAX_VALUE : (double) MAP_RX / Math.abs(dx), dz == 0 ? Double.MAX_VALUE : (double) MAP_RZ / Math.abs(dz));
            dx = (int) Math.round(dx * k);
            dz = (int) Math.round(dz * k);
        }
        return new int[]{leftPos + MAP_CX + dx, topPos + MAP_CY + dz, far ? 1 : 0};
    }

    private void renderMap(GuiGraphics g, List<BlockPos> links) {
        int x = leftPos, y = topPos;
        g.drawString(font, Component.translatable("screen.industrial_core.node.north"), x + MAP_CX - 2, y + MAP_Y + 3, 0xFF5A6066, false);
        g.drawString(font, Component.translatable("screen.industrial_core.node.distance", 16), x + MAP_X + 3, y + MAP_Y + MAP_H - 11, 0xFF5A6066, false);
        g.fill(x + MAP_CX - 2, y + MAP_CY - 2, x + MAP_CX + 3, y + MAP_CY + 3, COL_EDGE);
        g.fill(x + MAP_CX - 1, y + MAP_CY - 1, x + MAP_CX + 2, y + MAP_CY + 2, COL_NODE);
        for (int i = 0; i < links.size(); i++) {
            int[] m = markerPos(links.get(i));
            if (i == selectedIndex) g.fill(m[0] - 3, m[1] - 3, m[0] + 4, m[1] + 4, 0xFFE8E8E8);
            g.fill(m[0] - 2, m[1] - 2, m[0] + 3, m[1] + 3, COL_EDGE);
            g.fill(m[0] - 1, m[1] - 1, m[0] + 2, m[1] + 2, link(links.get(i)).color());
            String n = String.valueOf(i + 1);
            int lx = m[0] + 4 + font.width(n) > x + MAP_X + MAP_W - 2 ? m[0] - 3 - font.width(n) : m[0] + 4;   // номер не вылезает за карту
            g.drawString(font, n, lx, m[1] - 9, m[2] == 1 ? COL_ERR : 0xFFCFCFCF, true);
        }
    }

    private void renderDetails(GuiGraphics g, LogisticsNodeBlockEntity node, List<BlockPos> links) {
        int x = leftPos + DET_X, y = topPos + DET_Y;
        if (selectedIndex < 0) {   // ничего не выбрано — сводка по связям
            int hubs = 0, errors = 0;
            for (BlockPos p : links) { Link l = link(p); if (l.error()) errors++; else if (l.color() != COL_NODE) hubs++; }
            g.drawString(font, Component.translatable("screen.industrial_core.node.hubs", hubs), x, y + 8, hubs > 0 ? 0xFF44FF44 : COL_SUB, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.errors", errors), x + 70, y + 8, errors > 0 ? COL_ERR : COL_SUB, false);
            return;
        }
        BlockPos p = links.get(selectedIndex);
        Link l = link(p);
        if (!l.icon().isEmpty()) g.renderItem(l.icon(), x, y + 4);
        g.drawString(font, truncate("#" + (selectedIndex + 1) + " " + l.name().getString(), DET_W - BTN_W - 24), x + 20, y + 4, l.error() ? COL_ERR : COL_TEXT, false);
        g.drawString(font, "X " + p.getX() + "  Y " + p.getY() + "  Z " + p.getZ(), x + 20, y + 14, COL_SUB, false);
        Component d = Component.translatable("screen.industrial_core.node.distance", distance(p));
        if (!l.detail().getString().isEmpty()) d = d.copy().append("  ·  ").append(l.detail());
        g.drawString(font, d, x + 20, y + 24, COL_SUB, false);

        boolean on = p.equals(LogisticsHighlightRenderer.highlightPos) && System.currentTimeMillis() < LogisticsHighlightRenderer.highlightEndMs;
        int bx = x + DET_W - BTN_W, by = y + 6;
        g.fill(bx, by, bx + BTN_W, by + BTN_H, COL_EDGE);
        g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, on ? 0xFF2E7C88 : 0xFF1D4F58);
        Component bl = Component.translatable("screen.industrial_core.node.highlight");
        g.drawString(font, bl, bx + (BTN_W - font.width(bl)) / 2, by + 4, 0xFFFFFFFF, true);
    }

    private int markerAt(List<BlockPos> links, int mx, int my) {
        for (int i = links.size() - 1; i >= 0; i--) {
            int[] m = markerPos(links.get(i));
            if (Math.abs(mx - m[0]) <= 3 && Math.abs(my - m[1]) <= 3) return i;
        }
        return -1;
    }

    private String truncate(String s, int maxPx) {
        if (font.width(s) <= maxPx) return s;
        while (s.length() > 1 && font.width(s + "...") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        LogisticsNodeBlockEntity node = getNode();
        if (node == null || button != 0) return super.mouseClicked(mx, my, button);
        List<BlockPos> links = node.linkedPositions;

        // подсветка выбранной связи в мире на 5 с, повторный клик гасит
        if (selectedIndex >= 0 && selectedIndex < links.size()
                && hit(mx, my, leftPos + DET_X + DET_W - BTN_W, topPos + DET_Y + 6, BTN_W, BTN_H)) {
            BlockPos target = links.get(selectedIndex);
            boolean active = target.equals(LogisticsHighlightRenderer.highlightPos) && System.currentTimeMillis() < LogisticsHighlightRenderer.highlightEndMs;
            if (active) LogisticsHighlightRenderer.highlightPos = null;
            else {
                LogisticsHighlightRenderer.highlightPos = target;
                LogisticsHighlightRenderer.highlightEndMs = System.currentTimeMillis() + HIGHLIGHT_MS;
            }
            return true;
        }
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= links.size()) break;
            int ry = topPos + LIST_Y + i * ROW_STEP;
            if (!hit(mx, my, leftPos + LIST_X, ry, ROW_W, ROW_H)) continue;
            if (mx >= leftPos + HANDLE_X - 2) {   // ручка — перетащить (порядок = приоритет)
                dragging = true;
                dragFromIndex = idx;
                dragCurrentY = (int) my;
            } else selectedIndex = selectedIndex == idx ? -1 : idx;
            return true;
        }
        int m = markerAt(links, (int) mx, (int) my);
        if (m >= 0) {
            selectedIndex = m;
            if (m < scrollOffset) scrollOffset = m;
            else if (m >= scrollOffset + VISIBLE_ROWS) scrollOffset = m - VISIBLE_ROWS + 1;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { dragCurrentY = (int) my; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!dragging) return super.mouseReleased(mx, my, button);
        LogisticsNodeBlockEntity node = getNode();
        if (node != null && dragFromIndex >= 0) {
            int toIndex = dragFromIndex;
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int ry = topPos + LIST_Y + i * ROW_STEP;
                if (my >= ry && my < ry + ROW_STEP) { toIndex = i + scrollOffset; break; }
            }
            toIndex = Math.max(0, Math.min(node.linkedPositions.size() - 1, toIndex));
            if (toIndex != dragFromIndex) {
                ModMessages.sendToServer(new NodeReorderPacket(menu.nodePos, dragFromIndex, toIndex));
                if (selectedIndex == dragFromIndex) selectedIndex = toIndex;
            }
        }
        dragging = false;
        dragFromIndex = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        LogisticsNodeBlockEntity node = getNode();
        if (node != null) {
            int maxScroll = Math.max(0, node.linkedPositions.size() - VISIBLE_ROWS);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
