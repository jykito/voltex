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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HubScreen extends AbstractContainerScreen<HubMenu> {

    private static final ResourceLocation LIST_TEX   = new ResourceLocation(IndustrialCore.MODID, "textures/gui/connect_hub_gui.png");
    private static final ResourceLocation CONFIG_TEX  = new ResourceLocation(IndustrialCore.MODID, "textures/gui/config_hub_gui.png");
    private static final int TEX_W = 256, TEX_H = 315;

    // фильтр 3×3
    public static final int FGRID_X = 43, FGRID_Y = 50, FCELL = 19;
    // строка списка
    private static final int ROW_X = 36, ROW_Y = 17, ROW_STEP = 18, ROW_W = 184, ROW_H = 16;
    private static final int ROW_SRC_U = 35, ROW_SRC_V = 236;
    // чип режима (список)
    private static final int CHIP_X = 179, CHIP_W = 39, CHIP_H = 12;

    public int getGuiLeft() { return leftPos; }
    public int getGuiTop()  { return topPos; }
    public int getSelectedFace() { return menu.selectedFace; }

    public HubScreen(HubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 212;
    }

    @Override protected void init() { super.init(); this.titleLabelY = 10000; this.inventoryLabelY = 10000; }

    private List<Direction> connectedFaces() {
        List<Direction> out = new ArrayList<>();
        HubBlockEntity be = menu.be;
        if (be == null || be.getLevel() == null) return out;
        for (Direction d : Direction.values())
            if (!be.getLevel().getBlockState(be.getBlockPos().relative(d)).isAir()) out.add(d);
        return out;
    }

    private static Component tr(String key) { return Component.translatable("gui.industrial_core.hub." + key); }

    private static int listChipV(HubFaceMode m) {
        if (m == HubFaceMode.OFF) return 262;
        if (m.hasInput()) return 289; // ввод и both — входной чип
        return 275;                   // только вывод
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (menu.be == null) return;
        if (menu.selectedFace < 0) {
            g.blit(LIST_TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
            renderMain(g);
        } else {
            g.blit(CONFIG_TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
            renderConfig(g);
        }
    }

    private void renderMain(GuiGraphics g) {
        int x = leftPos, y = topPos;
        Component t = tr("title");
        g.drawString(font, t, x + 88 + (80 - font.width(t)) / 2, y + 7, 0xFFE0E0E0, false);
        List<Direction> faces = connectedFaces();
        if (faces.isEmpty()) {
            g.drawString(font, Component.translatable("gui.industrial_core.hub.no_neighbors"), x + 40, y + 30, 0xFF9A9A9A, false);
            return;
        }
        int i = 0;
        for (Direction d : faces) {
            int rowY = y + ROW_Y + i * ROW_STEP;
            BlockState ns = menu.be.getLevel().getBlockState(menu.be.getBlockPos().relative(d));
            g.blit(LIST_TEX, x + ROW_X, rowY, ROW_SRC_U, ROW_SRC_V, ROW_W, ROW_H, TEX_W, TEX_H);
            g.renderItem(new ItemStack(ns.getBlock()), x + 40, rowY);
            Component side = tr("side." + d.getName());
            g.drawString(font, side, x + 60, rowY + 4, 0xFFFFC24A, false);
            g.drawString(font, ns.getBlock().getName(), x + 60 + font.width(side) + 5, rowY + 4, 0xFFE8E8E8, false);
            g.blit(LIST_TEX, x + CHIP_X, rowY + 2, 183, listChipV(menu.be.getMode(d)), CHIP_W, CHIP_H, TEX_W, TEX_H);
            Component ml = chipLabel(menu.be.getMode(d));
            g.drawString(font, ml, x + CHIP_X + (CHIP_W - font.width(ml)) / 2, rowY + 3, 0xFFFFFFFF, true);
            i++;
        }
    }

    private void renderConfig(GuiGraphics g) {
        int x = leftPos, y = topPos, face = menu.selectedFace;
        Direction d = Direction.from3DDataValue(face);
        g.drawString(font, "<", x + 38, y + 7, 0xFFFFFFFF, false);

        BlockState ns = menu.be.getLevel().getBlockState(menu.be.getBlockPos().relative(d));
        g.renderItem(new ItemStack(ns.getBlock()), x + 57, y + 6);
        Component cside = tr("side." + d.getName());
        g.drawString(font, cside, x + 77, y + 10, 0xFFFFC24A, false);
        g.drawString(font, ns.getBlock().getName(), x + 77 + font.width(cside) + 5, y + 10, 0xFFE8E8E8, false);

        // подсветка активных направлений (both = обе кнопки разом)
        HubFaceMode m = menu.be.getMode(d);
        if (m.hasInput())  g.blit(CONFIG_TEX, x + 67,  y + 31, 17, 268, 39, 12, TEX_W, TEX_H);
        if (m.hasOutput()) g.blit(CONFIG_TEX, x + 108, y + 31, 17, 282, 39, 12, TEX_W, TEX_H);
        if (m == HubFaceMode.OFF) g.blit(CONFIG_TEX, x + 149, y + 31, 17, 255, 39, 12, TEX_W, TEX_H);
        modeLabel(g, x + 67,  y + 31, tr("mode.input"));
        modeLabel(g, x + 108, y + 31, tr("mode.output"));
        modeLabel(g, x + 149, y + 31, tr("mode.off"));

        // фильтр показанного направления (ввод/вывод) в сетке 3×3
        boolean out = menu.filterDir == 1;
        if (menu.be.fluid) {
            FluidStack[] arr = menu.be.fluidFilter(out);
            for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++) {
                int cx = x + FGRID_X + (i % 3) * FCELL, cy = y + FGRID_Y + (i / 3) * FCELL;
                FluidStack ff = arr[face * HubBlockEntity.FILTER_SLOTS + i];
                if (ff != null && !ff.isEmpty()) renderFluidIcon(g, ff, cx, cy);
            }
        } else {
            var fh = menu.be.filter(out);
            for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++) {
                int cx = x + FGRID_X + (i % 3) * FCELL, cy = y + FGRID_Y + (i / 3) * FCELL;
                int idx = face * HubBlockEntity.FILTER_SLOTS + i;
                ItemStack f = fh.getStackInSlot(idx);
                if (!f.isEmpty()) {
                    g.renderItem(f, cx, cy);
                    if (!out && menu.be.filterCaps[idx] > 0) // лимиты — только у ввода
                        g.renderItemDecorations(font, f, cx, cy, String.valueOf(menu.be.filterCaps[idx]));
                }
            }
        }

        // тумблеры: активный/неактивный спрайт + подпись
        toggle(g, x + 110, y + 52, menu.be.whitelist[face],  tr(menu.be.whitelist[face] ? "whitelist" : "blacklist"));
        toggle(g, x + 110, y + 70, menu.be.matchNbt[face],   tr(menu.be.matchNbt[face] ? "nbt_on" : "nbt_off"));
        toggle(g, x + 110, y + 88, menu.be.roundRobin[face], tr(menu.be.roundRobin[face] ? "round_robin" : "sequential"));

        // таб выбора фильтра ввод|вывод (рисуется кодом) + короткий хинт
        g.drawString(font, tr("filter"), x + 42, y + 114, 0xFF9A9A9A, false);
        dirTab(g, x + 82,  y + 110, !out, tr("filter_in"));
        dirTab(g, x + 112, y + 110, out,  tr("filter_out"));
        g.drawString(font, menu.be.fluid ? tr("hint_fluid") : tr(out ? "hint_out" : "hint_in"), x + 146, y + 114, 0xFF9A9A9A, false);
    }

    private void dirTab(GuiGraphics g, int x, int y, boolean active, Component label) {
        int w = 28, h = 11;
        g.fill(x, y, x + w, y + h, active ? 0xFF3A6B4A : 0xFF262626);
        g.fill(x, y, x + w, y + 1, 0xFF000000);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 2, active ? 0xFFFFFFFF : 0xFF9A9A9A, true);
    }

    private void toggle(GuiGraphics g, int x, int y, boolean on, Component label) {
        g.blit(CONFIG_TEX, x, y, 111, on ? 245 : 277, 71, 14, TEX_W, TEX_H);
        g.drawString(font, label, x + (71 - font.width(label)) / 2, y + 3, 0xFFFFFFFF, true);
    }

    private void modeLabel(GuiGraphics g, int x, int y, Component label) {
        g.drawString(font, label, x + (39 - font.width(label)) / 2, y + 2, 0xFFFFFFFF, true);
    }

    private static Component chipLabel(HubFaceMode m) {
        return switch (m) {
            case OFF -> tr("mode.off");
            case INPUT -> tr("mode.input");
            case OUTPUT -> tr("mode.output");
            case BOTH -> tr("mode.both");
        };
    }

    @Override
    public boolean mouseClicked(double mxD, double myD, int btn) {
        HubBlockEntity be = menu.be;
        // fluid-хаб: фантом-фильтр жидкости из ведра (ЛКМ) / очистка (ПКМ)
        if (be != null && be.fluid && menu.selectedFace >= 0) {
            int fcell = filterCellAt((int) mxD, (int) myD);
            if (fcell >= 0) {
                int f = menu.selectedFace; boolean out = menu.filterDir == 1;
                if (btn == 1) {
                    ModMessages.sendToServer(new HubFluidFilterPacket(be.getBlockPos(), f, fcell, out, FluidStack.EMPTY));
                    playClick(); return true;
                } else if (btn == 0) {
                    FluidStack fl = extractFluidFromBucket(menu.getCarried());
                    if (fl.isEmpty() && minecraft != null && minecraft.player != null)
                        fl = extractFluidFromBucket(minecraft.player.getInventory().getSelected());
                    if (!fl.isEmpty())
                        ModMessages.sendToServer(new HubFluidFilterPacket(be.getBlockPos(), f, fcell, out, fl));
                    playClick(); return true;
                }
            }
        }
        if (be != null && btn == 0) {
            int x = leftPos, y = topPos, mx = (int) mxD, my = (int) myD;
            if (menu.selectedFace < 0) {
                int i = 0;
                for (Direction d : connectedFaces()) {
                    int rowY = y + ROW_Y + i * ROW_STEP, f = d.get3DDataValue();
                    if (hit(mx, my, x + CHIP_X, rowY + 2, CHIP_W, CHIP_H)) { click(f); return true; }
                    if (hit(mx, my, x + ROW_X, rowY, ROW_W, ROW_H)) {
                        menu.selectedFace = f;
                        HubFaceMode fm = be.getMode(d);
                        menu.filterDir = fm.hasInput() || !fm.hasOutput() ? 0 : 1; // выход-only → сразу выходной фильтр
                        click(200 + f); return true;
                    }
                    i++;
                }
            } else {
                int f = menu.selectedFace;
                boolean shift = Screen.hasShiftDown();
                if (hit(mx, my, x + 35, y + 6, 10, 10)) { menu.selectedFace = -1; click(206); return true; }
                // режим: плейн = экскл., shift = переключить это направление (→ both)
                if (hit(mx, my, x + 67,  y + 31, 39, 12)) {
                    if (shift) click(400 + f * 2 + 0); else click(100 + f * 3 + 1);
                    menu.filterDir = 0; return true;
                }
                if (hit(mx, my, x + 108, y + 31, 39, 12)) {
                    if (shift) click(400 + f * 2 + 1); else click(100 + f * 3 + 2);
                    menu.filterDir = 1; return true;
                }
                if (hit(mx, my, x + 149, y + 31, 39, 12)) { click(100 + f * 3 + 0); return true; } // выкл
                if (hit(mx, my, x + 110, y + 52, 71, 14)) { click(300 + f * 3 + 0); return true; }
                if (hit(mx, my, x + 110, y + 70, 71, 14)) { click(300 + f * 3 + 1); return true; }
                if (hit(mx, my, x + 110, y + 88, 71, 14)) { click(300 + f * 3 + 2); return true; }
                // таб фильтра ввод|вывод (только вид)
                if (hit(mx, my, x + 82,  y + 110, 28, 11)) { menu.filterDir = 0; playClick(); return true; }
                if (hit(mx, my, x + 112, y + 110, 28, 11)) { menu.filterDir = 1; playClick(); return true; }
                int fi = filterCellAt(mx, my);
                if (fi >= 0) {
                    ModMessages.sendToServer(new HubFilterPacket(be.getBlockPos(), f, fi, menu.getCarried().copy(), menu.filterDir == 1));
                    playClick(); return true;
                }
            }
        }
        return super.mouseClicked(mxD, myD, btn);
    }

    @Override
    public boolean mouseScrolled(double mxD, double myD, double delta) {
        HubBlockEntity be = menu.be;
        if (be != null && !be.fluid && menu.selectedFace >= 0 && menu.filterDir == 0) { // лимиты — только у предметного ввода
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
        int x = leftPos, y = topPos;
        for (int i = 0; i < HubBlockEntity.FILTER_SLOTS; i++) {
            int cx = x + FGRID_X + (i % 3) * FCELL, cy = y + FGRID_Y + (i / 3) * FCELL;
            if (mx >= cx && mx < cx + 16 && my >= cy && my < cy + 16) return i;
        }
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
