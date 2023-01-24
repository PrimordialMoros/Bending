/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.ability.fire.sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.MultiUpdatable;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireSpin extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final MultiUpdatable<FireStream> streams = MultiUpdatable.empty();

  public FireSpin(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    Vector3d origin = user.location().add(Vector3d.PLUS_J);
    VectorUtil.circle(Vector3d.PLUS_I, Vector3d.PLUS_J, 40).forEach(
      v -> streams.add(new FireStream(new Ray(origin, v.multiply(userConfig.range))))
    );
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
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
    return streams.update();
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class FireStream extends ParticleStream {
    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed / 2, 0.5);
      canCollide = BlockType::isLiquid;
      steps = 2;
    }

    @Override
    public void render() {
      ParticleBuilder.fire(user, location).extra(0.01).spawn(user.world());
      TempLight.builder(++ticks).build(user.world().blockAt(location));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        entity.damage(userConfig.damage, user, description());
        BendingEffect.FIRE_TICK.apply(user, entity);
        entity.applyVelocity(FireSpin.this, ray.direction.normalize().multiply(userConfig.knockback));
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(block, 3, new Ray(location, ray.direction));
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1;
    @Modifiable(Attribute.RANGE)
    private double range = 6;
    @Comment("How many blocks the streams advance with each tick")
    @Modifiable(Attribute.SPEED)
    private double speed = 0.5;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.8;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "sequences", "firespin");
    }
  }
}