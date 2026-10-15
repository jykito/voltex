package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.custom.logistics.HubFaceMode;
import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.menu.HubMenu;
import com.jykito.industrialcore.networking.HubFilterPacket;
import com.jykito.industrialcore.networking.HubFilterQtyPacket;
import com.jykito.industrialcore.networking.HubFluidFilterPacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.Optional;

/**
 * Hub window: all six sides as a list on the left (empty ones grey), the selected one's config on the right: mode, 3x3 filter
 * for input or output, toggles, upgrade slot. Textures hub_item_gui / hub_fluid_gui from tools/gui_gen/build_hub_guis.ps1
 * contain only the static parts; lines, buttons and toggles are drawn here.
 */
public class HubScreen extends AbstractContainerScreen<HubMenu> {

    private static final ResourceLocation TEX_ITEM  = new ResourceLocation(IndustrialCore.MODID, "textures/gui/hub_item_gui.png");
    private static final ResourceLocation TEX_FLUID = new ResourceLocation(IndustrialCore.MODID, "textures/gui/hub_fluid_gui.png");
    private static final int TEX_W = 256, TEX_H = 315;

    // list of sides
    private static final Direction[] ORDER = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN};
    private static final int LIST_X = 15, LIST_Y = 29, ROW_W = 92, ROW_H = 19, ROW_STEP = 20;
    // selected side panel
    private static final int PX = 114, NAME_W = 85;
    private static final int MODE_Y = 53, MODE_W = 40, MODE_H = 13, MODE_STEP = 42;
    private static final int TAB_Y = 70, TAB_W = 36, TAB_H = 13;
    public static final int FGRID_X = PX + 1, FGRID_Y = 88, FCELL = 18;
    private static final int TOGGLE_X = PX + 58, TOGGLE_Y = 87, TOGGLE_STEP = 18, TOGGLE_W = 66, TOGGLE_H = 15;
    private static final int HINT_Y = 143;

    private static final int COL_IN = 0xFF4F9DFF, COL_OUT = 0xFFFFAE3D, COL_OFF = 0xFF4A4A4A;
    private static final int COL_IDLE = 0xFF262626, COL_ON = 0xFF3A6B4A, COL_EDGE = 0xFF0B0B0B;

    public int getGuiLeft() { return leftPos; }
    public int getGuiTop()  { return topPos; }
    public int getSelectedFace() { return menu.selectedFace; }

    public HubScreen(HubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 252;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
        if (menu.selectedFace < 0 && menu.be != null)   // the first side with a neighbour opens right away
            for (Direction d : ORDER) if (connected(d)) { select(d.get3DDataValue()); break; }
    }

    private boolean connected(Direction d) {
        HubBlockEntity be = menu.be;
        return be != null && be.getLevel() != null && !be.getLevel().getBlockState(be.getBlockPos().relative(d)).isAir();
    }

    private BlockState neighbor(Direction d) { return menu.be.getLevel().getBlockState(menu.be.getBlockPos().relative(d)); }

    private int connectedCount() { int n = 0; for (Direction d : ORDER) if (connected(d)) n++; return n; }

    private void select(int face) {
        menu.selectedFace = face;
        HubFaceMode fm = menu.be.getMode(Direction.from3DDataValue(face));
        menu.filterDir = fm.hasInput() || !fm.hasOutput() ? 0 : 1;   // output only: the output filter right away
        click(200 + face);
    }

    private static Component tr(String key) { return Component.translatable("gui.industrial_core.hub." + key); }

    private static Component modeLabel(HubFaceMode m) {
        return switch (m) {
            case OFF -> tr("mode.off");
            case INPUT -> tr("mode.input");
            case OUTPUT -> tr("mode.output");
            case BOTH -> tr("mode.both");
        };
    }

    private static int modeColor(HubFaceMode m) {
        return switch (m) {
            case OFF -> 0xFF8A8A8A;
            case OUTPUT -> COL_OUT;
            default -> COL_IN;
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
        renderHints(g, mx, my);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (menu.be == null) return;
        int x = leftPos, y = topPos;
        g.blit(menu.be.fluid ? TEX_FLUID : TEX_ITEM, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        g.drawString(font, menu.be.getBlockState().getBlock().getName(), x + 23, y + 13, 0xFFE0E0E0, false);
        Component count = Component.translatable("gui.industrial_core.hub.faces_count", connectedCount());
        g.drawString(font, count, x + 233 - font.width(count), y + 13, 0xFF9A9A9A, false);
        renderList(g);
        if (menu.selectedFace >= 0) renderPanel(g);
        else g.drawString(font, tr("no_neighbors"), x + PX, y + 33, 0xFF9A9A9A, false);
    }

    private int sideColor() { return menu.be.fluid ? 0xFF79A8FF : 0xFFFFC24A; }

    private void renderList(GuiGraphics g) {
        int x = leftPos + LIST_X, y0 = topPos + LIST_Y;
        for (int i = 0; i < ORDER.length; i++) {
            Direction d = ORDER[i];
            int ry = y0 + i * ROW_STEP;
            boolean sel = d.get3DDataValue() == menu.selectedFace, con = connected(d);
            g.fill(x, ry, x + ROW_W, ry + ROW_H, sel ? 0xFFE8E8E8 : COL_EDGE);
            g.fill(x + 1, ry + 1, x + ROW_W - 1, ry + ROW_H - 1, sel ? 0xFF2C2C2C : (i % 2 == 1 ? 0xFF202020 : 0xFF232323));
            Component side = tr("side." + d.getName());
            if (con) {
                HubFaceMode m = menu.be.getMode(d);
                modeStrip(g, x + 1, ry + 1, ROW_H - 2, m);
                g.renderItem(new ItemStack(neighbor(d).getBlock()), x + 5, ry + 1);
                g.drawString(font, side, x + 24, ry + 2, sideColor(), false);
                g.drawString(font, modeLabel(m), x + 24, ry + 10, modeColor(m), false);
            } else {
                g.drawString(font, side, x + 24, ry + 2, 0xFF6A6A6A, false);
                g.drawString(font, tr("empty"), x + 24, ry + 10, 0xFF555555, false);
            }
        }
    }

    /** Mode strip at the left edge of the line: for "both" the input on top, the output below. */
    private void modeStrip(GuiGraphics g, int x, int y, int h, HubFaceMode m) {
        if (m == HubFaceMode.BOTH) {
            g.fill(x, y, x + 2, y + h / 2, COL_IN);
            g.fill(x, y + h / 2, x + 2, y + h, COL_OUT);
        } else g.fill(x, y, x + 2, y + h, m == HubFaceMode.OFF ? COL_OFF : modeColor(m));
    }

    private void renderPanel(GuiGraphics g) {
        int x = leftPos + PX, y = topPos, face = menu.selectedFace;
        HubBlockEntity be = menu.be;
        Direction d = Direction.from3DDataValue(face);
        BlockState ns = neighbor(d);
        g.renderItem(new ItemStack(ns.getBlock()), x, y + 30);
        g.drawString(font, tr("side." + d.getName()), x + 20, y + 30, sideColor(), false);
        String name = ns.getBlock().getName().getString();
        if (font.width(name) > NAME_W) name = font.plainSubstrByWidth(name, NAME_W - font.width("...")) + "...";
        g.drawString(font, name, x + 20, y + 40, 0xFFE8E8E8, false);

        // mode: plain sets one, Shift adds the second direction (both)
        HubFaceMode m = be.getMode(d);
        button(g, x, y + MODE_Y, MODE_W, MODE_H, m.hasInput() ? COL_IN : COL_IDLE, tr("mode.input"), m.hasInput());
        button(g, x + MODE_STEP, y + MODE_Y, MODE_W, MODE_H, m.hasOutput() ? COL_OUT : COL_IDLE, tr("mode.output"), m.hasOutput());
        button(g, x + 2 * MODE_STEP, y + MODE_Y, MODE_W, MODE_H, m == HubFaceMode.OFF ? COL_OFF : COL_IDLE, tr("mode.off"), m == HubFaceMode.OFF);

        // which filter is shown: input or output
        boolean out = menu.filterDir == 1;
        Component fl = tr("filter");
        g.drawString(font, fl, x, y + TAB_Y + 3, 0xFF9A9A9A, false);
        int tx = tabX();
        button(g, tx, y + TAB_Y, TAB_W, TAB_H, out ? COL_IDLE : COL_ON, tr("filter_in"), !out);
        button(g, tx + TAB_W + 2, y + TAB_Y, TAB_W, TAB_H, out ? COL_ON : COL_IDLE, tr("filter_out"), out);

        if (be.fluid) {
            FluidStack[] arr = be.fluidFilter(out);
            for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++) {
                FluidStack ff = arr[face * HubBlockEntity.FILTER_SLOTS + i];
                if (ff != null && !ff.isEmpty()) renderFluidIcon(g, ff, cellX(i), cellY(i));
            }
        } else {
            var fh = be.filter(out);
            for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++) {
                int idx = face * HubBlockEntity.FILTER_SLOTS + i;
                ItemStack f = fh.getStackInSlot(idx);
                if (f.isEmpty()) continue;
                g.renderItem(f, cellX(i), cellY(i));
                if (!out && be.filterCaps[idx] > 0)   // limits only on input
                    g.renderItemDecorations(font, f, cellX(i), cellY(i), String.valueOf(be.filterCaps[idx]));
            }
        }

        boolean[][] flags = {be.whitelist, be.matchNbt, be.roundRobin};
        String[][] labels = {{"whitelist", "blacklist"}, {"nbt_on", "nbt_off"}, {"round_robin", "sequential"}};
        for (int t = 0; t < 3; t++) {
            boolean on = flags[t][face];
            button(g, leftPos + TOGGLE_X, y + TOGGLE_Y + t * TOGGLE_STEP, TOGGLE_W, TOGGLE_H, on ? COL_ON : COL_IDLE, tr(labels[t][on ? 0 : 1]), on);
        }
        g.drawString(font, be.fluid ? tr("hint_fluid") : tr(out ? "hint_out" : "hint_in"), x, y + HINT_Y, 0xFF7A7A7A, false);
    }

    private int tabX() { return leftPos + PX + font.width(tr("filter")) + 5; }
    private int cellX(int i) { return leftPos + FGRID_X + (i % 3) * FCELL; }
    private int cellY(int i) { return topPos + FGRID_Y + (i / 3) * FCELL; }

    /** Button: 1 px outline and fill, no drawn lighting; the label is white with a shadow on the active one. */
    private void button(GuiGraphics g, int x, int y, int w, int h, int fill, Component label, boolean active) {
        g.fill(x, y, x + w, y + h, COL_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, active ? 0xFFFFFFFF : 0xFF9A9A9A, active);
    }

    private void renderHints(GuiGraphics g, int mx, int my) {
        if (menu.be == null) return;
        for (int i = 0; i < ORDER.length; i++) {
            Direction d = ORDER[i];
            if (connected(d) && hit(mx, my, leftPos + LIST_X, topPos + LIST_Y + i * ROW_STEP, ROW_W, ROW_H)) {
                g.renderComponentTooltip(font, List.of(neighbor(d).getBlock().getName(), modeLabel(menu.be.getMode(d)).copy().withStyle(s -> s.withColor(modeColor(menu.be.getMode(d)) & 0xFFFFFF))), mx, my);
                return;
            }
        }
        if (menu.selectedFace < 0) return;
        if (hit(mx, my, leftPos + PX, topPos + MODE_Y, MODE_STEP + MODE_W, MODE_H))
            g.renderTooltip(font, tr("shift_both"), mx, my);
        else if (hoveredSlot == null && hit(mx, my, leftPos + HubMenu.UP_X, topPos + HubMenu.UP_Y, 16, 16)
                && menu.be.upgradeHandler.getStackInSlot(menu.selectedFace).isEmpty())
            g.renderTooltip(font, tr("upgrade"), mx, my);
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int btn) {
        HubBlockEntity be = menu.be;
        if (be == null) return super.mouseClicked(mxD, myD, btn);
        int mx = (int) mxD, my = (int) myD;

        // list: LMB selects a side, RMB cycles the mode
        for (int i = 0; i < ORDER.length; i++) {
            Direction d = ORDER[i];
            if (!hit(mx, my, leftPos + LIST_X, topPos + LIST_Y + i * ROW_STEP, ROW_W, ROW_H)) continue;
            if (!connected(d)) return true;
            int f = d.get3DDataValue();
            if (btn == 1) click(f); else if (btn == 0) select(f);
            return true;
        }
        int f = menu.selectedFace;
        if (f < 0) return super.mouseClicked(mxD, myD, btn);

        // fluid hub filter: LMB takes the fluid from a bucket, RMB clears
        int fcell = filterCellAt(mx, my);
        if (be.fluid && fcell >= 0) {
            boolean out = menu.filterDir == 1;
            if (btn == 1) {
                ModMessages.sendToServer(new HubFluidFilterPacket(be.getBlockPos(), f, fcell, out, FluidStack.EMPTY));
            } else if (btn == 0) {
                FluidStack fl = extractFluidFromBucket(menu.getCarried());
                if (fl.isEmpty() && minecraft != null && minecraft.player != null)
                    fl = extractFluidFromBucket(minecraft.player.getInventory().getSelected());
                if (!fl.isEmpty()) ModMessages.sendToServer(new HubFluidFilterPacket(be.getBlockPos(), f, fcell, out, fl));
            }
            playClick();
            return true;
        }
        if (btn == 0) {
            boolean shift = Screen.hasShiftDown();
            int x = leftPos + PX, y = topPos;
            // mode: plain = one, shift = toggle this direction (-> both)
            if (hit(mx, my, x, y + MODE_Y, MODE_W, MODE_H)) {
                if (shift) click(400 + f * 2); else click(100 + f * 3 + 1);
                menu.filterDir = 0; return true;
            }
            if (hit(mx, my, x + MODE_STEP, y + MODE_Y, MODE_W, MODE_H)) {
                if (shift) click(400 + f * 2 + 1); else click(100 + f * 3 + 2);
                menu.filterDir = 1; return true;
            }
            if (hit(mx, my, x + 2 * MODE_STEP, y + MODE_Y, MODE_W, MODE_H)) { click(100 + f * 3); return true; }
            for (int t = 0; t < 3; t++)
                if (hit(mx, my, leftPos + TOGGLE_X, y + TOGGLE_Y + t * TOGGLE_STEP, TOGGLE_W, TOGGLE_H)) { click(300 + f * 3 + t); return true; }
            int tx = tabX();
            if (hit(mx, my, tx, y + TAB_Y, TAB_W, TAB_H)) { menu.filterDir = 0; playClick(); return true; }
            if (hit(mx, my, tx + TAB_W + 2, y + TAB_Y, TAB_W, TAB_H)) { menu.filterDir = 1; playClick(); return true; }
            if (fcell >= 0) {
                ModMessages.sendToServer(new HubFilterPacket(be.getBlockPos(), f, fcell, menu.getCarried().copy(), menu.filterDir == 1));
                playClick(); return true;
            }
        }
        return super.mouseClicked(mxD, myD, btn);
    }

    @Override
    public boolean mouseScrolled(double mxD, double myD, double delta) {
        HubBlockEntity be = menu.be;
        if (be != null && !be.fluid && menu.selectedFace >= 0 && menu.filterDir == 0) { // limits only on item input
            int fi = filterCellAt((int) mxD, (int) myD);
            if (fi >= 0) {
                int idx = menu.selectedFace * HubBlockEntity.FILTER_SLOTS + fi;
                if (!be.filterHandler.getStackInSlot(idx).isEmpty()) {
                    int step = Screen.hasShiftDown() ? 16 : 1;
                    int cap = Math.max(0, be.filterCaps[idx] + (delta > 0 ? step : -step));
                    ModMessages.sendToServer(new HubFilterQtyPacket(be.getBlockPos(), menu.selectedFace, fi, cap));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mxD, myD, delta);
    }

    private int filterCellAt(int mx, int my) {
        for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++)
            if (hit(mx, my, cellX(i), cellY(i), 16, 16)) return i;
        return -1;
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void click(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); playClick(); }
    private void playClick() {
        if (minecraft != null) minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void renderFluidIcon(GuiGraphics g, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTex = ext.getStillTexture();
        if (stillTex == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTex);
        int color = ext.getTintColor();
        float r = ((color >> 16) & 0xFF) / 255f, gr = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f; if (a == 0f) a = 1f;
        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static FluidStack extractFluidFromBucket(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        Optional<IFluidHandlerItem> cap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (cap.isEmpty()) return FluidStack.EMPTY;
        FluidStack f = cap.get().getFluidInTank(0);
        return f.isEmpty() ? FluidStack.EMPTY : new FluidStack(f.getFluid(), 1000);
    }
}
