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

package me.moros.bending.common.ability.fire.sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireKick extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<UUID> affectedEntities = new HashSet<>();
  private final MultiUpdatable<FireStream> streams = MultiUpdatable.empty();

  public FireKick(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    double height = user.eyeHeight();
    Vector3d direction = user.direction().multiply(userConfig.range).add(0, height, 0).normalize();
    Vector3d origin = user.location();
    Vector3d dir = user.direction();
    Vector3d rotateAxis = dir.cross(Vector3d.PLUS_J).normalize().cross(dir);
    VectorUtil.createArc(direction, rotateAxis, Math.PI / 30, 11).forEach(
      v -> streams.add(new FireStream(Ray.of(origin, v.multiply(userConfig.range))))
    );
    removalPolicy = Policies.defaults();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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

  private class FireStream extends ParticleStream {
    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = BlockType::isLiquid;
    }

    @Override
    public void render() {
      ParticleBuilder.fire(user, location).count(4).offset(0.15).extra(0.01).spawn(user.world());
      TempLight.builder(++ticks).build(user.world().blockAt(location));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (affectedEntities.add(entity.uuid())) {
        BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
        entity.damage(userConfig.damage, user, description());
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(block, 3, Ray.of(location, ray.direction()));
      return true;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 25;
    @Modifiable(Attribute.RANGE)
    private double range = 7;
    @Comment("How many blocks the streams advance with each tick.")
    @Modifiable(Attribute.SPEED)
    private double speed = 1;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "sequences", "firekick");
    }
  }
}
