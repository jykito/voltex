package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Ядро криокамеры — управляющий блок мультиблока.
 * <p>
 * ПКМ открывает панель. Приседая — отчёт о структуре: собрана либо первое место, где не сходится.
 * Отчёт остался отдельной кнопкой потому, что искать одну неверную панель в оболочке 5×5×5
 * глазами больно, а панель об этом сказать не может.
 */
public class CryoCoreBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CryoCoreBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // лицом к игроку, как у остальных машин мода
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CryoCoreBlockEntity be) {
            String miss = be.firstMismatch(level, pos, state);
            // Несобранная камера не открывает панель: пустое окно ничего не объясняет, а вот
            // первое несовпадение — объясняет. Приседая, отчёт можно получить и на собранной.
            if (miss != null) {
                player.displayClientMessage(
                        Component.translatable("msg.industrial_core.cryo.missing", miss).withStyle(ChatFormatting.RED), false);
            } else if (player.isShiftKeyDown()) {
                player.displayClientMessage(
                        Component.translatable("msg.industrial_core.cryo.formed").withStyle(ChatFormatting.GREEN), false);
            } else {
                net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CryoCoreBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // проверка структуры серверная; createTickerHelper тут недоступен —
        // он у BaseEntityBlock, а мы наследуем HorizontalDirectionalBlock ради FACING
        if (level.isClientSide || type != ModBlockEntities.CRYO_CORE_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CryoCoreBlockEntity>) CryoCoreBlockEntity::serverTick;
    }
}
