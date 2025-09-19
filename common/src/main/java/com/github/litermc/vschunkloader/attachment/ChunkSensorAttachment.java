package com.github.litermc.vschunkloader.attachment;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCApi;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.ChunkSensor;
import com.github.litermc.vschunkloader.util.TaskUtil;
import com.github.litermc.vschunkloader.util.Utils;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.joml.AxisAngle4d;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.PoseVel;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChunkSensorAttachment implements ShipForcesInducer {
	private static final double DT = 1.0 / 60;

	private final ServerShipWorldCore world;
	private volatile LoadedServerShip ship = null;
	@JsonProperty
	private volatile boolean freezed = false;
	private volatile Vector3dc velocity = null;
	private volatile Vector3dc omega = null;

	private ChunkSensorAttachment() {
		this.world = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
	}

	public static ChunkSensorAttachment get(final ServerShip ship) {
		final ChunkSensorAttachment attachment = ship.getAttachment(ChunkSensorAttachment.class);
		if (attachment != null) {
			return attachment;
		}
		final ChunkSensorAttachment newAttachment = new ChunkSensorAttachment();
		ship.saveAttachment(ChunkSensorAttachment.class, newAttachment);
		return newAttachment;
	}

	@JsonGetter("velocity")
	public double[] getVelocity() {
		return this.freezed ? new double[]{this.velocity.x(), this.velocity.y(), this.velocity.z()} : null;
	}

	@JsonSetter("velocity")
	public void setVelocity(final double[] velocity) {
		if (velocity == null || velocity.length != 3) {
			this.velocity = null;
			return;
		}
		this.velocity = new Vector3d(velocity[0], velocity[1], velocity[2]);
	}

	@JsonGetter("omega")
	public double[] getOmega() {
		return this.freezed ? new double[]{this.omega.x(), this.omega.y(), this.omega.z()} : null;
	}

	@JsonSetter("omega")
	public void setOmega(final double[] omega) {
		if (omega == null || omega.length != 3) {
			this.omega = null;
			return;
		}
		this.omega = new Vector3d(omega[0], omega[1], omega[2]);
	}

	public void serverTick(final LoadedServerShip ship) {
		this.ship = ship;
		if (!this.freezed) {
			return;
		}
		final long shipId = ship.getId();
		if (!ship.isStatic()) {
			this.freezed = false;
			this.velocity = null;
			this.omega = null;
			Constants.LOG.warn("Ship " + shipId + " had been manually unfreezed");
			return;
		}
		final AABBic shipBox = ship.getShipAABB();
		if (shipBox == null) {
			return;
		}
		final ServerLevel level = Utils.getLevel(ship.getChunkClaimDimension());
		if (level == null) {
			return;
		}
		final Vector3dc velocity = this.velocity;
		final Vector3dc omega = this.omega;
		if (this.isShipMovingToUnloads(level, shipBox, ship.getTransform(), velocity, omega)) {
			return;
		}
		Constants.LOG.debug("Ship {} is unfreezing! position={} velocity={} omega={}", shipId, ship.getTransform().getPositionInWorld(), velocity, omega);
		this.freezed = false;
		this.velocity = null;
		this.omega = null;
		ship.setStatic(false);
		final ServerShipTransformProvider lastProvider = ship.getTransformProvider();
		ship.setTransformProvider(new ServerShipTransformProvider() {
			@Override
			public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
				ship.setTransformProvider(lastProvider);
				return new NextTransformAndVelocityData(nextTransform, velocity, omega);
			}
		});
	}

	@Override
	public void applyForces(final PhysShip phyShip) {
		final ServerShip ship = this.ship;
		if (ship == null) {
			return;
		}
		if (phyShip.isStatic() || ship.isStatic()) {
			return;
		}
		final long shipId = phyShip.getId();
		if (this.freezed) {
			// When freezing, a ship should not be ticked.
			// This may happenes when someone manually set a freezed ship unstatic.
			// So just discard the freeze datas.
			this.freezed = false;
			this.velocity = null;
			this.omega = null;
			Constants.LOG.warn("Ship " + shipId + " had been manually unfreezed.");
		}
		final AABBic shipBox = ship.getShipAABB();
		if (shipBox == null) {
			return;
		}
		final ServerLevel level = Utils.getLevel(ship.getChunkClaimDimension());
		if (level == null) {
			return;
		}
		if (!VSCApi.canFreezeShipForChunkLoad(ship)) {
			return;
		}
		final PoseVel poseVel = ((PhysShipImpl) (phyShip)).getPoseVel();
		final Vector3dc velocity = poseVel.getVel();
		final Vector3dc omega = poseVel.getOmega();
		if (!this.isShipMovingToUnloads(level, shipBox, phyShip.getTransform(), velocity, omega)) {
			return;
		}
		Constants.LOG.debug("Ship {} is freezing! position={} velocity={} omega={}", shipId, phyShip.getTransform().getPositionInWorld(), velocity, omega);
		phyShip.setStatic(true);
		ship.setStatic(true);
		this.freezed = true;
		this.velocity = velocity;
		this.omega = omega;
	}

	private boolean isShipMovingToUnloads(
		final ServerLevel level,
		final AABBic shipBox,
		final ShipTransform transform,
		final Vector3dc velocity,
		final Vector3dc omega
	) {
		final Matrix4dc oldTransform = transform.getShipToWorld();
		final Matrix4d newTransform = new Matrix4d();
		final double omegaSqr = omega.lengthSquared();
		if (omegaSqr > 1e-8) {
			final Vector3dc pos = transform.getPositionInWorld();
			newTransform
				.rotationAround(new Quaterniond(new AxisAngle4d(Math.sqrt(omegaSqr) * DT, omega.normalize(new Vector3d()))), pos.x(), pos.y(), pos.z())
				.mul(oldTransform);
		} else {
			newTransform.set(oldTransform);
		}
		newTransform.translate(velocity.mul(DT, new Vector3d()));
		final AABBd shipBoxd = new AABBd(shipBox.minX(), shipBox.minY(), shipBox.minZ(), shipBox.maxX(), shipBox.maxY(), shipBox.maxZ());
		final AABBd worldBox = shipBoxd.transform(oldTransform, new AABBd());
		final AABBd newWorldBox = shipBoxd.transform(newTransform, new AABBd());
		final int minX = Math.min(SectionPos.blockToSectionCoord(worldBox.minX), SectionPos.blockToSectionCoord(newWorldBox.minX)) - 1;
		final int maxX = Math.max(SectionPos.blockToSectionCoord(worldBox.maxX), SectionPos.blockToSectionCoord(newWorldBox.maxX)) + 1;
		final int minZ = Math.min(SectionPos.blockToSectionCoord(worldBox.minZ), SectionPos.blockToSectionCoord(newWorldBox.minZ)) - 1;
		final int maxZ = Math.max(SectionPos.blockToSectionCoord(worldBox.maxZ), SectionPos.blockToSectionCoord(newWorldBox.maxZ)) + 1;

		final ChunkSensor sensor = ChunkSensor.get(level);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!sensor.isChunkLoaded(x, z)) {
					TaskUtil.queueTickEnd(() -> ChunkLoaderManager.get(level).pingChunks(minX, maxX, minZ, maxZ));
					return true;
				}
			}
		}
		return false;
	}
}
