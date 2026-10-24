package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.custom.CryoCoreBlock;
import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.custom.LateMachineCoreBlock;
import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.multiblock.WandBuild;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DEV wand. Two functions, both on right click:
 * <ul>
 *   <li>on a multiblock core (magnet, cryo chamber, late machines, industrial miner): completes the structure;
 *       free in creative, in survival the blocks come from the inventory;</li>
 *   <li>on any other block: marks a corner of an area. Two corners in a row set a box
 *       that the client draws as a frame through walls ({@code StructureSelectionRenderer}).
 *       Useful for estimating multiblock dimensions right in the world.</li>
 * </ul>
 * Sneak plus right click clears the selection.
 */
public class StructureWandItem extends Item {

    private static final String KEY_A = "SelA", KEY_B = "SelB";

    public StructureWandItem(Properties props) { super(props); }

    @Nullable
    public static BlockPos getCorner(ItemStack stack, boolean first) {
        CompoundTag tag = stack.getTag();
        String key = first ? KEY_A : KEY_B;
        return tag != null && tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
    }

    private static void clear(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(KEY_A);
        tag.remove(KEY_B);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        // click on a multiblock core: complete the structure. In creative the blocks are free,
        // in survival they come from the inventory (WandBuild); the multiblock builds the layout itself and checks against the same one
        boolean magnet = state.getBlock() instanceof ElectromagnetCoreBlock;
        boolean cryo = state.getBlock() instanceof CryoCoreBlock;
        boolean late = state.getBlock() instanceof LateMachineCoreBlock;
        boolean miner = state.getBlock() instanceof com.jykito.industrialcore.block.custom.IndustrialMinerCoreBlock;
        boolean addon = state.getBlock() instanceof com.jykito.industrialcore.api.WandAssemblable;   // multiblocks of other mods
        if ((magnet || cryo || late || miner || addon) && player != null) {
            if (!level.isClientSide) {
                WandBuild wb = new WandBuild(player);
                if (addon)
                    ((com.jykito.industrialcore.api.WandAssemblable) state.getBlock()).buildWithWand(level, pos, state, wb);
                else if (magnet)
                    ElectromagnetCoreBlockEntity.buildStructure(level, pos, state.getValue(ElectromagnetCoreBlock.FACING), wb);
                else if (cryo)
                    CryoCoreBlockEntity.buildStructure(level, pos, state.getValue(CryoCoreBlock.FACING), wb);
                else if (miner)
                    com.jykito.industrialcore.miner.IndustrialMinerStructure.build(level, pos,
                            state.getValue(com.jykito.industrialcore.block.custom.IndustrialMinerCoreBlock.FACING), wb);
                else
                    ((LateMachineCoreBlock) state.getBlock()).pattern().build(level, pos, state.getValue(LateMachineCoreBlock.FACING), wb);
                reportBuild(player, wb);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (player != null && player.isShiftKeyDown()) {
            clear(stack);
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("msg.industrial_core.wand.cleared")
                        .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        CompoundTag tag = stack.getOrCreateTag();
        // the first click sets corner A, the second B, the third starts over
        if (!tag.contains(KEY_A) || tag.contains(KEY_B)) {
            tag.remove(KEY_B);
            tag.putLong(KEY_A, pos.asLong());
            if (!level.isClientSide && player != null)
                player.displayClientMessage(Component.translatable("msg.industrial_core.wand.corner_a",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), true);
        } else {
            tag.putLong(KEY_B, pos.asLong());
            BlockPos a = BlockPos.of(tag.getLong(KEY_A));
            int dx = Math.abs(a.getX() - pos.getX()) + 1;
            int dy = Math.abs(a.getY() - pos.getY()) + 1;
            int dz = Math.abs(a.getZ() - pos.getZ()) + 1;
            if (!level.isClientSide && player != null)
                player.displayClientMessage(Component.translatable("msg.industrial_core.wand.size",
                        dx, dy, dz, dx * dy * dz).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void reportBuild(Player player, WandBuild r) {
        if (r.placed() == 0 && r.blocked() == 0 && r.missing().isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.industrial_core.wand.late_complete")
                    .withStyle(ChatFormatting.GREEN), true);
            return;
        }
        if (r.placed() > 0)
            player.displayClientMessage(Component.translatable("msg.industrial_core.wand.late_placed", r.placed())
                    .withStyle(ChatFormatting.GREEN), false);
        if (!r.missing().isEmpty()) {
            MutableComponent list = Component.empty();
            boolean first = true;
            for (Map.Entry<Block, Integer> e : r.missing().entrySet()) {
                if (!first) list.append(", ");
                list.append(e.getKey().getName()).append(" ×" + e.getValue());
                first = false;
            }
            player.displayClientMessage(Component.translatable("msg.industrial_core.wand.late_missing", list)
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        if (r.blocked() > 0)
            player.displayClientMessage(Component.translatable("msg.industrial_core.wand.late_blocked", r.blocked())
                    .withStyle(ChatFormatting.RED), false);
    }

    /**
     * Click on air: sneaking clears, not sneaking EXPORTS the selection to a file.
     * The file goes to the instance root as {@code voltex_structure.txt}: a layer-by-layer schema
     * with a legend, so the structure can be read outside and put into code.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            clear(stack);
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("msg.industrial_core.wand.cleared")
                        .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        BlockPos a = getCorner(stack, true), b = getCorner(stack, false);
        if (a == null || b == null) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide) {
            String file = dump(level, a, b);
            player.displayClientMessage(Component.translatable("msg.industrial_core.wand.exported", file)
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Writes the schema layer by layer from bottom to top. Returns the file name. */
    private static String dump(Level level, BlockPos a, BlockPos b) {
        int x0 = Math.min(a.getX(), b.getX()), x1 = Math.max(a.getX(), b.getX());
        int y0 = Math.min(a.getY(), b.getY()), y1 = Math.max(a.getY(), b.getY());
        int z0 = Math.min(a.getZ(), b.getZ()), z1 = Math.max(a.getZ(), b.getZ());

        // a symbol for EACH unique state: rotation matters for directional blocks
        Map<String, Character> legend = new LinkedHashMap<>();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder out = new StringBuilder();

        out.append("Voltex structure dump\n")
           .append("size ").append(x1 - x0 + 1).append(" x ").append(y1 - y0 + 1)
           .append(" x ").append(z1 - z0 + 1).append("\n")
           .append("from ").append(x0).append(',').append(y0).append(',').append(z0)
           .append("  to ").append(x1).append(',').append(y1).append(',').append(z1).append("\n")
           .append("rows run north->south, columns west->east\n\n");

        StringBuilder layers = new StringBuilder();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            layers.append("layer y=").append(y).append(y == y0 ? "  (bottom)" : "").append('\n');
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    BlockState st = level.getBlockState(p.set(x, y, z));
                    String key = st.isAir() ? "" : describe(st);
                    char c;
                    if (key.isEmpty()) c = '.';
                    else {
                        Character existing = legend.get(key);
                        if (existing == null) {
                            c = chars.charAt(Math.min(legend.size(), chars.length() - 1));
                            legend.put(key, c);
                        } else c = existing;
                    }
                    layers.append(c);
                }
                layers.append('\n');
            }
            layers.append('\n');
        }

        out.append("legend\n  . = air\n");
        for (Map.Entry<String, Character> e : legend.entrySet())
            out.append("  ").append(e.getValue()).append(" = ").append(e.getKey()).append('\n');
        out.append('\n').append(layers);

        Path path = FMLPaths.GAMEDIR.get().resolve("voltex_structure.txt");
        try {
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "write error: " + ex.getMessage();
        }
        return path.getFileName().toString();
    }

    /** block id plus properties that differ from the default, otherwise the rotation would be lost. */
    private static String describe(BlockState st) {
        StringBuilder sb = new StringBuilder(ForgeRegistries.BLOCKS.getKey(st.getBlock()).toString());
        BlockState def = st.getBlock().defaultBlockState();
        StringBuilder props = new StringBuilder();
        for (Property<?> prop : st.getProperties()) {
            if (st.getValue(prop).equals(def.getValue(prop))) continue;
            if (props.length() > 0) props.append(',');
            props.append(prop.getName()).append('=').append(st.getValue(prop));
        }
        if (props.length() > 0) sb.append('[').append(props).append(']');
        return sb.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.structure_wand").withStyle(ChatFormatting.GRAY));
        BlockPos a = getCorner(stack, true), b = getCorner(stack, false);
        if (a != null && b != null) {
            int dx = Math.abs(a.getX() - b.getX()) + 1;
            int dy = Math.abs(a.getY() - b.getY()) + 1;
            int dz = Math.abs(a.getZ() - b.getZ()) + 1;
            tooltip.add(Component.literal(dx + " × " + dy + " × " + dz).withStyle(ChatFormatting.AQUA));
        }
    }
}
