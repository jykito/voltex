package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Species atlas toast in the top right, like advancements (author, 2026-10-07: "when you get a strain, a toast in the top right,
 * like an advancement, saying a new species is unlocked"): a tinted spider, a title in the event colour, the species name.
 * The background is P4 "Screen" (author, 2026-10-09: "P4 looks good"): a recessed screen with a sweep from nanite_toast.png
 * (tools/gui_gen/nanite_toast/build.js), a ring around the spider and a strip under the title in the event colour. Width 180:
 * "Atlas mark: Gold" and species names did not fit into the vanilla 160.
 */
public class NaniteAtlasToast implements Toast {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/nanite_toast.png");
    private static final int TEX_W = 256, TEX_H = 64, WIDTH = 180, RING = 22;
    private static final long SHOW_MS = 5000L;

    private final ItemStack icon;
    private final Component title, name;
    private final int color;

    public NaniteAtlasToast(ItemStack icon, Component title, Component name, int color) {
        this.icon = icon; this.title = title; this.name = name; this.color = color;
    }

    @Override public int width() { return WIDTH; }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        g.blit(TEXTURE, 0, 0, 0, 0, WIDTH, height(), TEX_W, TEX_H);
        float r = (color >> 16 & 255) / 255f, gr = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        RenderSystem.setShaderColor(r * 0.55f, gr * 0.55f, b * 0.55f, 1f);              // ring: event colour, muted
        g.blit(TEXTURE, 5, 5, 0, 32, RING, RING, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.fill(30, 16, WIDTH - 10, 17, 0xFF000000 | (int) (r * 0.45f * 255) << 16 | (int) (gr * 0.45f * 255) << 8 | (int) (b * 0.45f * 255));
        Font font = toasts.getMinecraft().font;
        g.drawString(font, title, 30, 7, color, false);
        g.drawString(font, font.plainSubstrByWidth(name.getString(), WIDTH - 36), 30, 18, 0xFFFFFFFF, false);
        g.renderFakeItem(icon, 8, 8);
        return time >= SHOW_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
