package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.block.custom.MePortBlock.MeKind;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import com.jykito.industrialcore.menu.MePortMenu;
import com.jykito.industrialcore.networking.MePortPacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.jykito.industrialcore.menu.MePortMenu.*;

/**
 * Окно МЭ-портов, раскладка «Пульт»: ряды сверху, табло снизу, справа колонка, продолжающая ряды
 * (жидкости пар у входа, кнопки и баки буфера у порта шаблонов). Текстуры — tools/gui_gen/me_port_gui2.js (К1).
 */
public class MePortScreen extends MachineScreen<MePortMenu> {

    private static final int TW = 256, TH = 320;
    // спрайты под окном
    private static final int SPR_Y = 260, LOCK_U = 0, BTN_U = 18, BTN_HOVER_U = 36, PATTERN_U = 54, FLUID_CELL_U = 72;
    private static final int ICON_BLOCK_ON = 90, ICON_BLOCK_OFF = 106, ICON_SHOW = 122, ICON_HIDE = 138, ICON_PRIO = 154;
    private static final int LCD_X = 47, LCD_W = 162, CELL = 18;
    /** Кнопки порта шаблонов в колонке напротив рядов шаблонов: блокировка, терминал, приоритет. */
    private static final int BTN_BLOCK = 0, BTN_TERMINAL = 1, BTN_PRIORITY = 2;

    private static final int C_ON = 0xFF6FD08C, C_LOW = 0xFFE0B050, C_OFF = 0xFFE06C6C, C_IDLE = 0xFF2A2A2A;

    private final ResourceLocation texture;
    private final boolean tall;
    private final int lcdY, ledY;
    /** Цвета табло и числа призраков: от цвета рамки порта к белому. */
    private final int cInfo, cInfo2, cGhost;

    public MePortScreen(MePortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.texture = menu.kind.gui;
        this.tall = menu.kind != MeKind.OUTPUT;
        this.imageHeight = tall ? TALL_H : SHORT_H;
        this.lcdY = tall ? 126 : 90;
        this.ledY = tall ? 2 : 25;
        boolean pattern = menu.kind == MeKind.PATTERN;
        this.cInfo = pattern ? 0xFFF1E2C1 : 0xFFB2D1EF;
        this.cInfo2 = pattern ? 0xFFEEDBB2 : 0xFF9FC6EB;
        this.cGhost = pattern ? 0xFFFFFAF0 : 0xFFEEF6FF;
    }

    // ── геометрия клеток (координаты окна) ──

    private static int colX(int i) { return GRID_X + (i % 9) * CELL; }
    private static int[] fluidCell(int t) { return new int[]{SIDE_X, CFG_Y[t]}; }
    private int[] tankCell(int t) {
        return switch (menu.kind) {
            case INPUT -> new int[]{SIDE_X, STO_Y[t], CELL, CELL};
            case PATTERN -> new int[]{SIDE_X, BUF_Y[t], CELL, CELL};
            case OUTPUT -> new int[]{SIDE_X, OUT_Y[0], CELL, 3 * CELL};
        };
    }
    private static int[] btnPos(int b) { return new int[]{SIDE_X, PAT_Y[b]}; }

    // ── фон ──

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(texture, x, y, 0, 0, imageWidth, imageHeight, TW, TH);
        g.fill(x + 126, y + ledY, x + 130, y + ledY + 2, menu.online() ? C_ON : C_OFF);

        int open = menu.activeSlots();
        switch (menu.kind) {
            case INPUT -> {
                for (int i = 0; i < PortUpgradeItem.MAX_SLOTS; i++) {
                    int r = i / 9, cx = x + colX(i);
                    if (i >= open) { lock(g, cx, y + CFG_Y[r]); lock(g, cx, y + STO_Y[r]); led(g, cx + 7, y + GAP_Y[r] + 1, C_IDLE); continue; }
                    ItemStack want = menu.meConfig(i), cur = menu.getSlot(i).getItem();
                    if (!want.isEmpty()) ghostItem(g, want, cx + 1, y + CFG_Y[r] + 1);
                    led(g, cx + 7, y + GAP_Y[r] + 1, want.isEmpty() ? C_IDLE : cur.getCount() >= want.getCount() ? C_ON : cur.isEmpty() ? C_OFF : C_LOW);
                }
                List<FluidTank> tanks = menu.blockEntity.tanks();
                for (int t = 0; t < 3; t++) {
                    int[] fc = fluidCell(t), tc = tankCell(t);
                    if (t >= menu.openTanks()) { lock(g, x + fc[0], y + fc[1]); lock(g, x + tc[0], y + tc[1]); led(g, x + SIDE_X + 7, y + GAP_Y[t] + 1, C_IDLE); continue; }
                    FluidStack want = menu.meFluidConfig(t);
                    if (want.isEmpty()) g.blit(texture, x + fc[0], y + fc[1], FLUID_CELL_U, SPR_Y, CELL, CELL, TW, TH);
                    else ghostFluid(g, want, x + fc[0] + 1, y + fc[1] + 1);
                    FluidTank tank = t < tanks.size() ? tanks.get(t) : null;
                    int have = tank == null ? 0 : tank.getFluidAmount();
                    drawTank(g, tank, x + tc[0], y + tc[1], tc[2], tc[3]);
                    led(g, x + SIDE_X + 7, y + GAP_Y[t] + 1, want.isEmpty() ? C_IDLE : have >= Math.min(want.getAmount(), menu.tankCapacity()) ? C_ON : have == 0 ? C_OFF : C_LOW);
                }
            }
            case PATTERN -> {
                for (int i = 0; i < PortUpgradeItem.MAX_SLOTS; i++) {
                    int r = i / 9, cx = x + colX(i);
                    if (i >= open) { lock(g, cx, y + PAT_Y[r]); lock(g, cx, y + BUF_Y[r]); continue; }
                    if (menu.getSlot(i).getItem().isEmpty()) g.blit(texture, cx, y + PAT_Y[r], PATTERN_U, SPR_Y, CELL, CELL, TW, TH);
                }
                List<FluidTank> tanks = menu.blockEntity.tanks();
                for (int t = 0; t < 3; t++) {
                    int[] tc = tankCell(t);
                    if (t >= menu.openTanks()) lock(g, x + tc[0], y + tc[1]);
                    else drawTank(g, t < tanks.size() ? tanks.get(t) : null, x + tc[0], y + tc[1], tc[2], tc[3]);
                }
                int[] icons = { menu.blocking() ? ICON_BLOCK_ON : ICON_BLOCK_OFF, menu.showInTerminal() ? ICON_SHOW : ICON_HIDE, ICON_PRIO };
                for (int b = 0; b < 3; b++) {
                    int[] p = btnPos(b);
                    g.blit(texture, x + p[0], y + p[1], in(mx, my, p[0], p[1], CELL, CELL) ? BTN_HOVER_U : BTN_U, SPR_Y, CELL, CELL, TW, TH);
                    g.blit(texture, x + p[0] + 1, y + p[1] + 1, icons[b], SPR_Y, 16, 16, TW, TH);
                }
            }
            case OUTPUT -> {
                for (int i = open; i < PortUpgradeItem.MAX_SLOTS; i++) lock(g, x + colX(i), y + OUT_Y[i / 9]);
                int[] tc = tankCell(0);
                drawTank(g, menu.blockEntity.getTank(), x + tc[0], y + tc[1], tc[2], tc[3]);
            }
        }
        drawLcd(g, x, y);
    }

    private void lock(GuiGraphics g, int x, int y) { g.blit(texture, x, y, LOCK_U, SPR_Y, CELL, CELL, TW, TH); }
    private static void led(GuiGraphics g, int x, int y, int color) { g.fill(x, y, x + 4, y + 1, color); }

    /** Призрак настройки: бледная иконка и число цвета рамки. */
    private void ghostItem(GuiGraphics g, ItemStack want, int x, int y) {
        g.renderItem(want, x, y);
        g.fill(x, y, x + 16, y + 16, 250, 0x88262626);
        corner(g, String.valueOf(want.getCount()), x, y);
    }

    private void ghostFluid(GuiGraphics g, FluidStack want, int x, int y) {
        FluidGauge.draw(g, want, want.getAmount(), x, y, 16, 16);
        g.fill(x, y, x + 16, y + 16, 250, 0x88262626);
        corner(g, shortMb(want.getAmount()), x, y);
    }

    /** Число в правом нижнем углу клетки, как у стопки. */
    private void corner(GuiGraphics g, String txt, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.drawString(font, txt, x + 17 - font.width(txt), y + 9, cGhost, true);
        g.pose().popPose();
    }

    /** Бак: жидкость снизу вверх и блик стекла по левому краю. */
    private static void drawTank(GuiGraphics g, FluidTank tank, int x, int y, int w, int h) {
        if (tank != null) FluidGauge.draw(g, tank.getFluid(), tank.getCapacity(), x + 1, y + 1, w - 2, h - 2);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x1FFFFFFF);
    }

    // ── табло: две строки мелким шрифтом ──

    private void drawLcd(GuiGraphics g, int x, int y) {
        int l1 = y + lcdY + 2, l2 = y + lcdY + 10, left = x + LCD_X + 3, right = x + LCD_X + LCD_W - 3;
        boolean on = menu.online();
        text(g, Component.translatable(on ? "gui.industrial_core.me.online" : "gui.industrial_core.me.offline"), left, l1, on ? C_ON : C_OFF, false);
        Component r1, a2, b2;
        switch (menu.kind) {
            case INPUT -> {
                int set = 0, full = 0;
                for (int i = 0; i < menu.activeSlots(); i++) {
                    ItemStack want = menu.meConfig(i);
                    if (want.isEmpty()) continue;
                    set++;
                    if (menu.getSlot(i).getItem().getCount() >= want.getCount()) full++;
                }
                List<FluidTank> tanks = menu.blockEntity.tanks();
                for (int t = 0; t < menu.openTanks(); t++) {
                    FluidStack want = menu.meFluidConfig(t);
                    if (want.isEmpty()) continue;
                    set++;
                    if (t < tanks.size() && tanks.get(t).getFluidAmount() >= Math.min(want.getAmount(), menu.tankCapacity())) full++;
                }
                r1 = Component.translatable("gui.industrial_core.me.slots", menu.activeSlots(), PortUpgradeItem.MAX_SLOTS);
                a2 = Component.translatable("gui.industrial_core.me.filled", full, set);
                b2 = Component.translatable("gui.industrial_core.me.tanks", menu.openTanks(), 3);
            }
            case PATTERN -> {
                r1 = Component.translatable("gui.industrial_core.me.patterns", menu.patternCount(), menu.activeSlots());
                a2 = Component.translatable(menu.blocking() ? "gui.industrial_core.me.blocking_on" : "gui.industrial_core.me.blocking_off");
                b2 = Component.translatable("gui.industrial_core.me.priority", menu.priority());
            }
            default -> {
                int items = 0;
                for (int i = 0; i < menu.activeSlots(); i++) items += menu.getSlot(i).getItem().getCount();
                r1 = Component.translatable("gui.industrial_core.me.slots", menu.activeSlots(), PortUpgradeItem.MAX_SLOTS);
                a2 = Component.translatable("gui.industrial_core.me.queue", items);
                b2 = Component.literal(num(menu.blockEntity.getTank().getFluidAmount()) + " mB");
            }
        }
        text(g, r1, right, l1, cInfo, true);
        text(g, a2, left, l2, cInfo2, false);
        text(g, b2, right, l2, cInfo2, true);
    }

    private void text(GuiGraphics g, Component c, int x, int y, int color, boolean alignRight) {
        float s = 0.8f;
        float w = font.width(c) * s;
        g.pose().pushPose();
        g.pose().translate(alignRight ? x - w : x, y, 0);
        g.pose().scale(s, s, 1f);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    // ── подсказки ──

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        if (in(mx, my, 124, ledY - 1, 8, 4))
            g.renderTooltip(font, Component.translatable(menu.online() ? "gui.industrial_core.me.online" : "gui.industrial_core.me.offline"), mx, my);
        if (menu.getSlot(menu.upgradeSlot).getItem().isEmpty() && in(mx, my, SIDE_X + 1, tall ? 127 : 91, 16, 16))
            g.renderTooltip(font, Component.translatable("gui.industrial_core.port.upgrade_slot"), mx, my);

        switch (menu.kind) {
            case INPUT -> {
                for (int i = 0; i < menu.activeSlots(); i++) {
                    if (!in(mx, my, colX(i), CFG_Y[i / 9], CELL, CELL)) continue;
                    ItemStack want = menu.meConfig(i);
                    tip(g, mx, my, want.isEmpty() ? Component.translatable("gui.industrial_core.me.config_empty")
                                    : Component.translatable("gui.industrial_core.me.config_keep", want.getHoverName(), want.getCount()),
                            "gui.industrial_core.me.config_hint", "gui.industrial_core.me.config_hint2");
                }
                for (int t = 0; t < menu.openTanks(); t++) {
                    int[] fc = fluidCell(t), tc = tankCell(t);
                    if (in(mx, my, fc[0], fc[1], CELL, CELL)) {
                        FluidStack want = menu.meFluidConfig(t);
                        tip(g, mx, my, want.isEmpty() ? Component.translatable("gui.industrial_core.me.fluid_none")
                                        : Component.translatable("gui.industrial_core.me.fluid_keep", want.getDisplayName(), num(want.getAmount())),
                                "gui.industrial_core.me.fluid_hint", "gui.industrial_core.me.fluid_hint2");
                    }
                    tankTip(g, mx, my, t, tc);
                }
            }
            case PATTERN -> {
                for (int i = 0; i < menu.activeSlots(); i++)
                    if (menu.getSlot(PortUpgradeItem.MAX_SLOTS + i).getItem().isEmpty() && in(mx, my, colX(i), BUF_Y[i / 9], CELL, CELL))
                        tip(g, mx, my, Component.translatable("gui.industrial_core.me.buffer"), "gui.industrial_core.me.buffer_hint");
                for (int t = 0; t < menu.openTanks(); t++) tankTip(g, mx, my, t, tankCell(t));
                if (in(mx, my, btnPos(BTN_BLOCK)[0], btnPos(BTN_BLOCK)[1], CELL, CELL))
                    tip(g, mx, my, Component.translatable(menu.blocking() ? "gui.industrial_core.me.blocking_on" : "gui.industrial_core.me.blocking_off"), "gui.industrial_core.me.blocking_hint");
                if (in(mx, my, btnPos(BTN_TERMINAL)[0], btnPos(BTN_TERMINAL)[1], CELL, CELL))
                    tip(g, mx, my, Component.translatable(menu.showInTerminal() ? "gui.industrial_core.me.terminal_on" : "gui.industrial_core.me.terminal_off"), "gui.industrial_core.me.terminal_hint");
                if (in(mx, my, btnPos(BTN_PRIORITY)[0], btnPos(BTN_PRIORITY)[1], CELL, CELL))
                    tip(g, mx, my, Component.translatable("gui.industrial_core.me.priority", menu.priority()), "gui.industrial_core.me.priority_hint", "gui.industrial_core.me.priority_hint2");
            }
            case OUTPUT -> tankTip(g, mx, my, 0, tankCell(0));
        }
    }

    private void tankTip(GuiGraphics g, int mx, int my, int t, int[] tc) {
        List<FluidTank> tanks = menu.blockEntity.tanks();
        if (t < tanks.size())
            FluidGauge.tooltip(g, font, mx, my, leftPos + tc[0], topPos + tc[1], tc[2], tc[3], tanks.get(t).getFluid(), tanks.get(t).getCapacity());
    }

    private void tip(GuiGraphics g, int mx, int my, Component head, String... hints) {
        List<Component> lines = new ArrayList<>();
        lines.add(head);
        for (String h : hints) lines.add(Component.translatable(h).withStyle(ChatFormatting.GRAY));
        g.renderComponentTooltip(font, lines, mx, my);
    }

    // ── мышь ──

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if ((btn == 0 || btn == 1) && minecraft != null && minecraft.gameMode != null) {
            if (menu.kind == MeKind.INPUT) {
                for (int i = 0; i < menu.activeSlots(); i++)
                    if (in(mx, my, colX(i), CFG_Y[i / 9], CELL, CELL)) {
                        int id = menu.cfgStart + i;
                        slotClicked(menu.getSlot(id), id, btn, ClickType.PICKUP);
                        return true;
                    }
                for (int t = 0; t < menu.openTanks(); t++)
                    if (in(mx, my, fluidCell(t)[0], fluidCell(t)[1], CELL, CELL)) return button((btn == 1 ? BTN_FLUID_RIGHT : BTN_FLUID_LEFT) + t);
            }
            if (menu.kind == MeKind.PATTERN) {
                if (btn == 0 && in(mx, my, btnPos(BTN_BLOCK)[0], btnPos(BTN_BLOCK)[1], CELL, CELL)) return button(BTN_BLOCKING);
                if (btn == 0 && in(mx, my, btnPos(BTN_TERMINAL)[0], btnPos(BTN_TERMINAL)[1], CELL, CELL)) return button(BTN_SHOW);
                if (btn == 1 && in(mx, my, btnPos(BTN_PRIORITY)[0], btnPos(BTN_PRIORITY)[1], CELL, CELL)) {
                    ModMessages.sendToServer(MePortPacket.priority(menu.containerId, 0));
                    click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    /** Колесо: количество в клетке настройки (±1, с Shift ±16; у жидкости ±1000 mB, с Shift ±10 000), приоритет (±1, с Shift ±10). */
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int dir = delta > 0 ? 1 : -1;
        boolean shift = Screen.hasShiftDown();
        if (menu.kind == MeKind.INPUT) {
            for (int i = 0; i < menu.activeSlots(); i++)
                if (in(mx, my, colX(i), CFG_Y[i / 9], CELL, CELL) && !menu.meConfig(i).isEmpty()) {
                    ModMessages.sendToServer(MePortPacket.amount(menu.containerId, i, dir * (shift ? 16 : 1)));
                    return true;
                }
            for (int t = 0; t < menu.openTanks(); t++)
                if (in(mx, my, fluidCell(t)[0], fluidCell(t)[1], CELL, CELL) && !menu.meFluidConfig(t).isEmpty()) {
                    ModMessages.sendToServer(MePortPacket.amount(menu.containerId, PortUpgradeItem.MAX_SLOTS + t, dir * (shift ? 10_000 : 1_000)));
                    return true;
                }
        }
        if (menu.kind == MeKind.PATTERN && in(mx, my, btnPos(BTN_PRIORITY)[0], btnPos(BTN_PRIORITY)[1], CELL, CELL)) {
            ModMessages.sendToServer(MePortPacket.priority(menu.containerId, menu.priority() + dir * (shift ? 10 : 1)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean button(int id) {
        if (minecraft == null || minecraft.gameMode == null) return false;
        click();
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    // ── цели перетаскивания из JEI (клетки настройки МЭ-входа) ──

    /** Цель JEI: номер для {@link MePortPacket#ghost} (0..26 предметы, 27..29 жидкости) и место на экране. */
    public record GhostTarget(int index, Rect2i area) {}

    public List<GhostTarget> ghostTargets(boolean fluid) {
        List<GhostTarget> out = new ArrayList<>();
        if (menu.kind != MeKind.INPUT) return out;
        if (fluid) for (int t = 0; t < menu.openTanks(); t++)
            out.add(new GhostTarget(PortUpgradeItem.MAX_SLOTS + t, new Rect2i(leftPos + SIDE_X + 1, topPos + CFG_Y[t] + 1, 16, 16)));
        else for (int i = 0; i < menu.activeSlots(); i++)
            out.add(new GhostTarget(i, new Rect2i(leftPos + colX(i) + 1, topPos + CFG_Y[i / 9] + 1, 16, 16)));
        return out;
    }

    // ── мелочи ──

    private boolean in(double mx, double my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w && my >= topPos + ry && my < topPos + ry + h;
    }

    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }

    /** Объём для угла клетки: 800, 8к, 1.5к. */
    private static String shortMb(int mb) {
        if (mb < 1000) return String.valueOf(mb);
        String k = mb % 1000 == 0 ? String.valueOf(mb / 1000) : String.format(Locale.ROOT, "%.1f", mb / 1000f);
        return Component.translatable("gui.industrial_core.me.kilo", k).getString();
    }
}
