/*
 * Copyright 2020-2025 Moros
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.display.BlockDisplayBuilder;
import me.moros.bending.api.platform.entity.display.Display;
import me.moros.bending.api.platform.entity.display.DisplayBuilder;
import me.moros.bending.api.platform.entity.display.ItemDisplayBuilder;
import me.moros.bending.api.platform.entity.display.TextDisplayBuilder;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempDisplayEntity extends Temporary {
  public static final TemporalManager<Integer, TempDisplayEntity> MANAGER = new TemporalManager<>(600) {
    @Override
    public void tick() {
      super.tick();
      TICKING_PHYSICS.values().forEach(DisplayMeta::tick);
    }
  };

  private static final Map<Integer, DisplayMeta> TICKING_PHYSICS = new HashMap<>();

  private final int id;
  private boolean reverted = false;

  private TempDisplayEntity(int id, int ticks) {
    this.id = id;
    MANAGER.addEntry(this.id, this, ticks);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    TICKING_PHYSICS.remove(id);
    Platform.instance().nativeAdapter().destroy(id);
    MANAGER.removeEntry(id);
    return true;
  }

  public static Builder<BlockDisplayBuilder> builder(BlockType data) {
    return builder(data.defaultState());
  }

  public static Builder<BlockDisplayBuilder> builder(BlockState data) {
    return builder(Display.block(data));
  }

  public static Builder<ItemDisplayBuilder> builder(Item data) {
    return builder(Display.item(data));
  }

  public static Builder<TextDisplayBuilder> builder(Component data) {
    return builder(Display.text(data));
  }

  public static <V, T extends DisplayBuilder<V, T>> Builder<T> builder(T builder) {
    return new Builder<>(builder);
  }

  public static final class Builder<B extends DisplayBuilder<?, B>> extends TempEntityBuilder<B, TempDisplayEntity, Builder<B>> {
    private static final Vector3d BLOCK_OFFSET = Vector3d.of(0.5, 0, 0.5);

    private double minYOffset = -0.1;

    private Builder(B data) {
      super(data);
      gravity(false);
    }

    public Builder<B> edit(Consumer<B> consumer) {
      consumer.accept(data);
      return this;
    }

    public Builder<B> minYOffset(double minYOffset) {
      this.minYOffset = minYOffset;
      return this;
    }

    public @Nullable TempDisplayEntity build(Block block) {
      return build(block.world(), block.toVector3d().add(BLOCK_OFFSET));
    }

    @Override
    public @Nullable TempDisplayEntity build(World world, Vector3d center) {
      return displayEntity(world, center);
    }

    private @Nullable TempDisplayEntity displayEntity(World world, Vector3d center) {
      boolean hasPhysics = gravity || velocity.lengthSq() > 0;
      if (hasPhysics) {
        data.positionInterpolationDuration(1);
      }
      var properties = data.build();
      var packet = Platform.instance().nativeAdapter().createDisplayEntity(center, properties);
      var id = packet.id();
      if (id <= 0) {
        return null;
      }
      if (viewers.isEmpty()) {
        packet.broadcast(world, center);
      } else {
        packet.send(Set.copyOf(viewers));
      }
      var result = new TempDisplayEntity(id, MANAGER.fromMillis(duration));
      if (hasPhysics) {
        Vector3d pos = center.add(properties.transformation().translation());
        TICKING_PHYSICS.put(id, new DisplayMeta(id, world, pos, velocity, minYOffset));
      }
      return result;
    }
  }

  private static final class DisplayMeta {
    // Matching falling block entities
    private static final Vector3d GRAVITY = Vector3d.of(0, -0.04, 0);
    private static final double DRAG = 0.98;

    private final int id;
    private final World world;
    private final double minYOffset;

    private Vector3d position;
    private Vector3d velocity;

    private DisplayMeta(int id, World world, Vector3d position, Vector3d velocity, double minYOffset) {
      this.id = id;
      this.world = world;
      this.position = position;
      this.minYOffset = position.y() + minYOffset;
      this.velocity = velocity;
    }

    private void tick() {
      this.velocity = velocity.add(GRAVITY).clampVelocity();
      move();
      this.velocity = velocity.multiply(DRAG);
    }

    private void move() {
      position = position.add(velocity);
      if (position.y() < minYOffset) {
        position = position.withY(minYOffset);
        return;
      }
      Platform.instance().nativeAdapter().updateDisplayPosition(id, position).broadcast(world, position);
    }
  }
}
