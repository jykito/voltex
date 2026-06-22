package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.BlastFurnaceBlock;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.BlastFurnaceMenu;
import com.jykito.industrialcore.recipe.BlastFurnaceRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlastFurnaceBlockEntity extends BlockEntity implements MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<BlastFurnaceRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();
    private final ItemStackHandler itemHandler = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 2) return false;
            if (slot >= 3 && slot <= 6) {

                if (stack.getItem() instanceof DirectionalUpgradeItem dir)
                    return dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PULL
                        || dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PUSH;

                if (!(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.BlastUpgradeItem)) return false;
                for (int s = 3; s <= 6; s++) {
                    if (s == slot) continue;
                    if (getStackInSlot(s).getItem() instanceof com.jykito.industrialcore.item.upgrade.BlastUpgradeItem) return false;
                }
                return true;
            }
            return !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int heat = 0;
    private final int MAX_HEAT = 4000;

    private int progress    = 0;
    private int maxProgress = 300;

    private boolean isReceivingHeat = false;

    protected final ContainerData data;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> BlastFurnaceBlockEntity.this.heat;
                    case 1 -> BlastFurnaceBlockEntity.this.progress;
                    case 2 -> BlastFurnaceBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> BlastFurnaceBlockEntity.this.heat = value;
                    case 1 -> BlastFurnaceBlockEntity.this.progress = value;
                    case 2 -> BlastFurnaceBlockEntity.this.maxProgress = value;
                }
            }
            @Override
            public int getCount() { return 3; }
        };
    }

    public void addHeat(int amount) {
        this.isReceivingHeat = true;
        if (this.heat < MAX_HEAT) {
            this.heat = Math.min(MAX_HEAT, this.heat + amount);
            setChanged();
        }
    }

    public int getHeat() { return heat; }
    public int getMaxHeat() { return MAX_HEAT; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BlastFurnaceMenu(id, inventory, this, this.data);
    }

    private java.util.Optional<BlastFurnaceRecipe> getRecipe(Level level) {
        return java.util.Optional.ofNullable(recipeCache.get(() -> {
            SimpleContainer c = new SimpleContainer(2);
            c.setItem(0, itemHandler.getStackInSlot(0));
            c.setItem(1, itemHandler.getStackInSlot(1));
            return level.getRecipeManager().getRecipeFor(ModRecipes.BLAST_FURNACE_TYPE.get(), c, level).orElse(null);
        }));
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        if (!isReceivingHeat && heat > 0) {
            heat -= 1;
            setChanged();
        }
        isReceivingHeat = false;

        java.util.Optional<BlastFurnaceRecipe> recipe = getRecipe(level);
        boolean isWorking = false;

        if (recipe.isPresent()) {
            BlastFurnaceRecipe r = recipe.get();
            maxProgress = r.getProcessingTime();

            float speedMul = 1.0f;
            int parallel   = 1;
            for (int s = 3; s <= 6; s++) {
                if (itemHandler.getStackInSlot(s).getItem()
                        instanceof com.jykito.industrialcore.item.upgrade.BlastUpgradeItem up) {
                    speedMul = up.getSpeedMultiplier();
                    parallel = up.getParallel();
                    break;
                }
            }
            maxProgress = Math.max(1, (int) (maxProgress * speedMul));

            boolean isHeated   = heat >= r.getRequiredHeat();
            ItemStack result   = r.getResultItem(level.registryAccess());
            ItemStack output   = itemHandler.getStackInSlot(2);

            int ingSlot  = r.ingredientSlot(itemHandler.getStackInSlot(0), itemHandler.getStackInSlot(1));
            int fuelSlot = ingSlot == 0 ? 1 : 0;
            int ingAvail  = itemHandler.getStackInSlot(ingSlot).getCount()  / Math.max(1, r.getIngredientCount());
            int fuelAvail = itemHandler.getStackInSlot(fuelSlot).getCount() / Math.max(1, r.getFuelCount());
            int outSpace  = output.isEmpty()
                    ? result.getMaxStackSize() / Math.max(1, result.getCount())
                    : (output.is(result.getItem())
                        ? (output.getMaxStackSize() - output.getCount()) / Math.max(1, result.getCount()) : 0);
            boolean canFit = ingAvail > 0 && fuelAvail > 0 && outSpace > 0;
            int actualParallel = Math.min(parallel, Math.min(ingAvail, Math.min(fuelAvail, outSpace)));
            if (actualParallel < 1) actualParallel = 1;

            if (isHeated && canFit) {
                progress++;
                isWorking = true;
                setChanged();
                if (progress >= maxProgress) {
                    itemHandler.extractItem(ingSlot,  r.getIngredientCount() * actualParallel, false);
                    itemHandler.extractItem(fuelSlot, r.getFuelCount()       * actualParallel, false);
                    int produced = result.getCount() * actualParallel;
                    if (output.isEmpty()) {
                        ItemStack out = result.copy();
                        out.setCount(produced);
                        itemHandler.setStackInSlot(2, out);
                    } else {
                        output.grow(produced);
                    }
                    progress = 0;
                }
            } else if (progress > 0) {
                progress = 0;
                setChanged();
            }
        } else if (progress > 0) {
            progress = 0;
            setChanged();
        }

        Direction itemPullDir = null, itemPushDir = null;
        for (int s = 3; s <= 6; s++) {
            ItemStack up = itemHandler.getStackInSlot(s);
            if (up.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(up);
                if (d == null) continue;
                if (dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PULL) itemPullDir = d;
                else if (dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PUSH) itemPushDir = d;
            }
        }
        if (itemPullDir != null) {
            final Direction d = itemPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(src -> {
                for (int i = 0; i < src.getSlots(); i++) {
                    ItemStack sim = src.extractItem(i, 64, true);
                    if (sim.isEmpty()) continue;
                    for (int target : new int[]{0, 1}) {
                        int accepted = sim.getCount() - itemHandler.insertItem(target, sim, true).getCount();
                        if (accepted > 0) {
                            itemHandler.insertItem(target, src.extractItem(i, accepted, false), false);
                            return;
                        }
                    }
                }
            });
        }
        if (itemPushDir != null && !itemHandler.getStackInSlot(2).isEmpty()) {
            final Direction d = itemPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(dst ->
                    itemHandler.setStackInSlot(2, ItemHandlerHelper.insertItem(dst, itemHandler.getStackInSlot(2), false)));
        }

        if (state.getValue(BlockStateProperties.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isWorking), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        heat = tag.getInt("heat");
        progress = tag.getInt("progress");
    }
}
