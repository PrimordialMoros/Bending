/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game.temporal;

import java.util.Objects;

import me.moros.bending.adapter.PacketAdapter;
import me.moros.bending.adapter.impl.NativeAdapter;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class TempPacketEntity extends TemporaryBase {
  public static final TemporalManager<Integer, TempPacketEntity> MANAGER = new TemporalManager<>("Packet Entity");

  private final int id;
  private final Entity entity;
  private boolean reverted = false;

  private TempPacketEntity(int id, Entity entity, long duration) {
    super();
    this.id = id;
    this.entity = entity;
    MANAGER.addEntry(id, this, Temporary.toTicks(duration));
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    if (entity != null) {
      entity.remove();
    } else {
      NativeAdapter.instance().packetAdapter().destroy(id);
    }
    MANAGER.removeEntry(id);
    return true;
  }

  public boolean valid() {
    return !reverted;
  }

  public static @NonNull Builder builder(@NonNull BlockData data) {
    return new Builder(Objects.requireNonNull(data));
  }

  private enum Type {
    ARMOR_STAND,
    FALLING_BLOCK
  }

  public static final class Builder {
    private static final Vector3d armorStandOffset = new Vector3d(0, 1.1, 0);
    private final BlockData data;

    private Vector3d velocity = Vector3d.ZERO;
    private boolean particles = false;
    private boolean gravity = true;
    private long duration = 30000;

    private Builder(BlockData data) {
      this.data = data;
    }

    public @NonNull Builder velocity(@NonNull Vector3d velocity) {
      this.velocity = Objects.requireNonNull(velocity);
      return this;
    }

    public @NonNull Builder particles(boolean particles) {
      this.particles = particles;
      return this;
    }

    public @NonNull Builder gravity(boolean gravity) {
      this.gravity = gravity;
      return this;
    }

    public @NonNull Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public @NonNull TempPacketEntity buildFallingBlock(@NonNull World world, @NonNull Vector3d center) {
      return build(Type.FALLING_BLOCK, world, center);
    }

    public @NonNull TempPacketEntity buildArmorStand(@NonNull World world, @NonNull Vector3d center) {
      return build(Type.ARMOR_STAND, world, center);
    }

    private TempPacketEntity build(Type type, World world, Vector3d center) {
      Objects.requireNonNull(world);
      Objects.requireNonNull(center);
      if (particles) {
        spawnParticles(world, type == Type.ARMOR_STAND ? center.add(armorStandOffset) : center);
      }
      if (NativeAdapter.instance().isNative()) {
        PacketAdapter packetAdapter = NativeAdapter.instance().packetAdapter();
        int id = switch (type) {
          case FALLING_BLOCK -> packetAdapter.createFallingBlock(world, center, data, velocity, gravity);
          case ARMOR_STAND -> packetAdapter.createArmorStand(world, center, data.getMaterial(), velocity, gravity);
        };
        return new TempPacketEntity(id, null, duration);
      } else {
        Entity entity = switch (type) {
          case FALLING_BLOCK -> spawnFallingBlock(world, center, data);
          case ARMOR_STAND -> spawnArmorStand(world, center, data);
        };
        return new TempPacketEntity(entity.getEntityId(), entity, duration);
      }
    }

    private void spawnParticles(World world, Vector3d spawnLoc) {
      Vector3d offset = new Vector3d(0.25, 0.125, 0.25);
      ParticleUtil.of(Particle.BLOCK_CRACK, spawnLoc).count(4).offset(offset).data(data).spawn(world);
      ParticleUtil.of(Particle.BLOCK_DUST, spawnLoc).count(6).offset(offset).data(data).spawn(world);
    }

    private Entity spawnFallingBlock(World world, Vector3d center, BlockData data) {
      FallingBlock entity = world.spawnFallingBlock(center.toLocation(world), data);
      entity.setDropItem(false);
      entity.setGravity(gravity);
      entity.setVelocity(velocity.clampVelocity());
      return entity;
    }

    private Entity spawnArmorStand(World world, Vector3d center, BlockData data) {
      Entity entity = world.spawn(center.toLocation(world), ArmorStand.class, as -> {
        as.setInvulnerable(true);
        as.setVisible(false);
        as.setGravity(gravity);
        as.getEquipment().setHelmet(new ItemStack(data.getMaterial()));
      });
      entity.setVelocity(velocity.clampVelocity());
      return entity;
    }
  }
}
