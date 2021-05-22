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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthShot extends AbilityInstance {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3(0.25, 0.25, 0.25));

  private enum Mode {ROCK, METAL, MAGMA}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;
  private Block source;
  private Block readySource;
  private BlockData data;
  private Vector3 location;
  private Vector3 lastVelocity;
  private BendingFallingBlock projectile;

  private boolean ready = false;
  private boolean launched = false;
  private boolean canConvert = false;
  private double damage;
  private int targetY;
  private long magmaStartTime = 0;

  public EarthShot(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (method == ActivationMethod.ATTACK) {
      Bending.game().abilityManager(user.world()).userInstances(user, EarthShot.class)
        .filter(e -> !e.launched).forEach(EarthShot::launch);
      return false;
    }

    this.user = user;
    recalculateConfig();

    long count = Bending.game().abilityManager(user.world()).userInstances(user, EarthShot.class).filter(e -> !e.launched).count();
    if (count >= userConfig.maxAmount) {
      return false;
    }

    canConvert = userConfig.allowConvertMagma && user.hasPermission("bending.lava");

    return prepare();
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  private boolean prepare() {
    source = SourceUtil.find(user, userConfig.selectRange, b -> EarthMaterials.isEarthbendable(user, b)).orElse(null);
    if (source == null) {
      return false;
    }
    mode = getType(source);
    int deltaY = 3;
    if (source.getY() >= user.headBlock().getY()) {
      targetY = source.getY() + 2;
    } else {
      targetY = user.locBlock().getY() + 2;
      deltaY = 1 + targetY - source.getY();
    }

    for (int i = 1; i <= deltaY; i++) {
      Block temp = source.getRelative(BlockFace.UP, i);
      if (!MaterialUtil.isTransparent(temp)) {
        return false;
      }
      BlockMethods.tryBreakPlant(temp);
    }

    data = source.getBlockData().clone();
    BlockData solidData;
    if (mode == Mode.MAGMA) {
      solidData = Material.MAGMA_BLOCK.createBlockData();
      canConvert = false;
    } else {
      solidData = MaterialUtil.getSolidType(source.getBlockData());
    }
    if (mode == Mode.METAL) {
      SoundUtil.METAL_SOUND.play(source.getLocation());
      canConvert = false;
    } else {
      SoundUtil.EARTH_SOUND.play(source.getLocation());
    }

    projectile = new BendingFallingBlock(source, solidData, new Vector3(0, 0.65, 0), false, 6000);
    if (!MaterialUtil.isLava(source)) {
      TempBlock.createAir(source, BendingProperties.EARTHBENDING_REVERT_TIME);
    }
    location = projectile.center();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange + 10, () -> location))
      .build();

    return true;
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (launched) {
      if (projectile == null || !projectile.fallingBlock().isValid()) {
        return UpdateResult.REMOVE;
      }

      Vector3 velocity = new Vector3(projectile.fallingBlock().getVelocity());
      if (lastVelocity.angle(velocity) > Math.PI / 4 || velocity.getNormSq() < 2.25) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking()) {
        Vector3 dir = user.direction().multiply(0.2);
        velocity = velocity.add(dir.setY(0));
      }
      projectile.fallingBlock().setVelocity(velocity.normalize().multiply(1.8).clampVelocity());
      lastVelocity = new Vector3(projectile.fallingBlock().getVelocity());
      if (CollisionUtil.handleEntityCollisions(user, BOX.at(projectile.center()), this::onEntityHit, true)) {
        return UpdateResult.REMOVE;
      }
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
    if (entity instanceof LivingEntity && userConfig.maxAmount > 1) {
      ((LivingEntity) entity).setNoDamageTicks(0);
    }
    DamageUtil.damageEntity(entity, user, damage, description());
    return true;
  }

  private void handleSource() {
    Block block = projectile.fallingBlock().getLocation().getBlock();
    if (block.getY() >= targetY) {
      TempBlock.create(block, projectile.fallingBlock().getBlockData());
      projectile.revert();
      location = new Vector3(block);
      readySource = block;
      ready = true;
    } else {
      location = projectile.center();
    }
  }

  private void handleMagma() {
    if (!canConvert) {
      return;
    }
    Block check = WorldMethods.blockCast(user.world(), user.ray(), userConfig.selectRange * 2).orElse(null);
    if (user.sneaking() && readySource.equals(check)) {
      if (magmaStartTime == 0) {
        magmaStartTime = System.currentTimeMillis();
        if (userConfig.chargeTime > 0) {
          SoundUtil.LAVA_SOUND.play(readySource.getLocation());
        }
      }

      Location spawnLoc = readySource.getLocation().add(0.5, 0.5, 0.5);
      ParticleUtil.create(Particle.LAVA, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.create(Particle.SMOKE_NORMAL, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.createRGB(spawnLoc, "FFA400").count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.createRGB(spawnLoc, "FF8C00").count(4).offset(0.5, 0.5, 0.5).spawn();

      if (userConfig.chargeTime <= 0 || System.currentTimeMillis() > magmaStartTime + userConfig.chargeTime) {
        mode = Mode.MAGMA;
        TempBlock.create(readySource, Material.MAGMA_BLOCK.createBlockData());
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

    Vector3 origin;
    if (prematureLaunch) {
      origin = projectile.center();
      Vector3 dir = getTarget(null).subtract(origin).normalize().multiply(userConfig.speed);
      projectile.fallingBlock().setGravity(true);
      projectile.fallingBlock().setVelocity(dir.add(new Vector3(0, 0.2, 0)).clampVelocity());
    } else {
      origin = Vector3.center(readySource);
      Vector3 dir = getTarget(readySource).subtract(origin).normalize().multiply(userConfig.speed);
      projectile = new BendingFallingBlock(readySource, readySource.getBlockData(), dir.add(new Vector3(0, 0.2, 0)), true, 30000);
      TempBlock.createAir(readySource);
    }
    location = projectile.center();
    lastVelocity = new Vector3(projectile.fallingBlock().getVelocity());

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, origin, () -> location))
      .build();

    user.addCooldown(description(), userConfig.cooldown);

    switch (mode) {
      case METAL:
        damage = userConfig.damage * BendingProperties.METAL_MODIFIER;
        break;
      case MAGMA:
        damage = userConfig.damage * BendingProperties.MAGMA_MODIFIER;
        break;
      default:
        damage = userConfig.damage;
        break;
    }
    launched = true;
  }

  private Vector3 getTarget(Block source) {
    Predicate<Block> predicate;
    if (source != null) {
      IntVector v = new IntVector(source);
      predicate = b -> b.isLiquid() || new IntVector(b).equals(v);
    } else {
      predicate = Block::isLiquid;
    }
    return user.rayTraceEntity(userConfig.range)
      .map(EntityMethods::entityCenter)
      .orElseGet(() -> user.rayTrace(userConfig.range, predicate));
  }

  @Override
  public void onDestroy() {
    if (projectile.fallingBlock().isValid()) {
      if (launched) {
        Location spawnLoc = projectile.center().toLocation(user.world());
        BlockData data = projectile.fallingBlock().getBlockData();
        ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(6).offset(1, 1, 1).data(data).spawn();
        ParticleUtil.create(Particle.BLOCK_DUST, spawnLoc).count(4).offset(1, 1, 1).data(data).spawn();
        if (mode == Mode.MAGMA) {
          ParticleUtil.create(Particle.SMOKE_LARGE, spawnLoc).count(12).offset(1, 1, 1).extra(0.05).spawn();
          ParticleUtil.create(Particle.FIREWORKS_SPARK, spawnLoc).count(8).offset(1, 1, 1).extra(0.07).spawn();
          SoundUtil.playSound(spawnLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0);
        }
        Block projected = projectile.center().add(lastVelocity.normalize().multiply(0.75)).toBlock(user.world());
        FragileStructure.tryDamageStructure(List.of(projected), mode == Mode.MAGMA ? 6 : 4);
      }
      projectile.revert();
    }
    if (!launched) {
      TempBlock.create(source, data, BendingProperties.EARTHBENDING_REVERT_TIME, true);
      if (readySource != null) {
        TempBlock.createAir(readySource);
      }
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return (!launched || projectile == null) ? List.of() : List.of(BOX.at(projectile.center()));
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.SELECTION)
    public double selectRange;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Attribute(Attribute.SPEED)
    public double speed;
    @Attribute(Attribute.AMOUNT)
    public int maxAmount;

    public boolean allowConvertMagma;
    public boolean allowQuickLaunch;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthshot");

      cooldown = abilityNode.node("cooldown").getLong(2000);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      range = abilityNode.node("range").getDouble(48.0);
      damage = abilityNode.node("damage").getDouble(3.0);
      chargeTime = abilityNode.node("charge-time").getLong(1000);
      speed = abilityNode.node("speed").getDouble(1.8);
      maxAmount = abilityNode.node("max-sources").getInt(1);
      allowConvertMagma = abilityNode.node("allow-convert-magma").getBoolean(true);
      allowQuickLaunch = abilityNode.node("allow-quick-launch").getBoolean(true);
    }
  }
}
