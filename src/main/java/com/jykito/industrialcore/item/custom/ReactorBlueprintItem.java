package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReactorBlueprintItem extends Item {

    public static final int GRID_SIZE = 25;

    public ReactorBlueprintItem(Properties props) {
        super(props);
    }

    public static void saveLayout(ItemStack blueprint, ItemStack[] gridSlots) {
        CompoundTag tag = blueprint.getOrCreateTag();
        CompoundTag layout = new CompoundTag();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack slot = gridSlots[i];
            String regName = slot.isEmpty() ? "" :
                    ForgeRegistries.ITEMS.getKey(slot.getItem()).toString();
            layout.putString("s" + i, regName);
        }
        tag.put("layout", layout);
    }

    public static String[] loadLayout(ItemStack blueprint) {
        String[] result = new String[GRID_SIZE];
        CompoundTag tag = blueprint.getOrCreateTag();
        if (!tag.contains("layout")) {
            for (int i = 0; i < GRID_SIZE; i++) result[i] = "";
            return result;
        }
        CompoundTag layout = tag.getCompound("layout");
        for (int i = 0; i < GRID_SIZE; i++) result[i] = layout.getString("s" + i);
        return result;
    }

    public static boolean hasLayout(ItemStack blueprint) {
        return blueprint.hasTag() && blueprint.getTag().contains("layout");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasLayout(stack)) {
            tooltip.add(Component.translatable("tooltip.industrial_core.blueprint_saved")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.industrial_core.blueprint_empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.industrial_core.blueprint_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
