package com.jykito.industrialcore.compat.jade;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.block.entity.BaseMachineBlockEntity;
import com.jykito.industrialcore.heat.HeatCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip for the mod's blocks: energy, heat, tank contents, progress.
 * Everything is read through capabilities and the shared {@code baseData}, so the block entities did not need changes.
 * <p>
 * ⚠️ Regular mod code must not reference this class: Jade loads it itself
 * by scanning for the {@link snownee.jade.api.WailaPlugin} annotation. Without Jade nobody touches the class.
 */
public enum MachineDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    INSTANCE;

    public static final ResourceLocation UID = new ResourceLocation(IndustrialCore.MODID, "machine");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    // ------------------------------------------------------------- server

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be == null || !isOurs(accessor)) return;

        be.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            if (energy.getMaxEnergyStored() > 0) {
                data.putInt("fe", energy.getEnergyStored());
                data.putInt("feMax", energy.getMaxEnergyStored());
            }
        });

        be.getCapability(HeatCapability.HEAT).ifPresent(heat -> {
            if (heat.getMaxHeat() > 0) {
                data.putInt("heat", heat.getHeat());
                data.putInt("heatMax", heat.getMaxHeat());
            }
        });

        be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tanks -> {
            for (int i = 0; i < tanks.getTanks(); i++) {
                FluidStack stack = tanks.getFluidInTank(i);
                if (stack.isEmpty()) continue;
                ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
                if (id == null) continue;
                // send the id, not a ready name: the locale is taken on the client, not the server's
                data.putString("fluid", id.toString());
                data.putInt("fluidAmount", stack.getAmount());
                data.putInt("fluidCap", tanks.getTankCapacity(i));
                break;
            }
        });

        if (be instanceof BaseMachineBlockEntity machine) {
            int progress = machine.baseData.get(4), max = machine.baseData.get(5);
            if (progress > 0 && max > 0) {
                data.putInt("progress", progress);
                data.putInt("progressMax", max);
            }
        }
    }

    // ------------------------------------------------------------- client

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!isOurs(accessor)) return;
        CompoundTag data = accessor.getServerData();

        if (data.contains("fe")) {
            tooltip.add(Component.translatable("jade.industrial_core.energy",
                            compact(data.getInt("fe")), compact(data.getInt("feMax")))
                    .withStyle(ModStyle.ENERGY_STYLE));
        }

        if (data.contains("heat")) {
            tooltip.add(Component.translatable("jade.industrial_core.heat",
                    compact(data.getInt("heat")), compact(data.getInt("heatMax"))));
        }

        if (data.contains("fluid")) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(data.getString("fluid")));
            if (fluid != null) {
                tooltip.add(Component.translatable("jade.industrial_core.fluid",
                        new FluidStack(fluid, 1).getDisplayName(),
                        compact(data.getInt("fluidAmount")), compact(data.getInt("fluidCap"))));
            }
        }

        if (data.contains("progress")) {
            int percent = data.getInt("progress") * 100 / Math.max(1, data.getInt("progressMax"));
            tooltip.add(Component.translatable("jade.industrial_core.progress", percent));
        }
    }

    // ------------------------------------------------------------- utils

    /** Jade calls the provider for any block under the crosshair; other mods' blocks are cut off right away. */
    private static boolean isOurs(BlockAccessor accessor) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(accessor.getBlock());
        return id != null && IndustrialCore.MODID.equals(id.getNamespace());
    }

    /** 1500000 -> "1.5M": full energy bank numbers do not fit into the tooltip. */
    private static String compact(int value) {
        if (value >= 1_000_000_000) return String.format("%.1fG", value / 1_000_000_000f);
        if (value >= 1_000_000)     return String.format("%.1fM", value / 1_000_000f);
        if (value >= 10_000)        return String.format("%.1fk", value / 1_000f);
        return Integer.toString(value);
    }
}
