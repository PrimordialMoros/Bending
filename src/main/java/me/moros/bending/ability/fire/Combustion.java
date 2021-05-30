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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundEffect;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Combustion extends AbilityInstance implements Explosive {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private CombustBeam beam;
  private Collider ignoreCollider;

  private boolean exploded;

  public Combustion(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (method == ActivationMethod.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, Combustion.class).ifPresent(Combustion::explode);
      return false;
    }

    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    if (Policies.IN_LIQUID.test(user, description()) || Bending.game().abilityManager(user.world()).hasAbility(user, Combustion.class)) {
      return false;
    }
    beam = new CombustBeam();
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (beam.distanceTravelled > userConfig.range) {
      return UpdateResult.REMOVE;
    }
    return beam.update();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(beam.collider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FireShield) {
      boolean sphere = ((FireShield) collidedAbility).isSphere();
      if (sphere) {
        ignoreCollider = collision.colliderOther();
      }
      explode();
    } else if (collidedAbility instanceof Combustion) {
      Combustion other = (Combustion) collidedAbility;
      Vector3 first = collision.colliderSelf().position();
      Vector3 second = collision.colliderOther().position();
      Vector3 center = first.add(second).multiply(0.5);
      createExplosion(center, userConfig.power + other.userConfig.power, userConfig.damage + other.userConfig.damage);
      other.exploded = true;
    } else if (collidedAbility instanceof Explosive) {
      explode();
    } else if (collidedAbility.description().element() == Element.EARTH && collision.removeSelf()) {
      explode();
    }
  }

  @Override
  public void explode() {
    createExplosion(beam.location(), userConfig.power, userConfig.damage);
  }

  private void createExplosion(Vector3 center, double size, double damage) {
    if (exploded) {
      return;
    }
    exploded = true;
    Location loc = center.toLocation(user.world());
    ParticleUtil.create(Particle.FLAME, loc, userConfig.particleRange).extra(0.2).count(20)
      .offset(1, 1, 1).spawn();
    ParticleUtil.create(Particle.SMOKE_LARGE, loc, userConfig.particleRange).extra(0.2).count(20)
      .offset(1, 1, 1).spawn();
    ParticleUtil.create(Particle.FIREWORKS_SPARK, loc, userConfig.particleRange).extra(0.2).count(20)
      .offset(1, 1, 1).spawn();

    BendingExplosion.builder()
      .size(size)
      .damage(damage)
      .fireTicks(userConfig.fireTicks)
      .ignoreInsideCollider(ignoreCollider)
      .soundEffect(new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 6, 0.8F))
      .buildAndExplode(user, description(), center);

    FragileStructure.tryDamageStructure(WorldMethods.nearbyBlocks(loc, size, WaterMaterials::isIceBendable), 0);

    if (loc.getBlock().isLiquid()) {
      return;
    }
    Predicate<Block> predicate = b -> !MaterialUtil.isAir(b) && !MaterialUtil.isUnbreakable(b) && !b.isLiquid();
    Collection<Block> blocks = new ArrayList<>();
    for (Block block : WorldMethods.nearbyBlocks(loc, size, predicate)) {
      if (user.canBuild(block)) {
        blocks.add(block);
        long delay = BendingProperties.EXPLOSION_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
        TempBlock.createAir(block, delay);
      }
    }
    for (Block block : blocks) {
      if (MaterialUtil.isIgnitable(block) && ThreadLocalRandom.current().nextInt(3) == 0) {
        long delay = BendingProperties.FIRE_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
        TempBlock.create(block, Material.FIRE.createBlockData(), delay, true);
      }
    }
  }

  private class CombustBeam extends ParticleStream {
    private double randomBeamDistance = 7;
    private double distanceTravelled = 0;

    public CombustBeam() {
      super(user, user.ray(userConfig.range), 0.35, 1);
      canCollide = Block::isLiquid;
      singleCollision = true;
      steps = 3;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      renderRing();
      Location bukkitLocation = bukkitLocation();
      ParticleUtil.create(Particle.SMOKE_NORMAL, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
      ParticleUtil.create(Particle.FIREWORKS_SPARK, bukkitLocation, userConfig.particleRange).extra(0.06).spawn();
    }

    @Override
    public @NonNull Vector3 controlDirection() {
      return user.direction().multiply(speed);
    }

    private void renderRing() {
      if (distanceTravelled >= randomBeamDistance) {
        SoundUtil.COMBUSTION.play(bukkitLocation(), 1.5F, 0);
        randomBeamDistance = distanceTravelled + 7 + 3 * ThreadLocalRandom.current().nextGaussian();
        double radius = ThreadLocalRandom.current().nextDouble(0.3, 0.6);
        VectorMethods.circle(Vector3.ONE, user.direction(), 20).forEach(v -> {
          Vector3 velocity = v.multiply(radius);
          ParticleUtil.create(Particle.FIREWORKS_SPARK, location.add(v.multiply(0.2)).toLocation(user.world()), userConfig.particleRange)
            .count(0).offset(velocity.x, velocity.y, velocity.z).extra(0.09).spawn();
        });
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.COMBUSTION.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      explode();
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      explode();
      return true;
    }

    private @NonNull Vector3 location() {
      return location;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Attribute(Attribute.STRENGTH)
    public double power;
    @Attribute(Attribute.RANGE)
    public double range;

    public int particleRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "combustion");

      cooldown = abilityNode.node("cooldown").getLong(12000);
      damage = abilityNode.node("damage").getDouble(4.0);
      fireTicks = abilityNode.node("fire-ticks").getInt(50);
      power = abilityNode.node("power").getDouble(3.4);
      range = abilityNode.node("range").getDouble(48.0);

      particleRange = NumberConversions.ceil(range);
    }
  }
}
