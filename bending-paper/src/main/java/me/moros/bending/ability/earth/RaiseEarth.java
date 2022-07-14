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

package me.moros.bending.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.Pillar;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
  private final Collection<Pillar> pillars = new ArrayList<>();

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
      WorldUtil.findTopBlock(origin, userConfig.columnMaxHeight, predicate).ifPresent(b -> createPillar(b, userConfig.columnMaxHeight));
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
    pillars.removeIf(pillar -> pillar.update() == UpdateResult.REMOVE);
    return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private void createPillar(Block block, int height) {
    for (Block b : new Block[]{block, block.getRelative(BlockFace.DOWN)}) { // require at least 2 blocks
      if (!predicate.test(b) || !TempBlock.isBendable(b) || isRaised(b)) {
        return;
      }
    }
    if (MaterialUtil.isTransparentOrWater(block.getRelative(BlockFace.DOWN, height))) {
      return;
    }
    Pillar.builder(user, block).interval(interval).predicate(predicate).build(height).ifPresent(pillars::add);
  }

  private void loadRaised() {
    raisedCache = user.game().abilityManager(user.world()).instances(RaiseEarth.class)
      .map(RaiseEarth::pillars).flatMap(Collection::stream)
      .map(Pillar::pillarBlocks).flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private boolean isRaised(Block block) {
    return raisedCache.contains(block);
  }

  private void raiseWall(int height, int width) {
    double w = (width - 1) / 2.0;
    Vector3d side = user.direction().cross(Vector3d.PLUS_J).normalize();
    Vector3d center = Vector3d.center(origin);
    int min = -FastMath.ceil(w);
    int max = FastMath.floor(w);
    for (int i = min; i <= max; i++) {
      Block check = center.add(side.multiply(i)).toBlock(user.world());
      if (MaterialUtil.isTransparentOrWater(check)) {
        for (int j = 1; j < height; j++) {
          Block block = check.getRelative(BlockFace.DOWN, j);
          if (predicate.test(block) && !isRaised(block)) {
            createPillar(block, height);
            break;
          } else if (!MaterialUtil.isTransparentOrWater(block)) {
            break;
          }
        }
      } else {
        WorldUtil.findTopBlock(check, height, predicate).ifPresent(b -> createPillar(b, height));
      }
    }
  }

  /**
   * @return an unmodifiable view of the pillars
   */
  public Collection<Pillar> pillars() {
    return List.copyOf(pillars);
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
    public Iterable<String> path() {
      return List.of("abilities", "earth", "raiseearth");
    }
  }
}
