package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class ModArmorItem extends ArmorItem {

    private final String materialName;

    public ModArmorItem(ArmorMaterial material, Type type, Properties properties, String materialName) {
        super(material, type, properties);
        this.materialName = materialName;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return IndustrialCore.MODID + ":textures/models/armor/" + materialName + "_layer_" + layer + ".png";
    }
}
