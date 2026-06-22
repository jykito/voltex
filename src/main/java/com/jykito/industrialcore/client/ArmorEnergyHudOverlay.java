package com.jykito.industrialcore.client;

import com.jykito.industrialcore.item.custom.EnergyBackpackItem;
import com.jykito.industrialcore.item.custom.NexiteArmorItem;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class ArmorEnergyHudOverlay {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };

    private static final int ROW_H = 22;

    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        List<ItemStack> energyItems = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                energyItems.add(stack);
            }
        }
        if (energyItems.isEmpty()) return;

        int x = 5;
        int baseY = screenHeight - 55;

        for (int i = 0; i < energyItems.size(); i++) {
            ItemStack stack = energyItems.get(i);
            IEnergyStorage energy = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            if (energy == null) continue;

            int stored = energy.getEnergyStored();
            int max    = energy.getMaxEnergyStored();
            int rowY   = baseY - i * ROW_H;

            guiGraphics.renderItem(stack, x, rowY);

            int barX = x + 1;
            int barY = rowY + 17;
            int barW = 14;
            guiGraphics.fill(barX, barY, barX + barW, barY + 2, 0xFF000000);
            if (max > 0) {
                int filled = (int)(barW * (float) stored / max);
                if (filled > 0)
                    guiGraphics.fill(barX, barY, barX + filled, barY + 2, barColor(stack));
            }

            PoseStack ps = guiGraphics.pose();
            ps.pushPose();
            ps.translate(x + 19, rowY + 1, 0);
            ps.scale(0.75f, 0.75f, 1f);

            guiGraphics.drawString(mc.font, fmt(stored) + " FE", 0, 0, 0xFFFFFF, true);

            if (stack.getItem() instanceof PlasmaArmorItem) {
                int burst = PlasmaArmorItem.getBurstCharge(stack);
                guiGraphics.drawString(mc.font,
                    "⚡ " + burst + "/" + PlasmaArmorItem.BURST_MAX,
                    0, 9, 0xFFFF6600, true);
            }

            ps.popPose();
        }
    };

    private static int barColor(ItemStack stack) {

        return 0xFF000000 | com.jykito.industrialcore.ModStyle.ENERGY;
    }

    private static String fmt(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000f);
        if (fe >= 1_000)     return String.format("%.1fK", fe / 1_000f);
        return String.valueOf(fe);
    }
}
