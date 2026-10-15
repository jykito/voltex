package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.custom.MultiblockShellBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.menu.ElectromagnetMenu;
import com.jykito.industrialcore.multiblock.WandBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
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
 * BE of the electromagnet / accelerator core. Validates the 7x7x8 skeleton (by the schema, rotated by facing),
 * sets FORMED. The body (shell, coil, columns) is drawn by ElectromagnetCoreBER when FORMED.
 */
public class ElectromagnetCoreBlockEntity extends BlockEntity implements MenuProvider {

    // Canonical schema (front = north). Layers bottom (Y=0)..top (Y=7); rows north (0)..south (6); columns west (0)..east (6).
    // S = structural, H = coil holder, C = core (anchor, not checked), '.' = empty.
    private static final String[] SCHEMA = {
        // Y=0 (bottom)
        ".SSSSS.","SSSSSSS","SSSSSSS","SSSSSSS","SSSSSSS","SSSSSSS",".SSSSS.",
        // Y=1
        "...C...",".S...S.","..SSS..","..SSS..","..SSS..",".S...S.",".......",
        // Y=2
        ".......",".S...S.",".......","...H...",".......",".S...S.",".......",
        // Y=3
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=4
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=5
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=6
        ".......",".S...S.",".......","...H...",".......",".S...S.",".......",
        // Y=7 (top)
        ".......",".S...S.","..SSS..","..SSS..","..SSS..",".S...S.",".......",
    };
    private static final int SIZE = 7, HEIGHT = 8;
    private static final int CORE_LAYER = 1, CORE_ROW = 0, CORE_COL = 3;

    // ── TEST: magnetic field (pulls entities to the centre) ──
    private static final double PULL_RADIUS = 8.0;
    private static final double PULL_STRENGTH = 0.10;
    private static final double LIFT_MULT = 1.8; // variant 1: extra lift when the entity is BELOW the centre
    private static final double DISINTEGRATE_RADIUS = 1.5; // TEST: touching the coil in the centre = disintegration
    private static final double BURST_RADIUS = 4.5;        // particle spread radius on disintegration
    // Attraction field: radius and strength grow with instability (0 without a coil). The radius is monitored from afar via the console.
    /** Attraction field radius (blocks): 0 without a coil, otherwise 2.5 + instab%*0.14. */
    public static double fieldRadius(float instability, int coilTier) {
        return coilTier <= 0 ? 0.0 : 2.5 + instability * 0.14;
    }

    private boolean formed = false;
    private boolean working = false;   // no accelerator logic yet -> the columns spin slowly
    private int checkCooldown = 0;

    // ── TEST: client spin-up and sound (smooth spin01 -> visuals + hum pitch/volume + column passes) ──
    private static final float IDLE_DEG = 1.2f;   // deg/tick idle
    private static final float WORK_DEG = 9.0f;   // deg/tick at full spin
    private static final float SPIN_EASE = 0.03f; // spin-up/spin-down rate (lower = slower build-up)
    private float spin01 = 0f, spin01Prev = 0f;         // 0..1 spin level (client)
    private float spinAngle = 0f, spinAnglePrev = 0f;   // accumulated angle of the columns/coil (client)
    private static final float INSTAB_SPIN = 0.06f;     // +deg/tick per % of instability

    // ── Coil (inserted with RMB on a holder) + instability ──
    // the coil lives in itemHandler, slot COIL_SLOT (10): a single source of truth (GUI slot + holder)
    private float instability = 0f;               // 0..100 %, synced to the client (field/rotation visuals)
    private float throttle = 0f;                  // 0..1 manual power (slider in the GUI); proportional to instability growth + FE usage
    private float effThrottle = 0f;               // effective power: redstone overrides the manual one if there is a signal
    private int   redstonePower = 0;              // input redstone 0..15 -> power
    private int   lastComparatorSignal = 0;       // cached comparator output (notify only on change)
    private int   driftPhase = 0;                 // phase of the resonance target drift (0..DRIFT_PERIOD)
    private float resonanceTarget = 0f;           // current instability target (drifts within the recipe band), synced to the GUI
    private float resonanceCharge = 0f;           // 0..100 resonance charge; full -> x2
    private float cachedCool = 0f;                 // throttle for the expensive column scan (cooling changes slowly)
    private int coolCd = 0;
    // throttle for the expensive AABB scan of field entities: the list is cached, the pull itself runs every tick (visuals/strength unchanged)
    private java.util.List<net.minecraft.world.entity.Entity> fieldEntities = java.util.Collections.emptyList();
    private int fieldScanCd = 0;
    private static final int FIELD_SCAN_TICKS = 5;
    private static final float RESPONSE_RATE = 0.08f; // fraction of the gap to the target closed per tick: a soft SYMMETRIC response (no dead zone, but no jerks)
    private static final float POWER_SMOOTH  = 0.2f;  // smoothing of the 16-step redstone power -> the slider does not jump and the loop "hunts" less
    private static final int   PHASE_LOCK_COST = 400; // FE/tick for the Phase Lock auto mode (the window holds the target by itself)
    private static final int QUENCH_RADIUS = 32;       // radius of the split wave at 100%
    // Heated antigrav columns dampen instability (heat -> stability): keep the flow without a quench
    private static final double COLUMN_RADIUS = 16.0;  // help radius from the structure centre
    private static final int    MAX_COLUMNS  = 8;      // cap on the columns' contribution (do not trivialize)
    private static final float  COLUMN_COOL  = 0.05f;  // cooling per tick for a full (100% heat) column
    // Spin-up: a powered magnet with a coil climbs to the instability ceiling of its tier; columns pull it down
    private static final float  BASE_QUENCH     = 50f;  // quench threshold WITHOUT pylons (low -> no high spin without cooling)
    private static final float  QUENCH_PER_COOL = 150f; // pylons raise the quench threshold (cooling = safe instability ceiling)
    private static final int    SPIN_UPKEEP = 200;     // FE/tick to sustain the spin at full throttle (proportional to throttle)
    // ── Resonance: drifting target + throttle + charge -> x2 (an active skill instead of a passive band) ──
    private static final int    DRIFT_PERIOD  = 600;   // ticks per target drift cycle (~30s), slower so the auto loop keeps up
    private static final float  RESONANCE_TOL = 3f;    // ±% tolerance: narrow ("percent for percent"), x2 by hand is skill; reliable x2 is the auto-follow upgrade
    private static final float  CHARGE_RISE   = 2.0f;  // charge/tick inside the tolerance (builds faster)
    private static final float  CHARGE_DECAY  = 0.7f;  // charge/tick outside the tolerance (melts lazily, so x2 survives short slips or lag)
    private static final float  RESONANCE_X2  = 85f;   // charge threshold for x2
    // ── Redstone: input = power (0..15), comparator output = resonance error ──
    private static final int    COMP_ON_TARGET = 7;    // 7 = instability exactly on target; >7 overshoot, <7 undershoot
    private static final float  COMP_ERR_SCALE = 3f;   // % of error per comparator step
    // Crafting speed grows with instability -> risk/reward is alive at x1 too (not only for x2)
    private static final float  SPEED_GAIN = 3.0f;     // speed multiplier gain per 100% above the exo gate
    private static final float  SPEED_MAX  = 3.0f;     // speed multiplier cap

    // ── Accelerator machine: 8 inputs (0-7) + output (8) + 2 upgrades (9, 10, locked for now) ──
    private final com.jykito.industrialcore.recipe.CachedRecipe<com.jykito.industrialcore.recipe.AcceleratorRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();
    private final ItemStackHandler itemHandler = new ItemStackHandler(11) {
        @Override protected void onContentsChanged(int slot) {
            recipeCache.markDirty(); setChanged();
            if (slot == COIL_SLOT) syncMagnet();   // the coil changed (including by dragging in the GUI) -> update the BER/field on the client
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 8) return false;                            // output is closed
            if (slot == 9) return stack.getItem() == com.jykito.industrialcore.item.ModItems.PHASE_LOCK_UPGRADE.get(); // upgrade slot: Phase Lock only
            if (slot == COIL_SLOT) return stack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem; // coil slot
            return super.isItemValid(slot, stack);
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot > 7) return stack;             // auto-insert only into inputs 0..7
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 8) return ItemStack.EMPTY;  // auto-extract only from the output
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    // ── Outside, for the port BEs of the bottom platform: the same caps the core exposes to its direct neighbours ──
    public IItemHandler getExternalItemHandler() { return automationHandler; }
    public IFluidHandler getExternalFluidHandler() { return combinedFluidHandler; }
    public net.minecraftforge.energy.IEnergyStorage getExternalEnergyHandler() { return energyStorage; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(1_000_000, 20_000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // 2 tanks, both input (external filling, no draining)
    public final FluidTank tankA = new FluidTank(16_000) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    public final FluidTank tankB = new FluidTank(16_000) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private final IFluidHandler combinedFluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? tankA.getFluid() : tankB.getFluid(); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? tankA.getCapacity() : tankB.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return true; }
        @Override public int fill(FluidStack resource, FluidAction action) {
            int f = tankA.fill(resource, action);
            if (f < resource.getAmount()) {
                FluidStack rest = resource.copy(); rest.shrink(f);
                f += tankB.fill(rest, action);
            }
            return f;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private final ContainerData data;
    private float progress = 0f;      // float for a smooth speed proportional to instability
    private int maxProgress = 1;

    public ElectromagnetCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROMAGNET_CORE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> (int) progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> (int) instability;        // 0..100 %, readability in the GUI
                    case 7 -> working ? 1 : 0;
                    case 8 -> inBandNow ? 1 : 0;        // resonance charge full -> x2 (indicator in the GUI)
                    case 9 -> (int) resonanceTarget;    // 0..100 drifting resonance target
                    case 10 -> (int) resonanceCharge;   // 0..100 resonance charge
                    case 11 -> (int) (effThrottle * 100f); // 0..100 effective power (redstone/slider)
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) { if (i == 0) progress = v; else if (i == 1) maxProgress = v; }
            @Override public int getCount() { return 12; }
        };
    }

    public boolean isWorking() { return working; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (formed) {
            redstonePower = level.getBestNeighborSignal(pos);                  // redstone input -> power
            if (redstonePower > 0) effThrottle += (redstonePower / 15f - effThrottle) * POWER_SMOOTH; // smooth the redstone steps (no jumping)
            else effThrottle = throttle;                                       // manual slider: immediately
            tickInstability(level, pos);  // spin-up and columns first -> fresh instability
            tickProcessing(level);        // craft on fresh instability: exo gate + resonance x2
            pullEntities(level, pos);     // field side effect (pulls towards the centre)
            updateComparator(level, pos); // redstone output: resonance error
            if (--outputCd <= 0) { outputCd = 8; autoOutput(level, pos); } // auto-output into an adjacent interface
        } else {
            working = false;
        }

        if (--checkCooldown > 0) return;
        checkCooldown = formed ? 20 : 40;

        if (formed) {
            Direction f = state.getValue(ElectromagnetCoreBlock.FACING);
            if (checkStructure(level, pos, f)) {
                setShellFormed(level, pos, f, true);   // keep the skeleton hidden (self-sync after relog/update)
                return;
            }
            formed = false;
            setShellFormed(level, pos, f, false);   // make the skeleton visible again
            level.setBlock(pos, state.setValue(ElectromagnetCoreBlock.FORMED, false), 3);
            setChanged();
            return;
        }

        // not formed: try to form in any of the 4 directions, set the facing that was found
        for (Direction f : Direction.Plane.HORIZONTAL) {
            if (checkStructure(level, pos, f)) {
                formed = true;
                com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.MAGNET_FORMED);
                level.setBlock(pos, state.setValue(ElectromagnetCoreBlock.FACING, f)
                        .setValue(ElectromagnetCoreBlock.FORMED, true), 3);
                setShellFormed(level, pos, f, true);   // hide the skeleton, the body takes its place
                setChanged();
                return;
            }
        }
    }

    // ── Coil + instability ──

    public float getInstability() { return instability; }
    /** Result of the current recipe (what is being processed), for the distant controller screen; EMPTY if there is no coil/recipe. */
    public ItemStack getInProgressResult() {
        if (level == null || getCoilTier() == 0) return ItemStack.EMPTY;
        var r = findRecipe(level);
        return r == null ? ItemStack.EMPTY : r.getResult();
    }
    /** Spin-up throttle 0..1, set by the player (MagnetThrottlePacket). */
    public void setThrottle(float t) { throttle = Mth.clamp(t, 0f, 1f); setChanged(); }
    public float getThrottle() { return throttle; }
    public float getResonanceTarget() { return resonanceTarget; }
    public float getResonanceCharge() { return resonanceCharge; }
    // The coil lives in slot 10 of itemHandler, a single source of truth: the GUI slot and the holder write here, all logic reads from here.
    private static final int COIL_SLOT = 10;
    public ItemStack getCoil() { return itemHandler.getStackInSlot(COIL_SLOT); }
    public int getCoilTier() {
        return getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getTier() : 0;
    }
    private float coilSpinMult() {
        return getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getSpinMult() : 1f;
    }

    /** Puts the coil (1 item) into the slot if the slot is empty and it is a coil. */
    public boolean installCoil(ItemStack stack) {
        if (!getCoil().isEmpty() || !(stack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem)) return false;
        itemHandler.setStackInSlot(COIL_SLOT, stack.copyWithCount(1));   // onContentsChanged → markDirty + syncMagnet
        return true;
    }
    /** Takes the coil out of the slot, returns the removed stack (or EMPTY). */
    public ItemStack removeCoil() {
        ItemStack out = getCoil();
        if (out.isEmpty()) return ItemStack.EMPTY;
        itemHandler.setStackInSlot(COIL_SLOT, ItemStack.EMPTY);
        instability = 0f;                                               // no coil -> reset instability
        return out;
    }
    private void syncMagnet() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** TEST drive: under load (redstone) with a coil instability rises (x tier), idle it falls; 100% -> quench. */
    private void tickInstability(Level level, BlockPos pos) {
        float prev = instability;
        int tier = getCoilTier();
        float ceiling = tierCeiling(tier);
        if (--coolCd <= 0) { coolCd = 5; cachedCool = columnCooling(level); } // expensive O(columns) scan once every 5 ticks
        float cool = cachedCool;
        float quenchThreshold = Mth.clamp(BASE_QUENCH + cool * QUENCH_PER_COOL, BASE_QUENCH, 100f); // pylons = safe instability ceiling
        driftPhase = (driftPhase + 1) % DRIFT_PERIOD;                                          // the resonance target drifts while formed
        // Phase Lock: the module in the upgrade slot holds the window on target by itself -> reliable x2 (but not above the safe ceiling)
        boolean phaseLock = itemHandler.getStackInSlot(9).getItem() == com.jykito.industrialcore.item.ModItems.PHASE_LOCK_UPGRADE.get();
        if (phaseLock && tier > 0 && resonanceTarget > 0f && energyStorage.getEnergyStored() >= PHASE_LOCK_COST) {
            energyStorage.consumeEnergy(PHASE_LOCK_COST);
            instability = Math.min(resonanceTarget, quenchThreshold - 1f);                     // sticks to the target, but not into a quench (without pylons it will not reach the band)
            if (Math.abs(instability - resonanceTarget) <= RESONANCE_TOL) resonanceCharge = 100f; // reached the target -> reliable x2
        } else {
            int upkeep = (int) (SPIN_UPKEEP * effThrottle);                                    // usage proportional to power (redstone/slider)
            boolean powered = tier > 0 && effThrottle > 0f && energyStorage.getEnergyStored() >= upkeep;
            float eq;                                                                          // instability "target": where power pulls it
            if (powered) {
                energyStorage.consumeEnergy(upkeep);
                eq = ceiling * effThrottle;                                                    // power sets the target linearly (the reach ceiling comes from the coil)
            } else {
                eq = 0f;                                                                       // no power / coil -> tends to 0
            }
            instability = Mth.clamp(instability + (eq - instability) * RESPONSE_RATE, 0f, 100f); // soft symmetric response, the window follows the power
        }
        if (instability >= quenchThreshold) {
            com.jykito.industrialcore.magnet.MagnetQuench.start((net.minecraft.server.level.ServerLevel) level, center(), QUENCH_RADIUS);
            instability = 0f;
            return;
        }
        if ((int) instability != (int) prev) // sync to the client when the whole % changes -> field/rotation visuals
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    /** Instability ceiling by coil tier: a higher coil goes deeper into the exo mode (closer to a quench). */
    private static float tierCeiling(int tier) {
        return switch (tier) { case 3 -> 105f; case 2 -> 80f; case 1 -> 45f; default -> 0f; };
    }

    /** Total cooling from active antigrav columns in the radius (x each one's heat fraction, cap MAX_COLUMNS). */
    private float columnCooling(Level level) {
        if (AntigravColumnBlockEntity.COLUMNS.isEmpty()) return 0f;
        net.minecraft.world.phys.Vec3 c = center();
        double r2 = COLUMN_RADIUS * COLUMN_RADIUS;
        float sum = 0f; int n = 0;
        for (var col : AntigravColumnBlockEntity.COLUMNS) {
            if (col.getLevel() != level || col.isRemoved() || !col.isActive()) continue;
            double dx = col.getBlockPos().getX() + 0.5 - c.x;
            double dy = col.getBlockPos().getY() + 0.5 - c.y;
            double dz = col.getBlockPos().getZ() + 0.5 - c.z;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            col.markEngaged();                    // visual: the column works for the magnet
            sum += col.getHeatFraction();
            if (++n >= MAX_COLUMNS) break;
        }
        return sum * COLUMN_COOL;
    }

    // ── Processing (assembly/collision: 1-8 shared inputs -> output; exo gate + x2 in the instability band) ──
    private boolean inBandNow = false; // the current craft runs in the optimal band -> x2 (indicator/logic)
    private int outputCd = 0;          // auto-output cooldown

    private ItemStack[] inputSlots() {
        ItemStack[] s = new ItemStack[8];
        for (int i = 0; i < 8; i++) s[i] = itemHandler.getStackInSlot(i);
        return s;
    }

    private com.jykito.industrialcore.recipe.AcceleratorRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack[] slots = inputSlots();
            int tier = getCoilTier();
            for (com.jykito.industrialcore.recipe.AcceleratorRecipe r :
                    level.getRecipeManager().getAllRecipesFor(com.jykito.industrialcore.recipe.ModRecipes.ACCELERATING_TYPE.get())) {
                if (tier >= r.getMinTier() && r.matches(slots)) return r;
            }
            return null;
        });
    }

    private boolean outputFits(ItemStack result, int mult) {
        int add = result.getCount() * mult;
        ItemStack cur = itemHandler.getStackInSlot(8);
        if (cur.isEmpty()) return add <= result.getMaxStackSize();
        return ItemStack.isSameItemSameTags(cur, result) && cur.getCount() + add <= cur.getMaxStackSize();
    }

    private void tickProcessing(Level level) {
        boolean was = working;
        working = false; inBandNow = false;
        int tier = getCoilTier();
        var recipe = (tier == 0) ? null : findRecipe(level);
        updateResonance(recipe);                                                         // target and charge always run (between crafts too)
        if (recipe == null || tier < recipe.getMinTier() || !outputFits(recipe.getResult(), 1)) {
            progress = 0; maxProgress = 1; if (was) syncMagnet(); return;                // no recipe / output full
        }
        if (instability < recipe.getMinInstab()) { if (was) syncMagnet(); return; }      // exo gate: wait for spin-up
        maxProgress = Math.max(1, recipe.getProcessingTime());
        float speed = craftSpeed(recipe);                                                // crafting speed proportional to instability (higher = faster)
        int energy = Math.max(1, (int) (recipe.getEnergyPerTick() * speed));             // faster -> more FE/tick (total per craft about the same)
        if (energyStorage.getEnergyStored() < energy) { if (was) syncMagnet(); return; } // low energy: pause
        working = true;
        inBandNow = resonanceCharge >= RESONANCE_X2;                                     // resonance charge full -> x2 indicator
        energyStorage.consumeEnergy(energy);
        progress += speed;
        if (progress >= maxProgress) { craft(recipe); progress = 0; }
        if (was != working) syncMagnet();
    }

    /** Crafting speed proportional to instability: x1 at the exo gate, faster towards the ceiling (capped). Instability matters even without x2. */
    private float craftSpeed(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        float over = Math.max(0f, instability - recipe.getMinInstab());
        float instabFactor = Math.min(SPEED_MAX, 1f + SPEED_GAIN * over / 100f);
        float coilBase = getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getCraftSpeed() : 1f;
        return coilBase * instabFactor;                                                        // coil = engine (base), instability = boost
    }

    /** Drift of the resonance target within the recipe band [bandLo,bandHi] + charge build-up/decay by how well instability hits it. */
    private void updateResonance(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        float lo = recipe == null ? 0f : recipe.getBandLo();
        float hi = recipe == null ? 0f : recipe.getBandHi();
        if (hi <= lo) {                                                                  // recipe without a band -> resonance impossible
            resonanceTarget = 0f;
            resonanceCharge = Math.max(0f, resonanceCharge - CHARGE_DECAY);
            return;
        }
        float f = 0.5f - 0.5f * Mth.cos((float) (2.0 * Math.PI * driftPhase / DRIFT_PERIOD)); // 0..1..0 smoothly
        resonanceTarget = lo + (hi - lo) * f;
        boolean inTol = Math.abs(instability - resonanceTarget) <= RESONANCE_TOL;
        resonanceCharge = Mth.clamp(resonanceCharge + (inTol ? CHARGE_RISE : -CHARGE_DECAY), 0f, 100f);
    }

    /** Comparator output: resonance error. 7 = instability on target, >7 overshoot, <7 undershoot, 0 = no target. */
    public int getComparatorSignal() {
        if (resonanceTarget <= 0f) return 0;                                              // 0 is reserved for "no target"
        return Mth.clamp(COMP_ON_TARGET + Math.round((instability - resonanceTarget) / COMP_ERR_SCALE), 1, 15);
    }

    /** Notify comparators only when the step changes (not every tick). */
    private void updateComparator(Level level, BlockPos pos) {
        int comp = getComparatorSignal();
        if (comp != lastComparatorSignal) {
            lastComparatorSignal = comp;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    private void craft(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        // input consumption: each ingredient gets its own slot 0-7 (greedy matching, as in matches)
        ItemStack[] slots = inputSlots();
        boolean[] used = new boolean[8];
        var ing = recipe.getInputs(); int[] cnt = recipe.getCounts();
        for (int i = 0; i < ing.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (used[j]) continue;
                if (ing[i].test(slots[j]) && slots[j].getCount() >= cnt[i]) { itemHandler.extractItem(j, cnt[i], false); used[j] = true; break; }
            }
        }
        // x2 if the instability band is held at completion and the output has room
        int mult = (resonanceCharge >= RESONANCE_X2 && outputFits(recipe.getResult(), 2)) ? 2 : 1; // x2 only for held resonance
        if (mult == 2)
            com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, worldPosition, 32,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.MAGNET_RESONANCE);
        ItemStack out = recipe.getResult().copy();
        out.setCount(out.getCount() * mult);
        ItemStack cur = itemHandler.getStackInSlot(8);
        if (cur.isEmpty()) itemHandler.setStackInSlot(8, out);
        else cur.grow(out.getCount());
        setChanged();
    }

    /** Auto-output: pushes the finished item from the output into inventories placed right against the bottom platform blocks. */
    private void autoOutput(Level level, BlockPos core) {
        if (itemHandler.getStackInSlot(8).isEmpty()) return;
        Direction facing = getBlockState().getValue(ElectromagnetCoreBlock.FACING);
        for (int row = 0; row < SIZE; row++) {
            String rowStr = SCHEMA[row];                            // bottom layer Y=0 (platform)
            for (int col = 0; col < SIZE; col++) {
                if (rowStr.charAt(col) != 'S') continue;
                int[] r = rot(col - CORE_COL, row - CORE_ROW, facing);
                BlockPos p = core.offset(r[0], -CORE_LAYER, r[1]);
                for (Direction d : Direction.values()) {
                    BlockEntity nb = level.getBlockEntity(p.relative(d));
                    if (nb == null || nb == this || nb instanceof MagnetPortBlockEntity) continue; // not into itself or its own ports (feedback)
                    var cap = nb.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite());
                    if (!cap.isPresent()) continue;
                    ItemStack out = itemHandler.getStackInSlot(8);
                    if (out.isEmpty()) return;
                    ItemStack rem = net.minecraftforge.items.ItemHandlerHelper.insertItem(cap.orElse(null), out.copy(), false);
                    if (rem.getCount() < out.getCount()) {
                        itemHandler.setStackInSlot(8, rem.isEmpty() ? ItemStack.EMPTY : rem);
                        setChanged();
                    }
                }
            }
        }
    }

    /**
     * Builds the skeleton from the schema around the core (structure wand). Places only into replaceable cells,
     * leaves finished ones alone; in survival blocks come from the inventory (see {@link WandBuild}).
     */
    public static void buildStructure(Level level, BlockPos core, Direction facing, WandBuild wb) {
        Block structural = ModBlocks.STRUCTURAL_BLOCK.get();
        Block holder     = ModBlocks.COIL_HOLDER.get();
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;                 // skip air + core
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    Block want = (c == 'H') ? holder : structural;
                    if (!level.getBlockState(p).is(want)) wb.place(level, p, want.defaultBlockState());
                }
            }
        }
    }

    /** The whole 7x7x8 skeleton from the schema (offsets rotated by facing). */
    private boolean checkStructure(Level level, BlockPos core, Direction facing) {
        Block structural = ModBlocks.STRUCTURAL_BLOCK.get();
        Block holder     = ModBlocks.COIL_HOLDER.get();
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    Block want = (c == 'H') ? holder : structural;
                    if (!level.getBlockState(p).is(want)) return false;
                }
            }
        }
        return true;
    }

    /** Rotate offset (dx,dz) like the models in the BER: NORTH=0/WEST=90/SOUTH=180/EAST=270 (YP axis). */
    public static int[] rot(int dx, int dz, Direction facing) {
        return switch (facing) {
            case WEST  -> new int[]{ dz, -dx};
            case SOUTH -> new int[]{-dx, -dz};
            case EAST  -> new int[]{-dz,  dx};
            default    -> new int[]{ dx,  dz}; // NORTH
        };
    }

    /** TEST: pulls all entities in the radius to the structure centre. */
    private void pullEntities(Level level, BlockPos core) {
        if (getCoilTier() == 0) return; // no coil -> no field -> no pull
        double radius = fieldRadius(instability, getCoilTier()); // growing radius: 2.5 + instab%*0.14
        net.minecraft.world.phys.Vec3 center = center();
        net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) level;

        // the expensive AABB scan is throttled (once every FIELD_SCAN_TICKS) and the list is cached; the pull itself runs EVERY tick over the cache,
        // with fresh positions/distance/strength -> visuals and mechanics 1:1, the heavy getEntities runs 5 times less often.
        if (--fieldScanCd <= 0) {
            fieldScanCd = FIELD_SCAN_TICKS;
            double r = radius + 1.0; // a bit wider than the radius: catch entities entering between scans (and radius growth)
            AABB box = new AABB(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
            fieldEntities = level.getEntities((net.minecraft.world.entity.Entity) null, box,
                    en -> !en.isSpectator() && en.isAlive());
        }

        for (net.minecraft.world.entity.Entity e : fieldEntities) {
            if (!e.isAlive() || e.isSpectator()) continue;              // may have left or died between scans
            net.minecraft.world.phys.Vec3 to = center.subtract(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
            double dist = to.length();
            if (dist > radius) continue;
            if (dist < DISINTEGRATE_RADIUS) { disintegrate(sl, e, center); continue; } // touching the coil -> disintegration
            double force = (1.0 - dist / radius) * PULL_STRENGTH * (1.0 + instability * 0.05); // force scale grows with instability
            net.minecraft.world.phys.Vec3 pull = to.normalize().scale(force);
            if (to.y > 0.0) pull = pull.add(0.0, force * LIFT_MULT, 0.0); // below the centre -> toss into an open cell
            e.setDeltaMovement(e.getDeltaMovement().add(pull));
            e.hasImpulse = true;
            if (e instanceof net.minecraft.server.level.ServerPlayer sp) sp.hurtMarked = true;
        }
    }

    /** TEST: touching the coil means disintegration: a big particle burst, spiralling into the centre + death. */
    private void disintegrate(net.minecraft.server.level.ServerLevel sl, net.minecraft.world.entity.Entity e,
                              net.minecraft.world.phys.Vec3 center) {
        // spare creative/spectator, so you can safely stand in the centre and test
        if (e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return;
        double ex = e.getX(), ey = e.getY() + e.getBbHeight() * 0.5, ez = e.getZ();
        net.minecraft.core.particles.SimpleParticleType mote = com.jykito.industrialcore.particle.ModParticles.ENERGY_MOTE.get();
        // a burst of custom motes around the entity; they swirl towards the coil axis and pull into a line
        // (the orbit centre is passed through the velocity slots of sendParticles with speed=1)
        for (int i = 0; i < 70; i++) {
            double az = Math.random() * Math.PI * 2, pol = Math.acos(2 * Math.random() - 1);
            double rr = Math.cbrt(Math.random()) * BURST_RADIUS;
            double ox = ex + rr * Math.sin(pol) * Math.cos(az);
            double oy = ey + rr * Math.cos(pol);
            double oz = ez + rr * Math.sin(pol) * Math.sin(az);
            sl.sendParticles(mote, ox, oy, oz, 0, center.x, center.y, center.z, 1.0);
        }
        // WAVE: a ring of motes around the coil axis collapses into the centre
        int ring = 48;
        for (int i = 0; i < ring; i++) {
            double a = i * (Math.PI * 2.0 / ring);
            double ox = center.x + BURST_RADIUS * Math.cos(a);
            double oz = center.z + BURST_RADIUS * Math.sin(a);
            sl.sendParticles(mote, ox, center.y, oz, 0, center.x, center.y, center.z, 1.0);
        }
        // honest "evaporation": a player is guaranteed to die, bypassing armor/resistance/totem (genericKill, like /kill);
        // a mob/item is discarded quietly, without drops or a message
        if (e instanceof net.minecraft.world.entity.player.Player) {
            e.kill();
        } else {
            e.discard();
        }
    }

    // ── TEST: client spin-up + sound ──

    public float getSpinLevel() { return spin01; }

    /** Smooth column/coil angle for the BER (interpolated by partialTick, wraps 360 -> 0). */
    public float getSpinAngle(float partialTick) {
        float a0 = spinAnglePrev, a1 = spinAngle;
        if (a1 < a0) a1 += 360f;
        return (a0 + (a1 - a0) * partialTick) % 360f;
    }

    /** Structure centre (where the field pulls and the sound comes from): core + rot(0,3) horizontally, +3 in Y. */
    private net.minecraft.world.phys.Vec3 center() {
        Direction facing = getBlockState().getValue(ElectromagnetCoreBlock.FACING);
        int[] r = rot(0, 3, facing);
        return new net.minecraft.world.phys.Vec3(
                worldPosition.getX() + 0.5 + r[0],
                worldPosition.getY() + 0.5 + 3.0,
                worldPosition.getZ() + 0.5 + r[1]);
    }

    /**
     * Client tick: smooth spin-up/spin-down (spin01), column angle accumulation + sound:
     * a looping hum with pitch/volume by speed, a whoosh on every column pass (every 90°), powerup at the start.
     * TEST spin-up trigger: a redstone signal on the core (replace with accelerator logic later).
     */
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(ElectromagnetCoreBlock.FORMED)) { spin01 = spin01Prev = 0f; return; }
        boolean hasCoil = getCoilTier() > 0;
        // no coil -> the columns stand still; coil + idle -> slow; working -> fast; instability speeds it up
        float target = !hasCoil ? 0f : (isWorking() ? 1f : 0.30f);
        spin01Prev = spin01;
        spin01 += (target - spin01) * SPIN_EASE;
        float speed = Mth.lerp(spin01, 0f, WORK_DEG) * coilSpinMult() + instability * INSTAB_SPIN;
        spinAnglePrev = spinAngle;
        spinAngle = (spinAngle + speed) % 360f;

        com.jykito.industrialcore.client.MagnetSoundClient.ensureHum(this); // looping hum (client-only class)

        if (spin01 > 0.2f && spin01Prev <= 0.2f) {            // spin-up start -> powerup
            net.minecraft.world.phys.Vec3 c = center();
            level.playLocalSound(c.x, c.y, c.z, com.jykito.industrialcore.sound.ModSounds.MAGNET_POWERUP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.9f * (float) Config.soundVolume, 1.0f, false);
        }
        int q = (int) (spinAngle / 90f) & 3, qp = (int) (spinAnglePrev / 90f) & 3;
        if (q != qp) {                                        // a column passed the nearest point -> pass
            net.minecraft.world.phys.Vec3 c = center();
            level.playLocalSound(c.x, c.y, c.z, com.jykito.industrialcore.sound.ModSounds.MAGNET_WHOOSH.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (0.30f + 0.60f * spin01) * (float) Config.soundVolume, 0.80f + 0.35f * spin01, false);
        }
    }

    /** Hides (true) / shows (false) the skeleton blocks so the OBJ body does not z-fight with them. */
    public static void setShellFormed(Level level, BlockPos core, Direction facing, boolean formedVal) {
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    BlockState bs = level.getBlockState(p);
                    if (!(bs.getBlock() instanceof MultiblockShellBlock)) continue;
                    if (bs.getValue(MultiblockShellBlock.FORMED) != formedVal)
                        level.setBlock(p, bs.setValue(MultiblockShellBlock.FORMED, formedVal), 3);
                    bindPort(level, p, core, formedVal, dy == -1); // bottom of the platform (dy=-1) = input surface
                }
            }
        }
    }

    /** Links/unlinks a skeleton block's port BE to the core; for old (pre-update) structures it forces the BE to exist. */
    private static void bindPort(Level level, BlockPos p, BlockPos core, boolean formedVal, boolean isInput) {
        BlockState cur = level.getBlockState(p);
        if (!(cur.getBlock() instanceof MultiblockShellBlock)) return;
        BlockEntity be = level.getBlockEntity(p);
        if (be == null && formedVal && cur.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb) {
            be = eb.newBlockEntity(p, cur);            // the block became an EntityBlock after the update, so create the BE
            if (be != null) level.setBlockEntity(be);
        }
        if (be instanceof MagnetPortBlockEntity port) {
            if (formedVal) port.bindCore(core, isInput);
            else port.unbind();
        }
    }

    /** The x7 body sticks far out of the block, so the bbox is expanded when formed, otherwise frustum culling clips it. */
    @Override
    public AABB getRenderBoundingBox() {
        // IMPORTANT: read the blockstate (synced to the client), NOT the formed field (on the client it is false),
        // otherwise bbox = 1 block and the body gets culled by the frustum at an angle.
        if (!getBlockState().getValue(ElectromagnetCoreBlock.FORMED)) return super.getRenderBoundingBox();
        return new AABB(worldPosition).inflate(16); // covers the body + max reach of the field arcs (for off-screen culling)
    }

    // ── Menu / capabilities / client sync (tank fluids are drawn from the BE) ──
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.electromagnet_core"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ElectromagnetMenu(id, inv, this, this.data);
    }

    /** Menu for the remote console: data from the core, distance validated against anchor (the controller position). */
    public AbstractContainerMenu remoteMenu(int id, Inventory inv, net.minecraft.core.BlockPos anchor) {
        return new ElectromagnetMenu(id, inv, this, this.data, anchor);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (getBlockState().getValue(ElectromagnetCoreBlock.FORMED)) { // the machine is active only when formed
            if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
            if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyFluidHandler  = LazyOptional.of(() -> combinedFluidHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("formed", formed);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("tankA", tankA.writeToNBT(new CompoundTag()));
        tag.put("tankB", tankB.writeToNBT(new CompoundTag()));
        tag.putInt("progress", (int) progress);
        tag.putFloat("instability", instability);
        tag.putFloat("throttle", throttle);
        tag.putBoolean("working", working);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        formed = tag.getBoolean("formed");
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        tankA.readFromNBT(tag.getCompound("tankA"));
        tankB.readFromNBT(tag.getCompound("tankB"));
        progress = tag.getInt("progress");
        // migration of old saves: the coil used to be stored in "coil", move it to slot COIL_SLOT
        if (tag.contains("coil") && getCoil().isEmpty()) {
            ItemStack legacy = ItemStack.of(tag.getCompound("coil"));
            if (!legacy.isEmpty()) itemHandler.setStackInSlot(COIL_SLOT, legacy);
        }
        instability = tag.getFloat("instability");
        throttle = tag.getFloat("throttle");
        working = tag.getBoolean("working");
    }
}
