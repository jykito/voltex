package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.menu.RecipeExporterMenu;
import com.jykito.industrialcore.networking.ExportRecipePacket;
import com.jykito.industrialcore.networking.ModMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RecipeExporterScreen extends AbstractContainerScreen<RecipeExporterMenu> {

    private boolean shapeless = false;
    private Button modeButton;

    public RecipeExporterScreen(RecipeExporterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;

        modeButton = Button.builder(modeText(), b -> {
            shapeless = !shapeless;
            modeButton.setMessage(modeText());
        }).bounds(x + 96, y + 15, 72, 18).build();
        addRenderableWidget(modeButton);

        addRenderableWidget(Button.builder(Component.literal("Save"), b ->
                ModMessages.sendToServer(new ExportRecipePacket(menu.blockEntity.getBlockPos(), shapeless))
        ).bounds(x + 96, y + 58, 72, 18).build());
    }

    private Component modeText() {
        return Component.literal(shapeless ? "Type: shapeless" : "Type: grid");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        g.fill(x, y, x + imageWidth, y + 1, 0xFF555555);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                slotBg(g, x + 30 + c * 18, y + 17 + r * 18);

        slotBg(g, x + 124, y + 35);

        g.drawString(this.font, "→", x + 100, y + 39, 0xFF404040, false);

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                slotBg(g, x + 8 + j * 18, y + 84 + i * 18);
        for (int k = 0; k < 9; k++)
            slotBg(g, x + 8 + k * 18, y + 142);
    }

    private void slotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        g.fill(x, y, x + 16, y + 16, 0xFF373737);
    }
}
