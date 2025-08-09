package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.attachment.AmmoShipAttachment;
import com.github.litermc.vschunkloader.compat.CompatMods;
import com.github.litermc.vschunkloader.config.Config;
import com.github.litermc.vschunkloader.util.Pair;
import com.github.litermc.vschunkloader.util.ShipAllocator;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import org.joml.Quaterniond;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AmmoAssemblerBlockEntity extends BlockEntity {
	private static final int MAX_DIM = 8 * 16;
	private static final String AMMO_SLUG_PREFIX = "+assembled+ammo+";

	private boolean triggering = false;
	private volatile AssembleResult assembleResult = AssembleResult.SUCCESS;
	private String shipSlug = null;
	private int energyStored = 0;
	private int energyConsumption = Config.ammoAssembleEnergy; // TODO
	private final Queue<BlockPos> queueing = new ArrayDeque<>();
	private final DenseBlockPosSet blocks = new DenseBlockPosSet();
	private final DenseBlockPosSet checked = new DenseBlockPosSet();
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
			this.assemble(null);
		}
	}

	Object createPeripheral() {
		final AmmoAssemblerPeripheral peripheral = new AmmoAssemblerPeripheral(this);
		this.assembleFinishCallback = peripheral::onAssembleFinish;
		return peripheral;
	}

	public void serverTick() {
		if (this.isAssembling()) {
			this.assembleTick((ServerLevel)(this.getLevel()));
		}
	}

	boolean assemble(final String slug) {
		if (this.isAssembling()) {
			return false;
		}
		if (this.energyStored < this.energyConsumption) {
			this.finishAssemble(AssembleResult.NO_ENERGY);
			return true;
		}
		this.setAssembleResult(AssembleResult.WORKING);
		this.shipSlug = slug;
		this.queueing.clear();
		this.blocks.clear();
		this.checked.clear();
		final BlockPos facingBlockPos = this.getBlockPos().relative(this.getBlockState().getValue(DirectionalBlock.FACING));
		this.queueing.add(facingBlockPos);
		this.box
			.setMin(facingBlockPos.getX(), facingBlockPos.getY(), facingBlockPos.getZ())
			.setMax(facingBlockPos.getX(), facingBlockPos.getY(), facingBlockPos.getZ());
		return true;
	}

	private void finishAssemble(final AssembleResult result) {
		this.setAssembleResult(result);
		this.shipSlug = null;
		this.queueing.clear();
		this.blocks.clear();
		this.checked.clear();
		if (this.assembleFinishCallback != null) {
			this.assembleFinishCallback.run();
		}
	}

	private void assembleTick(final ServerLevel level) {
		final BlockPos selfPos = this.getBlockPos();
		int ticked = 0;
		while (true) {
			final BlockPos pos = this.queueing.poll();
			if (pos == null) {
				this.checked.clear();
				if (ticked > 0) {
					return;
				}
				break;
			}
			if (this.blocks.size() > Config.ammoMaxBlocks) {
				this.finishAssemble(AssembleResult.TOO_MANY_BLOCKS);
				return;
			}
			if (pos.equals(selfPos)) {
				this.finishAssemble(AssembleResult.ASSEMBLING_SELF);
				return;
			}
			this.checkBlock(pos);
			if (!this.isAssembling()) {
				return;
			}
			ticked++;
			if (ticked > 16 * 16 * 16) {
				return;
			}
		}
		if (this.blocks.isEmpty()) {
			this.finishAssemble(AssembleResult.NO_BLOCK);
			return;
		}
		if (this.box.lengthX() > MAX_DIM || this.box.lengthY() > MAX_DIM || this.box.lengthZ() > MAX_DIM) {
			this.finishAssemble(AssembleResult.SIZE_OVERFLOW);
			return;
		}

		this.createShip(level);
	}

	private void checkBlock(final BlockPos pos) {
		final Level level = this.getLevel();
		if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
			this.finishAssemble(AssembleResult.CHUNK_UNLOADED);
			return;
		}
		final BlockState block = level.getBlockState(pos);
		if (this.isAirBlock(block)) {
			return;
		}
		if (!this.canAssembleBlock(block)) {
			this.finishAssemble(AssembleResult.UNABLE_ASSEMBLE);
			return;
		}
		this.box.union(pos.getX(), pos.getY(), pos.getZ());
		this.blocks.add(pos.getX(), pos.getY(), pos.getZ());
		for (final Direction dir : Direction.values()) {
			final BlockPos p = pos.relative(dir);
			final BlockState targetState = level.getBlockState(p);
			if (targetState.getBlock() instanceof AmmoAssemblerBlock && targetState.getValue(DirectionalBlock.FACING) == dir.getOpposite()) {
				final AmmoAssemblerBlockEntity otherAssembler = (AmmoAssemblerBlockEntity) (level.getBlockEntity(p));
				if (otherAssembler != this && otherAssembler.isAssembling()) {
					this.finishAssemble(AssembleResult.OTHER_ASSEMBLING);
					return;
				}
				continue;
			}
			if (this.checked.add(p.getX(), p.getY(), p.getZ())) {
				this.queueing.add(p);
			}
		}
	}

	protected boolean isAirBlock(final BlockState state) {
		return state.isAir();
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

	private void createShip(final ServerLevel level) {
		if (this.energyStored < this.energyConsumption) {
			this.finishAssemble(AssembleResult.NO_ENERGY);
			return;
		}
		this.energyStored -= this.energyConsumption;
		this.setChanged();

		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final Vector3i worldCenter = new Vector3i(this.box.center(new Vector3d()), RoundingMode.TRUNCATE);
		final ServerShip ship = ShipAllocator.get(level).allocShip(worldCenter);
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i offset = shipCenter.sub(worldCenter, new Vector3i());
		final List<Pair<BlockPos, BlockState>> blockStates = new ArrayList<>(this.blocks.size());
		final List<Entity> entities = new ArrayList<>();

		ship.setSlug(this.shipSlug == null ? AMMO_SLUG_PREFIX + ship.getId() : this.shipSlug);

		// get attachable entities
		for (final Entity entity : level.getEntities(null, new AABB(this.box.minX - 1, this.box.minY - 1, this.box.minZ - 1, this.box.maxX + 2, this.box.maxY + 2, this.box.maxZ + 2))) {
			if (entity instanceof final HangingEntity he) {
				final BlockPos hanging = he.getPos().relative(he.getDirection().getOpposite());
				if (this.blocks.contains(hanging.getX(), hanging.getY(), hanging.getZ())) {
					entities.add(entity);
				}
			} else if (CompatMods.CREATE.isLoaded()) {
				if (entity instanceof final SeatEntity seat) {
					final BlockPos p = seat.blockPosition();
					if (this.blocks.contains(p.getX(), p.getY(), p.getZ())) {
						entities.add(entity);
					}
				} else if (entity instanceof final SuperGlueEntity glue) {
					final AABB box = glue.getBoundingBox();
					if (streamBlocksInAABB(box).anyMatch((p) -> this.blocks.contains(p.getX(), p.getY(), p.getZ()))) {
						entities.add(entity);
					}
				}
			}
		}

		// move blocks
		this.blocks.forEach((x, y, z) -> {
			final BlockPos pos = new BlockPos(x, y, z);
			final BlockPos target = pos.offset(offset.x, offset.y, offset.z);
			BlockState state = level.getBlockState(pos);

			final BlockEntity be = level.getBlockEntity(pos);

			state = level.getBlockState(pos);
			blockStates.add(new Pair<>(pos, state));
			final CompoundTag nbt = level.getChunkAt(pos).getBlockEntityNbtForSaving(pos);
			if (nbt != null) {
				final BlockPos targetPos = pos.offset(offset.x, offset.y, offset.z);
				nbt.putInt("x", targetPos.getX());
				nbt.putInt("y", targetPos.getY());
				nbt.putInt("z", targetPos.getZ());
				level.getChunkAt(targetPos).setBlockEntityNbt(nbt);
			}

			Clearable.tryClear(be);

			level.setBlock(target, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			// Note: Block.UPDATE_SUPPRESS_DROPS only works for Level.destroyBlock which drop the block's item form,
			// and it does not prevent contents from dropping.
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			return null;
		});

		// move entities
		for (final Entity entity : entities) {
			final Vec3 pos = entity.position();
			entity.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
		}

		// update blocks
		for (final Pair<BlockPos, BlockState> value : blockStates) {
			final BlockPos pos = value.left();
			final Block block = value.right().getBlock();
			level.blockUpdated(pos, block);
			level.blockUpdated(pos.offset(offset.x, offset.y, offset.z), block);
		}

		AmmoShipAttachment.create(ship);

		final AABBic box = ship.getShipAABB();
		final Vector3d absPosition = ship.getTransform().getPositionInWorld().add(ship.getInertiaData().getCenterOfMassInShip(), new Vector3d()).sub(shipCenter.x, shipCenter.y, shipCenter.z);
		final Vector3d position = new Vector3d(absPosition);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		final Vector3d scaling = new Vector3d(1);
		double scale = 1.0;

		final ServerShip selfShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
		if (selfShip != null) {
			final ShipTransform selfTransform = selfShip.getTransform();
			selfTransform.getShipToWorld().transformPosition(position);
			rotation.set(selfTransform.getShipToWorldRotation());
			velocity.set(selfShip.getVelocity());
			omega.set(selfShip.getOmega());
			scaling.set(selfTransform.getShipToWorldScaling());
			scale = Math.sqrt(scaling.lengthSquared() / 3);
		}
		shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, levelId, scale));

		// fix new ship's velocity and omega
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					if (!transform.getPositionInWorld().equals(nextTransform.getPositionInWorld()) || !transform.getShipToWorldRotation().equals(nextTransform.getShipToWorldRotation())) {
						ship.setTransformProvider(null);
						return null;
					}
					if (ship.getVelocity().lengthSquared() == 0 && ship.getOmega().lengthSquared() == 0) {
						if (selfShip != null) {
							final ShipTransform selfTransform2 = selfShip.getTransform();
							selfTransform2.getShipToWorld().transformPosition(absPosition, position);
							rotation.set(selfTransform2.getShipToWorldRotation());
							velocity.set(selfShip.getVelocity());
							omega.set(selfShip.getOmega());
							scaling.set(selfTransform2.getShipToWorldScaling());
						}
						return new NextTransformAndVelocityData(new ShipTransformImpl(position, nextTransform.getPositionInShip(), rotation, scaling), velocity, omega);
					}
					return null;
				}
			});
		}

		this.finishAssemble(AssembleResult.SUCCESS);
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
