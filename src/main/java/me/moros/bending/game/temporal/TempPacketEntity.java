/*
 * Copyright 2020-2021 Moros
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

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.packet.PacketUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TempPacketEntity implements Temporary {
  public static final TemporalManager<Integer, TempPacketEntity> MANAGER = new TemporalManager<>();

  protected final int id;
  protected boolean reverted = false;

  public static void init() {
  }

  private TempPacketEntity(int id, long duration) {
    this.id = id;
    MANAGER.addEntry(id, this, Temporary.toTicks(duration));
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    PacketUtil.destroy(id);
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
      int id = switch (type) {
        case FALLING_BLOCK -> PacketUtil.createFallingBlock(world, center, data, velocity, gravity);
        case ARMOR_STAND -> PacketUtil.createArmorStand(world, center, data.getMaterial(), velocity, gravity);
      };
      if (particles) {
        spawnParticles(center.add(armorStandOffset).toLocation(world));
      }
      return new TempPacketEntity(id, duration);
    }

    private void spawnParticles(Location spawnLoc) {
      ParticleUtil.of(Particle.BLOCK_CRACK, spawnLoc).count(4).offset(0.25, 0.125, 0.25)
        .data(data).spawn();
      ParticleUtil.of(Particle.BLOCK_DUST, spawnLoc).count(6).offset(0.25, 0.125, 0.25)
        .data(data).spawn();
    }
  }
}
