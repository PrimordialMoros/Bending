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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.raytrace.Context;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempEntity.TempFallingBlock;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthShot extends AbilityInstance implements Explosive {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(Vector3d.of(0.25, 0.25, 0.25));

  private enum Mode {ROCK, METAL, MAGMA}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;
  private Block source;
  private Block readySource;
  private BlockState data;
  private Vector3d location;
  private Vector3d lastVelocity;
  private TempFallingBlock projectile;

  private boolean ready = false;
  private boolean launched = false;
  private boolean canConvert = false;
  private boolean exploded = false;
  private double damage;
  private int targetY;
  private long magmaStartTime = 0;

  public EarthShot(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldKey()).userInstances(user, EarthShot.class)
        .filter(e -> !e.launched).forEach(EarthShot::launch);
      return false;
    }

    this.user = user;
    loadConfig();

    long count = user.game().abilityManager(user.worldKey()).userInstances(user, EarthShot.class).filter(e -> !e.launched).count();
    if (count >= userConfig.maxAmount) {
      return false;
    }

    canConvert = userConfig.allowConvertMagma && user.hasPermission("bending.lava");

    return prepare();
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  private boolean prepare() {
    source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthbendable(user, b));
    if (source == null) {
      return false;
    }
    mode = getType(source);

    if (source.blockY() < user.eyeBlock().blockY()) {
      targetY = user.eyeBlock().blockY() + 1;
    } else {
      targetY = source.blockY() + 2;
    }
    int deltaY = Math.abs(source.blockY() - targetY);
    for (int i = 1; i <= deltaY; i++) {
      Block temp = source.offset(Direction.UP, i);
      if (!MaterialUtil.isTransparent(temp)) {
        return false;
      }
      WorldUtil.tryBreakPlant(temp);
    }

    data = source.state();
    BlockState solidData;
    if (mode == Mode.MAGMA) {
      solidData = BlockType.MAGMA_BLOCK.defaultState();
      canConvert = false;
    } else {
      solidData = MaterialUtil.solidType(data.type()).defaultState();
    }
    if (mode == Mode.METAL) {
      SoundEffect.METAL.play(source);
      canConvert = false;
    } else {
      SoundEffect.EARTH.play(source);
    }

    projectile = TempEntity.fallingBlock(solidData).velocity(Vector3d.of(0, 0.65, 0))
      .gravity(false).duration(6000).buildReal(source);
    if (!MaterialUtil.isLava(source)) {
      TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(source);
    }
    location = projectile.center();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange + 10, () -> location))
      .build();

    return true;
  }

  @Override
  public UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (launched) {
      if (projectile == null || !projectile.entity().valid()) {
        return UpdateResult.REMOVE;
      }

      Vector3d velocity = projectile.entity().velocity();
      double minLength = userConfig.speed * 0.85;
      if (lastVelocity.angle(velocity) > Math.PI / 4 || velocity.lengthSq() < minLength * minLength) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking()) {
        Vector3d dir = user.direction().multiply(0.2);
        velocity = velocity.add(dir.withY(0));
      }
      projectile.entity().applyVelocity(this, velocity.normalize().multiply(userConfig.speed));
      lastVelocity = projectile.entity().velocity();
      location = projectile.center();
      Collider c = BOX.at(location);
      boolean magma = mode == Mode.MAGMA;
      if (CollisionUtil.handle(user, c, this::onEntityHit, true, false, magma)) {
        return UpdateResult.REMOVE;
      }
      projectile.state().asParticle(projectile.entity().location()).count(3).offset(0.25).spawn(user.world());
    } else {
      if (!ready) {
        handleSource();
      } else {
        handleMagma();
      }
    }

    return UpdateResult.CONTINUE;
  }

  private boolean onEntityHit(Entity entity) {
    if (mode == Mode.MAGMA) {
      explode();
      return false;
    }
    entity.damage(damage, user, description());
    Vector3d velocity = projectile.entity().velocity().normalize().multiply(0.4);
    entity.applyVelocity(this, velocity);
    return true;
  }

  private void handleSource() {
    Block block = projectile.entity().block();
    if (block.blockY() >= targetY) {
      TempBlock.builder(projectile.state()).build(block);
      projectile.revert();
      location = block.toVector3d();
      readySource = block;
      ready = true;
    } else {
      location = projectile.center();
      projectile.state().asParticle(projectile.entity().location()).count(3).offset(0.25).spawn(user.world());
    }
  }

  private void handleMagma() {
    if (!canConvert) {
      return;
    }
    Block check = user.rayTrace(userConfig.selectRange * 2).ignoreLiquids(false).blocks(user.world()).block();
    if (user.sneaking() && readySource.equals(check)) {
      if (magmaStartTime == 0) {
        magmaStartTime = System.currentTimeMillis();
        if (userConfig.chargeTime > 0) {
          SoundEffect.LAVA.play(readySource);
        }
      }
      Vector3d spawnLoc = readySource.center();
      Particle.LAVA.builder(spawnLoc).count(2).offset(0.5).spawn(user.world());
      Particle.SMOKE.builder(spawnLoc).count(2).offset(0.5).spawn(user.world());
      ParticleBuilder.rgb(spawnLoc, "#FFA400").count(2).offset(0.5).spawn(user.world());
      ParticleBuilder.rgb(spawnLoc, "#FF8C00").count(4).offset(0.5).spawn(user.world());
      if (userConfig.chargeTime <= 0 || System.currentTimeMillis() > magmaStartTime + userConfig.chargeTime) {
        mode = Mode.MAGMA;
        TempBlock.builder(BlockType.MAGMA_BLOCK).build(readySource);
        canConvert = false;
      }
    } else {
      if (magmaStartTime != 0 && ThreadLocalRandom.current().nextInt(6) == 0) {
        removalPolicy = (u, d) -> true; // Remove in next tick
        return;
      }
      magmaStartTime = 0;
    }
  }

  private Mode getType(Block block) {
    if (EarthMaterials.isLavaBendable(block)) {
      return Mode.MAGMA;
    } else if (EarthMaterials.isMetalBendable(block)) {
      return Mode.METAL;
    } else {
      return Mode.ROCK;
    }
  }

  private void launch() {
    if (launched) {
      return;
    }

    boolean prematureLaunch = false;
    if (!ready) {
      if (!userConfig.allowQuickLaunch) {
        return;
      }
      prematureLaunch = true;
    }

    Vector3d origin;
    if (prematureLaunch) {
      origin = projectile.center();
      Vector3d dir = getTarget(null).subtract(origin).normalize().multiply(userConfig.speed);
      projectile.entity().gravity(true);
      projectile.entity().applyVelocity(this, dir.add(0, 0.2, 0));
    } else {
      origin = readySource.center();
      Vector3d dir = getTarget(readySource).subtract(origin).normalize().multiply(userConfig.speed).add(0, 0.2, 0);
      projectile = TempFallingBlock.fallingBlock(readySource.state()).velocity(dir).buildReal(readySource);
      TempBlock.air().build(readySource);
    }
    location = projectile.center();
    lastVelocity = projectile.entity().velocity();

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, origin, () -> location))
      .build();

    user.addCooldown(description(), userConfig.cooldown);

    damage = switch (mode) {
      case METAL -> BendingProperties.instance().metalModifier(userConfig.damage);
      case MAGMA -> BendingProperties.instance().magmaModifier(userConfig.damage);
      default -> userConfig.damage;
    };
    launched = true;
  }

  private Vector3d getTarget(@Nullable Block source) {
    return user.rayTrace(userConfig.range).ignore(source).cast(user.world()).entityCenterOrPosition();
  }

  @Override
  public void explode() {
    if (exploded || mode != Mode.MAGMA) {
      return;
    }
    exploded = true;
    Vector3d center = projectile.center();
    Particle.SMOKE.builder(center).count(12).offset(1).extra(0.05).spawn(user.world());
    Particle.FIREWORK.builder(center).count(8).offset(1).extra(0.07).spawn(user.world());
    BendingExplosion.builder()
      .size(userConfig.explosionRadius)
      .damage(damage)
      .fireTicks(0)
      .particles(false)
      .sound(SoundEffect.EXPLOSION)
      .buildAndExplode(this, center);
  }

  @Override
  public void onDestroy() {
    if (projectile != null) {
      if (launched) {
        Vector3d center = projectile.center();
        projectile.state().asParticle(center).count(8).offset(1).spawn(user.world());
        Block projected = Context.builder(center, lastVelocity).blocks(user.world()).block();
        if (projected != null) {
          FragileStructure.tryDamageStructure(projected, mode == Mode.MAGMA ? 6 : 4, new Ray(center, lastVelocity));
        }
        explode();
      }
      projectile.revert();
    }
    if (!launched) {
      TempBlock.builder(data).bendable(true).duration(BendingProperties.instance().earthRevertTime()).build(source);
      if (readySource != null) {
        TempBlock.air().build(readySource);
      }
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return (!launched || projectile == null) ? List.of() : List.of(BOX.at(projectile.center()));
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 6;
    @Modifiable(Attribute.RANGE)
    private double range = 48;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 1000;
    @Modifiable(Attribute.SPEED)
    private double speed = 1.6;
    @Modifiable(Attribute.AMOUNT)
    private int maxAmount = 1;
    private boolean allowQuickLaunch = true;
    private boolean allowConvertMagma = true;
    @Modifiable(Attribute.RADIUS)
    private double explosionRadius = 2.5;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthshot");
    }
  }
}
