package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;

import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.block.entity.SolarPanelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlock extends BaseSolarPanel {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends SolarPanelBlock> codec() { return MapCodec.unit(this); }

    public final SolarPanelTier tier;

    public SolarPanelBlock(SolarPanelTier tier, BlockBehaviour.Properties props) {
        super(props);
        this.tier = tier;
    }

    @Override public int getDayOutput()    { return tier.dayGen; }
    @Override public int getNightOutput()  { return tier.nightGen; }
    @Override public int getBufferSize()   { return tier.bufferSize; }
    @Override public boolean requiresSky() { return true; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var beType = switch (tier) {
            case T1 -> ModBlockEntities.SOLAR_PANEL_T1_BE.get();
            case T2 -> ModBlockEntities.SOLAR_PANEL_T2_BE.get();
            case T3 -> ModBlockEntities.SOLAR_PANEL_T3_BE.get();
            case T4 -> ModBlockEntities.SOLAR_PANEL_T4_BE.get();
            case T5 -> ModBlockEntities.SOLAR_PANEL_T5_BE.get();
            case T6 -> ModBlockEntities.SOLAR_PANEL_T6_BE.get();
            case T7 -> ModBlockEntities.SOLAR_PANEL_T7_BE.get();
            case T8 -> ModBlockEntities.SOLAR_PANEL_T8_BE.get();
            case T9 -> ModBlockEntities.SOLAR_PANEL_T9_BE.get();
            case T10 -> ModBlockEntities.SOLAR_PANEL_T10_BE.get();
        };
        return new SolarPanelBlockEntity(beType, pos, state);
    }
}
