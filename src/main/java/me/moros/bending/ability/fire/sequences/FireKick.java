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

package me.moros.bending.ability.fire.sequences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class FireKick extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final Collection<FireStream> streams = new ArrayList<>();

  public FireKick(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    double height = user.entity().getEyeHeight();
    Vector3 direction = user.direction().scalarMultiply(userConfig.range).add(new Vector3(0, height, 0)).normalize();
    Vector3 origin = user.location();
    Vector3 dir = user.direction();
    Vector3 rotateAxis = dir.crossProduct(Vector3.PLUS_J).normalize().crossProduct(dir);
    VectorMethods.createArc(direction, rotateAxis, FastMath.PI / 30, 11).forEach(
      v -> streams.add(new FireStream(new Ray(origin, v.scalarMultiply(userConfig.range))))
    );

    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private class FireStream extends ParticleStream {
    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      ParticleUtil.createFire(user, bukkitLocation()).count(2)
        .offset(0.25, 0.25, 0.25).extra(0.01).spawn();
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE_SOUND.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        FireTick.LARGER.apply(user, entity, userConfig.fireTicks);
      }
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(Collections.singletonList(block), 3);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.SPEED)
    public double speed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "sequences", "firekick");

      cooldown = abilityNode.node("cooldown").getLong(4000);
      damage = abilityNode.node("damage").getDouble(2.5);
      fireTicks = abilityNode.node("fire-ticks").getInt(25);
      range = abilityNode.node("range").getDouble(7.0);
      speed = abilityNode.node("speed").getDouble(1.0);

      abilityNode.node("speed").comment("How many blocks the streams advance with each tick.");
    }
  }
}
