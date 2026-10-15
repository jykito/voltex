package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.CrateBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CrateMenu extends MachineMenu {

    public final CrateBlockEntity blockEntity;
    private final int rows;
    private final int containerSlots;

    public CrateMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CrateBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // ── GUI layout: coordinates from the author's art, shared by the menu and the screen ──
    // The file is 256x315 and displayed whole. Grid step 19 both ways.
    private static final int SLOT_X = 44, STEP = 19;
    public static final int BTN_X = 220, BTN_SIZE = 10;
    public static final int GUI_W = 256, GUI_H = 315;

    /**
     * Where the screen puts the player inventory REGARDLESS of tier: otherwise when opening different
     * crates the bottom half of the window jumps, because the contents sit at different heights in the three pngs.
     * We take invY of the tallest tier (T3), so the others only shift down,
     * into the empty area of the canvas, and nothing goes off the top of the screen.
     */
    public static final int ANCHOR_INV_Y = 204;

    /**
     * Y coordinates and texture per tier. Buttons from top to bottom: restock, dump matching, sort.
     * The texture name lives here too, otherwise the "rows -> tier" mapping would have to be kept
     * as a second switch in the screen and both updated when a tier is added.
     */
    public record Layout(int crateY, int invY, int hotbarY, int restockY, int depositY, int sortY, String tier) {}

    public static Layout layout(int rows) {
        return switch (rows) {
            case 8  -> new Layout(36, 195, 258, 32, 45, 58, "t2");
            case 10 -> new Layout(7,  204, 267,  3, 16, 29, "t3");
            default -> new Layout(24, 145, 208, 20, 33, 46, "t1");   // 6 rows
        };
    }

    public CrateMenu(int id, Inventory inv, CrateBlockEntity be) {
        super(ModMenuTypes.CRATE_MENU.get(), id);
        this.blockEntity = be;
        this.rows = be.getRows();
        this.containerSlots = rows * 9;
        var h = be.getItems();
        Layout l = layout(rows);

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new SlotItemHandler(h, c + r * 9, SLOT_X + c * STEP, l.crateY() + r * STEP));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, SLOT_X + j * STEP, l.invY() + i * STEP));
        for (int j = 0; j < 9; j++)
            addSlot(new Slot(inv, j, SLOT_X + j * STEP, l.hotbarY()));
    }

    public int getRows() { return rows; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();
            if (index < containerSlots) {
                if (!moveItemStackTo(stack, containerSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, containerSlots, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == ret.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return ret;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide()) {
            level.playSound(null, blockEntity.getBlockPos(), SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.6f, 0.75f);
        }
    }

    // ── GUI buttons (standard path: the client sends handleInventoryButtonClick -> the server calls this) ──
    // 0 = sort, 1 = dump matching, 2 = restock
    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStackHandler h = blockEntity.getItems();
        switch (id) {
            case 0 -> sortContents(h);
            case 1 -> depositMatching(player, h);
            case 2 -> restock(player, h);
            default -> { return false; }
        }
        blockEntity.setChanged();
        broadcastChanges();
        return true;
    }

    /** Merge identical stacks and sort by id (by registry name, then by count descending). */
    private static void sortContents(ItemStackHandler h) {
        int n = h.getSlots();
        List<ItemStack> buckets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) {
                boolean merged = false;
                for (ItemStack b : buckets)
                    if (ItemStack.isSameItemSameTags(b, s)) { b.grow(s.getCount()); merged = true; break; }
                if (!merged) buckets.add(s.copy());
            }
            h.setStackInSlot(i, ItemStack.EMPTY);
        }
        buckets.sort(Comparator
                .comparing((ItemStack st) -> ForgeRegistries.ITEMS.getKey(st.getItem()).toString())
                .thenComparingInt(st -> -st.getCount()));
        int slot = 0;
        for (ItemStack b : buckets) {
            int remaining = b.getCount(), max = b.getMaxStackSize();
            while (remaining > 0 && slot < n) {
                int c = Math.min(max, remaining);
                ItemStack out = b.copy();
                out.setCount(c);
                h.setStackInSlot(slot++, out);
                remaining -= c;
            }
        }
    }

    private static boolean crateHas(ItemStackHandler h, ItemStack s) {
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack cs = h.getStackInSlot(i);
            if (!cs.isEmpty() && ItemStack.isSameItemSameTags(cs, s)) return true;
        }
        return false;
    }

    /** Dump from the inventory into the crate everything whose type is already in the crate. */
    private static void depositMatching(Player player, ItemStackHandler h) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty() || !crateHas(h, s)) continue;
            items.set(i, ItemHandlerHelper.insertItemStacked(h, s, false));
        }
    }

    /** Restock from the crate: top up the player's existing stacks to the maximum. */
    private static void restock(Player player, ItemStackHandler h) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack p = items.get(i);
            if (p.isEmpty()) continue;
            int need = p.getMaxStackSize() - p.getCount();
            for (int c = 0; c < h.getSlots() && need > 0; c++) {
                ItemStack cs = h.getStackInSlot(c);
                if (cs.isEmpty() || !ItemStack.isSameItemSameTags(cs, p)) continue;
                ItemStack got = h.extractItem(c, Math.min(need, cs.getCount()), false);
                p.grow(got.getCount());
                need -= got.getCount();
            }
        }
    }
}
