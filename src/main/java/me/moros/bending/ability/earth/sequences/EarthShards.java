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

package me.moros.bending.ability.earth.sequences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

// TODO restrictions based on earthglove cooldown, add bleed effect
public class EarthShards extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<ShardStream> streams = new ArrayList<>();

  private int firedShots = 0;

  private long nextFireTime;

  public EarthShards(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
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

    if (firedShots < userConfig.maxShots) {
      long time = System.currentTimeMillis();
      if (time >= nextFireTime) {
        nextFireTime = time + userConfig.interval;
        Vector3 rightOrigin = user.handSide(true);
        Vector3 leftOrigin = user.handSide(false);
        Vector3 target = user.rayTrace(userConfig.range);
        double distance = target.distance(user.eyeLocation());
        for (int i = 0; i < 2; i++) {
          if (firedShots >= userConfig.maxShots) {
            break;
          }
          firedShots++;
          Vector3 origin = (i == 0) ? rightOrigin : leftOrigin;
          Vector3 dir = VectorMethods.randomOffset(target, distance * userConfig.spread).subtract(origin);
          streams.add(new ShardStream(new Ray(origin, dir)));
        }
      }
    }

    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return (streams.isEmpty() && firedShots >= userConfig.maxShots) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Bending.game().abilityManager(user.world()).destroyInstance(this);
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  @Override
  public @NonNull User user() {
    return user;
  }


  private class ShardStream extends ParticleStream {
    public ShardStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = Block::isLiquid;
      SoundUtil.playSound(ray.origin.toLocation(user.world()), Sound.BLOCK_STONE_BREAK, 1, 2);
    }

    @Override
    public void render() {
      ParticleUtil.createRGB(bukkitLocation(), "555555", 0.8F)
        .count(3).offset(0.1, 0.1, 0.1).spawn();
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.SPEED)
    public double speed;

    public double spread;
    public int maxShots;
    public long interval;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "sequences", "earthshards");

      cooldown = abilityNode.node("cooldown").getLong(10000);
      damage = abilityNode.node("damage").getDouble(0.5);
      range = abilityNode.node("range").getDouble(16.0);
      speed = abilityNode.node("speed").getDouble(0.8);
      spread = abilityNode.node("spread").getDouble(0.2);
      maxShots = abilityNode.node("max-shots").getInt(10);
      interval = abilityNode.node("interval").getLong(100);

      abilityNode.node("speed").comment("How many blocks the streams advance with each tick.");
    }
  }
}
