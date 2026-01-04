package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.attachment.AmmoShipAttachment;
import com.github.litermc.vschunkloader.compat.CompatMods;
import com.github.litermc.vschunkloader.config.Config;
import com.github.litermc.vtil.block.AbstractAssemblerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniond;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AmmoAssemblerBlockEntity extends AbstractAssemblerBlockEntity {
	private static final int MAX_DIM = 8 * 16;
	private static final String AMMO_DEFAULT_SLUG_PREFIX = "+assembled+ammo+";

	private boolean triggering = false;
	private volatile AssembleResult assembleResult = AssembleResult.SUCCESS;
	private int energyStored = 0;
	private int energyConsumption = Config.ammoAssembleEnergy; // TODO
	private final AABBi box = new AABBi();

	private Runnable assembleFinishCallback = null;
	Object energyStorage = null;
	Object peripheral = null;

	public AmmoAssemblerBlockEntity(BlockPos pos, BlockState state) {
		super(VSCRegistry.BlockEntities.AMMO_ASSEMBLER.get(), pos, state);
	}

	public boolean isAssembling() {
		return this.assembleResult.isWorking();
	}

	public boolean isAssembleSuccessed() {
		return this.assembleResult.isSuccess();
	}

	public int getEnergyConsumption() {
		return this.energyConsumption;
	}

	public AssembleResult getAssembleResult() {
		return this.assembleResult;
	}

	private void setAssembleResult(final AssembleResult result) {
		if (this.assembleResult == result) {
			return;
		}
		this.assembleResult = result;
		this.setChanged();
		this.getLevel().setBlock(this.getBlockPos(), this.getBlockState().setValue(AmmoAssemblerBlock.LED, result.getLED()), Block.UPDATE_ALL);
	}

	public int receiveEnergy(final int maxReceive, final boolean simulate) {
		final int avaliable = this.getMaxEnergyStored() - this.energyStored;
		if (avaliable <= 0) {
			return 0;
		}
		final int received = Math.min(Math.min(avaliable, maxReceive), (this.getMaxEnergyStored() + 99) / 100);
		if (!simulate) {
			this.energyStored += received;
		}
		return received;
	}

	public int getEnergyStored() {
		return this.energyStored;
	}

	public int getMaxEnergyStored() {
		return this.getEnergyConsumption();
	}

	@Override
	public void load(final CompoundTag data) {
		this.energyStored = data.getInt("EnergyStored");
		this.triggering = data.getBoolean("Powered");
		try {
			this.assembleResult = AssembleResult.valueOf(data.getString("AssembleResult"));
		} catch (IllegalArgumentException e) {
			this.assembleResult = AssembleResult.SUCCESS;
		}
	}

	@Override
	public void saveAdditional(final CompoundTag data) {
		data.putInt("EnergyStored", this.energyStored);
		data.putBoolean("Powered", this.triggering);
		this.saveShared(data);
	}

	public void saveShared(final CompoundTag data) {
		data.putString("AssembleResult", this.assembleResult.toString());
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		this.saveShared(data);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void neighborChanged(final Block neighbor, final BlockPos neighborPos, final boolean moving) {
		final Level level = this.getLevel();
		final BlockPos pos = this.getBlockPos();
		final boolean shouldTrigger = Direction.stream()
			.filter(dir -> dir != this.getBlockState().getValue(DirectionalBlock.FACING))
			.anyMatch(dir -> level.getSignal(pos.relative(dir), dir) > 0);
		if (this.triggering == shouldTrigger) {
			return;
		}
		this.triggering = shouldTrigger;
		if (shouldTrigger) {
			this.startAssemble(null);
		}
	}

	Object createPeripheral() {
		final AmmoAssemblerPeripheral peripheral = new AmmoAssemblerPeripheral(this);
		this.assembleFinishCallback = peripheral::onAssembleFinish;
		return peripheral;
	}

	@Override
	public boolean startAssemble(final String slug) {
		if (this.isAssembling()) {
			return false;
		}
		if (this.energyStored < this.energyConsumption) {
			this.finishAssemble(AssembleResult.NO_ENERGY);
			return true;
		}
		return super.startAssemble(slug);
	}

	@Override
	protected void setAssembling(final boolean assembling) {
		super.setAssembling(assembling);
		if (assembling) {
			this.setAssembleResult(AssembleResult.WORKING);
		}
	}

	protected void finishAssemble(final AssembleResult result) {
		this.setAssembleResult(result);
		this.finishAssemble();
	}

	@Override
	protected void finishAssemble() {
		super.finishAssemble();
		if (this.assembleFinishCallback != null) {
			this.assembleFinishCallback.run();
		}
	}

	@Override
	protected void finishAssembleAsSuccess() {
		this.finishAssemble(AssembleResult.SUCCESS);
	}

	@Override
	protected void finishAssembleAsAssembleSelf() {
		this.finishAssemble(AssembleResult.ASSEMBLING_SELF);
	}

	@Override
	protected void finishAssembleAsTooManyBlocks() {
		this.finishAssemble(AssembleResult.TOO_MANY_BLOCKS);
	}

	@Override
	protected void finishAssembleAsNoBlockToAssemble() {
		this.finishAssemble(AssembleResult.NO_BLOCK);
	}

	@Override
	protected void finishAssembleAsConflicts() {
		this.finishAssemble(AssembleResult.OTHER_ASSEMBLING);
	}

	@Override
	protected void addAssemblingBlock(final BlockPos pos) {
		final Level level = this.getLevel();
		if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
			this.finishAssemble(AssembleResult.CHUNK_UNLOADED);
			return;
		}
		final BlockState block = level.getBlockState(pos);
		if (block.isAir()) {
			return;
		}
		if (!this.canAssembleBlock(block)) {
			this.finishAssemble(AssembleResult.UNABLE_ASSEMBLE);
			return;
		}
		this.box.union(pos.getX(), pos.getY(), pos.getZ());
		if (this.box.lengthX() > MAX_DIM || this.box.lengthY() > MAX_DIM || this.box.lengthZ() > MAX_DIM) {
			this.finishAssemble(AssembleResult.SIZE_OVERFLOW);
			return;
		}
		super.addAssemblingBlock(pos);
	}

	protected boolean canAssembleBlock(final BlockState state) {
		final Block block = state.getBlock();
		if (block instanceof GameMasterBlock) {
			return false;
		}
		final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
		if (Config.ammoAssembleBlacklist.contains(blockId)) {
			return false;
		}
		return true;
	}

	@Override
	protected void onAssembleSuccess(final ServerShip ship) {
		if (ship.getSlug() == null) {
			ship.setSlug(AMMO_DEFAULT_SLUG_PREFIX + ship.getId());
		}
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld((ServerLevel) (this.getLevel()));
		final LoadedServerShip loadedShip = shipWorld.getLoadedShips().getById(ship.getId());
		AmmoShipAttachment.create(loadedShip == null ? ship : loadedShip);
	}

	private static Stream<BlockPos> streamBlocksInAABB(AABB box) {
		final int
			minX = (int) (Math.round(box.minX)), maxX = (int) (Math.round(box.maxX)),
			minY = (int) (Math.round(box.minY)), maxY = (int) (Math.round(box.maxY)),
			minZ = (int) (Math.round(box.minZ)), maxZ = (int) (Math.round(box.maxZ));
		final int widthX = maxX - minX, widthY = maxY - minY, widthZ = maxZ - minZ;
		return IntStream.range(0, widthX * widthY * widthZ).mapToObj((i) -> {
			final int x = i % widthX + minX;
			i /= widthX;
			final int z = i % widthZ + minZ;
			i /= widthZ;
			final int y = i + minY;
			return new BlockPos(x, y, z);
		});
	}
}
