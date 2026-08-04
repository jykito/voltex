package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** DEV: ПКМ по ядру магнита → строит весь мультиблок-скелет вокруг него (чтоб не собирать руками). */
public class StructureWandItem extends Item {
    public StructureWandItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ElectromagnetCoreBlock) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(ElectromagnetCoreBlock.FACING);
                ElectromagnetCoreBlockEntity.buildStructure(level, pos, facing);
                if (ctx.getPlayer() != null)
                    ctx.getPlayer().displayClientMessage(Component.translatable("msg.industrial_core.structure_built"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
