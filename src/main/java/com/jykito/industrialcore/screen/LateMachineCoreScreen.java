package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.LateMachineCoreMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/** Экран ядра лейт-мультиблока: окно 256×228 — табло статуса, энергия, прогресс, строки параметров, текущий продукт. */
public class LateMachineCoreScreen extends MachineScreen<LateMachineCoreMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/late_machine_core_gui.png");

    private static final int WINDOW_H = 228;
    private static final int STATUS_X = 20, STATUS_Y = 16, STATUS_W = 220;
    private static final int ENERGY_X = 23, ENERGY_Y = 47, BAR_W = 218, BAR_H = 6;
    private static final int PROG_X = 23, PROG_Y = 59;
    /** Шевроны прогресса бегут заметно быстрее, чем мерцает энергия: свой счётчик кадров. */
    private static final int CHEVRON_FRAME_TICKS = 6;
    private static final int INFO_X = 18, INFO_Y = 74, LINE = 11;
    private static final int PRODUCT_X = 222, PRODUCT_Y = 105;

    private final FillBar chevrons = new FillBar(CHEVRON_FRAME_TICKS);

    private static final int C_OK = 0xFF6BE06B, C_BAD = 0xFFFF7A6B, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0, C_ENERGY = 0xFF00FFCC;

    public LateMachineCoreScreen(LateMachineCoreMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = WINDOW_H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.scaledEnergy(BAR_W), BAR_H);
        int p = menu.scaledProgress(BAR_W);
        chevrons.advance();
        chevrons.draw(g, FillBar.PROGRESS, x + PROG_X, y + PROG_Y, p, BAR_H);

        if (menu.nanite) {   // ячейка штамма в табло, как у промышленного шахтёра (общая текстура ядер её не содержит)
            int cx = x + LateMachineCoreMenu.NANITE_X - 1, cy = y + LateMachineCoreMenu.NANITE_Y - 1;
            g.fill(cx, cy, cx + 18, cy + 18, 0xFF1B1B1B);
            g.fill(cx + 1, cy + 1, cx + 18, cy + 18, 0xFF6E6E6E);
            g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF2C2C2C);
        }
        drawStatus(g, x, y);
        drawInfo(g, x, y);

        ItemStack product = menu.product();
        if (!product.isEmpty()) {
            g.renderItem(product, x + PRODUCT_X, y + PRODUCT_Y);
            g.renderItemDecorations(font, product, x + PRODUCT_X, y + PRODUCT_Y, menu.ops() > 1 ? "×" + menu.ops() : null);
        }
    }

    private void drawStatus(GuiGraphics g, int x, int y) {
        if (menu.isFormed()) {
            drawText(g, tr("formed"), x + STATUS_X, y + STATUS_Y, C_OK);
            drawText(g, menu.ops() > 0 ? tr("working", menu.ops()) : tr("idle"), x + STATUS_X, y + STATUS_Y + 10, C_VALUE);
            return;
        }
        drawText(g, tr("not_formed"), x + STATUS_X, y + STATUS_Y, C_BAD);
        String detail = switch (menu.problem()) {
            case WRONG_BLOCK -> {
                BlockPos at = menu.problemPos();
                yield tr("wrong_block", tr("cell." + menu.expected().name().toLowerCase(Locale.ROOT)), at.getX(), at.getY(), at.getZ());
            }
            case NO_INPUT -> tr("no_input");
            case NO_OUTPUT -> tr("no_output");
            case NO_ENERGY -> tr("no_energy");
            case NO_HEAT -> tr("no_heat");
            case NONE -> "";
        };
        drawText(g, detail, x + STATUS_X, y + STATUS_Y + 10, C_VALUE);
    }

    private void drawInfo(GuiGraphics g, int x, int y) {
        drawText(g, tr("parallel", menu.parallel(), menu.speedPercent()), x + INFO_X, y + INFO_Y, C_LABEL);
        drawText(g, tr("energy_use", num(menu.energyPerTick())), x + INFO_X, y + INFO_Y + LINE, C_ENERGY);
        drawText(g, menu.usesHeat()
                ? tr("ports_heat", menu.inputs(), menu.outputs(), menu.energyPorts(), menu.heatPorts())
                : tr("ports", menu.inputs(), menu.outputs(), menu.energyPorts()), x + INFO_X, y + INFO_Y + 2 * LINE, C_LABEL);
        if (menu.modeCount() > 1) {
            // строка режима — она же кнопка: клик переключает режим по кругу
            String txt = tr("mode", tr("mode." + menu.modeKey())) + "  ⇄";
            modeW = (int) Math.ceil(font.width(txt) * 0.9f);
            boolean hover = in(lastMx, lastMy, INFO_X - 2, MODE_Y - 2, modeW + 4, 11);
            if (hover) g.fill(x + INFO_X - 2, y + MODE_Y - 2, x + INFO_X + modeW + 2, y + MODE_Y + 9, 0x33FFFFFF);
            drawText(g, txt, x + INFO_X, y + MODE_Y, hover ? 0xFFFFFFFF : C_MODE);
        }
        if (menu.usesHeat()) {
            // строка тепла под режимом (или на его месте): красная, пока до порога партии не догрето
            int need = menu.heatNeeded(), hy = menu.modeCount() > 1 ? MODE_Y + LINE : MODE_Y;
            String txt = need > 0 ? tr("heat_need", num(menu.heat()), num(need)) : tr("heat", num(menu.heat()));
            drawText(g, txt, x + INFO_X, y + hy, menu.heat() >= need ? C_HEAT : C_BAD);
        }
    }

    private static final int MODE_Y = INFO_Y + 3 * LINE;
    private static final int C_MODE = 0xFFE0C060, C_HEAT = 0xFFFFA347;
    private int modeW, lastMx, lastMy;

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        lastMx = mx; lastMy = my;
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && menu.modeCount() > 1 && in((int) mx, (int) my, INFO_X - 2, MODE_Y - 2, modeW + 4, 11)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + ENERGY_X, topPos + ENERGY_Y, BAR_W, BAR_H, menu.energy(), menu.energyCap());
        if (in(mx, my, PROG_X, PROG_Y, BAR_W, BAR_H) && menu.maxProgress() > 0)
            g.renderTooltip(font, Component.literal(menu.progress() + " / " + menu.maxProgress() + " t"), mx, my);
        if (in(mx, my, PRODUCT_X, PRODUCT_Y, 16, 16) && !menu.product().isEmpty())
            g.renderTooltip(font, menu.product(), mx, my);
        if (menu.nanite && hoveredSlot != null && hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
            g.renderComponentTooltip(font, List.of(Component.literal(tr("nanite_slot")), Component.literal(tr("nanite_slot_hint"))), mx, my);
            return;
        }
        // полный текст причины и подсказка по наведению на табло
        if (!menu.isFormed() && in(mx, my, STATUS_X - 4, STATUS_Y - 4, STATUS_W, 26)) {
            List<FormattedCharSequence> lines = font.split(Component.literal(tr("hint")), 220);
            g.renderTooltip(font, lines, mx, my);
        }
    }

    private boolean in(int mx, int my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w && my >= topPos + ry && my < topPos + ry + h;
    }

    private void drawText(GuiGraphics g, String txt, int x, int y, int col) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(0.9f, 0.9f, 1f);
        g.drawString(font, txt, 0, 0, col, false);
        g.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.industrial_core.late_core." + key, args).getString();
    }

    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
