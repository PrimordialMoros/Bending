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

package me.moros.bending.common.ability.fire;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.SimpleAbility;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;

public class FireBreath extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final ExpiringSet<UUID> affectedEntities = new ExpiringSet<>(500);
  private final MultiUpdatable<FireStream> streams = MultiUpdatable.empty();

  public FireBreath(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, FireBreath.class)) {
      return false;
    }
    if (Policies.UNDER_WATER.test(user, description()) || Policies.UNDER_LAVA.test(user, description())) {
      return false;
    }
    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

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
    Vector3d offset = Vector3d.of(0, -0.1, 0);
    Ray ray = Ray.of(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new FireStream(ray));
    return streams.update();
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(SimpleAbility::collider).toList();
  }

  private class FireStream extends ParticleStream {
    private static final double ORIGINAL_COLLISION_RADIUS = 0.5;
    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, 0.4, ORIGINAL_COLLISION_RADIUS);
      canCollide = t -> t.isLiquid() || t == BlockType.SNOW;
    }

    @Override
    public void render(Vector3d location) {
      double offset = 0.2 * distanceTravelled;
      collisionRadius = ORIGINAL_COLLISION_RADIUS + offset;
      ParticleBuilder.fire(user, location).count(FastMath.ceil(0.75 * distanceTravelled))
        .offset(offset).extra(0.02).spawn(user.world());
      TempLight.builder(++ticks).build(user.world().blockAt(location));
    }

    @Override
    public void postRender(Vector3d location) {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      BendingEffect.FIRE_TICK.apply(user, entity);
      if (affectedEntities.add(entity.uuid())) {
        entity.damage(userConfig.damage, user, description());
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (WorldUtil.tryMelt(user, block)) {
        return true;
      }
      Block above = block.offset(Direction.UP);
      if (MaterialUtil.isIgnitable(above) && user.canBuild(above)) {
        TempBlock.fire().duration(BendingProperties.instance().fireRevertTime()).ability(FireBreath.this).build(above);
      }
      return true;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 12_000;
    @Modifiable(Attribute.RANGE)
    private double range = 9;
    @Modifiable(Attribute.DURATION)
    private long duration = 2000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 0.75;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "firebreath");
    }
  }
}
