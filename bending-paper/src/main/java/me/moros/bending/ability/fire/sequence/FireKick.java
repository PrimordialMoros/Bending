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

package me.moros.bending.ability.fire.sequence;

import java.util.ArrayList;
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
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireKick extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final Collection<FireStream> streams = new ArrayList<>();

  public FireKick(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    double height = user.entity().getEyeHeight();
    Vector3d direction = user.direction().multiply(userConfig.range).add(0, height, 0).normalize();
    Vector3d origin = user.location();
    Vector3d dir = user.direction();
    Vector3d rotateAxis = dir.cross(Vector3d.PLUS_J).normalize().cross(dir);
    VectorUtil.createArc(direction, rotateAxis, Math.PI / 30, 11).forEach(
      v -> streams.add(new FireStream(new Ray(origin, v.multiply(userConfig.range))))
    );

    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
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
      super(user, ray, userConfig.speed, 0.5);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      ParticleUtil.fire(user, location).count(4).offset(0.15).extra(0.01).spawn(user.world());
      TempLight.builder(++ticks).build(location.toBlock(user.world()));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 3);
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
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
    public Iterable<String> path() {
      return List.of("abilities", "fire", "sequences", "firekick");
    }
  }
}
