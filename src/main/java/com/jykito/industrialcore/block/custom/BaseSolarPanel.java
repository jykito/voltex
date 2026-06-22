package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.SolarPanelBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseSolarPanel extends BaseEntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected BaseSolarPanel(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public abstract int getDayOutput();

    public abstract int getNightOutput();

    public abstract int getBufferSize();

    public abstract boolean requiresSky();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp
                && level.getBlockEntity(pos) instanceof MenuProvider mp) {
            NetworkHooks.openScreen(sp, mp, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof SolarPanelBlockEntity sp) sp.tick(lvl, pos, st);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.solar_day", formatFE(getDayOutput()))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.industrial_core.solar_night", formatFE(getNightOutput()))
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000_000) return trim(fe / 1_000_000_000.0) + "B";
        if (fe >= 1_000_000)     return trim(fe / 1_000_000.0) + "M";
        if (fe >= 1_000)         return trim(fe / 1_000.0) + "k";
        return String.valueOf(fe);
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
