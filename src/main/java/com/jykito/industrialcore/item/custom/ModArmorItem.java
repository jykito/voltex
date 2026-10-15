package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class ModArmorItem extends ArmorItem {

    private final String materialName;

    public ModArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Properties properties, String materialName) {
        super(material, type, properties.durability(com.jykito.industrialcore.item.ModArmorMaterials.durability(material, type)));
        this.materialName = materialName;
    }

    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        int layerNo = innerModel ? 2 : 1;
        return net.minecraft.resources.ResourceLocation.parse(IndustrialCore.MODID + ":textures/models/armor/" + materialName + "_layer_" + layerNo + ".png");
    }
}
