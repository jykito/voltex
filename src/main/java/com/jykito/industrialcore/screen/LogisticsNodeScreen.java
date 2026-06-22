package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.logistics.FluidConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.ItemConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.LogisticsConnectorMode;
import com.jykito.industrialcore.block.entity.FluidConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.ItemConnectorBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LogisticsNodeScreen extends AbstractContainerScreen<LogisticsNodeMenu> {

    private static final ResourceLocation TEXTURE_BG    = new ResourceLocation(IndustrialCore.MODID, "textures/gui/logistics_node_bg.png");
    private static final ResourceLocation TEXTURE_PANEL = new ResourceLocation(IndustrialCore.MODID, "textures/gui/logistics_node_panel.png");
    private static final ResourceLocation TEXTURE_ROWS  = new ResourceLocation(IndustrialCore.MODID, "textures/gui/logistics_node_rows.png");

    private static final int GUI_W        = 200;
    private static final int GUI_H        = 180;
    private static final int PANEL_W      = 118;
    private static final int PANEL_H      = 180;
    private static final int ROW_H        = 20;
    private static final int VISIBLE_ROWS = 6;

    private static final int TITLE_X  = 5;
    private static final int TITLE_Y  = 5;
    private static final int TITLE_W  = 190;
    private static final int TITLE_H  = 10;
    private static final int LIST_X   = 3;
    private static final int LIST_Y   = 18;
    private static final int LIST_W   = 194;
    private static final int STATS_X  = 5;
    private static final int STATS_Y  = 158;
    private static final int STATS_W  = 190;
    private static final int STATS_H  = 17;

    private static final int ROW_U_NORMAL_A = 0;
    private static final int ROW_U_NORMAL_B = 194;
    private static final int ROW_U_SELECTED = 388;
    private static final int ROW_U_DRAG     = 582;
    private static final int ROWS_TEX_W     = 776;

    private static final int HIGHLIGHT_BTN_X = 96;
    private static final int HIGHLIGHT_BTN_Y = 18;
    private static final int HIGHLIGHT_BTN_W = 12;
    private static final int HIGHLIGHT_BTN_H = 12;
    private static final long HIGHLIGHT_MS   = 5000L;

    private static final int COL_TEXT    = 0xFFDDDDDD;
    private static final int COL_SUBTEXT = 0xFF888888;
    private static final int COL_PUSH    = 0xFF44CC44;
    private static final int COL_PULL    = 0xFF44AAFF;
    private static final int COL_ACTIVE  = 0xFF44FF44;
    private static final int COL_IDLE    = 0xFF777777;
    private static final int COL_ERROR   = 0xFFFF5555;
    private static final int COL_HANDLE  = 0xFF666666;

    private int scrollOffset = 0;
    private int selectedIndex = -1;

    private boolean dragging    = false;
    private int dragFromIndex   = -1;
    private int dragCurrentY    = 0;

    private boolean panelOnRight = true;

    private static final java.util.Map<BlockPos, Boolean> PANEL_SIDE_MEMORY = new java.util.HashMap<>();
    private static final String SIDES_FILE = "config/ic3_panel_sides.json";
    private static boolean sidesLoaded = false;

    public LogisticsNodeScreen(LogisticsNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        loadPanelSides();
        panelOnRight = PANEL_SIDE_MEMORY.getOrDefault(menu.nodePos, true);
    }

    private static void loadPanelSides() {
        if (sidesLoaded) return;
        sidesLoaded = true;
        try {
            java.io.File f = new java.io.File(
                net.minecraft.client.Minecraft.getInstance().gameDirectory, SIDES_FILE);
            if (!f.exists()) return;
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(
                new java.io.FileReader(f), com.google.gson.JsonObject.class);
            if (json == null) return;
            for (var entry : json.entrySet()) {
                String[] p = entry.getKey().split(",");
                PANEL_SIDE_MEMORY.put(
                    new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])),
                    entry.getValue().getAsBoolean()
                );
            }
        } catch (Exception ignored) {}
    }

    private static void savePanelSides() {
        try {
            java.io.File f = new java.io.File(
                net.minecraft.client.Minecraft.getInstance().gameDirectory, SIDES_FILE);
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            for (var entry : PANEL_SIDE_MEMORY.entrySet()) {
                BlockPos p = entry.getKey();
                json.addProperty(p.getX() + "," + p.getY() + "," + p.getZ(), entry.getValue());
            }
            try (var w = new java.io.FileWriter(f)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, w);
            }
        } catch (Exception ignored) {}
    }

    private LogisticsNodeBlockEntity getNode() {
        if (minecraft == null || minecraft.level == null) return null;
        BlockEntity be = minecraft.level.getBlockEntity(menu.nodePos);
        return be instanceof LogisticsNodeBlockEntity n ? n : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);

        if (selectedIndex >= 0) {
            LogisticsNodeBlockEntity node = getNode();
            if (node != null && selectedIndex < node.linkedPositions.size()) {
                renderSidePanel(g, getPanelX(), topPos, node.linkedPositions.get(selectedIndex));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;

        g.blit(TEXTURE_BG, x, y, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);

        LogisticsNodeBlockEntity node = getNode();
        if (node == null) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.not_found").getString(), x + LIST_X + 4, y + LIST_Y + 6, COL_ERROR, false);
            return;
        }

        List<BlockPos> links = node.linkedPositions;
        int total = links.size();

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= total) break;
            if (dragging && idx == dragFromIndex) continue;

            int ry = y + LIST_Y + i * ROW_H;
            int uRow;
            if (idx == selectedIndex)    uRow = ROW_U_SELECTED;
            else if (i % 2 == 0)         uRow = ROW_U_NORMAL_A;
            else                         uRow = ROW_U_NORMAL_B;
            g.blit(TEXTURE_ROWS, x + LIST_X, ry, uRow, 0, LIST_W, ROW_H, ROWS_TEX_W, ROW_H);
            renderRowContents(g, x, ry, links.get(idx), idx);
        }

        if (total == 0) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.no_links").getString(),   x + LIST_X + 4, y + LIST_Y + 6,  COL_SUBTEXT, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.use_linker").getString(), x + LIST_X + 4, y + LIST_Y + 18, COL_SUBTEXT, false);
        }

        if (dragging && dragFromIndex >= 0 && dragFromIndex < total) {
            int ry = dragCurrentY - ROW_H / 2;
            g.blit(TEXTURE_ROWS, x + LIST_X, ry, ROW_U_DRAG, 0, LIST_W, ROW_H, ROWS_TEX_W, ROW_H);
            renderRowContents(g, x, ry, links.get(dragFromIndex), dragFromIndex);
        }

        renderStats(g, x, y, node);
    }

    private void renderRowContents(GuiGraphics g, int x, int ry, BlockPos linkPos, int idx) {
        if (minecraft == null || minecraft.level == null) return;
        BlockEntity be = minecraft.level.getBlockEntity(linkPos);
        BlockState linkState = minecraft.level.getBlockState(linkPos);

        ItemStack icon = ItemStack.EMPTY;
        String name;
        String modeName;
        int modeColor;
        int dotColor;

        if (be instanceof ItemConnectorBlockEntity c) {
            icon = attachedBlockIcon(linkPos, linkState.getValue(ItemConnectorBlock.FACING));
            boolean push = c.mode == LogisticsConnectorMode.PUSH;
            name = "Item Connector";
            modeName = push ? "PUSH" : "PULL";
            modeColor = push ? COL_PUSH : COL_PULL;
            dotColor  = c.transferState == 1 ? COL_ACTIVE : COL_IDLE;
        } else if (be instanceof FluidConnectorBlockEntity c) {
            icon = attachedBlockIcon(linkPos, linkState.getValue(FluidConnectorBlock.FACING));
            boolean push = c.mode == LogisticsConnectorMode.PUSH;
            name = "Fluid Connector";
            modeName = push ? "PUSH" : "PULL";
            modeColor = push ? COL_PUSH : COL_PULL;
            dotColor  = c.transferState == 1 ? COL_ACTIVE : COL_IDLE;
        } else if (be instanceof LogisticsNodeBlockEntity) {
            icon = new ItemStack(ModBlocks.LOGISTICS_NODE.get());
            name = "Logistics Node";
            modeName = "NODE";
            modeColor = 0xFFFFAA00;
            dotColor  = COL_ACTIVE;
        } else {
            name = be != null ? "Unknown" : "Unloaded";
            modeName = "?";
            modeColor = COL_SUBTEXT;
            dotColor  = COL_ERROR;
        }

        if (!icon.isEmpty())
            g.renderItem(icon, x + LIST_X + 1, ry + 2);

        g.drawString(font, "#" + (idx + 1),         x + LIST_X + 19,  ry + 6, COL_SUBTEXT, false);
        g.drawString(font, truncate(name, 84),       x + LIST_X + 33,  ry + 6, COL_TEXT,    false);
        g.drawString(font, modeName,                 x + LIST_X + 122, ry + 6, modeColor,   false);
        g.fill(x + LIST_X + 170, ry + 7, x + LIST_X + 175, ry + 12, dotColor);

        int hx = x + LIST_X + LIST_W - 11;
        for (int li = 0; li < 3; li++)
            g.fill(hx, ry + 4 + li * 4, hx + 8, ry + 6 + li * 4, COL_HANDLE);
    }

    private ItemStack attachedBlockIcon(BlockPos connectorPos, Direction facing) {
        BlockPos targetPos = connectorPos.relative(facing);
        BlockState target = minecraft.level.getBlockState(targetPos);
        if (target.isAir()) return ItemStack.EMPTY;
        return new ItemStack(target.getBlock());
    }

    private void renderStats(GuiGraphics g, int x, int y, LogisticsNodeBlockEntity node) {
        List<BlockPos> links = node.linkedPositions;
        int total = links.size();
        int active = 0, errors = 0;

        if (minecraft != null && minecraft.level != null) {
            for (BlockPos lp : links) {
                BlockEntity be = minecraft.level.getBlockEntity(lp);
                if (be == null) { errors++; continue; }
                if (be instanceof ItemConnectorBlockEntity c) {
                    if (c.transferState == 1) active++;
                } else if (be instanceof FluidConnectorBlockEntity c) {
                    if (c.transferState == 1) active++;
                }
            }
        }

        int sy = y + STATS_Y + (STATS_H - 9) / 2;
        g.drawString(font, Component.translatable("screen.industrial_core.node.connected", total).getString(), x + STATS_X,       sy, COL_TEXT,                               false);
        g.drawString(font, Component.translatable("screen.industrial_core.node.active_count", active).getString(), x + STATS_X + 88,  sy, active > 0 ? COL_ACTIVE : COL_IDLE,   false);
        g.drawString(font, Component.translatable("screen.industrial_core.node.errors", errors).getString(),   x + STATS_X + 136, sy, errors > 0 ? COL_ERROR : COL_SUBTEXT, false);
    }

    private int getPanelX() {
        return panelOnRight ? leftPos + GUI_W + 2 : leftPos - PANEL_W - 2;
    }

    private void renderSidePanel(GuiGraphics g, int px, int py, BlockPos linkPos) {
        g.blit(TEXTURE_PANEL, px, py, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        if (LogisticsHighlightRenderer.highlightPos != null
                && LogisticsHighlightRenderer.highlightPos.equals(linkPos)
                && System.currentTimeMillis() < LogisticsHighlightRenderer.highlightEndMs) {
            g.fill(px + HIGHLIGHT_BTN_X,     py + HIGHLIGHT_BTN_Y,
                   px + HIGHLIGHT_BTN_X + HIGHLIGHT_BTN_W, py + HIGHLIGHT_BTN_Y + HIGHLIGHT_BTN_H,
                   0x7744FF44);
        }

        g.drawCenteredString(font, Component.translatable("screen.industrial_core.node.details").getString(), px + PANEL_W / 2, py + 5, COL_TEXT);

        int row = py + 15 + 4;
        g.drawString(font, "X: " + linkPos.getX(), px + 4, row,      COL_SUBTEXT, false);
        g.drawString(font, "Y: " + linkPos.getY(), px + 4, row + 10, COL_SUBTEXT, false);
        g.drawString(font, "Z: " + linkPos.getZ(), px + 4, row + 20, COL_SUBTEXT, false);

        if (minecraft == null || minecraft.level == null) return;
        BlockEntity be = minecraft.level.getBlockEntity(linkPos);

        int r2 = row + 36;
        if (be instanceof ItemConnectorBlockEntity c) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.type_items").getString(), px + 4, r2, COL_TEXT, false);
            boolean push = c.mode == LogisticsConnectorMode.PUSH;
            g.drawString(font, Component.translatable("screen.industrial_core.node.mode").getString(),  px + 4,  r2 + 12, COL_SUBTEXT, false);
            g.drawString(font, push ? "PUSH" : "PULL", px + 46, r2 + 12, push ? COL_PUSH : COL_PULL, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.status").getString(), px + 4,  r2 + 24, COL_SUBTEXT, false);
            g.drawString(font, Component.translatable(c.transferState == 1 ? "screen.industrial_core.node.active_state" : "screen.industrial_core.node.waiting").getString(),
                    px + 46, r2 + 24, c.transferState == 1 ? COL_ACTIVE : COL_IDLE, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.filter", Component.translatable(c.filterWhitelist ? "screen.industrial_core.node.whitelist" : "screen.industrial_core.node.blacklist")).getString(),
                    px + 4, r2 + 36, COL_SUBTEXT, false);
        } else if (be instanceof FluidConnectorBlockEntity c) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.type_fluids").getString(), px + 4, r2, COL_TEXT, false);
            boolean push = c.mode == LogisticsConnectorMode.PUSH;
            g.drawString(font, Component.translatable("screen.industrial_core.node.mode").getString(),  px + 4,  r2 + 12, COL_SUBTEXT, false);
            g.drawString(font, push ? "PUSH" : "PULL", px + 46, r2 + 12, push ? COL_PUSH : COL_PULL, false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.status").getString(), px + 4,  r2 + 24, COL_SUBTEXT, false);
            g.drawString(font, Component.translatable(c.transferState == 1 ? "screen.industrial_core.node.active_state" : "screen.industrial_core.node.waiting").getString(),
                    px + 46, r2 + 24, c.transferState == 1 ? COL_ACTIVE : COL_IDLE, false);
        } else if (be instanceof LogisticsNodeBlockEntity) {
            g.drawString(font, Component.translatable("screen.industrial_core.node.type_node").getString(),    px + 4, r2,      COL_TEXT,    false);
            g.drawString(font, Component.translatable("screen.industrial_core.node.relay").getString(), px + 4, r2 + 12, COL_SUBTEXT, false);
        } else {
            g.drawString(font, Component.translatable(be != null ? "screen.industrial_core.node.unknown" : "screen.industrial_core.node.unloaded").getString(), px + 4, r2, COL_ERROR, false);
        }
    }

    private String truncate(String s, int maxPx) {
        if (font.width(s) <= maxPx) return s;
        while (s.length() > 1 && font.width(s + "...") > maxPx)
            s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        int titleCenterX = TITLE_X + TITLE_W / 2;
        int titleY = TITLE_Y + (TITLE_H - 9) / 2;
        g.drawCenteredString(font, title, titleCenterX, titleY, COL_TEXT);

        LogisticsNodeBlockEntity node = getNode();
        boolean online = node != null && !node.linkedPositions.isEmpty();
        g.fill(TITLE_X + TITLE_W - 6, TITLE_Y + 2, TITLE_X + TITLE_W - 1, TITLE_Y + 7,
                online ? COL_ACTIVE : COL_ERROR);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (selectedIndex >= 0) {
            int px = getPanelX();

            if (mx >= px + HIGHLIGHT_BTN_X && mx < px + HIGHLIGHT_BTN_X + HIGHLIGHT_BTN_W
                    && my >= topPos + HIGHLIGHT_BTN_Y && my < topPos + HIGHLIGHT_BTN_Y + HIGHLIGHT_BTN_H) {
                LogisticsNodeBlockEntity node = getNode();
                if (node != null && selectedIndex < node.linkedPositions.size()) {
                    BlockPos target = node.linkedPositions.get(selectedIndex);
                    boolean active = target.equals(LogisticsHighlightRenderer.highlightPos)
                            && System.currentTimeMillis() < LogisticsHighlightRenderer.highlightEndMs;
                    if (active) {
                        LogisticsHighlightRenderer.highlightPos = null;
                    } else {
                        LogisticsHighlightRenderer.highlightPos   = target;
                        LogisticsHighlightRenderer.highlightEndMs = System.currentTimeMillis() + HIGHLIGHT_MS;
                    }
                }
                return true;
            }

            if (mx >= px && mx < px + PANEL_W && my >= topPos && my < topPos + 15) {
                panelOnRight = !panelOnRight;
                PANEL_SIDE_MEMORY.put(menu.nodePos, panelOnRight);
                savePanelSides();
                return true;
            }
        }

        LogisticsNodeBlockEntity node = getNode();
        if (node == null) return super.mouseClicked(mx, my, button);

        List<BlockPos> links = node.linkedPositions;
        int x = leftPos, y = topPos;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = i + scrollOffset;
            if (idx >= links.size()) break;

            int ry = y + LIST_Y + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;

            if (mx >= x + GUI_W - 14 && mx < x + GUI_W - 1) {
                dragging = true;
                dragFromIndex = idx;
                dragCurrentY  = (int) my;
                return true;
            }

            if (mx >= x + 1 && mx < x + GUI_W - 14) {
                selectedIndex = (selectedIndex == idx) ? -1 : idx;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            dragCurrentY = (int) my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            LogisticsNodeBlockEntity node = getNode();
            if (node != null && dragFromIndex >= 0) {
                int x = leftPos, y = topPos;
                int toIndex = dragFromIndex;
                for (int i = 0; i < VISIBLE_ROWS; i++) {
                    int ry = y + LIST_Y + i * ROW_H;
                    if (my >= ry && my < ry + ROW_H) {
                        toIndex = i + scrollOffset;
                        break;
                    }
                }
                int maxIdx = node.linkedPositions.size() - 1;
                toIndex = Math.max(0, Math.min(maxIdx, toIndex));
                if (toIndex != dragFromIndex)
                    ModMessages.sendToServer(new NodeReorderPacket(menu.nodePos, dragFromIndex, toIndex));
            }
            dragging = false;
            dragFromIndex = -1;
            return true;
        }
        return super.mouseReleased(mx, my, button);
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
