package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;

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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
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
        var resKey = BuiltInRegistries.ITEM.getKey(result.getItem());
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

    private com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> jsonOps() {
        return level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
    }

    /** 1.21 result format: {"id", "count", "components"}. */
    private JsonObject resultJson(ItemStack result) {
        return ItemStack.CODEC.encodeStart(jsonOps(), result).getOrThrow().getAsJsonObject();
    }

    private JsonObject ingredientJson(ItemStack s) {
        if (s.getComponentsPatch().isEmpty()) {
            JsonObject ing = new JsonObject();
            ing.addProperty("item", idOf(s));
            return ing;
        }
        // exact components, like the strict forge:nbt ingredient of 1.20.1
        var ing = net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true, s);
        return net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(jsonOps(), ing).getOrThrow().getAsJsonObject();
    }

    private String signature(ItemStack s) {
        return idOf(s) + "|" + (s.getComponentsPatch().isEmpty() ? "" : s.getComponentsPatch().toString());
    }

    private String idOf(ItemStack s) {
        var k = BuiltInRegistries.ITEM.getKey(s.getItem());
        return k == null ? "minecraft:air" : k.toString();
    }

    private void msg(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
    }
}
