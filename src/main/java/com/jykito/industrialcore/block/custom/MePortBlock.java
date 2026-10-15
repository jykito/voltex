package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.compat.ae2.MePorts;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * ME port of a late multiblock: input, output or pattern port. For the structure it is a regular input or
 * output port (the core takes and puts the same way), and with AE2 it has a grid node:
 * <ul>
 *   <li>the input keeps the configured items and fluid in stock, requesting more from the network (like an interface);</li>
 *   <li>the output sends everything the core put into it to the network;</li>
 *   <li>the pattern port is a processing pattern provider for autocrafting: it puts ingredients straight into the machine input.</li>
 * </ul>
 * Without AE2 the block works as a regular port (see {@link MePorts}).
 */
public class MePortBlock extends MultiblockPortBlock {

    public enum MeKind {
        INPUT("me_input_port_gui"), OUTPUT("me_output_port_gui"), PATTERN("me_pattern_port_gui");
        public final ResourceLocation gui;
        MeKind(String tex) { this.gui = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/" + tex + ".png"); }
    }

    private final MeKind meKind;

    public MePortBlock(MeKind meKind, Properties props) {
        super(meKind == MeKind.OUTPUT ? Kind.OUTPUT : Kind.INPUT, props);
        this.meKind = meKind;
    }

    public MeKind meKind() { return meKind; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return MePorts.create(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.ME_PORT_BE.get(), ItemFluidPortBlockEntity::serverTick);
    }
}
