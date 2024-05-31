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

package me.moros.bending.common.ability.water;

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
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FrostBreath extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<FrostStream> streams = MultiUpdatable.empty();
  private final Set<UUID> affectedEntities = new HashSet<>();

  public FrostBreath(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, FrostBreath.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(Policies.UNDER_WATER)
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
    affectedEntities.clear();
    user.editProperty(EntityProperties.REMAINING_OXYGEN, air -> air - 5);
    Vector3d offset = Vector3d.of(0, -0.1, 0);
    Ray ray = Ray.of(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new FrostStream(ray));
    return streams.update();
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class FrostStream extends ParticleStream {
    public FrostStream(Ray ray) {
      super(user, ray, 0.6, 0.5);
      canCollide = BlockType::isLiquid;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      double offset = 0.15 * distanceTravelled;
      collider = Sphere.of(location, collisionRadius + offset);
      int count = FastMath.ceil(0.75 * distanceTravelled);
      Particle.ITEM_SNOWBALL.builder(location).count(count).offset(offset).extra(0.02).spawn(user.world());
    }

    @Override
    public void postRender() {
      for (Block block : user.world().nearbyBlocks(location, collider.radius())) {
        if (!user.canBuild(block)) {
          continue;
        }
        onBlockHit(block);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (affectedEntities.add(entity.uuid())) {
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
        BlockType.ICE.asParticle(entity.center()).count(5).offset(0.5).spawn(user.world());
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      long duration = BendingProperties.instance().iceRevertTime(2000);
      if (MaterialUtil.isWater(block)) {
        TempBlock.ice().duration(duration).build(block);
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
          SoundEffect.ICE.play(block);
        }
      } else if (MaterialUtil.isTransparent(block)) {
        Block below = block.offset(Direction.DOWN);
        if (below.type().isSolid() && !WaterMaterials.isIceBendable(below) && TempBlock.isBendable(below)) {
          TempBlock.builder(BlockType.SNOW).bendable(true).duration(duration).build(block);
        }
      }
      WorldUtil.tryCoolLava(user, block);
      return true;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10000;
    @Modifiable(Attribute.RANGE)
    private double range = 7;
    @Modifiable(Attribute.DURATION)
    private long duration = 1500;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 5;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "frostbreath");
    }
  }
}
