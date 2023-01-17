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

package me.moros.bending.ability.earth.passive;

import java.util.List;
import java.util.function.Predicate;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class DensityShift extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;

  public DensityShift(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  private boolean isSoftened() {
    if (!user.canBend(description())) {
      return false;
    }
    Block block = user.block().offset(Direction.DOWN);
    if (EarthMaterials.isEarthbendable(user, block)) {
      softenArea();
      return true;
    }
    return MaterialUtil.isTransparent(block);
  }

  public static boolean isSoftened(User user) {
    return user.game().abilityManager(user.worldKey()).firstInstance(user, DensityShift.class)
      .map(DensityShift::isSoftened).orElse(false);
  }

  private void softenArea() {
    Vector3d center = user.block().offset(Direction.DOWN).center();
    Predicate<Block> predicate = b -> EarthMaterials.isEarthOrSand(b) && !b.offset(Direction.UP).type().isCollidable();
    for (Block b : user.world().nearbyBlocks(center, userConfig.radius, predicate)) {
      if (MaterialUtil.isAir(b.offset(Direction.DOWN)) || !TempBlock.isBendable(b)) {
        continue;
      }
      TempBlock.builder(MaterialUtil.softType(b.type())).bendable(true).weak(true).duration(userConfig.duration).build(b);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.DURATION)
    private long duration = 6000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 2;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "passives", "densityshift");
    }
  }
}

