package com.jykito.industrialcore.client;

import com.jykito.industrialcore.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Общий рендер «крафтящегося предмета в центре машины» (реструктор/взраститель). Чисто визуально. */
@OnlyIn(Dist.CLIENT)
public final class MachineItemRender {
    private MachineItemRender() {}

    public static void render(ItemStack stack, Level level, float partialTick,
                              PoseStack ps, MultiBufferSource buf, int overlay,
                              double x, double y, double z) {
        if (level == null || stack == null || stack.isEmpty()) return; // рисуем только когда есть выход (= работает)
        if (!Config.showMachineItem) return;                          // конфиг-тумблер для крупных баз
        float t = (float) (level.getGameTime() % 36000L) + partialTick;
        ps.pushPose();
        ps.translate(x, y, z);                                         // позиция предмета (задаёт рендерер машины)
        ps.translate(0, Mth.sin(t * 0.08f) * 0.06f, 0);                // лёгкое покачивание
        ps.mulPose(Axis.YP.rotationDegrees(t * 3f));                   // вращение
        ps.scale(0.55f, 0.55f, 0.55f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.GROUND,
                LightTexture.FULL_BRIGHT, overlay,
                ps, buf, level, 0);
        ps.popPose();
    }
}
