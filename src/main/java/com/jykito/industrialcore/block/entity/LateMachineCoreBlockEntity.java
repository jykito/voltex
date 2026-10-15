package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.LateMachineCoreBlock;
import com.jykito.industrialcore.block.custom.MechanismUpgradeBlock;
import com.jykito.industrialcore.menu.LateMachineCoreMenu;
import com.jykito.industrialcore.multiblock.LateMultiblockPattern;
import com.jykito.industrialcore.multiblock.LateRecipe;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteProcessing;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Late multiblock core. It has no slots of its own: it takes input from input ports (items and fluid
 * by port mode), puts the result (items and/or fluid) into output ports, pulls energy from connectors (GregTech model).
 * The shape and recipes come from the core block.
 * <p>
 * Batch: the first recipe the inputs are enough for, up to {@link #parallel()} operations per cycle,
 * as many as the inputs and output room allow. Time comes from the recipe, energy is the recipe usage times the number of operations;
 * speed blocks inside the structure shorten the cycle and raise usage, energy efficiency blocks lower it.
 * <p>
 * Heat (liquefier): a recipe with a threshold runs only while the total heat in the heat ports is not below the threshold.
 * Heat is not spent, the ports cool down by themselves without inflow.
 */
public class LateMachineCoreBlockEntity extends BlockEntity implements MenuProvider {

    /** Parallel without upgrade blocks; parallel blocks add their bonus to it. */
    public static final int BASE_PARALLEL = 64;
    private static final int CHECK_INTERVAL = 20, IDLE_INTERVAL = 10;
    /** Number of ContainerData slots; the menu on the client creates the same number. */
    public static final int DATA_COUNT = 29;

    private LateMultiblockPattern.Result structure;
    private int checkTimer, idleTimer;

    // current batch; the recipe is stored by id and looked up again after the world loads
    @Nullable private ResourceLocation jobId;
    @Nullable private LateRecipe job;
    private int jobOps, progress, maxProgress, energyPerTick;

    // snapshot for the GUI
    private int energyStored, energyCap, heat;
    /** Heat the first recipe that has input and output room is still waiting for (0 = none), for the GUI. */
    private int heatWanted;

    /** Core mode index (always 0 for cores without modes). */
    private int mode;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            LateMultiblockPattern.Result r = structure;
            BlockPos at = r != null && r.at() != null ? r.at().subtract(worldPosition) : BlockPos.ZERO;
            int outId = job == null || job.result().isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(job.result().getItem());
            return switch (i) {
                case 0 -> r != null && r.formed() ? 1 : 0;
                case 1 -> r == null ? 0 : r.problem().ordinal();
                case 2 -> r == null ? 0 : r.expected().ordinal();
                case 3 -> at.getX();
                case 4 -> at.getY();
                case 5 -> at.getZ();
                case 6 -> progress;
                case 7 -> maxProgress;
                case 8 -> energyStored & 0xFFFF;
                case 9 -> (energyStored >>> 16) & 0xFFFF;
                case 10 -> energyCap & 0xFFFF;
                case 11 -> (energyCap >>> 16) & 0xFFFF;
                case 12 -> parallel();
                case 13 -> jobOps;
                case 14 -> r == null ? 0 : r.inputs().size();
                case 15 -> r == null ? 0 : r.outputs().size();
                case 16 -> r == null ? 0 : r.energy().size();
                case 17 -> energyPerTick & 0xFFFF;
                case 18 -> (energyPerTick >>> 16) & 0xFFFF;
                case 19 -> outId & 0xFFFF;
                case 20 -> (outId >>> 16) & 0xFFFF;
                case 21 -> 100 - timePercent();
                case 22 -> mode;
                case 23 -> ((LateMachineCoreBlock) getBlockState().getBlock()).modes().size();
                case 24 -> heat & 0xFFFF;
                case 25 -> (heat >>> 16) & 0xFFFF;
                case 26 -> job == null ? heatWanted : job.heat();
                case 27 -> ((LateMachineCoreBlock) getBlockState().getBlock()).pattern().needsHeat() ? 1 : 0;
                case 28 -> r == null ? 0 : r.heat().size();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    /** Processor strain: only the multiblock ore washer has a slot for it ({@link #acceptsNanites}). */
    private final ItemStackHandler nanites = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return acceptsNanites() && stack.getItem() instanceof NaniteStrainItem && NaniteGenome.kindOf(stack) == NaniteGenome.Kind.PROCESSOR;
        }
        @Override public int getSlotLimit(int slot) { return 1; }
    };

    public ItemStackHandler getNanites() { return nanites; }
    public boolean acceptsNanites() { return getBlockState().getBlock() == ModBlocks.MULTIBLOCK_WASHER.get(); }

    public LateMachineCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATE_MACHINE_CORE_BE.get(), pos, state);
    }

    public int parallel() { return BASE_PARALLEL + (structure == null ? 0 : structure.parallelBonus()); }

    private int speedBlocks()      { return structure == null ? 0 : structure.speedBlocks(); }
    private int efficiencyBlocks() { return structure == null ? 0 : structure.efficiencyBlocks(); }

    /** Fraction of the base cycle time in percent: each speed block removes its share, down to the cap. */
    private int timePercent() {
        return Math.max(MechanismUpgradeBlock.MIN_TIME_PERCENT, 100 - MechanismUpgradeBlock.SPEED_TIME_PERCENT * speedBlocks());
    }

    /** Fraction of the base FE/t usage in percent: speed adds, energy efficiency removes, as a sum, down to the floor. */
    private int energyPercent() {
        return Math.max(MechanismUpgradeBlock.MIN_ENERGY_PERCENT, 100
                + MechanismUpgradeBlock.SPEED_ENERGY_PERCENT * speedBlocks()
                - MechanismUpgradeBlock.EFFICIENCY_ENERGY_PERCENT * efficiencyBlocks());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LateMachineCoreBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        LateMachineCoreBlock block = (LateMachineCoreBlock) state.getBlock();
        if (structure == null || ++checkTimer >= CHECK_INTERVAL) {
            checkTimer = 0;
            boolean wasFormed = structure != null && structure.formed();
            structure = block.pattern().validate(level, pos, state.getValue(LateMachineCoreBlock.FACING));
            if (!wasFormed && structure.formed())
                com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.LATE_FORMED);
        }
        List<EnergyPortBlockEntity> energy = energyPorts(level);
        energyStored = 0; energyCap = 0;
        for (EnergyPortBlockEntity e : energy) {
            energyStored = (int) Math.min(Integer.MAX_VALUE, (long) energyStored + e.getEnergy().getEnergyStored());
            energyCap = (int) Math.min(Integer.MAX_VALUE, (long) energyCap + e.getEnergy().getMaxEnergyStored());
        }
        heat = heat(level);

        if (!structure.formed()) { resetJob(); setLit(level, pos, state, false); return; }

        Ports ports = ports(level);
        if (job == null && jobId != null) job = find(block.recipes(mode).all(level), jobId);   // after the world loads
        if (job == null) {
            jobId = null; jobOps = 0;
            if (++idleTimer < IDLE_INTERVAL || !startJob(level, block, ports)) { setLit(level, pos, state, false); return; }
        }

        if (progress < maxProgress) {
            // cooled below the threshold: the cycle stands, energy is not spent
            boolean powered = heat >= job.heat() && drawEnergy(energy, energyPerTick);
            if (powered) progress++;
            setLit(level, pos, state, powered);
            setChanged();
            return;
        }
        // cycle done: deliver if the input is still there and the output fits; otherwise wait
        if (available(ports, job) < jobOps) { resetJob(); setChanged(); return; }
        if (outputRoom(ports, job) < jobOps) { setLit(level, pos, state, false); return; }
        consume(ports, job, jobOps);
        // the chance is rolled for each operation separately, like a single machine; room is reserved for the worst case
        if (!job.result().isEmpty()) {
            int made = rolls(level, jobOps, job.resultChance());
            insert(ports.outputs, job.result(), job.result().getCount() * made);
            if (acceptsNanites()) naniteBonus(level, ports, made);
        }
        if (!job.byproduct().isEmpty())
            insert(ports.outputs, job.byproduct(), job.byproduct().getCount() * rolls(level, jobOps, job.byproductChance()));
        if (!job.fluidResult().isEmpty()) fill(ports.fluidOutputs, job.fluidResult(), job.fluidResult().getAmount() * jobOps);
        resetJob();
        setChanged();
    }

    private boolean startJob(Level level, LateMachineCoreBlock block, Ports ports) {
        idleTimer = 0;
        int wanted = 0;
        for (LateRecipe r : block.recipes(mode).all(level)) {
            if (r.result().isEmpty() && r.fluidResult().isEmpty()) continue;
            if (r.heat() > heat) {
                if (available(ports, r) > 0 && outputRoom(ports, r) > 0) wanted = wanted == 0 ? r.heat() : Math.min(wanted, r.heat());
                continue;
            }
            int ops = Math.min(parallel(), Math.min(available(ports, r), outputRoom(ports, r)));
            if (ops <= 0) continue;
            job = r; jobId = r.id(); jobOps = ops;
            heatWanted = 0;
            progress = 0;
            maxProgress = Math.max(1, (int) Math.ceil(r.time() * timePercent() / 100.0));
            energyPerTick = (int) Math.min(Integer.MAX_VALUE, (long) r.energyPerTick() * ops * energyPercent() / 100);
            return true;
        }
        heatWanted = wanted;
        return false;
    }

    /**
     * Processor: of the {@code made} products that dropped, part comes out a second time, as much as fits into the output ports.
     * The material comes from the recipe input; strain wear is counted over all operations of the batch.
     */
    private void naniteBonus(Level level, Ports ports, int made) {
        ItemStack strain = nanites.getStackInSlot(0);
        if (strain.isEmpty() || job.items().length == 0) return;
        ItemStack[] inputs = job.items()[0].getItems();
        if (inputs.length == 0) return;
        int extra = NaniteProcessing.extra(level, strain, inputs[0].getItem(), made);
        int per = Math.max(1, job.result().getCount());
        extra = Math.min(extra, capacity(ports.outputs, job.result()) / per);
        if (extra > 0) insert(ports.outputs, job.result(), per * extra);
        nanites.setStackInSlot(0, NaniteProcessing.wear(level, strain, jobOps));
    }

    /** Next mode, cycling. The current batch is reset: input is only consumed at the end of a cycle, so nothing is lost. */
    public boolean cycleMode() {
        int count = ((LateMachineCoreBlock) getBlockState().getBlock()).modes().size();
        if (count < 2) return false;
        mode = (mode + 1) % count;
        resetJob();
        idleTimer = IDLE_INTERVAL;     // a new recipe is looked up right away, without a pause
        setChanged();
        return true;
    }

    private void resetJob() {
        job = null; jobId = null;
        jobOps = 0; progress = 0; maxProgress = 0; energyPerTick = 0;
    }

    @Nullable
    private static LateRecipe find(List<LateRecipe> all, ResourceLocation id) {
        for (LateRecipe r : all) if (r.id().equals(id)) return r;
        return null;
    }

    // ── ports ──

    private record Ports(List<ItemFluidPortBlockEntity> items, List<ItemFluidPortBlockEntity> fluids,
                         List<ItemFluidPortBlockEntity> outputs, List<ItemFluidPortBlockEntity> fluidOutputs) {}

    private Ports ports(Level level) {
        List<ItemFluidPortBlockEntity> items = new ArrayList<>(), fluids = new ArrayList<>();
        List<ItemFluidPortBlockEntity> outputs = new ArrayList<>(), fluidOutputs = new ArrayList<>();
        for (BlockPos p : structure.inputs())
            if (level.getBlockEntity(p) instanceof ItemFluidPortBlockEntity port) {
                port.linkCore(worldPosition);   // the pattern port is labelled in the AE2 terminal with this machine's name
                if (port.acceptsItems()) items.add(port);
                if (port.acceptsFluids()) fluids.add(port);   // an "items + fluid" input goes into both lists
            }
        for (BlockPos p : structure.outputs())
            if (level.getBlockEntity(p) instanceof ItemFluidPortBlockEntity port) {
                port.linkCore(worldPosition);
                if (port.acceptsItems()) outputs.add(port);
                if (port.acceptsFluids()) fluidOutputs.add(port);   // an "items + fluid" output into both as well
            }
        return new Ports(items, fluids, outputs, fluidOutputs);
    }

    /** Total heat in all heat ports of the structure. */
    private int heat(Level level) {
        long sum = 0;
        if (structure != null)
            for (BlockPos p : structure.heat())
                if (level.getBlockEntity(p) instanceof HeatPortBlockEntity port) sum += port.getHeat();
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    private List<EnergyPortBlockEntity> energyPorts(Level level) {
        List<EnergyPortBlockEntity> out = new ArrayList<>();
        if (structure == null) return out;
        for (BlockPos p : structure.energy())
            if (level.getBlockEntity(p) instanceof EnergyPortBlockEntity port) out.add(port);
        return out;
    }

    /** How many recipe operations can be made from what is in the inputs. */
    private static int available(Ports ports, LateRecipe r) {
        long ops = Integer.MAX_VALUE;
        for (int i = 0; i < r.items().length; i++)
            ops = Math.min(ops, count(ports.items, r.items()[i]) / Math.max(1, r.counts()[i]));
        if (!r.fluid().isEmpty())
            ops = Math.min(ops, fluidAmount(ports.fluids, r.fluid()) / r.fluid().getAmount());
        return (int) ops;
    }

    private static long count(List<ItemFluidPortBlockEntity> ports, Ingredient ing) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (!s.isEmpty() && ing.test(s)) n += s.getCount();
            }
        return n;
    }

    private static long fluidAmount(List<ItemFluidPortBlockEntity> ports, FluidStack like) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (FluidTank tank : port.tanks())   // an ME input and a pattern port have up to three tanks
                if (tank.getFluid().isFluidEqual(like)) n += tank.getFluidAmount();
        return n;
    }

    private static void consume(Ports ports, LateRecipe r, int ops) {
        for (int k = 0; k < r.items().length; k++) {
            long left = (long) r.counts()[k] * ops;
            for (ItemFluidPortBlockEntity port : ports.items)
                for (int i = 0; i < port.activeSlots() && left > 0; i++) {
                    ItemStack s = port.getItems().getStackInSlot(i);
                    if (s.isEmpty() || !r.items()[k].test(s)) continue;
                    left -= port.getItems().extractItem(i, (int) Math.min(left, s.getCount()), false).getCount();
                }
        }
        if (!r.fluid().isEmpty()) {
            int left = r.fluid().getAmount() * ops;
            for (ItemFluidPortBlockEntity port : ports.fluids)
                for (FluidTank tank : port.tanks()) {
                    if (left <= 0) break;
                    if (!tank.getFluid().isFluidEqual(r.fluid())) continue;
                    left -= tank.drain(Math.min(left, tank.getFluidAmount()), IFluidHandler.FluidAction.EXECUTE).getAmount();
                }
        }
    }

    /** How many of {@code ops} operations rolled true with chance {@code chance}. */
    private static int rolls(Level level, int ops, float chance) {
        if (chance >= 1f) return ops;
        if (chance <= 0f) return 0;
        int n = 0;
        for (int i = 0; i < ops; i++) if (level.random.nextFloat() < chance) n++;
        return n;
    }

    /** How many recipe operations fit into the output: by items and by fluid, whichever is tighter. */
    private static int outputRoom(Ports ports, LateRecipe r) {
        long ops = Integer.MAX_VALUE;
        if (!r.result().isEmpty() && !r.byproduct().isEmpty())
            ops = Math.min(ops, pairRoom(ports.outputs, r.result(), r.byproduct()));
        else if (!r.result().isEmpty())
            ops = Math.min(ops, capacity(ports.outputs, r.result()) / Math.max(1, r.result().getCount()));
        else if (!r.byproduct().isEmpty())
            ops = Math.min(ops, capacity(ports.outputs, r.byproduct()) / Math.max(1, r.byproduct().getCount()));
        if (!r.fluidResult().isEmpty())
            ops = Math.min(ops, fluidRoom(ports.fluidOutputs, r.fluidResult()) / r.fluidResult().getAmount());
        return (int) ops;
    }

    /** How many mB of fluid {@code like} fit into the output tanks: empty ones and those with the same fluid. */
    private static long fluidRoom(List<ItemFluidPortBlockEntity> ports, FluidStack like) {
        long n = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (FluidTank tank : port.tanks())
                if (tank.isEmpty() || tank.getFluid().isFluidEqual(like)) n += tank.getCapacity() - tank.getFluidAmount();
        return n;
    }

    private static void fill(List<ItemFluidPortBlockEntity> ports, FluidStack like, int amount) {
        for (ItemFluidPortBlockEntity port : ports)
            for (FluidTank tank : port.tanks()) {
                if (amount <= 0) return;
                if (!tank.isEmpty() && !tank.getFluid().isFluidEqual(like)) continue;
                amount -= tank.fill(new FluidStack(like, amount), IFluidHandler.FluidAction.EXECUTE);
            }
    }

    /**
     * How many operations with two products fit into the outputs if both drop on every operation.
     * Empty slots are shared between the products, so they cannot be counted separately: we top up matching
     * stacks of each product and split the rest by empty slots. The number of operations is found by binary search.
     */
    private static int pairRoom(List<ItemFluidPortBlockEntity> ports, ItemStack a, ItemStack b) {
        if (ItemStack.isSameItemSameTags(a, b))
            return capacity(ports, a) / Math.max(1, a.getCount() + b.getCount());
        long partialA = 0, partialB = 0;
        int empty = 0;
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (s.isEmpty()) empty++;
                else if (ItemStack.isSameItemSameTags(s, a)) partialA += Math.max(0, a.getMaxStackSize() - s.getCount());
                else if (ItemStack.isSameItemSameTags(s, b)) partialB += Math.max(0, b.getMaxStackSize() - s.getCount());
            }
        int maxA = a.getMaxStackSize(), maxB = b.getMaxStackSize(), perA = Math.max(1, a.getCount()), perB = Math.max(1, b.getCount());
        int lo = 0, hi = (int) Math.min(Integer.MAX_VALUE, (partialA + partialB + (long) empty * Math.max(maxA, maxB)));
        while (lo < hi) {
            int mid = (int) (((long) lo + hi + 1) / 2);
            long needA = Math.max(0, (long) mid * perA - partialA), needB = Math.max(0, (long) mid * perB - partialB);
            long slots = (needA + maxA - 1) / maxA + (needB + maxB - 1) / maxB;
            if (slots <= empty) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    /** How many {@code like} fit into all outputs: empty and matching open slots. */
    private static int capacity(List<ItemFluidPortBlockEntity> ports, ItemStack like) {
        long n = 0;
        int max = like.getMaxStackSize();
        for (ItemFluidPortBlockEntity port : ports)
            for (int i = 0; i < port.activeSlots(); i++) {
                ItemStack s = port.getItems().getStackInSlot(i);
                if (s.isEmpty()) n += max;
                else if (ItemStack.isSameItemSameTags(s, like)) n += Math.max(0, max - s.getCount());
            }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    private static void insert(List<ItemFluidPortBlockEntity> ports, ItemStack like, int amount) {
        int max = like.getMaxStackSize();
        for (ItemFluidPortBlockEntity port : ports) {
            while (amount > 0) {
                int chunk = Math.min(max, amount);
                ItemStack rest = port.insertOutput(like.copyWithCount(chunk), false);
                int placed = chunk - rest.getCount();
                amount -= placed;
                if (placed == 0) break;     // this port is full, move to the next
            }
            if (amount <= 0) return;
        }
    }

    private static boolean drawEnergy(List<EnergyPortBlockEntity> ports, int need) {
        long have = 0;
        for (EnergyPortBlockEntity e : ports) have += e.getEnergy().getEnergyStored();
        if (have < need) return false;
        for (EnergyPortBlockEntity e : ports) {
            if (need <= 0) break;
            int take = Math.min(need, e.getEnergy().getEnergyStored());
            e.getEnergy().consumeEnergy(take);
            e.setChanged();
            need -= take;
        }
        return true;
    }

    private static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LateMachineCoreBlock.LIT) != lit) level.setBlock(pos, state.setValue(LateMachineCoreBlock.LIT, lit), 3);
    }

    // ── GUI ──

    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LateMachineCoreMenu(id, inv, this, data);
    }

    // ── saving ──

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (jobId != null) tag.putString("job", jobId.toString());
        tag.putInt("jobOps", jobOps);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putInt("energyPerTick", energyPerTick);
        tag.putInt("mode", mode);
        if (!nanites.getStackInSlot(0).isEmpty()) tag.put("nanites", nanites.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        jobId = tag.contains("job") ? ResourceLocation.tryParse(tag.getString("job")) : null;
        job = null;
        jobOps = tag.getInt("jobOps");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        energyPerTick = tag.getInt("energyPerTick");
        mode = tag.getInt("mode");
        if (tag.contains("nanites")) nanites.deserializeNBT(tag.getCompound("nanites"));
    }
}
