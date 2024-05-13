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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Collapse extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Predicate<Block> predicate;
  private final MultiUpdatable<Pillar> pillars = MultiUpdatable.empty();

  private int height;

  public Collapse(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    Block origin = user.find(userConfig.selectRange, predicate);
    if (origin == null) {
      return false;
    }

    height = userConfig.maxHeight;
    boolean sneak = method == Activation.SNEAK;
    if (sneak) {
      int offset = FastMath.ceil(userConfig.radius);
      int size = offset * 2 + 1;
      // Micro optimization, construct 2d map of pillar locations to avoid instantiating pillars in the same x, z with different y
      boolean[][] checked = new boolean[size][size];
      for (Block block : user.world().nearbyBlocks(origin.center(), userConfig.radius, predicate)) {
        if (block.blockY() < origin.blockY()) {
          continue;
        }
        int dx = offset + origin.blockX() - block.blockX();
        int dz = offset + origin.blockZ() - block.blockZ();
        if (checked[dx][dz]) {
          continue;
        }
        Optional<Pillar> pillar = block.world().findBottom(block, height, predicate).flatMap(this::createPillar);
        if (pillar.isPresent()) {
          checked[dx][dz] = true;
          pillars.add(pillar.get());
        }
      }
    } else {
      origin.world().findBottom(origin, height, predicate).flatMap(this::createPillar).ifPresent(pillars::add);
    }
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.defaults();
      return true;
    }
    return false;
  }

  public boolean activate(User user, Collection<Block> sources, int height) {
    this.user = user;
    loadConfig();
    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    this.height = height;
    for (Block block : sources) {
      block.world().findBottom(block, this.height, predicate).flatMap(this::createPillar).ifPresent(pillars::add);
    }
    if (!pillars.isEmpty()) {
      removalPolicy = Policies.defaults();
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

  private Optional<Pillar> createPillar(Block block) {
    if (!predicate.test(block) || !TempBlock.isBendable(block)) {
      return Optional.empty();
    }
    return Pillar.builder(user, block)
      .direction(Direction.DOWN)
      .interval(75)
      .predicate(predicate).build(height);
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 18;
    @Modifiable(Attribute.RADIUS)
    private double radius = 6;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;
    @Modifiable(Attribute.HEIGHT)
    private int maxHeight = 6;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "collapse");
    }
  }
}
