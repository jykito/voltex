package com.jykito.industrialcore.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.util.Lazy;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {

    CARBON_FIBER("carbon_fiber", 35, new int[]{3, 7, 9, 3}, 12,
            SoundEvents.ARMOR_EQUIP_GENERIC, 2.5f, 0.0f,
            () -> Ingredient.of(ModItems.CARBON_PLATE.get())),

    CRYSTAL("crystal", 42, new int[]{4, 8, 10, 4}, 18,
            SoundEvents.ARMOR_EQUIP_GENERIC, 3.5f, 0.1f,
            () -> Ingredient.of(ModItems.CRYSTAL_PLATE.get())),

    NEXITE("nexite", 60, new int[]{4, 8, 10, 4}, 20,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0f, 0.3f,
            () -> Ingredient.of(ModItems.KRONIT.get())),

    PLASMA("plasma", 80, new int[]{4, 9, 11, 4}, 25,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0f, 0.4f,
            () -> Ingredient.of(ModItems.KRONIT.get()));

    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Lazy<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantability,
                      SoundEvent equipSound, float toughness, float knockbackResistance,
                      Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = Lazy.of(repairIngredient);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.getSlot().getIndex()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() { return enchantability; }

    @Override
    public SoundEvent getEquipSound() { return equipSound; }

    @Override
    public Ingredient getRepairIngredient() { return repairIngredient.get(); }

    @Override
    public String getName() { return name; }

    @Override
    public float getToughness() { return toughness; }

    @Override
    public float getKnockbackResistance() { return knockbackResistance; }
}
