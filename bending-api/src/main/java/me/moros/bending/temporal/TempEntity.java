/*
 * Copyright 2020-2023 Moros
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
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.world.World;
import me.moros.math.Vector3d;
import me.moros.tasker.TimerWheel;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempEntity extends Temporary {
  public static final TemporalManager<Integer, TempEntity> MANAGER = new TemporalManager<>(TimerWheel.simple(600));

  private final TempEntityData data;
  private boolean reverted = false;

  private TempEntity(TempEntityData data, int ticks) {
    this.data = data;
    MANAGER.addEntry(data.id, this, ticks);
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

  public static FallingBlockBuilder fallingBlock(BlockState data) {
    return new FallingBlockBuilder(Objects.requireNonNull(data));
  }

  public static ArmorStandBuilder armorStand(Item data) {
    return new ArmorStandBuilder(Objects.requireNonNull(data));
  }

  private static final Vector3d armorStandOffset = Vector3d.of(0, 1.8, 0);
  private static final Vector3d fallingBlockOffset = Vector3d.of(0.5, 0, 0.5);

  public static final class FallingBlockBuilder extends TempEntityBuilder<BlockState, TempEntity, FallingBlockBuilder> {
    private FallingBlockBuilder(BlockState data) {
      super(data);
    }

    public TempFallingBlock buildReal(Block block) {
      return buildReal(block.world(), block.toVector3d().add(fallingBlockOffset));
    }

    public TempFallingBlock buildReal(World world, Vector3d center) {
      return (TempFallingBlock) packetIfSupported(false).build(world, center);
    }

    public TempEntity build(Block block) {
      return build(block.world(), block.toVector3d().add(fallingBlockOffset));
    }

    @Override
    public TempEntity build(World world, Vector3d center) {
      Objects.requireNonNull(center);
      if (particles) {
        Vector3d offset = Vector3d.of(0.25, 0.125, 0.25);
        data.asParticle(center).count(6).offset(offset).spawn(world);
      }
      return packetIfSupported ? spawn(world, center) : spawnReal(world, center);
    }

    public TempEntity spawn(World world, Vector3d center) {
      int id = NativeAdapter.instance().createFallingBlock(world, center, data, velocity, gravity);
      return new TempEntity(new TempEntityData(id), MANAGER.fromMillis(duration, 600));
    }

    public TempFallingBlock spawnReal(World world, Vector3d center) {
      Entity entity = world.createFallingBlock(center, data, gravity);
      entity.velocity(velocity);
      return new TempFallingBlock(entity, data, MANAGER.fromMillis(duration, 600));
    }
  }

  public static final class ArmorStandBuilder extends TempEntityBuilder<Item, TempEntity, ArmorStandBuilder> {
    private ArmorStandBuilder(Item data) {
      super(data);
    }

    public TempEntity build(World world, Vector3d center) {
      Objects.requireNonNull(world);
      Objects.requireNonNull(center);
      if (particles) {
        Vector3d offset = Vector3d.of(0.25, 0.125, 0.25);
        data.asParticle(center.add(armorStandOffset)).count(6).offset(offset).spawn(world);
      }
      return new TempEntity(armorStand(world, center), MANAGER.fromMillis(duration, 600));
    }

    private TempEntityData armorStand(World world, Vector3d center) {
      if (packetIfSupported) {
        return new TempEntityData(NativeAdapter.instance().createArmorStand(world, center, data, velocity, gravity));
      }
      Entity entity = world.createArmorStand(center, data, gravity);
      entity.velocity(velocity);
      return new TempEntityData(entity);
    }
  }

  private record TempEntityData(int id, @Nullable Entity entity) {
    private TempEntityData(int id) {
      this(id, null);
    }

    private TempEntityData(Entity entity) {
      this(entity.id(), entity);
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
    private final Entity fallingBlock;
    private final BlockState state;

    private TempFallingBlock(Entity fallingBlock, BlockState state, int ticks) {
      super(new TempEntityData(fallingBlock), ticks);
      this.fallingBlock = fallingBlock;
      this.state = state;
    }

    public Entity entity() {
      return fallingBlock;
    }

    public BlockState state() {
      return state;
    }

    public Vector3d center() {
      return fallingBlock.location().add(0, 0.5, 0);
    }

    public boolean isValid() {
      return !isReverted() && fallingBlock.valid();
    }
  }
}
