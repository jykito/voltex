package com.jykito.industrialcore.block;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * Bridge from the old Block.use (1.20.1) to useItemOn (1.21). useItemOn is called for both hands, as use was;
 * PASS becomes SKIP_DEFAULT_BLOCK_INTERACTION so the item acts next, not a repeated useWithoutItem.
 */
public final class BlockUse {
    private BlockUse() {}

    public static ItemInteractionResult toItemResult(InteractionResult r) {
        return switch (r) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            case PASS -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
