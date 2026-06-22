package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.ExtractorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExtractorScreen extends BaseMachineScreen<ExtractorMenu> {
    public ExtractorScreen(ExtractorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle,
                new ResourceLocation(IndustrialCore.MODID, "textures/gui/extractor_gui.png"));
        PROG_X     = 112;
        PROG_Y     = 60;
        PROG_W     = 23;
        PROG_H     = 13;
        PROG_SRC_U = 117;
        PROG_SRC_V = 256;
    }
}
