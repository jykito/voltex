package com.jykito.industrialcore.command;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("plasmaburst")
                .requires(src -> src.getEntity() instanceof Player p && p.isCreative())
                .executes(ctx -> executeBurst(ctx.getSource()))
        );
    }

    private static int executeBurst(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) return 0;

        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof PlasmaArmorItem) pieces.add(stack);
        }

        if (pieces.isEmpty()) return 0;

        PlasmaArmorEventHandler.triggerPlasmaBurst(player, pieces);
        return Command.SINGLE_SUCCESS;
    }
}
