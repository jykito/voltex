package com.jykito.industrialcore.block.entity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jykito.industrialcore.menu.RecipeExporterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class RecipeExporterBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public RecipeExporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECIPE_EXPORTER_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.recipe_exporter"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RecipeExporterMenu(id, inventory, this);
    }

    public void exportRecipe(ServerPlayer player, boolean shapeless) {
        if (level == null || level.getServer() == null) return;

        ItemStack result = itemHandler.getStackInSlot(9);
        if (result.isEmpty()) {
            msg(player, "Place the result item in the right slot.", ChatFormatting.RED);
            return;
        }

        JsonObject root;
        try {
            root = shapeless ? buildShapeless(result) : buildShaped(result);
        } catch (IllegalStateException e) {
            msg(player, e.getMessage(), ChatFormatting.RED);
            return;
        }
        if (root == null) {
            msg(player, "Grid is empty — add at least one ingredient.", ChatFormatting.RED);
            return;
        }

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
        var resKey = ForgeRegistries.ITEMS.getKey(result.getItem());
        String name = resKey == null ? "recipe" : resKey.getPath();

        try {
            MinecraftServer server = level.getServer();
            Path packRoot = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("ic_exported");
            Path recipesDir = packRoot.resolve("data/industrial_core/recipes");
            Files.createDirectories(recipesDir);

            Path mcmeta = packRoot.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                Files.writeString(mcmeta,
                        "{\"pack\":{\"pack_format\":15,\"description\":\"Industrial Core exported recipes\"}}");
            }

            Path file = recipesDir.resolve(name + ".json");
            Files.writeString(file, json);

            msg(player, "Recipe saved: " + name + ".json", ChatFormatting.GREEN);
            msg(player, "Run /reload to apply (datapack ic_exported).", ChatFormatting.GRAY);
        } catch (Exception e) {
            msg(player, "Write error: " + e.getMessage(), ChatFormatting.RED);
        }
    }

    private JsonObject buildShaped(ItemStack result) {
        char[][] cells = new char[3][3];
        LinkedHashMap<String, Character> letters = new LinkedHashMap<>();
        LinkedHashMap<Character, ItemStack> letterStacks = new LinkedHashMap<>();
        char next = 'A';
        boolean any = false;
        int minR = 3, maxR = -1, minC = 3, maxC = -1;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack s = itemHandler.getStackInSlot(r * 3 + c);
                if (s.isEmpty()) { cells[r][c] = ' '; continue; }
                String sig = signature(s);
                Character ch = letters.get(sig);
                if (ch == null) { ch = next++; letters.put(sig, ch); letterStacks.put(ch, s.copy()); }
                cells[r][c] = ch;
                any = true;
                minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                minC = Math.min(minC, c); maxC = Math.max(maxC, c);
            }
        }
        if (!any) return null;

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");
        JsonArray pattern = new JsonArray();
        for (int r = minR; r <= maxR; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = minC; c <= maxC; c++) row.append(cells[r][c]);
            pattern.add(row.toString());
        }
        root.add("pattern", pattern);

        JsonObject key = new JsonObject();
        for (var e : letterStacks.entrySet()) {
            key.add(String.valueOf(e.getKey()), ingredientJson(e.getValue()));
        }
        root.add("key", key);
        root.add("result", resultJson(result));
        return root;
    }

    private JsonObject buildShapeless(ItemStack result) {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < 9; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ingredients.add(ingredientJson(s));
        }
        if (ingredients.isEmpty()) return null;
        if (ingredients.size() > 9) throw new IllegalStateException("Too many ingredients for shapeless.");

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        root.add("ingredients", ingredients);
        root.add("result", resultJson(result));
        return root;
    }

    private JsonObject resultJson(ItemStack result) {
        JsonObject res = new JsonObject();
        res.addProperty("item", idOf(result));
        if (result.getCount() > 1) res.addProperty("count", result.getCount());
        if (result.hasTag()) res.addProperty("nbt", result.getTag().toString());
        return res;
    }

    private JsonObject ingredientJson(ItemStack s) {
        JsonObject ing = new JsonObject();
        if (s.hasTag()) {
            ing.addProperty("type", "forge:nbt");
            ing.addProperty("item", idOf(s));
            ing.addProperty("nbt", s.getTag().toString());
        } else {
            ing.addProperty("item", idOf(s));
        }
        return ing;
    }

    private String signature(ItemStack s) {
        return idOf(s) + "|" + (s.hasTag() ? s.getTag().toString() : "");
    }

    private String idOf(ItemStack s) {
        var k = ForgeRegistries.ITEMS.getKey(s.getItem());
        return k == null ? "minecraft:air" : k.toString();
    }

    private void msg(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }
}
