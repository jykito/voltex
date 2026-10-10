package com.jykito.industrialcore.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Бак жидкости в GUI: заливка снизу вверх текстурой жидкости и подсказка с объёмом. */
public final class FluidGauge {

    private static final int WATER_TINT = 0xFF3F76E4;

    private FluidGauge() {}

    /** Стекло бака рисует сам экран поверх, этот метод даёт только жидкость. */
    public static void draw(GuiGraphics g, FluidStack stack, int capacity, int x, int y, int w, int h) {
        if (stack.isEmpty() || capacity <= 0) return;
        int fluidH = Math.min(h, (int) ((float) stack.getAmount() / capacity * h));
        if (fluidH <= 0) return;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation still = ext.getStillTexture(stack);
        if (still == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

        // вода в мире красится биомом, у стека своего цвета нет, берём ванильный
        int color = stack.getFluid() == Fluids.WATER || stack.getFluid() == Fluids.FLOWING_WATER
                ? WATER_TINT : ext.getTintColor(stack);
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0f) a = 1f;

        RenderSystem.enableBlend();
        g.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, a);
        for (int i = 0; i < w; i += 16) {
            for (int j = 0; j < fluidH; j += 16) {
                int dw = Math.min(w - i, 16), dh = Math.min(fluidH - j, 16);
                g.blit(x + i, y + h - j - dh, 0, dw, dh, sprite);
            }
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /** «Вода: 4000 / 8000 mB» или «Пусто», когда курсор над баком. */
    public static void tooltip(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, int h,
                               FluidStack stack, int capacity) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return;
        g.renderTooltip(font, text(stack, capacity), mx, my);
    }

    public static Component text(FluidStack stack, int capacity) {
        return stack.isEmpty()
                ? Component.translatable("gui.industrial_core.empty")
                : Component.literal("").append(stack.getDisplayName())
                        .append(": " + stack.getAmount() + " / " + capacity + " mB");
    }
}
