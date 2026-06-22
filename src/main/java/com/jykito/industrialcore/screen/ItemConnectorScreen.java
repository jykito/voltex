package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.ItemConnectorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemConnectorScreen extends AbstractContainerScreen<ItemConnectorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/item_connector_gui.png");

    private static final int TEX_W = 256, TEX_H = 315;
    private static final int GUI_W = 256, GUI_H = 201;

    public static final int FILTER_COL_START = ItemConnectorMenu.FILTER_X0;
    public static final int FILTER_ROW_START = ItemConnectorMenu.FILTER_Y0;
    public static final int FILTER_GAP       = ItemConnectorMenu.FILTER_GAP;
    public static final int FILTER_COLS      = 5;

    private static final int BTN_MODE_X      = 173, BTN_MODE_Y      = 88;
    private static final int BTN_DMG_X       = 173, BTN_DMG_Y       = 58;
    private static final int BTN_NBT_X       = 173, BTN_NBT_Y       = 68;
    private static final int BTN_WHITELIST_X = 173, BTN_WHITELIST_Y = 78;

    private static final int ALT_DMG_U       = 151, ALT_DMG_V       = 223;
    private static final int ALT_NBT_U       = 151, ALT_NBT_V       = 236;
    private static final int ALT_WHITELIST_U = 151, ALT_WHITELIST_V = 249;
    private static final int ALT_MODE_U      = 151, ALT_MODE_V      = 260;

    public int getGuiLeft() { return leftPos; }
    public int getGuiTop()  { return topPos; }

    public ItemConnectorScreen(ItemConnectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        super.render(g, mx, my, partial);

        if      (isOver(mx, my, BTN_MODE_X,      BTN_MODE_Y))
            g.renderTooltip(font, menu.getMode() == 0
                    ? Component.literal("PUSH").withStyle(ChatFormatting.GREEN)
                    : Component.literal("PULL").withStyle(ChatFormatting.AQUA), mx, my);
        else if (isOver(mx, my, BTN_WHITELIST_X, BTN_WHITELIST_Y))
            g.renderTooltip(font, menu.isWhitelist()
                    ? Component.literal("Whitelist").withStyle(ChatFormatting.GREEN)
                    : Component.literal("Blacklist").withStyle(ChatFormatting.RED), mx, my);
        else if (isOver(mx, my, BTN_NBT_X, BTN_NBT_Y))
            g.renderTooltip(font, menu.isMatchNbt()
                    ? Component.translatable("screen.industrial_core.connector.nbt_on").withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("screen.industrial_core.connector.nbt_off").withStyle(ChatFormatting.GRAY), mx, my);
        else if (isOver(mx, my, BTN_DMG_X, BTN_DMG_Y))
            g.renderTooltip(font, menu.isMatchDmg()
                    ? Component.translatable("screen.industrial_core.connector.durability_on").withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("screen.industrial_core.connector.durability_off").withStyle(ChatFormatting.GRAY), mx, my);
        else
            this.renderTooltip(g, mx, my);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_W, GUI_H, TEX_W, TEX_H);

        if (menu.getMode() != 0)
            g.blit(TEXTURE, leftPos + BTN_MODE_X, topPos + BTN_MODE_Y,
                    ALT_MODE_U, ALT_MODE_V, 7, 7, TEX_W, TEX_H);

        if (!menu.isWhitelist())
            g.blit(TEXTURE, leftPos + BTN_WHITELIST_X, topPos + BTN_WHITELIST_Y,
                    ALT_WHITELIST_U, ALT_WHITELIST_V, 7, 7, TEX_W, TEX_H);

        if (menu.isMatchNbt())
            g.blit(TEXTURE, leftPos + BTN_NBT_X, topPos + BTN_NBT_Y,
                    ALT_NBT_U, ALT_NBT_V, 7, 7, TEX_W, TEX_H);

        if (menu.isMatchDmg())
            g.blit(TEXTURE, leftPos + BTN_DMG_X, topPos + BTN_DMG_Y,
                    ALT_DMG_U, ALT_DMG_V, 7, 7, TEX_W, TEX_H);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isOver(mx, my, BTN_MODE_X,      BTN_MODE_Y))      { press(0); return true; }
        if (isOver(mx, my, BTN_WHITELIST_X, BTN_WHITELIST_Y)) { press(1); return true; }
        if (isOver(mx, my, BTN_NBT_X,       BTN_NBT_Y))       { press(2); return true; }
        if (isOver(mx, my, BTN_DMG_X,       BTN_DMG_Y))       { press(3); return true; }
        return super.mouseClicked(mx, my, button);
    }

    private void press(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private boolean isOver(double mx, double my, int gx, int gy) {
        return mx >= leftPos + gx && mx < leftPos + gx + 7
            && my >= topPos  + gy && my < topPos  + gy + 7;
    }
}
