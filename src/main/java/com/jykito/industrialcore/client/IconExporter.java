package com.jykito.industrialcore.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "industrial_core", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class IconExporter {

    private static final int ICON_SIZE = 64;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("voltexicons")
                        .executes(ctx -> queue(ctx.getSource(), "industrial_core"))
                        .then(Commands.argument("namespace", StringArgumentType.word())
                                .executes(ctx -> queue(ctx.getSource(), StringArgumentType.getString(ctx, "namespace")))));
    }

    private static int queue(CommandSourceStack src, String namespace) {
        Minecraft mc = Minecraft.getInstance();
        src.sendSuccess(() -> Component.literal("§e[Voltex] Exporting icons: " + namespace + " …"), false);

        mc.execute(() -> export(mc, namespace, src));
        return 1;
    }

    private static void export(Minecraft mc, String namespace, CommandSourceStack src) {
        File outDir = new File(mc.gameDirectory, "voltex_export");
        File iconDir = new File(outDir, "icons");
        iconDir.mkdirs();

        List<String> itemsJson = new ArrayList<>();
        int count = 0;

        TextureTarget target = new TextureTarget(ICON_SIZE, ICON_SIZE, true, Minecraft.ON_OSX);
        target.setClearColor(0f, 0f, 0f, 0f);

        try {
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null) continue;
                if (!"all".equals(namespace) && !id.getNamespace().equals(namespace)) continue;

                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) continue;

                try (NativeImage img = new NativeImage(ICON_SIZE, ICON_SIZE, false)) {
                    renderToTarget(mc, target, stack);
                    target.bindRead();
                    img.downloadTexture(0, false);
                    img.flipY();
                    target.unbindRead();

                    File f = new File(iconDir, id.getNamespace() + "__" + id.getPath() + ".png");
                    img.writeToFile(f);

                    String name = stack.getHoverName().getString().replace("\\", "\\\\").replace("\"", "\\\"");
                    itemsJson.add("  {\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"icon\":\""
                            + id.getNamespace() + "__" + id.getPath() + ".png\"}");
                    count++;
                } catch (Exception ignored) {

                }
            }
        } finally {
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }

        try {
            Files.write(new File(outDir, "items.json").toPath(),
                    ("[\n" + String.join(",\n", itemsJson) + "\n]").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        final int c = count;
        src.sendSuccess(() -> Component.literal("§a[Voltex] Done: " + c
                + " icons → " + outDir.getAbsolutePath()), false);
    }

    private static void renderToTarget(Minecraft mc, TextureTarget target, ItemStack stack) {
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.viewport(0, 0, ICON_SIZE, ICON_SIZE);

        Matrix4f proj = new Matrix4f().setOrtho(0.0f, 16.0f, 16.0f, 0.0f, -10000.0f, 10000.0f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.ORTHOGRAPHIC_Z);

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        gg.renderItem(stack, 0, 0);
        gg.flush();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private IconExporter() {}
}
