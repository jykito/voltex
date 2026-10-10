package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import com.jykito.industrialcore.menu.ItemFluidPortMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public class ItemFluidPortScreen extends MachineScreen<ItemFluidPortMenu> {

    private static final ResourceLocation INPUT_TEX =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/multiblock_input_port_gui.png");
    private static final ResourceLocation OUTPUT_TEX =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/multiblock_output_port_gui.png");

    // ячейки сетки на арте на 1 px левее и выше предмета
    private static final int CELL_X = ItemFluidPortMenu.GRID_X - 1, CELL_Y = ItemFluidPortMenu.GRID_Y - 1, CELL = 18;
    // бак поверх сетки в режиме жидкости; рамка 1 px
    private static final int TANK_X = CELL_X, TANK_Y = CELL_Y, TANK_W = 162, TANK_H = 54;
    private static final int TANK_SRC_U = 0, TANK_SRC_V = 232;
    private static final int LOCK_SRC_U = 164, LOCK_SRC_V = 232;
    /** Стекло широкого бака в стиле сжижителя: отдельный спрайт 160x52 поверх жидкости. */
    private static final ResourceLocation TANK_GLASS =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/multiblock_port_tank_glass.png");
    // кнопка режима
    private static final int BTN_X = 47, BTN_Y = 93, BTN = 18;
    private static final int BTN_SRC_U = 184, BTN_HOVER_U = 204, BTN_SRC_V = 232;
    // кнопка автообмена рядом с кнопкой режима
    private static final int AUTO_X = 67, AUTO_Y = 93;
    // подписи между кнопками и слотом улучшения
    private static final int INFO_X = 89, INFO_Y = 95;

    private static final int C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0;

    private final ResourceLocation texture;

    public ItemFluidPortScreen(ItemFluidPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.texture = menu.isOutput() ? OUTPUT_TEX : INPUT_TEX;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(texture, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        if (menu.isFluidMode()) drawTank(g, x, y);
        else drawLockedCells(g, x, y);

        boolean hover = inButton(mx, my);
        g.blit(texture, x + BTN_X, y + BTN_Y, hover ? BTN_HOVER_U : BTN_SRC_U, BTN_SRC_V, BTN, BTN, TEX_W, TEX_H);
        g.renderItem(new ItemStack(menu.isFluidMode() ? Items.BUCKET : Items.CHEST), x + BTN_X + 1, y + BTN_Y + 1);
        if (menu.isBothMode()) {   // смешанный режим: поверх сундука маленькое ведро в углу
            g.pose().pushPose(); g.pose().translate(x + BTN_X + 8, y + BTN_Y + 8, 100); g.pose().scale(0.55f, 0.55f, 1f);
            g.renderItem(new ItemStack(Items.WATER_BUCKET), 0, 0);
            g.pose().popPose();
        }

        boolean autoHover = inAuto(mx, my);
        g.blit(texture, x + AUTO_X, y + AUTO_Y, autoHover ? BTN_HOVER_U : BTN_SRC_U, BTN_SRC_V, BTN, BTN, TEX_W, TEX_H);
        g.renderItem(new ItemStack(Items.HOPPER), x + AUTO_X + 1, y + AUTO_Y + 1);
        // выключено: воронка гаснет под полупрозрачной заливкой кнопки
        if (!menu.isAuto()) g.fill(x + AUTO_X + 1, y + AUTO_Y + 1, x + AUTO_X + BTN - 1, y + AUTO_Y + BTN - 1, 200, 0xB0202020);

        drawInfo(g, x, y);
    }

    private void drawTank(GuiGraphics g, int x, int y) {
        g.blit(texture, x + TANK_X, y + TANK_Y, TANK_SRC_U, TANK_SRC_V, TANK_W, TANK_H, TEX_W, TEX_H);
        FluidGauge.draw(g, menu.blockEntity.getTank().getFluid(), menu.getFluidCapacity(),
                x + TANK_X + 1, y + TANK_Y + 1, TANK_W - 2, TANK_H - 2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TANK_GLASS, x + TANK_X + 1, y + TANK_Y + 1, 0, 0, TANK_W - 2, TANK_H - 2, TANK_W - 2, TANK_H - 2);
        RenderSystem.disableBlend();
    }

    private void drawLockedCells(GuiGraphics g, int x, int y) {
        for (int i = menu.activeSlots(); i < PortUpgradeItem.MAX_SLOTS; i++)
            g.blit(texture, x + CELL_X + (i % 9) * CELL, y + CELL_Y + (i / 9) * CELL, LOCK_SRC_U, LOCK_SRC_V, CELL, CELL, TEX_W, TEX_H);
    }

    private void drawInfo(GuiGraphics g, int x, int y) {
        String label, value;
        if (menu.isBothMode()) {
            label = Component.translatable("gui.industrial_core.port.slots", menu.activeSlots(), PortUpgradeItem.MAX_SLOTS).getString();
            value = num(menu.getFluidAmount()) + " / " + num(menu.getFluidCapacity()) + " mB";
        } else if (menu.isFluidMode()) {
            label = Component.translatable("gui.industrial_core.port.fluid").getString();
            value = num(menu.getFluidAmount()) + " / " + num(menu.getFluidCapacity()) + " mB";
        } else {
            label = Component.translatable("gui.industrial_core.port.items").getString();
            value = Component.translatable("gui.industrial_core.port.slots", menu.activeSlots(), PortUpgradeItem.MAX_SLOTS).getString();
        }
        drawScaled(g, label, x + INFO_X, y + INFO_Y, C_LABEL, 0.8f);
        drawScaled(g, value, x + INFO_X, y + INFO_Y + 9, C_VALUE, 0.8f);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        if (menu.isFluidMode())
            FluidGauge.tooltip(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                    menu.blockEntity.getTank().getFluid(), menu.getFluidCapacity());
        if (menu.isBothMode())   // строка с объёмом: по наведению название жидкости
            FluidGauge.tooltip(g, font, mx, my, leftPos + INFO_X, topPos + INFO_Y + 8, 98, 10,
                    menu.blockEntity.getTank().getFluid(), menu.getFluidCapacity());

        if (inButton(mx, my)) {
            Component mode = Component.translatable(menu.isBothMode() ? "gui.industrial_core.port.both"
                    : menu.isFluidMode() ? "gui.industrial_core.port.fluid" : "gui.industrial_core.port.items");
            Component hint = menu.blockEntity.isEmpty()
                    ? Component.translatable("gui.industrial_core.port.toggle").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.industrial_core.port.toggle_blocked").withStyle(ChatFormatting.RED);
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.industrial_core.port.mode", mode), hint), mx, my);
        }

        if (inAuto(mx, my)) {
            Component state = Component.translatable(menu.isAuto() ? "gui.industrial_core.port.auto_on" : "gui.industrial_core.port.auto_off");
            Component what = Component.translatable(menu.isOutput() ? "gui.industrial_core.port.auto_push" : "gui.industrial_core.port.auto_pull")
                    .withStyle(ChatFormatting.GRAY);
            g.renderComponentTooltip(font, List.of(state, what), mx, my);
        }

        if (menu.getSlot(PortUpgradeItem.MAX_SLOTS).getItem().isEmpty()
                && in(mx, my, ItemFluidPortMenu.UPGRADE_X, ItemFluidPortMenu.UPGRADE_Y, 16, 16))
            g.renderTooltip(font, Component.translatable("gui.industrial_core.port.upgrade_slot"), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && minecraft != null && minecraft.gameMode != null) {
            int id = inButton(mx, my) ? ItemFluidPortMenu.BTN_MODE : inAuto(mx, my) ? ItemFluidPortMenu.BTN_AUTO : -1;
            if (id >= 0) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean inButton(double mx, double my) { return in(mx, my, BTN_X, BTN_Y, BTN, BTN); }
    private boolean inAuto(double mx, double my)   { return in(mx, my, AUTO_X, AUTO_Y, BTN, BTN); }

    private boolean in(double mx, double my, int rx, int ry, int w, int h) {
        return mx >= leftPos + rx && mx < leftPos + rx + w && my >= topPos + ry && my < topPos + ry + h;
    }

    private void drawScaled(GuiGraphics g, String txt, float x, float y, int col, float s) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(s, s, 1f);
        g.drawString(font, txt, 0, 0, col, false);
        g.pose().popPose();
    }

    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
