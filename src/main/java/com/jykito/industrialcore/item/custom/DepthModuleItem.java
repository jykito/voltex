package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Модуль глубины: разрешение криокамере уходить на ступень {@link #getTier()}.
 * <p>
 * Гейт стоит именно на камере, а не на источнике тепла, потому что тепло масштабируется —
 * четыре термостанции взяли бы любую ступень количеством. Модуль это предмет, и числом
 * котельных его не обойти.
 * <p>
 * Не расходуется и не изнашивается: это установленное оборудование, а не топливо.
 */
public class DepthModuleItem extends Item {

    private final int tier;

    public DepthModuleItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    /** Максимальная ступень, на которую модуль пускает камеру. */
    public int getTier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        UpgradeTargets.append(tooltip, ModBlocks.CRYO_CORE);
    }
}
