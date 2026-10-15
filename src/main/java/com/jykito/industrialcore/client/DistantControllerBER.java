package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.custom.DistantControllerBlock;
import com.jykito.industrialcore.block.entity.DistantControllerBlockEntity;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Distant controller screen: shows the item of the linked core (what is being processed) in the centre
 * and the instability % below it. Data comes from the linked core (findRecipe/getInstability),
 * the recipe lookup is throttled (once every 10 ticks) through the client cache in the BE.
 */
@OnlyIn(Dist.CLIENT)
public class DistantControllerBER implements BlockEntityRenderer<DistantControllerBlockEntity> {

    // ── centre of the screen face (computed from the model: the screen element, 22.5° tilt) + adjustments ──
    private static final float DCX = 0.503f, DCY = 0.656f, DCZ = 0.569f; // centre of the face, block coords
    private static final float TILT       = 22.5f;
    private static final float ITEM_SCALE = 0.30f;                 // item size
    private static final float ITEM_Y = 0.10f, ITEM_Z = 0.031f;    // item: above the centre + offset (flat icon), −0.3px towards the screen
    private static final float TEXT_Y = -0.15f, TEXT_Z = 0.019f;   // text: below the centre + offset above the face (~0.3px)
    private static final float TEXT_SCALE = 0.010f;

    private final Font font;
    private final ItemRenderer itemRenderer;

    public DistantControllerBER(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(DistantControllerBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        ElectromagnetCoreBlockEntity core = be.resolveCore();
        if (core == null) return;

        long gt = be.getLevel().getGameTime();
        if (gt - be.clientDisplayTick >= 10) {              // recipe lookup throttle
            be.clientDisplayItem = core.getInProgressResult();
            be.clientDisplayTick = gt;
        }
        ItemStack item = be.clientDisplayItem;
        int instab = (int) core.getInstability();   // floor, like in the GUI (ContainerData), otherwise a residual 0.9 rounded to 1% while the real value was 0

        Direction facing = be.getBlockState().getValue(DistantControllerBlock.FACING);
        float yDeg = switch (facing) {
            case WEST -> 90f; case SOUTH -> 180f; case EAST -> 270f; default -> 0f;
        };

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));           // orientation by the block face
        ps.translate(DCX - 0.5, DCY - 0.5, DCZ - 0.5);       // to the centre of the screen face
        ps.mulPose(Axis.YP.rotationDegrees(180f));           // facing outwards (the face looks to -Z)
        ps.mulPose(Axis.XP.rotationDegrees(-TILT));          // screen tilt -> face normal; local +Z = outwards (up out of the screen)

        // ── item in the centre ──
        if (!item.isEmpty()) {
            ps.pushPose();
            ps.translate(0f, ITEM_Y, ITEM_Z);
            ps.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE * 0.06f);   // flatten in depth -> a flat icon (even for blocks)
            itemRenderer.renderStatic(item, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, overlay,
                    ps, buf, be.getLevel(), 0);
            ps.popPose();
        }

        // ── instability under the item ──
        String txt = instab + "%";
        int col = instab >= 80 ? 0xFFFF5555 : instab >= 50 ? 0xFFFFD24A : 0xFF55FF7A;
        ps.pushPose();
        ps.translate(0f, TEXT_Y, TEXT_Z);
        ps.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);       // small text, Y up
        float w = font.width(txt);
        font.drawInBatch(txt, -w / 2f, 0f, col, false, ps.last().pose(), buf,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        ps.popPose();

        ps.popPose();
    }

    @Override public int getViewDistance() { return 64; }
}
