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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FrostBreath extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<FrostStream> streams = new ArrayList<>();
  private final Set<Entity> affectedEntities = new HashSet<>();

  public FrostBreath(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, FrostBreath.class)) {
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    affectedEntities.clear();
    user.entity().setRemainingAir(Math.max(-20, user.entity().getRemainingAir() - 5));
    Vector3d offset = new Vector3d(0, -0.1, 0);
    Ray ray = new Ray(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new FrostStream(ray));
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class FrostStream extends ParticleStream {
    public FrostStream(Ray ray) {
      super(user, ray, 0.6, 0.5);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      double offset = 0.15 * distanceTravelled;
      collider = new Sphere(location, collisionRadius + offset);
      ParticleUtil.of(Particle.SNOW_SHOVEL, location).count(FastMath.ceil(0.75 * distanceTravelled))
        .offset(offset).extra(0.02).spawn(user.world());
      ParticleUtil.of(Particle.BLOCK_CRACK, location).count(FastMath.ceil(0.4 * distanceTravelled))
        .offset(offset).extra(0.02).data(Material.ICE.createBlockData()).spawn(user.world());
    }

    @Override
    public void postRender() {
      for (Block block : WorldUtil.nearbyBlocks(user.world(), location, collider.radius)) {
        if (!user.canBuild(block)) {
          continue;
        }
        onBlockHit(block);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
        ParticleUtil.of(Particle.BLOCK_CRACK, EntityUtil.entityCenter(entity)).count(5)
          .offset(0.5).data(Material.ICE.createBlockData()).spawn(user.world());
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      long duration = BendingProperties.instance().iceRevertTime(2000);
      if (MaterialUtil.isWater(block)) {
        TempBlock.ice().duration(duration).build(block);
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
          SoundUtil.ICE.play(block);
        }
      } else if (MaterialUtil.isTransparent(block)) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.isSolid() && !WaterMaterials.isIceBendable(below) && TempBlock.isBendable(below)) {
          TempBlock.builder(Material.SNOW.createBlockData()).bendable(true).duration(duration).build(block);
        }
      }
      WorldUtil.tryCoolLava(user, block);
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10000;
    @Modifiable(Attribute.RANGE)
    private double range = 7;
    @Modifiable(Attribute.DURATION)
    private long duration = 1500;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 5;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "frostbreath");
    }
  }
}
