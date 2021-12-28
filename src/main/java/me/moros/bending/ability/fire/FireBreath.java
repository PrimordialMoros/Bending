/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FireBreath extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(500);
  private final Collection<FireStream> streams = new ArrayList<>();

  public FireBreath(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, FireBreath.class)) {
      return false;
    }

    if (Policies.IN_LIQUID.test(user, description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(Policies.IN_LIQUID)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    Vector3d offset = new Vector3d(0, -0.1, 0);
    Ray ray = new Ray(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new FireStream(ray));
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class FireStream extends ParticleStream {
    private double distanceTravelled = 0;

    public FireStream(Ray ray) {
      super(user, ray, 0.4, 0.5);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      Location spawnLoc = bukkitLocation();
      double offset = 0.2 * distanceTravelled;
      collider = new Sphere(location, collisionRadius + offset);
      ParticleUtil.fire(user, spawnLoc).count(FastMath.ceil(0.75 * distanceTravelled))
        .offset(offset, offset, offset).extra(0.02).spawn();
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.FIRE.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      BendingEffect.FIRE_TICK.apply(user, entity);
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
      }
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (WorldUtil.tryMelt(user, block)) {
        return true;
      }
      Block above = block.getRelative(BlockFace.UP);
      if (MaterialUtil.isIgnitable(above) && user.canBuild(above)) {
        TempBlock.fire().duration(BendingProperties.FIRE_REVERT_TIME).build(block);
      }
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.DAMAGE)
    public double damage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firebreath");

      cooldown = abilityNode.node("cooldown").getLong(12000);
      range = abilityNode.node("range").getDouble(9.0);
      duration = abilityNode.node("duration").getLong(2000);
      damage = abilityNode.node("damage").getDouble(0.75);
    }
  }
}
