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
import java.util.function.Predicate;

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
import me.moros.bending.model.collision.Collider;
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
import org.checkerframework.checker.nullness.qual.NonNull;

public class Catapult extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;

  private Block base;
  private Pillar pillar;

  private boolean sneak;
  private long startTime;

  public Catapult(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (!user.isOnGround()) {
      return false;
    }

    this.user = user;
    recalculateConfig();

    base = getBase();
    if (!TempBlock.isBendable(base) || !Bending.game().protectionSystem().canBuild(user, base)) {
      return false;
    }

    sneak = method == ActivationMethod.SNEAK;

    launch();
    startTime = System.currentTimeMillis();
    return true;
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
    AABB entityBounds = AABBUtils.entityBounds(user.entity()).grow(new Vector3(0, 0.1, 0));
    AABB floorBounds = new AABB(new Vector3(-1, -0.5, -1), new Vector3(1, 0, 1)).at(user.location());
    Predicate<Block> predicate = b -> entityBounds.intersects(AABBUtils.blockBounds(b)) && !b.isLiquid() && EarthMaterials.isEarthbendable(user, b);
    return WorldMethods.nearbyBlocks(user.world(), floorBounds, predicate).stream()
      .min(Comparator.comparingDouble(b -> new Vector3(b).add(Vector3.HALF).distanceSq(user.location())))
      .orElse(user.locBlock().getRelative(BlockFace.DOWN));
  }

  private boolean launch() {
    user.addCooldown(description(), userConfig.cooldown);
    double power = sneak ? userConfig.sneakPower : userConfig.clickPower;

    Predicate<Block> predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    pillar = Pillar.builder(user, base, EarthPillar::new).predicate(predicate).build(3, 1).orElse(null);
    SoundUtil.EARTH_SOUND.play(base.getLocation());

    double angle = Vector3.angle(Vector3.PLUS_J, user.direction());
    Vector3 direction = angle > userConfig.angle ? Vector3.PLUS_J : user.direction();

    Vector3 origin = user.location().add(new Vector3(0, 0.5, 0));

    ParticleUtil.create(Particle.BLOCK_CRACK, origin.toLocation(user.world()))
      .count(8).offset(0.4, 0.4, 0.4).data(base.getBlockData()).spawn();

    Collider collider = new Sphere(origin, 1.5);
    return CollisionUtil.handleEntityCollisions(user, collider, entity -> {
      entity.setVelocity(direction.scalarMultiply(power).clampVelocity());
      return true;
    }, true, true);
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
    public double angle;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "catapult");

      cooldown = abilityNode.node("cooldown").getLong(3000);
      sneakPower = abilityNode.node("sneak-power").getDouble(2.65);
      clickPower = abilityNode.node("click-power").getDouble(1.8);
      angle = FastMath.toRadians(abilityNode.node("angle").getInt(60));
    }
  }
}
