/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractSpout;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockStateProperties;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempBlock.Builder;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.bending.common.ability.SpoutAbility;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class WaterSpout extends AbilityInstance implements SpoutAbility {
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
    if (user.game().abilityManager(user.worldKey()).destroyUserInstances(user, WaterSpout.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    double h = userConfig.height + 2;
    Block block = AbstractSpout.blockCast(user.block(), h);
    if (block == null || !predicate.test(block)) {
      return false;
    }

    removalPolicy = Policies.defaults();

    spout = new Spout();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
    spout.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(spout.collider());
  }

  @Override
  public void handleMovement(Vector3d velocity) {
    if (spout != null) {
      spout.limitVelocity(velocity, userConfig.maxSpeed);
    }
  }

  private final class Spout extends AbstractSpout {
    private Position lastPosition;
    private final Vector3d g = Vector3d.of(0, -0.1, 0); // Applied as extra gravity

    private Spout() {
      super(user, userConfig.height);
      validBlock = predicate;
    }

    @Override
    public void render() {
      Position newPosition = user.location().toVector3i();
      if (newPosition.equals(lastPosition)) {
        return;
      }
      lastPosition = newPosition;
      column.forEach(this::clean);
      column.clear();
      ignore.clear();
      Block block = user.block();
      TempBlock.water().build(block).ifPresent(tb -> column.add(block));
      BlockState state = BlockType.BUBBLE_COLUMN.defaultState().withProperty(BlockStateProperties.DRAG, false);
      Builder bubbles = TempBlock.builder(state);
      for (int i = 1; i < distance - 1; i++) {
        bubbles.build(block.offset(Direction.DOWN, i)).ifPresent(tb -> column.add(tb.block()));
      }
      ignore.addAll(column);
    }

    @Override
    public void postRender() {
      if (user.checkProperty(EntityProperties.FLYING) != TriState.TRUE) {
        user.applyVelocity(WaterSpout.this, user.velocity().add(g));
      }
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundEffect.WATER.play(user.world(), user.location());
      }
    }

    private void clean(Block block) {
      if (MaterialUtil.isWater(block)) {
        TempBlock.air().build(block);
      }
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 0;
    @Modifiable(Attribute.HEIGHT)
    private double height = 14;
    @Modifiable(Attribute.SPEED)
    private double maxSpeed = 0.2;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "waterspout");
    }
  }
}
