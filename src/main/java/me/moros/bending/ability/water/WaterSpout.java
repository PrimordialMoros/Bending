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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractSpout;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.EntityUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class WaterSpout extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> column = new ArrayList<>();
  private final Predicate<Block> predicate = WaterMaterials::isWaterNotPlant;
  private Spout spout;

  public WaterSpout(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, WaterSpout.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    double h = userConfig.height + 2;
    if (EntityUtil.distanceAboveGround(user.entity()) > h) {
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(spout.collider());
  }

  public void handleMovement(@NonNull Vector3d velocity) {
    AbstractSpout.limitVelocity(user.entity(), velocity, userConfig.maxSpeed);
  }

  private class Spout extends AbstractSpout {
    private Vector3i lastPosition;
    private final Vector3d g = new Vector3d(0, -0.1, 0); // Applied as extra gravity

    public Spout() {
      super(user, userConfig.height);
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
      for (int i = 0; i < distance - 1; i++) {
        TempBlock.create(block.getRelative(BlockFace.DOWN, i), Material.WATER.createBlockData()).ifPresent(tb -> column.add(tb.block()));
      }
      ignore.addAll(column);
    }

    @Override
    public void postRender() {
      if (!user.flying()) {
        EntityUtil.applyVelocity(WaterSpout.this, user.entity(), user.velocity().add(g));
      }
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundUtil.WATER.play(user.entity().getLocation());
      }
    }

    protected void clean(Block block) {
      if (MaterialUtil.isWater(block)) {
        TempBlock.createAir(block);
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.HEIGHT)
    public double height;
    @Modifiable(Attribute.SPEED)
    public double maxSpeed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "waterspout");

      cooldown = abilityNode.node("cooldown").getLong(0);
      height = abilityNode.node("height").getDouble(14.0);
      maxSpeed = abilityNode.node("max-speed").getDouble(0.2);
    }
  }
}
