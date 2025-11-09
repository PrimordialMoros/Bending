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

package me.moros.bending.common.ability.earth;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.Pillar;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

public class Catapult extends AbilityInstance {
  private static final double ANGLE = Math.toRadians(60);

  private Config userConfig;

  private BlockState data;
  private Vector3d push;
  private Pillar pillar;

  private boolean launched;
  private long startTime;

  public Catapult(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    if (prepareLaunch(method == Activation.SNEAK)) {
      user.addCooldown(description(), userConfig.cooldown);
      return true;
    }
    return false;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    launch();
    if (System.currentTimeMillis() > startTime + 100) {
      return pillar == null ? UpdateResult.REMOVE : pillar.update();
    }
    return UpdateResult.CONTINUE;
  }

  private @Nullable Block getBase() {
    Vector3d center = user.location();
    AABB entityBounds = user.bounds().grow(Vector3d.of(0, 0.2, 0));
    AABB floorBounds = AABB.of(Vector3d.of(-1, -0.5, -1), Vector3d.of(1, 0, 1)).at(center);
    return user.world().nearbyBlocks(floorBounds, b -> entityBounds.intersects(b.bounds())).stream()
      .filter(this::isValidBlock)
      .min(Comparator.comparingDouble(b -> b.center().distanceSq(center)))
      .orElse(null);
  }

  private boolean isValidBlock(Block block) {
    if (block.type().isLiquid() || !TempBlock.isBendable(block) || !user.canBuild(block)) {
      return false;
    }
    return EarthMaterials.isEarthbendable(user, block);
  }

  private boolean prepareLaunch(boolean sneak) {
    Vector3d direction = user.direction();
    double angle = Vector3d.PLUS_J.angle(direction);
    int length = 0;
    Block base = getBase();
    double basePower = sneak ? userConfig.sneakPower : userConfig.clickPower;
    if (base != null) {
      length = getLength(base.offset(Direction.UP).toVector3d(), Vector3d.MINUS_J);
      if (angle > ANGLE) {
        direction = Vector3d.PLUS_J;
      }
      pillar = Pillar.builder(user, base, EarthPillar::new)
        .predicate(b -> !b.type().isLiquid() && EarthMaterials.isEarthbendable(user, b))
        .build(3, 1).orElse(null);
    } else {
      if (angle >= ANGLE && angle <= 2 * ANGLE) {
        length = getLength(user.location(), direction.negate());
        basePower *= userConfig.horizontalFactor;
      }
    }
    double factor = length / (double) userConfig.length;
    double power = basePower * factor;
    push = direction.multiply(power);
    return power > 0;
  }

  private void launch() {
    if (launched) {
      return;
    }
    launched = true;
    startTime = System.currentTimeMillis();
    Vector3d origin = user.location().add(0, 0.5, 0);
    SoundEffect.EARTH.play(user.world(), origin);
    data.asParticle(origin).count(16).offset(0.4).spawn(user.world());
    CollisionUtil.handle(user, Sphere.of(origin, 1.5), entity -> {
      BendingEffect.FIRE_TICK.reset(entity);
      entity.applyVelocity(this, push);
      return true;
    }, true, true);
  }

  private int getLength(Vector3d origin, Vector3d direction) {
    Set<Block> checked = new HashSet<>();
    for (double i = 0.5; i <= userConfig.length; i += 0.5) {
      Block block = user.world().blockAt(origin.add(direction.multiply(i)));
      if (checked.add(block) && !isValidBlock(block)) {
        return FastMath.ceil(i) - 1;
      }
      if (data == null) {
        data = block.state();
      }
    }
    return userConfig.length;
  }

  private static final class EarthPillar extends Pillar {
    private EarthPillar(Builder<EarthPillar> builder) {
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

  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "catapult");
    }
  }
}
