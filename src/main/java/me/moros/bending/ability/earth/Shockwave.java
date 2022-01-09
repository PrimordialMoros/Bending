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

package me.moros.bending.ability.earth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.BlockLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempPacketEntity;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.packet.PacketUtil;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Shockwave extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Ripple> streams = new ArrayList<>();
  private final Set<Entity> affectedEntities = new HashSet<>();
  private final Set<Block> affectedBlocks = new HashSet<>();
  private final Collection<Block> blockBuffer = new ArrayList<>();
  private final ExpiringSet<Block> recentAffectedBlocks = new ExpiringSet<>(500);
  private Vector3d origin;

  private boolean released;
  private double range;
  private long startTime;
  private int ticks;

  public Shockwave(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, Shockwave.class)
        .ifPresent(s -> s.release(true));
      return false;
    }

    if (Bending.game().abilityManager(user.world()).hasAbility(user, Shockwave.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    released = false;
    if (method == Activation.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      release(false);
    }

    startTime = System.currentTimeMillis();
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
    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide().toLocation(user.world())).spawn();
        if (!user.sneaking() && !release(false)) {
          return UpdateResult.REMOVE;
        }
      } else {
        if (!user.sneaking()) {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }
    if (++ticks % 4 == 0) {
      PacketUtil.refreshBlocks(blockBuffer, user.world(), origin);
      blockBuffer.clear();
    }
    Set<Block> positions = recentAffectedBlocks.snapshot();
    if (!positions.isEmpty()) {
      CollisionUtil.handle(user, new Sphere(origin, range + 2), e -> onEntityHit(e, positions), false);
    }
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private boolean onEntityHit(Entity entity, Set<Block> positions) {
    if (!affectedEntities.contains(entity)) {
      boolean inRange = false;
      if (positions.contains(entity.getLocation().getBlock())) {
        inRange = true;
      } else if (entity instanceof LivingEntity livingEntity) {
        Block eyeBlock = livingEntity.getEyeLocation().getBlock();
        if (positions.contains(eyeBlock)) {
          inRange = true;
        }
      }

      Vector3d loc = new Vector3d(entity.getLocation());

      if (!inRange) {
        for (Block block : positions) {
          AABB blockBounds = AABB.BLOCK_BOUNDS.grow(new Vector3d(0.5, 1, 0.5)).at(new Vector3d(block));
          if (blockBounds.intersects(AABBUtil.entityBounds(entity))) {
            inRange = true;
            break;
          }
        }
      }

      if (inRange) {
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        double deltaY = Math.min(0.9, 0.6 + loc.distance(origin) / (1.5 * range));
        Vector3d push = loc.subtract(origin).normalize().setY(deltaY).multiply(userConfig.knockback);
        EntityUtil.applyVelocity(this, entity, push);
        affectedEntities.add(entity);
      }
    }
    return false;
  }

  private boolean isCharged() {
    return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
  }

  private boolean release(boolean cone) {
    if (released || !isCharged() || !user.isOnGround()) {
      return false;
    }
    released = true;
    range = cone ? userConfig.coneRange : userConfig.ringRange;

    origin = user.location().snapToBlockCenter();
    Vector3d dir = user.direction().setY(0).normalize();
    if (cone) {
      double deltaAngle = Math.PI / (3 * range);
      VectorUtil.createArc(dir, Vector3d.PLUS_J, deltaAngle, FastMath.ceil(range / 1.5)).forEach(v ->
        streams.add(new Ripple(new Ray(origin, v.multiply(range)), 0))
      );
    } else {
      VectorUtil.circle(dir, Vector3d.PLUS_J, FastMath.ceil(6 * range)).forEach(v ->
        streams.add(new Ripple(new Ray(origin, v.multiply(range)), 75))
      );
    }

    // First update in same tick to only apply cooldown if there are valid ripples
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    if (streams.isEmpty()) {
      removalPolicy = (u, d) -> true; // Remove in next tick
    } else {
      removalPolicy = Policies.builder().build();
      user.addCooldown(description(), userConfig.cooldown);
    }
    return true;
  }

  @Override
  public void onDestroy() {
    if (released) {
      PacketUtil.refreshBlocks(blockBuffer, user.world(), origin);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Ripple extends BlockLine {
    public Ripple(Ray ray, long interval) {
      super(user, ray);
      this.interval = interval;
    }

    @Override
    public boolean isValidBlock(@NonNull Block block) {
      if (block.isLiquid() || !MaterialUtil.isTransparent(block)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, block.getRelative(BlockFace.DOWN));
    }

    @Override
    public void render(@NonNull Block block) {
      if (!affectedBlocks.add(block)) {
        return;
      }
      recentAffectedBlocks.add(block);
      double deltaY = Math.min(0.25, 0.05 + distanceTravelled / (3 * range));
      Vector3d velocity = new Vector3d(0, deltaY, 0);
      Block below = block.getRelative(BlockFace.DOWN);
      blockBuffer.add(below);
      BlockData data = below.getBlockData();
      TempPacketEntity.builder(data).velocity(velocity).duration(500).buildFallingBlock(user.world(), Vector3d.center(below));
      ParticleUtil.of(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 1.25, 0.5))
        .count(5).offset(0.5, 0.25, 0.5).data(data).spawn();
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.EARTH.play(block.getLocation());
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.RANGE)
    public double coneRange;
    @Modifiable(Attribute.RANGE)
    public double ringRange;
    public double fallThreshold;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "shockwave");

      cooldown = abilityNode.node("cooldown").getLong(8000);
      chargeTime = abilityNode.node("charge-time").getInt(2500);
      damage = abilityNode.node("damage").getDouble(3.0);
      knockback = abilityNode.node("knockback").getDouble(1.2);
      coneRange = abilityNode.node("cone-range").getDouble(14.0);
      ringRange = abilityNode.node("ring-range").getDouble(9.0);
      fallThreshold = abilityNode.node("fall-threshold").getDouble(12.0);
    }
  }
}
