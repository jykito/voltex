package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity.SiteView;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.menu.IndustrialMinerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Industrial miner window 256x262: status display with the nanite strain slot on the right, energy, cycle progress, drill fluid, up to four sites
 * (enable/disable is the square on the left, unlink is the cross on the right), the output row and the inventory.
 */
public class IndustrialMinerScreen extends MachineScreen<IndustrialMinerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/industrial_miner_gui.png");

    private static final int WINDOW_H = 300;
    private static final int STATUS_X = 20, STATUS_Y = 16;
    private static final int BAR_X = 23, BAR_W = 218, BAR_H = 6, ENERGY_Y = 47, PROG_Y = 59, FLUID_Y = 71;
    private static final int SITES_X = 18, SITES_Y = 82, ROW_Y = 93, ROW = 11, ROW_W = 222;
    private static final int TOGGLE_X = 18, ORES_X = 156, ORE_STEP = 26, UNLINK_X = 234;

    private static final int C_OK = 0xFF6BE06B, C_BAD = 0xFFFF7A6B, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0,
            C_GREEN = 0xFF05FF00, C_DIM = 0xFF3A3A3A;

    private final FillBar chevrons = new FillBar(6);

    public IndustrialMinerScreen(IndustrialMinerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + ENERGY_Y, menu.scaledEnergy(BAR_W), BAR_H);
        chevrons.advance();
        chevrons.draw(g, FillBar.PROGRESS, x + BAR_X, y + PROG_Y, menu.scaledProgress(BAR_W), BAR_H);
        int fw = menu.scaledFluid(BAR_W);
        if (fw > 0) FluidGauge.draw(g, new FluidStack(ModFluids.SOURCE_DRILL_FLUID.get(), menu.fluidCap()), menu.fluidCap(), x + BAR_X, y + FLUID_Y, fw, BAR_H);

        drawStatus(g, x, y);
        drawSites(g, x, y, mx, my);
    }

    private void drawStatus(GuiGraphics g, int x, int y) {
        IndustrialMinerCoreBlockEntity.Status s = menu.status();
        boolean ok = s == IndustrialMinerCoreBlockEntity.Status.WORKING;
        text(g, tr("status." + s.name().toLowerCase(Locale.ROOT)), x + STATUS_X, y + STATUS_Y, ok ? C_OK : C_BAD);
        if (ok || menu.lastOres() > 0)
            text(g, tr("cycle", menu.parallel(), num(menu.lastOres())), x + STATUS_X, y + STATUS_Y + 10, C_VALUE);
    }

    private void drawSites(GuiGraphics g, int x, int y, int mx, int my) {
        List<SiteView> sites = menu.core.getClientSites();
        text(g, tr("sites", sites.size(), IndustrialMinerCoreBlockEntity.MAX_SITES), x + SITES_X, y + SITES_Y, C_LABEL);
        if (sites.isEmpty()) {
            text(g, tr("sites_hint"), x + SITES_X, y + ROW_Y, C_LABEL);
            return;
        }
        for (int i = 0; i < sites.size(); i++) {
            SiteView v = sites.get(i);
            int ry = y + ROW_Y + i * ROW;
            if (in(mx, my, SITES_X - 2, ROW_Y + i * ROW - 1, ROW_W, ROW)) g.fill(x + SITES_X - 2, ry - 1, x + SITES_X - 2 + ROW_W, ry + ROW - 1, 0x22FFFFFF);
            // toggle
            g.fill(x + TOGGLE_X, ry, x + TOGGLE_X + 8, ry + 8, 0xFF141414);
            g.fill(x + TOGGLE_X + 1, ry + 1, x + TOGGLE_X + 7, ry + 7, v.enabled() ? C_GREEN : C_DIM);
            // where
            int col = !v.enabled() ? C_LABEL : v.hasData() ? C_VALUE : C_BAD;
            text(g, dimName(v.dim()) + "  " + v.pos().getX() + " " + v.pos().getY() + " " + v.pos().getZ(), x + TOGGLE_X + 12, ry, col);
            // top three ores
            if (v.hasData() && v.total() > 0) {
                for (int k = 0; k < v.top().size(); k++) {
                    int ox = x + ORES_X + k * ORE_STEP;
                    g.pose().pushPose(); g.pose().translate(ox, ry, 0); g.pose().scale(0.5f, 0.5f, 1f);
                    g.renderItem(new ItemStack(v.top().get(k)), 0, 0);
                    g.pose().popPose();
                    small(g, v.topPercent().get(k) + "%", ox + 9, ry + 1, C_LABEL);
                }
            } else text(g, tr(v.hasData() ? "site_empty" : "site_no_data"), x + ORES_X, ry, C_BAD);
            // unlink
            text(g, "×", x + UNLINK_X, ry, in(mx, my, UNLINK_X - 1, ROW_Y + i * ROW - 1, 8, 10) ? 0xFFFFFFFF : C_LABEL);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            List<SiteView> sites = menu.core.getClientSites();
            for (int i = 0; i < sites.size(); i++) {
                int ry = ROW_Y + i * ROW;
                if (in((int) mx, (int) my, TOGGLE_X - 1, ry - 1, 10, 10)) { press(i); return true; }
                if (in((int) mx, (int) my, UNLINK_X - 1, ry - 1, 8, 10)) { press(IndustrialMinerCoreBlockEntity.MAX_SITES + i); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + ENERGY_Y, BAR_W, BAR_H, menu.energy(), menu.energyCap());
        if (in(mx, my, BAR_X, PROG_Y, BAR_W, BAR_H))
            g.renderTooltip(font, Component.literal(menu.progress() + " / " + menu.cycle() + " t"), mx, my);
        if (in(mx, my, BAR_X, FLUID_Y, BAR_W, BAR_H))
            FluidGauge.tooltip(g, font, mx, my, leftPos + BAR_X, topPos + FLUID_Y, BAR_W, BAR_H,
                    new FluidStack(ModFluids.SOURCE_DRILL_FLUID.get(), menu.fluid()), menu.fluidCap());
        if (hoveredSlot != null && hoveredSlot.index == IndustrialMinerMenu.NANITE_SLOT && !hoveredSlot.hasItem()) {
            g.renderComponentTooltip(font, List.of(Component.literal(tr("nanite_slot")), Component.literal(tr("nanite_slot_hint"))), mx, my);
            return;
        }
        List<SiteView> sites = menu.core.getClientSites();
        for (int i = 0; i < sites.size(); i++) {
            int ry = ROW_Y + i * ROW;
            if (in(mx, my, TOGGLE_X - 1, ry - 1, 10, 10)) { g.renderTooltip(font, Component.literal(tr("toggle")), mx, my); return; }
            if (in(mx, my, UNLINK_X - 1, ry - 1, 8, 10)) { g.renderTooltip(font, Component.literal(tr("unlink")), mx, my); return; }
            if (in(mx, my, SITES_X - 2, ry - 1, ROW_W, ROW)) { g.renderComponentTooltip(font, siteTooltip(sites.get(i)), mx, my); return; }
        }
    }

    private List<Component> siteTooltip(SiteView v) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal(dimName(v.dim()) + "  " + v.pos().getX() + " " + v.pos().getY() + " " + v.pos().getZ()));
        if (!v.hasData()) { out.add(Component.literal(tr("site_no_data_long"))); return out; }
        out.add(Component.literal(tr("site_total", num((int) Math.min(Integer.MAX_VALUE, v.total())), v.avgY())));
        for (int k = 0; k < v.top().size(); k++)
            out.add(Component.literal(v.topPercent().get(k) + "%  ").append(new ItemStack(v.top().get(k)).getHoverName()));
        if (!v.enabled()) out.add(Component.literal(tr("site_off")));
        return out;
    }

    static String dimName(ResourceKey<Level> dim) {
        if (dim == Level.OVERWORLD) return tr2("dim.overworld");
        if (dim == Level.NETHER) return tr2("dim.the_nether");
        if (dim == Level.END) return tr2("dim.the_end");
        return dim.location().getPath();
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
        return Component.translatable("gui.industrial_core.industrial_miner." + key, args).getString();
    }

    private static String tr2(String key) {
        return Component.translatable("gui.industrial_core." + key).getString();
    }

    static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
