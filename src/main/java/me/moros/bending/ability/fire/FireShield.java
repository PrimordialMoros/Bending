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

package me.moros.bending.ability.fire;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;

public class FireShield extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Shield shield;
  private ThreadLocalRandom rand;

  private boolean sphere = false;

  public FireShield(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, FireShield.class)) {
      return false;
    }

    this.user = user;
    recalculateConfig();

    if (user.headBlock().isLiquid()) {
      return false;
    }

    rand = ThreadLocalRandom.current();
    if (method == ActivationMethod.SNEAK) {
      sphere = true;
      shield = new SphereShield();
      removalPolicy = Policies.builder()
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .add(ExpireRemovalPolicy.of(userConfig.shieldDuration))
        .add(Policies.NOT_SNEAKING).build();
    } else {
      shield = new DiskShield();
      removalPolicy = Policies.builder()
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .add(ExpireRemovalPolicy.of(userConfig.diskDuration)).build();
    }

    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    shield.render();
    CollisionUtil.handleEntityCollisions(user, shield.collider(), entity -> {
      if (sphere && entity instanceof Projectile) {
        entity.remove();
        return true;
      }
      FireTick.LARGER.apply(user, entity, userConfig.fireTicks);
      return true;
    }, false);

    shield.update();
    return UpdateResult.CONTINUE;
  }

  public boolean isSphere() {
    return sphere;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), sphere ? userConfig.shieldCooldown : userConfig.diskCooldown);
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return Collections.singletonList(shield.collider());
  }

  private interface Shield {
    void update();

    void render();

    Collider collider();
  }

  private class DiskShield implements Shield {
    private Disk disk;
    private Vector3 location;
    private long nextRenderTime = 0;

    private DiskShield() {
      update();
    }

    @Override
    public void update() {
      location = user.eyeLocation().add(user.direction().scalarMultiply(userConfig.diskRange));
      double r = userConfig.diskRadius;
      AABB aabb = new AABB(new Vector3(-r, -r, -1), new Vector3(r, r, 1));
      Vector3 right = user.rightSide();
      Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(user.yaw()), RotationConvention.VECTOR_OPERATOR);
      rotation = rotation.applyTo(new Rotation(right, FastMath.toRadians(user.pitch()), RotationConvention.VECTOR_OPERATOR));
      OBB obb = new OBB(aabb, rotation).addPosition(location);
      disk = new Disk(obb, new Sphere(location, userConfig.diskRadius));
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time < nextRenderTime) {
        return;
      }
      nextRenderTime = time + 200;
      Rotation rotation = new Rotation(user.direction(), FastMath.toRadians(20), RotationConvention.VECTOR_OPERATOR);
      double[] array = Vector3.PLUS_J.crossProduct(user.direction()).normalize().toArray();
      for (int i = 0; i < 18; i++) {
        for (double j = 0.2; j <= 1; j += 0.2) {
          Location spawnLoc = location.add(new Vector3(array).scalarMultiply(j * userConfig.diskRadius)).toLocation(user.world());
          ParticleUtil.createFire(user, spawnLoc)
            .offset(0.2, 0.1, 0.2).extra(0.01).spawn();
          if (rand.nextInt(12) == 0) {
            SoundUtil.FIRE_SOUND.play(spawnLoc);
          }
        }
        rotation.applyTo(array, array);
      }
    }

    @Override
    public Collider collider() {
      return disk;
    }
  }

  private class SphereShield implements Shield {
    private Sphere sphere;
    private int currentPoint = 0;

    private SphereShield() {
      update();
    }

    @Override
    public Collider collider() {
      return sphere;
    }

    @Override
    public void update() {
      sphere = new Sphere(center(), userConfig.shieldRadius);
    }

    @Override
    public void render() {
      Vector3 center = center();
      double radius = userConfig.shieldRadius;
      currentPoint++;
      double spacing = radius / 8;
      for (int i = 1; i < 16; i++) {
        double y = (i * spacing) - radius;
        double factor = 1 - (y * y) / (radius * radius);
        if (factor <= 0.2) {
          continue;
        }
        double x = radius * factor * FastMath.cos(i * currentPoint);
        double z = radius * factor * FastMath.sin(i * currentPoint);
        Location spawnLoc = center.add(new Vector3(x, y, z)).toLocation(user.world());
        ParticleUtil.createFire(user, spawnLoc)
          .offset(0.2, 0.1, 0.2).extra(0.01).spawn();
        if (rand.nextInt(12) == 0) {
          SoundUtil.FIRE_SOUND.play(spawnLoc);
        }
      }
    }

    private Vector3 center() {
      return EntityMethods.entityCenter(user.entity());
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Attribute(Attribute.COOLDOWN)
    public long diskCooldown;
    @Attribute(Attribute.DURATION)
    public long diskDuration;
    @Attribute(Attribute.RADIUS)
    public double diskRadius;
    @Attribute(Attribute.RANGE)
    public double diskRange;

    @Attribute(Attribute.COOLDOWN)
    public long shieldCooldown;
    @Attribute(Attribute.DURATION)
    public long shieldDuration;
    @Attribute(Attribute.RADIUS)
    public double shieldRadius;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireshield");
      fireTicks = abilityNode.node("fire-ticks").getInt(45);

      diskCooldown = abilityNode.node("disk", "cooldown").getLong(1000);
      diskDuration = abilityNode.node("disk", "duration").getLong(1000);
      diskRadius = abilityNode.node("disk", "radius").getDouble(2.0);
      diskRange = abilityNode.node("disk", "range").getDouble(1.5);

      shieldCooldown = abilityNode.node("shield", "cooldown").getLong(2000);
      shieldDuration = abilityNode.node("shield", "duration").getLong(10000);
      shieldRadius = abilityNode.node("shield", "radius").getDouble(3.0);
    }
  }
}
