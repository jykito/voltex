package com.jykito.industrialcore.item;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Armor materials. In 1.21 a material is a registry entry; durability moved out of it into the item properties,
 * so the 1.20.1 durability multipliers live here and the armor items apply them with {@link #durability}.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, IndustrialCore.MODID);

    private static final Map<Holder<ArmorMaterial>, Integer> DURABILITY = new HashMap<>();

    // defense: {boots, leggings, chestplate, helmet}, as in 1.20.1 (indexed by equipment slot)
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CARBON_FIBER = register("carbon_fiber", 35,
            new int[]{3, 7, 9, 3}, 12, SoundEvents.ARMOR_EQUIP_GENERIC, 2.5f, 0.0f,
            () -> Ingredient.of(ModItems.CARBON_PLATE.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CRYSTAL = register("crystal", 42,
            new int[]{4, 8, 10, 4}, 18, SoundEvents.ARMOR_EQUIP_GENERIC, 3.5f, 0.1f,
            () -> Ingredient.of(ModItems.CRYSTAL_PLATE.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NEXITE = register("nexite", 60,
            new int[]{4, 8, 10, 4}, 20, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0f, 0.3f,
            () -> Ingredient.of(ModItems.KRONIT.get()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLASMA = register("plasma", 80,
            new int[]{4, 9, 11, 4}, 25, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0f, 0.4f,
            () -> Ingredient.of(ModItems.KRONIT.get()));

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, int durabilityMultiplier,
            int[] defense, int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance,
            Supplier<Ingredient> repair) {
        Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        def.put(ArmorItem.Type.BOOTS, defense[0]);
        def.put(ArmorItem.Type.LEGGINGS, defense[1]);
        def.put(ArmorItem.Type.CHESTPLATE, defense[2]);
        def.put(ArmorItem.Type.HELMET, defense[3]);
        def.put(ArmorItem.Type.BODY, defense[2]);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, name)));
        DeferredHolder<ArmorMaterial, ArmorMaterial> holder = MATERIALS.register(name, () ->
                new ArmorMaterial(def, enchantability, equipSound, repair, layers, toughness, knockbackResistance));
        DURABILITY.put(holder, durabilityMultiplier);
        return holder;
    }

    /** Max damage of an armor piece of this material, the same numbers as in 1.20.1. */
    public static int durability(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return type.getDurability(DURABILITY.getOrDefault(material, 1));
    }

    public static void register(IEventBus bus) { MATERIALS.register(bus); }
}
