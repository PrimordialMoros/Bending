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

package me.moros.bending.temporal;

import java.util.Objects;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.util.ParticleUtil;
import me.moros.math.Vector3d;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempEntity extends TemporaryBase {
  public static final TemporalManager<Integer, TempEntity> MANAGER = new TemporalManager<>("Entity");

  private final TempEntityData data;
  private boolean reverted = false;

  private TempEntity(TempEntityData data, long duration) {
    super();
    this.data = data;
    MANAGER.addEntry(data.id, this, Temporary.toTicks(duration));
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    MANAGER.removeEntry(data.destroy());
    return true;
  }

  protected boolean isReverted() {
    return reverted;
  }

  public static @Nullable TempEntity register(Entity entity, long duration) {
    if (entity instanceof Player) {
      return null;
    }
    return MANAGER.get(entity.getEntityId()).orElseGet(() -> new TempEntity(new TempEntityData(entity), duration));
  }

  public static TempEntity register(int entityId, long duration) {
    return MANAGER.get(entityId).orElseGet(() -> new TempEntity(new TempEntityData(entityId), duration));
  }

  public static Builder builder(BlockData data) {
    return new Builder(Objects.requireNonNull(data));
  }

  private static final Vector3d armorStandOffset = Vector3d.of(0, 1.8, 0);
  private static final Vector3d fallingBlockOffset = Vector3d.of(0.5, 0, 0.5);

  public static final class Builder {
    private final BlockData data;

    private Vector3d velocity = Vector3d.ZERO;
    private boolean packetIfSupported = NativeAdapter.hasNativeSupport();
    private boolean particles = false;
    private boolean gravity = true;
    private long duration = 30_000;

    private Builder(BlockData data) {
      this.data = data;
    }

    public Builder velocity(Vector3d velocity) {
      this.velocity = Objects.requireNonNull(velocity);
      return this;
    }

    public Builder packetIfSupported(boolean packet) {
      if (NativeAdapter.hasNativeSupport()) {
        this.packetIfSupported = packet;
      }
      return this;
    }

    public Builder particles(boolean particles) {
      this.particles = particles;
      return this;
    }

    public Builder gravity(boolean gravity) {
      this.gravity = gravity;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public TempFallingBlock build(Block block) {
      return buildAt(block, Vector3d.from(block).add(fallingBlockOffset));
    }

    public TempFallingBlock buildAt(Block block, Vector3d center) {
      Objects.requireNonNull(block);
      World world = block.getWorld();
      if (particles) {
        spawnParticles(world, center);
      }
      return new TempFallingBlock(spawnFallingBlock(this, world, center), duration);
    }

    public TempEntity build(TempEntityType type, World world, Vector3d center) {
      Objects.requireNonNull(world);
      Objects.requireNonNull(center);
      if (particles) {
        spawnParticles(world, type == TempEntityType.ARMOR_STAND ? center.add(armorStandOffset) : center);
      }
      TempEntityData entityData = type.factory.create(this, world, center);
      return new TempEntity(entityData, duration);
    }

    private void spawnParticles(World world, Vector3d spawnLoc) {
      Vector3d offset = Vector3d.of(0.25, 0.125, 0.25);
      ParticleUtil.of(Particle.BLOCK_CRACK, spawnLoc).count(4).offset(offset).data(data).spawn(world);
      ParticleUtil.of(Particle.BLOCK_DUST, spawnLoc).count(6).offset(offset).data(data).spawn(world);
    }
  }

  public enum TempEntityType {
    ARMOR_STAND(TempEntity::armorStand),
    FALLING_BLOCK(TempEntity::fallingBlock);

    private final EntityFactory factory;

    TempEntityType(EntityFactory factory) {
      this.factory = factory;
    }
  }

  private interface EntityFactory {
    TempEntityData create(Builder builder, World world, Vector3d center);
  }

  private static TempEntityData armorStand(Builder builder, World world, Vector3d center) {
    Material mat = builder.data.getMaterial();
    Vector3d vel = builder.velocity;
    boolean gravity = builder.gravity;
    if (builder.packetIfSupported) {
      return new TempEntityData(NativeAdapter.instance().createArmorStand(world, center, mat, vel, gravity));
    }
    Entity entity = world.spawn(center.toLocation(world), ArmorStand.class, as -> {
      as.setInvulnerable(true);
      as.setVisible(false);
      as.setGravity(gravity);
      as.getEquipment().setHelmet(new ItemStack(mat));
    });
    entity.setVelocity(vel.clampVelocity());
    return new TempEntityData(entity);
  }

  private static TempEntityData fallingBlock(Builder builder, World world, Vector3d center) {
    BlockData data = builder.data;
    Vector3d vel = builder.velocity;
    boolean gravity = builder.gravity;
    if (builder.packetIfSupported) {
      return new TempEntityData(NativeAdapter.instance().createFallingBlock(world, center, data, vel, gravity));
    }
    return new TempEntityData(spawnFallingBlock(builder, world, center));
  }

  private static FallingBlock spawnFallingBlock(Builder builder, World world, Vector3d center) {
    FallingBlock entity = world.spawnFallingBlock(center.toLocation(world), builder.data);
    entity.setVelocity(builder.velocity.clampVelocity());
    entity.setGravity(builder.gravity);
    entity.setDropItem(false);
    return entity;
  }

  private record TempEntityData(int id, @Nullable Entity entity) {
    private TempEntityData(int id) {
      this(id, null);
    }

    private TempEntityData(Entity entity) {
      this(entity.getEntityId(), entity);
    }

    private int destroy() {
      if (entity != null) {
        entity.remove();
      } else {
        NativeAdapter.instance().destroy(id);
      }
      return id;
    }
  }

  public static final class TempFallingBlock extends TempEntity {
    private final FallingBlock fallingBlock;

    private TempFallingBlock(FallingBlock fallingBlock, long duration) {
      super(new TempEntityData(fallingBlock), duration);
      this.fallingBlock = fallingBlock;
    }

    public FallingBlock entity() {
      return fallingBlock;
    }

    public Vector3d center() {
      return Vector3d.from(fallingBlock.getLocation()).add(fallingBlockOffset);
    }

    public boolean isValid() {
      return !isReverted() && fallingBlock.isValid();
    }
  }
}
