package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.FluidConnectorMenu;
import com.jykito.industrialcore.networking.ConnectorFilterFluidPacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class FluidConnectorScreen extends AbstractContainerScreen<FluidConnectorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/fluid_connector_gui.png");

    private static final int TEX_W = 256, TEX_H = 315;
    private static final int GUI_W = 256, GUI_H = 201;

    public static final int FILTER_COL_START = 86;
    public static final int FILTER_ROW_START = 54;
    public static final int FILTER_GAP       = 17;
    public static final int FILTER_SLOTS     = 5;

    private static final int BTN_MODE_X      = 173, BTN_MODE_Y      = 88;
    private static final int BTN_NBT_X       = 173, BTN_NBT_Y       = 68;
    private static final int BTN_WHITELIST_X = 173, BTN_WHITELIST_Y = 78;

    private static final int ALT_NBT_U       = 151, ALT_NBT_V       = 236;
    private static final int ALT_WHITELIST_U = 151, ALT_WHITELIST_V = 249;
    private static final int ALT_MODE_U      = 151, ALT_MODE_V      = 260;

    public int getGuiLeft() { return leftPos; }
    public int getGuiTop()  { return topPos; }

    public FluidConnectorScreen(FluidConnectorMenu menu, Inventory inv, Component title) {
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

        renderFluidSlots(g, mx, my);

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
    }

    private void renderFluidSlots(GuiGraphics g, int mx, int my) {
        FluidStack[] filters = menu.blockEntity.fluidFilters;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int sx = leftPos + FILTER_COL_START + i * FILTER_GAP;
            int sy = topPos  + FILTER_ROW_START;
            boolean hovered = mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16;

            FluidStack fluid = filters[i];
            if (fluid != null && !fluid.isEmpty()) {
                renderFluidIcon(g, fluid, sx, sy);
                if (hovered) g.renderTooltip(font, fluid.getDisplayName(), mx, my);
            }

            if (hovered) g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
        }
    }

    private void renderFluidIcon(GuiGraphics g, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation stillTex = ext.getStillTexture();
        if (stillTex == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTex);
        int color = ext.getTintColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float gr = ((color >> 8)  & 0xFF) / 255f;
        float b = (color & 0xFF)         / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0f) a = 1f;
        RenderSystem.setShaderColor(r, gr, b, a);
        g.blit(x, y, 0, 16, 16, sprite);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isOver(mx, my, BTN_MODE_X,      BTN_MODE_Y))      { pressBtn(0); return true; }
        if (isOver(mx, my, BTN_WHITELIST_X, BTN_WHITELIST_Y)) { pressBtn(1); return true; }
        if (isOver(mx, my, BTN_NBT_X,       BTN_NBT_Y))       { pressBtn(2); return true; }

        for (int i = 0; i < FILTER_SLOTS; i++) {
            int sx = leftPos + FILTER_COL_START + i * FILTER_GAP;
            int sy = topPos  + FILTER_ROW_START;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                if (button == 1) {
                    ModMessages.sendToServer(new ConnectorFilterFluidPacket(
                            menu.blockEntity.getBlockPos(), i, FluidStack.EMPTY));
                } else {
                    FluidStack fluid = extractFluidFromBucket(menu.getCarried());
                    if (fluid.isEmpty()) fluid = extractFluidFromBucket(
                            minecraft.player.getInventory().getSelected());
                    if (!fluid.isEmpty())
                        ModMessages.sendToServer(new ConnectorFilterFluidPacket(
                                menu.blockEntity.getBlockPos(), i, fluid));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void pressBtn(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private boolean isOver(double mx, double my, int gx, int gy) {
        return mx >= leftPos + gx && mx < leftPos + gx + 7
            && my >= topPos  + gy && my < topPos  + gy + 7;
    }

    private static FluidStack extractFluidFromBucket(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        Optional<IFluidHandlerItem> cap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (cap.isEmpty()) return FluidStack.EMPTY;
        FluidStack f = cap.get().getFluidInTank(0);
        return f.isEmpty() ? FluidStack.EMPTY : new FluidStack(f.getFluid(), 1000);
    }
}
