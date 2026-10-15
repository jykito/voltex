package com.jykito.industrialcore.block.entity;
import com.jykito.industrialcore.block.ModBlocks;

import com.jykito.industrialcore.block.custom.CryoCasingBlock;
import com.jykito.industrialcore.block.custom.CryoCoreBlock;
import com.jykito.industrialcore.block.custom.CryoGlassBlock;
import com.jykito.industrialcore.block.custom.RefrigerationChamberBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.DepthModuleItem;
import com.jykito.industrialcore.menu.CryoChamberMenu;
import com.jykito.industrialcore.recipe.CachedRecipe;
import com.jykito.industrialcore.recipe.CryoRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cryo chamber core.
 *
 * <h2>The point</h2>
 * Heat here is <b>fuel</b>, not a by-product: the absorption principle, like gas refrigerators.
 * The chain is {@code FE -> thermal station -> heat -> heat pipes -> cryo chamber -> cold}.
 * <p>
 * The chamber is not a machine but an <b>installation</b>. It accumulates {@link #depth DEPTH}, and depth is
 * the real resource: it builds up over minutes of steady supply and is lost faster than it grows.
 * That is why the chamber is never switched off.
 *
 * <h2>How depth is computed</h2>
 * Every tick the heat buffer is zeroed and its contents are read as the inflow for that tick. Inflow
 * above the current depth makes depth grow, below makes it fall. Equilibrium is exactly where
 * depth equals inflow, so the stage table reads directly in "heat per tick".
 * <p>
 * Zeroing the buffer also keeps it empty, and a heat pipe pumps into a consumer by free
 * space; if the buffer stayed full, the supply would choke.
 *
 *
 * <h2>No failure on purpose</h2>
 * A supply drop just pauses the work, progress is kept. A penalty for interruption
 * (losing the batch, cracking panels) was discussed and <b>rejected</b>: explosions are already taken by the magnet
 * quench, and a second failure of the same kind would make all the mod's multiblocks feel alike.
 * Do not bring it back.
 */
public class CryoCoreBlockEntity extends BlockEntity implements MenuProvider {

    // ---- structure (tier 2 differs only in these numbers) ----
    private static final int DEPTH_D = 4, HEIGHT = 4, HALF_WIDTH = 2;
    private static final int FRIDGE_D = 2, FRIDGE_S = 0;
    private static final int CHECK_INTERVAL = 20;

    // ---- slots ----
    private static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1, SLOT_ROD_FIRST = 2, SLOT_ROD_LAST = 4;
    /** Depth module: permission to go deeper. Not consumed, a higher one overrides a lower one. */
    private static final int SLOT_MODULE = 5;

    // ---- stages: depth threshold and coolant usage per tick ----
    /**
     * The thresholds are tied to what can actually be delivered. A full thermal station tower on cryogenic
     * heat exchangers gives 1920 heat per tick, and its throughput with fifteen radiators is 1900.
     * Both limits meet almost at one point, so the top stage sits at 1800: the endgame is
     * a finished tower, not "place a second station".
     */
    private static final int[] STAGE_DEPTH   = {200, 800, 1800};
    private static final int[] STAGE_COOLANT = { 10,  40,  120};
    public  static final int MAX_DEPTH = 1800;

    /** Depth grows by one per tick and is lost three times faster: a supply failure must be costly. */
    private static final int DEPTH_RISE = 1, DEPTH_FALL = 3;

    private static final int TANK_CAP = 8000;
    private static final int ENERGY_CAPACITY = 200_000;
    private static final int MAX_HEAT = 20_000;

    /**
     * How much wear accumulates per one point of rod durability. The accumulation itself goes with the square
     * of the stage, so depth is the price: a T1 rod on the third stage lasts about four minutes,
     * and on the first one for hours.
     */
    private static final int WEAR_PER_POINT = 40;

    private final CachedRecipe<CryoRecipe> recipeCache = new CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Output is ALLOWED here on purpose: the machine puts the result through this same storage, and blocking
            // it at the storage level would silently eat the finished item. Hands and automation cannot
            // put into it; that is caught in the menu (mayPlace) and in automationHandler.
            return switch (slot) {
                case SLOT_INPUT, SLOT_OUTPUT -> !isRod(stack) && !(stack.getItem() instanceof DepthModuleItem);
                case SLOT_MODULE             -> stack.getItem() instanceof DepthModuleItem;
                default                      -> isRod(stack);
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private static boolean isRod(ItemStack s) {
        return s.getItem() == ModItems.CRYO_ROD.get()
            || s.getItem() == ModItems.CRYO_ROD_T2.get()
            || s.getItem() == ModItems.CRYO_ROD_T3.get();
    }

    /** The rod speeds up the work, the better the rod the more. */
    private static float rodBonus(ItemStack s) {
        if (s.getItem() == ModItems.CRYO_ROD_T3.get()) return 1.00f;
        if (s.getItem() == ModItems.CRYO_ROD_T2.get()) return 0.50f;
        if (s.getItem() == ModItems.CRYO_ROD.get())    return 0.25f;
        return 0f;
    }

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == SLOT_OUTPUT) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(ENERGY_CAPACITY, ENERGY_CAPACITY);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    /** Coolant in. */
    public final FluidTank coolantTank = new FluidTank(TANK_CAP,
            fs -> fs.getFluid() == ModFluids.SOURCE_COOLANT.get()) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };
    /** Spent coolant out; the enricher refreshes it (enrich/coolant_regen: spent coolant + lapis dust). */
    public final FluidTank hotTank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };
    /** Liquid product: the third stage gives superfluid condensate. */
    public final FluidTank productTank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };

    private void markTankChanged() {
        recipeCache.markDirty();
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** From outside: we accept coolant, give out spent coolant and product. */
    private final IFluidHandler combinedFluid = new IFluidHandler() {
        @Override public int getTanks() { return 3; }
        @Override public @NotNull FluidStack getFluidInTank(int t) {
            return t == 0 ? coolantTank.getFluid() : t == 1 ? hotTank.getFluid() : productTank.getFluid();
        }
        @Override public int getTankCapacity(int t) { return TANK_CAP; }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack fs) { return t == 0 && coolantTank.isFluidValid(fs); }
        @Override public int fill(FluidStack resource, FluidAction action) { return coolantTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack out = productTank.drain(resource, action);
            return out.isEmpty() ? hotTank.drain(resource, action) : out;
        }
        @Override public @NotNull FluidStack drain(int max, FluidAction action) {
            FluidStack out = productTank.drain(max, action);
            return out.isEmpty() ? hotTank.drain(max, action) : out;
        }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    // ---- heat ----
    /** Inflow buffer per tick. Zeroed every tick, so the pipe always pumps at full rate. */
    private int heatAccum = 0;
    /** Inflow of the previous tick: what the player sees and what sets the depth. */
    private int heatInflow = 0;

    private final com.jykito.industrialcore.heat.IHeatHandler heatHandler = new com.jykito.industrialcore.heat.IHeatHandler() {
        @Override public int getHeat() { return heatAccum; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heatAccum);
            if (!simulate && accepted > 0) { heatAccum += accepted; setChanged(); }
            return accepted;
        }
    };
    private LazyOptional<com.jykito.industrialcore.heat.IHeatHandler> lazyHeat = LazyOptional.empty();

    // ---- state ----
    private int checkTimer = 0;
    private boolean formed = false;
    /** Stage of the previous tick, only for the advancement (not saved). */
    private int lastStage = 0;
    private int depth = 0;
    private int progress = 0;
    private int maxProgress = 400;
    private int rodWear = 0;

    protected final ContainerData data;

    public CryoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYO_CORE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> depth;
                    case 7 -> heatInflow;
                    case 8 -> formed ? 1 : 0;
                    case 9 -> allowedStage();
                    default -> 0;
                };
            }
            @Override
            public void set(int i, int v) {
                switch (i) {
                    case 0 -> progress = v;
                    case 1 -> maxProgress = v;
                    case 6 -> depth = v;
                    case 7 -> heatInflow = v;
                    case 8 -> formed = v != 0;
                    default -> { }
                }
            }
            @Override public int getCount() { return 10; }
        };
    }

    public boolean isFormed()  { return formed; }
    public int getDepth()      { return depth; }
    public int getHeatInflow() { return heatInflow; }

    /** Stage by current depth: 0 means the chamber cannot do anything yet. */
    public int getStage() { return stageFor(depth); }

    /** Up to which stage the inserted module allows. Without a module, only the first. */
    public int allowedStage() {
        ItemStack m = itemHandler.getStackInSlot(SLOT_MODULE);
        return m.getItem() instanceof DepthModuleItem mod ? mod.getTier() : 1;
    }

    private static int stageFor(int d) {
        for (int i = STAGE_DEPTH.length - 1; i >= 0; i--)
            if (d >= STAGE_DEPTH[i]) return i + 1;
        return 0;
    }

    public static int depthForStage(int stage) {
        return stage >= 1 && stage <= STAGE_DEPTH.length ? STAGE_DEPTH[stage - 1] : 0;
    }

    /** Coolant usage per tick at this stage; JEI shows it with the same number. */
    public static int coolantPerTick(int stage) {
        return stage >= 1 && stage <= STAGE_COOLANT.length ? STAGE_COOLANT[stage - 1] : STAGE_COOLANT[0];
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CryoCoreBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        // Inflow per tick and zeroing the buffer: keep the space free, otherwise the pipe stops pumping.
        heatInflow = heatAccum;
        heatAccum = 0;

        if (++checkTimer >= CHECK_INTERVAL) {
            checkTimer = 0;
            boolean nowFormed = checkStructure(level, pos, state) == null;
            if (nowFormed && !formed)
                com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.CRYO_FORMED);
            if (nowFormed != formed) { formed = nowFormed; setChanged(); }
            linkCasings(level, pos, state, nowFormed);
        }

        // An unformed chamber loses depth at once: there is nothing to hold it.
        if (!formed) {
            if (depth > 0) { depth = Math.max(0, depth - DEPTH_FALL * 4); setChanged(); }
            progress = 0;
            setRunning(level, pos, state, false);
            return;
        }

        // Depth follows inflow but stops at the module cap: without permission the chamber does not go deeper
        // however much heat you pour in. That is exactly what cannot be bypassed by adding more boilers.
        int cap = depthForStage(allowedStage());
        if (heatInflow > depth)      depth = Math.min(cap, depth + DEPTH_RISE);
        else if (heatInflow < depth) depth = Math.max(0, depth - DEPTH_FALL);
        if (depth > cap) depth = cap;   // the module was removed while running

        int stage = getStage();
        if (stage == 3 && lastStage != 3)   // advancement "At the Very Bottom": only at the moment of transition, not every tick
            com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.CRYO_STAGE_3);
        lastStage = stage;
        CryoRecipe recipe = findRecipe(level, stage);

        boolean working = false;
        if (recipe != null) {
            int coolantPerTick = STAGE_COOLANT[Math.max(0, recipe.getRequiredStage() - 1)];
            int energyPerTick  = recipe.getEnergyCostPerTick();

            this.maxProgress = Math.max(1, (int)(recipe.getProcessingTime() / rodSpeedFactor()));

            boolean deepEnough = stage >= recipe.getRequiredStage();
            boolean hasCoolant = coolantTank.getFluidAmount() >= coolantPerTick
                    && hotTank.getSpace() >= coolantPerTick;
            boolean hasEnergy  = energyStorage.getEnergyStored() >= energyPerTick;

            // If any single condition fails, the work just stops and progress stays where it is.
            // There is no penalty for a drop on purpose: explosions are already taken by the magnet quench, and a second
            // failure of the same kind would make all the mod's multiblocks alike.
            if (deepEnough && hasCoolant && hasEnergy) {
                energyStorage.consumeEnergy(energyPerTick);
                cycleCoolant(coolantPerTick);
                wearRods(level, stage);
                progress++;
                working = true;
                if (progress >= maxProgress) { craft(recipe); progress = 0; }
            }
        } else {
            progress = 0;
        }

        setRunning(level, pos, state, working);
        setChanged();
    }

    /** Coolant passes through the chamber and leaves hot; the enricher refreshes it. */
    private void cycleCoolant(int amount) {
        coolantTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        hotTank.fill(new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Rods are heat exchangers inside: they speed up the work and wear out faster the deeper the chamber is. */
    private float rodSpeedFactor() {
        float f = 1f;
        for (int s = SLOT_ROD_FIRST; s <= SLOT_ROD_LAST; s++)
            f += rodBonus(itemHandler.getStackInSlot(s));
        return f;
    }

    private void wearRods(Level level, int stage) {
        rodWear += Math.max(1, stage * stage); // the third stage wears them nine times faster than the first
        if (rodWear < WEAR_PER_POINT) return;
        rodWear -= WEAR_PER_POINT;

        // ALL inserted rods wear at once: three rods give four times the speed, but also
        // wear out three times faster. Otherwise filling every slot would be a free win.
        for (int s = SLOT_ROD_FIRST; s <= SLOT_ROD_LAST; s++) {
            ItemStack rod = itemHandler.getStackInSlot(s);
            if (rod.isEmpty()) continue;
            if (rod.hurt(1, level.random, null)) itemHandler.setStackInSlot(s, ItemStack.EMPTY);
        }
    }

    private void setRunning(Level level, BlockPos pos, BlockState state, boolean running) {
        boolean lit = MachineLit.tick(this, level, pos, state, CryoCoreBlock.LIT, running);
        lightFridges(level, pos, state.getValue(CryoCoreBlock.FACING), lit);
    }

    @Nullable
    private CryoRecipe findRecipe(Level level, int stage) {
        return recipeCache.get(() -> {
            ItemStack in = itemHandler.getStackInSlot(SLOT_INPUT);
            if (in.isEmpty()) return null;
            for (CryoRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRYO_FREEZING_TYPE.get())) {
                if (!r.inputMatches(in)) continue;
                if (r.hasItemResult() && !fitsOutput(r.getResult())) continue;
                if (r.hasFluidResult()) {
                    FluidStack out = r.makeFluidResult();
                    if (!productTank.isEmpty() && !productTank.getFluid().isFluidEqual(out)) continue;
                    if (productTank.getSpace() < out.getAmount()) continue;
                }
                return r;
            }
            return null;
        });
    }

    private boolean fitsOutput(ItemStack result) {
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(CryoRecipe recipe) {
        itemHandler.extractItem(SLOT_INPUT, recipe.getIngredientCount(), false);
        if (recipe.hasItemResult())
            itemHandler.insertItem(SLOT_OUTPUT, recipe.getResult().copy(), false);
        if (recipe.hasFluidResult())
            productTank.fill(recipe.makeFluidResult(), IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Tells the casing where the core is, so heat can be fed into any floor or ceiling block.
     * <p>
     * That is about fifty faces outside instead of the core's six, and a deep stage needs several pipe
     * lines at once. Only two casing layers are visited, once every {@link #CHECK_INTERVAL} ticks; the link
     * is rewritten every time, so a replaced block is picked up by itself.
     */
    /** Broken core: rods, depth module, input and output fall out; the tanks are lost, as with the other machines. */
    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!s.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        }
    }

    private void linkCasings(Level level, BlockPos pos, BlockState state, boolean formedNow) {
        Direction facing = state.getValue(CryoCoreBlock.FACING);
        for (int h : new int[]{0, HEIGHT}) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // the core itself
                    BlockEntity be = level.getBlockEntity(local(pos, facing, d, s, h));
                    if (!(be instanceof CryoCasingBlockEntity casing)) continue;
                    if (formedNow) casing.setCore(pos); else casing.clearCore();
                }
            }
        }
    }

    // Panel frosting by stage was removed. Tinting was tried and did not and will not work:
    // tint MULTIPLIES the colour, so it can only darken, and frost has to make the glass whiter.
    // If we come back to it, only as a layer over the panel with a separate frost texture.

    /** The refrigerator column lights up together with the core. */
    private void lightFridges(Level level, BlockPos pos, Direction facing, boolean lit) {
        for (int h = 1; h < HEIGHT; h++) {
            BlockPos p = local(pos, facing, FRIDGE_D, FRIDGE_S, h);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof RefrigerationChamberBlock
                    && st.getValue(RefrigerationChamberBlock.LIT) != lit)
                level.setBlock(p, st.setValue(RefrigerationChamberBlock.LIT, lit), 3);
        }
    }

    /** The first mismatch with the structure description, or null if everything is in place. */
    @Nullable
    public Component firstMismatch(Level level, BlockPos pos, BlockState state) {
        return checkStructure(level, pos, state);
    }

    /** Position in the core's local axes: d inwards, s sideways, h up. */
    private static BlockPos local(BlockPos core, Direction facing, int d, int s, int h) {
        Direction back  = facing.getOpposite();
        Direction right = facing.getClockWise();
        return core.relative(back, d).relative(right, s).above(h);
    }

    @Nullable
    private Component checkStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(CryoCoreBlock.FACING);

        for (int h = 0; h <= HEIGHT; h++) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // the core itself

                    BlockPos p = local(pos, facing, d, s, h);
                    Part want = partAt(d, s, h);
                    if (!fits(want, level.getBlockState(p)))
                        return Component.empty().append(want.label()).append(" @ " + p.getX() + ", " + p.getY() + ", " + p.getZ());
                }
            }
        }
        return null;
    }

    /** What must stand in this cell. One layout description for both the check and the wand build. */
    private enum Part {
        CASING, GLASS, FRIDGE, AIR;

        /** Name shown to the player in the "Missing: ..." message. */
        Component label() {
            return switch (this) {
                case CASING -> ModBlocks.CRYO_CASING.get().getName();
                case GLASS  -> ModBlocks.CRYO_GLASS.get().getName();
                case FRIDGE -> ModBlocks.REFRIGERATION_CHAMBER.get().getName();
                case AIR    -> Component.translatable("msg.industrial_core.cryo.empty_space");
            };
        }
    }

    /** Whether the block fits the cell. A shared criterion for the check and for the wand build. */
    private static boolean fits(Part want, BlockState st) {
        return switch (want) {
            case CASING -> st.getBlock() instanceof CryoCasingBlock;
            case GLASS  -> st.getBlock() instanceof CryoGlassBlock;
            case FRIDGE -> st.getBlock() instanceof RefrigerationChamberBlock;
            case AIR    -> st.isAir();
        };
    }

    private static Part partAt(int d, int s, int h) {
        if (h == 0 || h == HEIGHT) return Part.CASING;
        if (d == 0 || d == DEPTH_D || s == -HALF_WIDTH || s == HALF_WIDTH) return Part.GLASS;
        if (d == FRIDGE_D && s == FRIDGE_S) return Part.FRIDGE;
        return Part.AIR;
    }

    /**
     * Builds the chamber around the core with the structure wand: in creative for free and over everything, in survival from the inventory and only into empty cells.
     * <p>
     * The layout comes from {@link #partAt}, the same one the check uses. Otherwise the build and
     * the validator would drift apart over time, and the wand would place what the chamber does not accept.
     */
    public static void buildStructure(Level level, BlockPos corePos, Direction facing, WandBuild wb) {
        for (int h = 0; h <= HEIGHT; h++) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // the core itself
                    BlockPos p = local(corePos, facing, d, s, h);
                    Part part = partAt(d, s, h);
                    if (fits(part, level.getBlockState(p))) continue;
                    // in creative the wand clears and overwrites everything, as before;
                    // in survival it does not break other blocks: it only places into empty cells and from the inventory
                    if (part == Part.AIR) {
                        if (wb.free()) wb.overwrite(level, p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        else wb.markBlocked();
                        continue;
                    }
                    BlockState put = switch (part) {
                        case CASING -> com.jykito.industrialcore.block.ModBlocks.CRYO_CASING.get().defaultBlockState();
                        case GLASS  -> com.jykito.industrialcore.block.ModBlocks.CRYO_GLASS.get().defaultBlockState();
                        default     -> com.jykito.industrialcore.block.ModBlocks.REFRIGERATION_CHAMBER.get().defaultBlockState();
                    };
                    // flag 3 inside: neighbours get an update, so panel and casing connections
                    // come together by themselves, without a second pass
                    if (wb.free()) wb.overwrite(level, p, put);
                    else wb.place(level, p, put);
                }
            }
        }
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.cryo_core"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CryoChamberMenu(id, inv, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        if (cap == com.jykito.industrialcore.heat.HeatCapability.HEAT) return lazyHeat.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyFluidHandler  = LazyOptional.of(() -> combinedFluid);
        lazyHeat          = LazyOptional.of(() -> heatHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyHeat.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("coolantTank", coolantTank.writeToNBT(new CompoundTag()));
        tag.put("hotTank",     hotTank.writeToNBT(new CompoundTag()));
        tag.put("productTank", productTank.writeToNBT(new CompoundTag()));
        tag.putBoolean("formed", formed);
        tag.putInt("depth", depth);
        tag.putInt("progress", progress);
        tag.putInt("rodWear", rodWear);
        tag.putInt("heatInflow", heatInflow);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // ⚠️ deserializeNBT takes the slot count from NBT, not from the code. Chambers saved before
        // the module slot existed bring Size=5 and shrink the storage back; the tick then
        // crashes on the sixth slot. We drop Size, so the size stays as in the code and the items
        // go to their own indices. The same trap waits for any machine whose
        // slot list grew.
        CompoundTag inv = tag.getCompound("inventory").copy();
        inv.remove("Size");
        itemHandler.deserializeNBT(inv);
        energyStorage.setEnergy(tag.getInt("energy"));
        coolantTank.readFromNBT(tag.getCompound("coolantTank"));
        hotTank.readFromNBT(tag.getCompound("hotTank"));
        productTank.readFromNBT(tag.getCompound("productTank"));
        formed = tag.getBoolean("formed");
        depth = tag.getInt("depth");
        progress = tag.getInt("progress");
        rodWear = tag.getInt("rodWear");
        heatInflow = tag.getInt("heatInflow");
    }
}
