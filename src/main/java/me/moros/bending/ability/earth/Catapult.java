/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.earth;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Catapult extends AbilityInstance {
  private static final Config config = new Config();
  private static final double ANGLE = FastMath.toRadians(60);

  private User user;
  private Config userConfig;

  private Pillar pillar;

  private long startTime;

  public Catapult(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    return launch( method == ActivationMethod.SNEAK);
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (System.currentTimeMillis() > startTime + 100) {
      return pillar == null ? UpdateResult.REMOVE : pillar.update();
    }
    return UpdateResult.CONTINUE;
  }

  private Block getBase() {
    AABB entityBounds = AABBUtils.entityBounds(user.entity()).grow(new Vector3(0, 0.2, 0));
    AABB floorBounds = new AABB(new Vector3(-1, -0.5, -1), new Vector3(1, 0, 1)).at(user.location());
    return WorldMethods.nearbyBlocks(user.world(), floorBounds, b -> entityBounds.intersects(AABBUtils.blockBounds(b))).stream()
      .filter(this::isValidBlock)
      .min(Comparator.comparingDouble(b -> new Vector3(b).add(Vector3.HALF).distanceSq(user.location())))
      .orElse(null);
  }

  private boolean isValidBlock(Block block) {
    if (block.isLiquid() || !TempBlock.isBendable(block) || !Bending.game().protectionSystem().canBuild(user, block)) {
      return false;
    }
    return EarthMaterials.isEarthbendable(user, block);
  }


  private boolean launch(boolean sneak) {
    double angle = Vector3.angle(Vector3.PLUS_J, user.direction());

    int length = 0;
    Block base = getBase();
    boolean forceVertical = false;
    if (base != null) {
      length = getLength(new Vector3(base.getRelative(BlockFace.UP)), Vector3.MINUS_J);
      if (angle > ANGLE) {
        forceVertical = true;
      }
      pillar = Pillar.builder(user, base, EarthPillar::new)
        .predicate(b -> !b.isLiquid() && EarthMaterials.isEarthbendable(user, b))
        .build(3, 1).orElse(null);
    } else {
      if (angle >= ANGLE && angle <= 2 * ANGLE) {
        Vector3 reverse = user.direction().scalarMultiply(-1);
        length = getLength(user.location(), reverse);
      }
    }

    if (length == 0) {
      return false;
    }

    startTime = System.currentTimeMillis();
    user.addCooldown(description(), userConfig.cooldown);

    Vector3 origin = user.location().add(new Vector3(0, 0.5, 0));
    SoundUtil.EARTH_SOUND.play(origin.toLocation(user.world()));
    if (base != null) {
      ParticleUtil.create(Particle.BLOCK_CRACK, origin.toLocation(user.world()))
        .count(8).offset(0.4, 0.4, 0.4).data(base.getBlockData()).spawn();
    }

    Vector3 direction = forceVertical ? Vector3.PLUS_J : user.direction();
    double factor = length / (double) userConfig.length;
    double power = factor * (sneak ? userConfig.sneakPower : userConfig.clickPower);
    return CollisionUtil.handleEntityCollisions(user, new Sphere(origin, 1.5), entity -> {
      entity.setVelocity(direction.scalarMultiply(power).clampVelocity());
      return true;
    }, true, true);
  }

  private int getLength(Vector3 origin, Vector3 direction) {
    Set<Block> checked = new HashSet<>();
    for (double i = 0.5; i <= userConfig.length; i += 0.5) {
      Block block = origin.add(direction.scalarMultiply(i)).toBlock(user.world());
      if (!checked.contains(block)) {
        if (!isValidBlock(block)) {
          return NumberConversions.ceil(i) - 1;
        }
        checked.add(block);
      }
    }
    return userConfig.length;
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private static class EarthPillar extends Pillar {
    protected EarthPillar(@NonNull PillarBuilder builder) {
      super(builder);
    }

    @Override
    public void playSound(@NonNull Block block) {
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.STRENGTH)
    public double sneakPower;
    @Attribute(Attribute.STRENGTH)
    public double clickPower;
    public int length;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "catapult");

      cooldown = abilityNode.node("cooldown").getLong(3000);
      sneakPower = abilityNode.node("sneak-power").getDouble(2.65);
      clickPower = abilityNode.node("click-power").getDouble(1.8);
      length = FastMath.max(1, abilityNode.node("length").getInt(7));
    }
  }
}
