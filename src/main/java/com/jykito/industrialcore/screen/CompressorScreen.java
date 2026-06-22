package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CompressorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CompressorScreen extends BaseMachineScreen<CompressorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/compressor_gui.png");

    public CompressorScreen(CompressorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, TEXTURE);
        PROG_X     = 116;
        PROG_Y     = 61;
        PROG_SRC_U = 111;
        PROG_SRC_V = 260;
    }
}
