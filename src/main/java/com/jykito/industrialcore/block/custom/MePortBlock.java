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
 * МЭ-порт лейт-мультиблока: вход, выход или порт шаблонов. Для структуры это обычный входной или
 * выходной порт (ядро берёт и кладёт так же), а с AE2 у него есть узел сети:
 * <ul>
 *   <li>вход держит в наличии заданные предметы и жидкость, дозапрашивая их из сети (как интерфейс);</li>
 *   <li>выход отдаёт всё, что положило ядро, в сеть;</li>
 *   <li>порт шаблонов — поставщик шаблонов обработки для автокрафта: ингредиенты кладёт прямо во вход машины.</li>
 * </ul>
 * Без AE2 блок работает как обычный порт (см. {@link MePorts}).
 */
public class MePortBlock extends MultiblockPortBlock {

    public enum MeKind {
        INPUT("me_input_port_gui"), OUTPUT("me_output_port_gui"), PATTERN("me_pattern_port_gui");
        public final ResourceLocation gui;
        MeKind(String tex) { this.gui = new ResourceLocation(IndustrialCore.MODID, "textures/gui/" + tex + ".png"); }
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
