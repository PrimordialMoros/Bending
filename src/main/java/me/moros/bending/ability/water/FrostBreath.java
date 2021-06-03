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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FrostBreath extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<FrostStream> streams = new ArrayList<>();
  private final Set<Entity> affectedEntities = new HashSet<>();

  public FrostBreath(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, FrostBreath.class)) {
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

    user.entity().setRemainingAir(user.entity().getRemainingAir() - 5);
    Vector3 offset = new Vector3(0, -0.1, 0);
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  private class FrostStream extends ParticleStream {
    private double distanceTravelled = 0;

    public FrostStream(Ray ray) {
      super(user, ray, 0.6, 0.5);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      Location spawnLoc = bukkitLocation();
      double offset = 0.15 * distanceTravelled;
      collider = new Sphere(location, collisionRadius + offset);
      ParticleUtil.create(Particle.SNOW_SHOVEL, spawnLoc).count(NumberConversions.ceil(0.75 * distanceTravelled))
        .offset(offset, offset, offset).extra(0.02).spawn();
      ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(NumberConversions.ceil(0.4 * distanceTravelled))
        .offset(offset, offset, offset).extra(0.02).data(Material.ICE.createBlockData()).spawn();
    }

    @Override
    public void postRender() {
      for (Block block : WorldMethods.nearbyBlocks(bukkitLocation(), collider.radius)) {
        if (!user.canBuild(block)) {
          continue;
        }
        onBlockHit(block);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        int potionDuration = NumberConversions.round(userConfig.slowDuration / 50.0);
        PotionUtil.tryAddPotion(entity, PotionEffectType.SLOW, potionDuration, userConfig.power);
        ParticleUtil.create(Particle.BLOCK_CRACK, ((LivingEntity) entity).getEyeLocation()).count(5)
          .offset(0.5, 0.5, 0.5).data(Material.ICE.createBlockData()).spawn();
      }
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      long duration = BendingProperties.ICE_DURATION + ThreadLocalRandom.current().nextLong(2000);
      if (MaterialUtil.isWater(block)) {
        TempBlock.create(block, Material.ICE.createBlockData(), duration, true);
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
          SoundUtil.ICE.play(block.getLocation());
        }
      } else if (MaterialUtil.isTransparent(block)) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (below.isSolid() && !WaterMaterials.isIceBendable(below) && TempBlock.isBendable(below)) {
          TempBlock.create(block, Material.SNOW.createBlockData(), duration, true);
        }
      }
      BlockMethods.tryCoolLava(user, block);
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
    @Modifiable(Attribute.STRENGTH)
    public int power;
    @Modifiable(Attribute.DURATION)
    public long slowDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "frostbreath");

      cooldown = abilityNode.node("cooldown").getLong(10000);
      range = abilityNode.node("range").getDouble(7.0);
      duration = abilityNode.node("duration").getLong(1500);
      power = abilityNode.node("slow-power").getInt(2) - 1;
      slowDuration = abilityNode.node("slow-duration").getLong(2500);
    }
  }
}
