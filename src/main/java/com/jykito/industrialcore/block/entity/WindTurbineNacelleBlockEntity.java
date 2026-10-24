package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;

import com.jykito.industrialcore.block.custom.wind.WindRotors;
import com.jykito.industrialcore.block.custom.wind.WindTurbine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Nacelle of a wind turbine. Holds the rotor ({@link WindRotors}); once a second it checks the build and counts the output
 * ({@link WindTurbine#evaluate}), every tick it hands the output to the base. The output and the rotor go to the client:
 * the rotor is drawn by the item it is, its speed and the glow follow the output, the turning after the wind the client
 * takes from the game time.
 */
public class WindTurbineNacelleBlockEntity extends BlockEntity {

    private static final int CHECK_EVERY = 20;

    private WindTurbine.Status status = WindTurbine.Status.NO_NACELLE;
    @Nullable private BlockPos base;
    private int output;
    private ItemStack rotor = ItemStack.EMPTY;
    private boolean recheck = true;

    /** Client: rotor and anemometer angles and the rotor speed, kept between frames by the renderer. */
    public float clientRotor, clientSpeed, clientCups;
    public double clientLastTime = -1;

    public WindTurbineNacelleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE_NACELLE_BE.get(), pos, state);
    }

    public WindTurbine.Status status() { return status; }

    public int output() { return output; }

    public ItemStack rotor() { return rotor; }

    /** Puts {@code next} in as the rotor (EMPTY takes it out) and returns the one that was there. */
    public ItemStack swapRotor(ItemStack next) {
        ItemStack old = rotor;
        rotor = next.isEmpty() ? ItemStack.EMPTY : next.copyWithCount(1);
        recheck = true;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return old;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        // turbines check on different ticks, so a wind farm does not scan all at once
        if (recheck || (level.getGameTime() + Math.floorMod(pos.hashCode(), CHECK_EVERY)) % CHECK_EVERY == 0) {
            recheck = false;
            BlockPos.MutableBlockPos found = new BlockPos.MutableBlockPos();
            status = WindTurbine.evaluate(level, pos, WindRotors.get(rotor), found);
            base = status.problem() == WindTurbine.Problem.OK ? found.immutable() : null;
            if (status.output() != output) {
                output = status.output();
                setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
        if (output > 0 && base != null && level.getBlockEntity(base) instanceof WindTurbineBaseBlockEntity b) b.receiveWind(output);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) WindTurbine.track(level, worldPosition, true);
    }

    @Override
    public void setRemoved() {
        if (level != null) WindTurbine.track(level, worldPosition, false);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) WindTurbine.track(level, worldPosition, false);
        super.onChunkUnloaded();
    }

    private void writeShared(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("output", output);
        if (!rotor.isEmpty()) tag.put("rotor", rotor.save(registries));
    }

    private void readShared(CompoundTag tag, HolderLookup.Provider registries) {
        output = tag.getInt("output");
        rotor = tag.contains("rotor") ? ItemStack.parseOptional(registries, tag.getCompound("rotor")) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeShared(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readShared(tag, registries);
        recheck = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeShared(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readShared(tag, registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag(), registries);
    }
}
