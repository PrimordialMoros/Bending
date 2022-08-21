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
import java.util.Optional;
import java.util.function.Predicate;

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
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Collapse extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Predicate<Block> predicate;
  private final Collection<Pillar> pillars = new ArrayList<>();

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
      for (Block block : WorldUtil.nearbyBlocks(user.world(), Vector3d.center(origin), userConfig.radius, predicate)) {
        if (block.getY() < origin.getY()) {
          continue;
        }
        int dx = offset + origin.getX() - block.getX();
        int dz = offset + origin.getZ() - block.getZ();
        if (checked[dx][dz]) {
          continue;
        }
        Optional<Pillar> pillar = WorldUtil.findBottomBlock(block, height, predicate).flatMap(this::createPillar);
        if (pillar.isPresent()) {
          checked[dx][dz] = true;
          pillars.add(pillar.get());
        }
      }
    } else {
      WorldUtil.findBottomBlock(origin, height, predicate).flatMap(this::createPillar).ifPresent(pillars::add);
    }
    if (!pillars.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
      removalPolicy = Policies.builder().build();
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
      WorldUtil.findBottomBlock(block, this.height, predicate).flatMap(this::createPillar).ifPresent(pillars::add);
    }
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

  private Optional<Pillar> createPillar(Block block) {
    if (!predicate.test(block) || !TempBlock.isBendable(block)) {
      return Optional.empty();
    }
    return Pillar.builder(user, block)
      .direction(BlockFace.DOWN)
      .interval(75)
      .predicate(predicate).build(height);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 18;
    @Modifiable(Attribute.RADIUS)
    private double radius = 6;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 500;
    @Modifiable(Attribute.HEIGHT)
    private int maxHeight = 6;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "collapse");
    }
  }
}