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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.ability.common.basic.AbstractSpout;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempBlock.Builder;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.BubbleColumn;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class WaterSpout extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> column = new ArrayList<>();
  private final Predicate<Block> predicate = WaterMaterials::isWaterNotPlant;
  private Spout spout;

  public WaterSpout(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).destroyUserInstance(user, WaterSpout.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    double h = userConfig.height + 2;
    if (EntityUtil.distanceAboveGround(user.entity(), h + 1) > h) {
      return false;
    }

    Block block = AbstractSpout.blockCast(user.locBlock(), h);
    if (block == null || !predicate.test(block)) {
      return false;
    }

    removalPolicy = Policies.builder().build();

    spout = new Spout();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    return spout.update();
  }

  @Override
  public void onDestroy() {
    column.forEach(spout::clean);
    spout.flight().flying(false);
    spout.flight().release();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(spout.collider());
  }

  public void handleMovement(Vector3d velocity) {
    AbstractSpout.limitVelocity(user.entity(), velocity, userConfig.maxSpeed);
  }

  private final class Spout extends AbstractSpout {
    private Vector3i lastPosition;
    private final Vector3d g = new Vector3d(0, -0.1, 0); // Applied as extra gravity

    private Spout() {
      super(user.game().flightManager().get(user), userConfig.height);
      validBlock = predicate;
    }

    @Override
    public void render() {
      Vector3i newPosition = user.location().toVector3i();
      if (newPosition.equals(lastPosition)) {
        return;
      }
      lastPosition = newPosition;
      column.forEach(this::clean);
      column.clear();
      ignore.clear();
      Block block = user.locBlock();
      TempBlock.water().build(block).ifPresent(tb -> column.add(block));
      Builder bubbles = TempBlock.builder(Material.BUBBLE_COLUMN.createBlockData(d -> ((BubbleColumn) d).setDrag(false)));
      for (int i = 1; i < distance - 1; i++) {
        bubbles.build(block.getRelative(BlockFace.DOWN, i)).ifPresent(tb -> column.add(tb.block()));
      }
      ignore.addAll(column);
    }

    @Override
    public void postRender() {
      if (!user.flying()) {
        EntityUtil.applyVelocity(WaterSpout.this, user.entity(), user.velocity().add(g));
      }
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundUtil.WATER.play(user.world(), user.location());
      }
    }

    protected void clean(Block block) {
      if (MaterialUtil.isWater(block)) {
        TempBlock.air().build(block);
      }
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 0;
    @Modifiable(Attribute.HEIGHT)
    private double height = 14;
    @Modifiable(Attribute.SPEED)
    private double maxSpeed = 0.2;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "waterspout");
    }
  }
}
