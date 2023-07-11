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

package me.moros.bending.api.temporal;

import java.util.Objects;

import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.DelegateEntity;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempEntity extends Temporary {
  public static final TemporalManager<Integer, TempEntity> MANAGER = new TemporalManager<>(600);

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

  @Deprecated
  public static ArmorStandBuilder armorStand(Item data) {
    return new ArmorStandBuilder(Objects.requireNonNull(data));
  }

  public static final class FallingBlockBuilder extends TempEntityBuilder<BlockState, TempEntity, FallingBlockBuilder> {
    private static final Vector3d FALLING_BLOCK_OFFSET = Vector3d.of(0.5, 0, 0.5);

    private FallingBlockBuilder(BlockState data) {
      super(data);
    }

    public TempFallingBlock buildReal(Block block) {
      return buildReal(block.world(), block.toVector3d().add(FALLING_BLOCK_OFFSET));
    }

    public TempFallingBlock buildReal(World world, Vector3d center) {
      return spawnReal(world, center);
    }

    public TempEntity build(Block block) {
      return build(block.world(), block.toVector3d().add(FALLING_BLOCK_OFFSET));
    }

    @Override
    public TempEntity build(World world, Vector3d center) {
      int id = Platform.instance().nativeAdapter().createFallingBlock(world, center, data, velocity, gravity);
      if (id > 0) {
        return new TempEntity(new TempEntityData(id), MANAGER.fromMillis(duration));
      }
      return spawnReal(world, center);
    }

    private TempFallingBlock spawnReal(World world, Vector3d center) {
      Entity entity = world.createFallingBlock(center, data, gravity);
      entity.velocity(velocity);
      return new TempFallingBlock(entity, data, MANAGER.fromMillis(duration));
    }
  }

  @Deprecated
  public static final class ArmorStandBuilder extends TempEntityBuilder<Item, TempEntity, ArmorStandBuilder> {
    private ArmorStandBuilder(Item data) {
      super(data);
    }

    @Override
    public TempEntity build(World world, Vector3d center) {
      return new TempEntity(armorStand(world, center), MANAGER.fromMillis(duration));
    }

    private TempEntityData armorStand(World world, Vector3d center) {
      int id = Platform.instance().nativeAdapter().createArmorStand(world, center, data, velocity, gravity);
      if (id > 0) {
        return new TempEntityData(id);
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
        Platform.instance().nativeAdapter().destroy(id);
      }
      return id;
    }
  }

  public static final class TempFallingBlock extends TempEntity implements DelegateEntity {
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

    @Override
    public Vector3d center() {
      return fallingBlock.location().add(0, 0.5, 0);
    }

    @Override
    public boolean valid() {
      return !isReverted();
    }
  }
}
