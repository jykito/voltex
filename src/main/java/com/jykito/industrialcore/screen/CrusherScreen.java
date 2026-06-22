package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CrusherMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrusherScreen extends BaseMachineScreen<CrusherMenu> {
    public CrusherScreen(CrusherMenu pMenu, Inventory pPlayerInventory, Component pTitle) {

        super(pMenu, pPlayerInventory, pTitle, new ResourceLocation(IndustrialCore.MODID + ":textures/gui/crusher_gui.png"));
    }
}
