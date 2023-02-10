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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.Pillar;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class RaiseEarth extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Block origin;
  private Predicate<Block> predicate;
  private Collection<Block> raisedCache;
  private final MultiUpdatable<Pillar> pillars = MultiUpdatable.empty();

  private long interval = 100;

  public RaiseEarth(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    origin = user.find(userConfig.selectRange, predicate);
    if (origin == null) {
      return false;
    }

    loadRaised();

    boolean wall = method == Activation.SNEAK;
    if (wall) {
      raiseWall(userConfig.wallMaxHeight, userConfig.wallWidth);
    } else {
      origin.world().findTop(origin, userConfig.columnMaxHeight, predicate).ifPresent(b -> createPillar(b, userConfig.columnMaxHeight));
    }
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), wall ? userConfig.wallCooldown : userConfig.columnCooldown);
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
  }

  public boolean activate(User user, Block source, int height, int width, long interval) {
    this.user = user;
    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    origin = source;

    loadRaised();

    this.interval = interval;
    raiseWall(height, width);
    if (!pillars.isEmpty()) {
      removalPolicy = Policies.builder().build();
      return true;
    }
    return false;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    return pillars.update();
  }

  private void createPillar(Block block, int height) {
    for (Block b : new Block[]{block, block.offset(Direction.DOWN)}) { // require at least 2 blocks
      if (!predicate.test(b) || !TempBlock.isBendable(b) || isRaised(b)) {
        return;
      }
    }
    if (MaterialUtil.isTransparentOrWater(block.offset(Direction.DOWN, height))) {
      return;
    }
    Pillar.builder(user, block).interval(interval).predicate(predicate).build(height).ifPresent(pillars::add);
  }

  private void loadRaised() {
    raisedCache = user.game().abilityManager(user.worldKey()).instances(RaiseEarth.class)
      .flatMap(RaiseEarth::pillars).map(Pillar::pillarBlocks).flatMap(Collection::stream)
      .collect(Collectors.toUnmodifiableSet());
  }

  private boolean isRaised(Block block) {
    return raisedCache.contains(block);
  }

  private void raiseWall(int height, int width) {
    double w = (width - 1) / 2.0;
    Vector3d side = user.direction().cross(Vector3d.PLUS_J).normalize();
    Vector3d center = origin.center();
    int min = -FastMath.ceil(w);
    int max = FastMath.floor(w);
    for (int i = min; i <= max; i++) {
      Block check = user.world().blockAt(center.add(side.multiply(i)));
      if (MaterialUtil.isTransparentOrWater(check)) {
        for (int j = 1; j < height; j++) {
          Block block = check.offset(Direction.DOWN, j);
          if (predicate.test(block) && !isRaised(block)) {
            createPillar(block, height);
            break;
          } else if (!MaterialUtil.isTransparentOrWater(block)) {
            break;
          }
        }
      } else {
        check.world().findTop(check, height, predicate).ifPresent(b -> createPillar(b, height));
      }
    }
  }

  /**
   * @return a stream of the pillars
   */
  public Stream<Pillar> pillars() {
    return pillars.stream();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 16;
    @Modifiable(Attribute.COOLDOWN)
    private long columnCooldown = 500;
    @Modifiable(Attribute.HEIGHT)
    private int columnMaxHeight = 6;
    @Modifiable(Attribute.COOLDOWN)
    private long wallCooldown = 1500;
    @Modifiable(Attribute.HEIGHT)
    private int wallMaxHeight = 6;
    @Modifiable(Attribute.RADIUS)
    private int wallWidth = 6;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "raiseearth");
    }
  }
}
