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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Catapult extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);
  private static final double ANGLE = Math.toRadians(60);

  private User user;
  private Config userConfig;

  private Pillar pillar;

  private long startTime;

  public Catapult(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    return launch(method == Activation.SNEAK);
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (System.currentTimeMillis() > startTime + 100) {
      return pillar == null ? UpdateResult.REMOVE : pillar.update();
    }
    return UpdateResult.CONTINUE;
  }

  private @Nullable Block getBase() {
    AABB entityBounds = AABBUtil.entityBounds(user.entity()).grow(new Vector3d(0, 0.2, 0));
    AABB floorBounds = new AABB(new Vector3d(-1, -0.5, -1), new Vector3d(1, 0, 1)).at(user.location());
    return WorldUtil.nearbyBlocks(user.world(), floorBounds, b -> entityBounds.intersects(AABBUtil.blockBounds(b))).stream()
      .filter(this::isValidBlock)
      .min(Comparator.comparingDouble(b -> Vector3d.center(b).distanceSq(user.location())))
      .orElse(null);
  }

  private boolean isValidBlock(Block block) {
    if (block.isLiquid() || !TempBlock.isBendable(block) || !user.canBuild(block)) {
      return false;
    }
    return EarthMaterials.isEarthbendable(user, block);
  }

  private boolean launch(boolean sneak) {
    double angle = Vector3d.PLUS_J.angle(user.direction());

    int length = 0;
    Block base = getBase();
    boolean forceVertical = false;
    boolean horizontal = false;
    if (base != null) {
      length = getLength(new Vector3d(base.getRelative(BlockFace.UP)), Vector3d.MINUS_J);
      if (angle > ANGLE) {
        forceVertical = true;
      }
      pillar = Pillar.builder(user, base, EarthPillar::new)
        .predicate(b -> !b.isLiquid() && EarthMaterials.isEarthbendable(user, b))
        .build(3, 1).orElse(null);
    } else {
      if (angle >= ANGLE && angle <= 2 * ANGLE) {
        length = getLength(user.location(), user.direction().negate());
        horizontal = true;
      }
    }

    if (length == 0) {
      return false;
    }

    startTime = System.currentTimeMillis();
    user.addCooldown(description(), userConfig.cooldown);

    Vector3d origin = user.location().add(new Vector3d(0, 0.5, 0));
    SoundUtil.EARTH.play(user.world(), origin);
    if (base != null) {
      ParticleUtil.of(Particle.BLOCK_CRACK, origin).count(8).offset(0.4).data(base.getBlockData()).spawn(user.world());
    }

    Vector3d direction = forceVertical ? Vector3d.PLUS_J : user.direction();
    double horizontalFactor = horizontal ? userConfig.horizontalFactor : 1;
    double factor = length / (double) userConfig.length;
    double power = horizontalFactor * factor * (sneak ? userConfig.sneakPower : userConfig.clickPower);
    return CollisionUtil.handle(user, new Sphere(origin, 1.5), entity -> {
      BendingEffect.FIRE_TICK.reset(entity);
      EntityUtil.applyVelocity(this, entity, direction.multiply(power));
      return true;
    }, true, true);
  }

  private int getLength(Vector3d origin, Vector3d direction) {
    Set<Block> checked = new HashSet<>();
    for (double i = 0.5; i <= userConfig.length; i += 0.5) {
      Block block = origin.add(direction.multiply(i)).toBlock(user.world());
      if (!checked.contains(block)) {
        if (!isValidBlock(block)) {
          return FastMath.ceil(i) - 1;
        }
        checked.add(block);
      }
    }
    return userConfig.length;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static final class EarthPillar extends Pillar {
    private EarthPillar(Builder builder) {
      super(builder);
    }

    @Override
    public void playSound(Block block) {
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Modifiable(Attribute.STRENGTH)
    private double sneakPower = 2.65;
    @Modifiable(Attribute.STRENGTH)
    private double clickPower = 1.8;
    @Modifiable(Attribute.STRENGTH)
    private double horizontalFactor = 1.4;
    private int length = 7;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "catapult");
    }
  }
}
