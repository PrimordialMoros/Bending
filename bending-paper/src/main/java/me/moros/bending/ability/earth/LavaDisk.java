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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import com.destroystokyo.paper.MaterialSetTag;
import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class LavaDisk extends AbilityInstance {
  private static final String[] colors = {"2F1600", "5E2C00", "8C4200", "B05300", "C45D00", "F05A00", "F0A000", "F0BE00"};
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d location;
  private Collider collider;

  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(1000);

  private boolean launched = false;
  private double distance;
  private double distanceTravelled = 0;
  private double currentPower;
  private int rotationAngle = 0;

  public LavaDisk(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, LavaDisk.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && !EarthMaterials.isMetalBendable(b);
    Block source = user.find(userConfig.selectRange, predicate);
    if (source == null) {
      return false;
    }
    double r = 1.3;
    location = Vector3d.center(source);
    AABB aabb = new AABB(new Vector3d(-r, -0.3, -r), new Vector3d(r, 0.3, r));
    collider = new Disk(new OBB(aabb), new Sphere(r)).at(location);
    for (Block block : WorldUtil.nearbyBlocks(user.world(), aabb.at(location))) {
      if (MaterialUtil.isWater(block) || MaterialUtil.isWater(block.getRelative(BlockFace.UP))) {
        return false;
      }
      if (!MaterialUtil.isLava(block)) {
        TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
      }
    }
    distance = location.distance(user.eyeLocation());
    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, () -> location))
      .add(SwappedSlotsRemovalPolicy.of(description())).build();
    currentPower = userConfig.power;
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

    if (launched && distanceTravelled > userConfig.range) {
      return UpdateResult.REMOVE;
    }

    if (!isLocationSafe() || currentPower <= 0) {
      return UpdateResult.REMOVE;
    }

    if (!user.sneaking()) {
      launched = true;
    }

    distance = location.distance(user.eyeLocation());
    Vector3d targetLocation = user.eyeLocation().add(user.direction().multiply(launched ? userConfig.range + 5 : 3));
    if (location.distanceSq(targetLocation) > 0.5 * 0.5) {
      double speed = launched ? userConfig.speed : 0.66 * userConfig.speed;
      Vector3d direction = targetLocation.subtract(location).normalize();
      location = location.add(direction.multiply(speed));
      collider = collider.at(location);
      if (launched) {
        distanceTravelled += speed;
      }
    }

    double deltaDistance = distance - userConfig.selectRange;
    double distanceModifier = (deltaDistance <= 0) ? 1 : ((distance >= userConfig.range) ? 0 : 1 - (deltaDistance / userConfig.range));
    int deltaSpeed = Math.max(5, FastMath.ceil(15 * distanceModifier));
    rotationAngle += (deltaSpeed % 2 == 0) ? ++deltaSpeed : deltaSpeed;
    if (rotationAngle >= 360) {
      rotationAngle = 0;
    }
    displayLavaDisk();
    double damage = Math.max(userConfig.minDamage, userConfig.maxDamage * distanceModifier);
    CollisionUtil.handle(user, collider, e -> damageEntity(e, damage));
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    ParticleUtil.of(Particle.BLOCK_CRACK, location).count(16).offset(0.1).extra(0.01)
      .data(Material.MAGMA_BLOCK.createBlockData()).spawn(user.world());
    ParticleUtil.of(Particle.LAVA, location).count(2).offset(0.1).extra(0.01).spawn(user.world());
    SoundUtil.of(Sound.BLOCK_STONE_BREAK, 1, 1.5F).play(user.world(), location);
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(collider);
  }

  private boolean damageEntity(Entity entity, double damage) {
    if (!affectedEntities.contains(entity)) {
      affectedEntities.add(entity);
      BendingEffect.FIRE_TICK.apply(user, entity);
      DamageUtil.damageEntity(entity, user, damage, description());
      currentPower -= userConfig.powerDiminishPerEntity;
      ParticleUtil.of(Particle.LAVA, EntityUtil.entityCenter(entity)).count(4).offset(0.5).extra(0.1).spawn(user.world());
      return true;
    }
    return false;
  }

  private boolean damageBlock(Block block) {
    if (currentPower <= 0) {
      return false;
    }
    FragileStructure.tryDamageStructure(List.of(block), 0);
    if (!TempBlock.isBendable(block) || !user.canBuild(block)) {
      return false;
    }
    if (MaterialUtil.isLava(block)) {
      return true;
    }
    Material mat = block.getType();
    boolean tree = MaterialSetTag.LEAVES.isTagged(mat) || MaterialSetTag.LOGS_THAT_BURN.isTagged(mat);
    if (tree || EarthMaterials.isEarthOrSand(block)) {
      currentPower -= block.getType().getHardness();
      TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
      Vector3d center = Vector3d.center(block);
      ParticleUtil.of(Particle.LAVA, center).offset(0.5).extra(0.05).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundUtil.of(Sound.BLOCK_GRINDSTONE_USE, 0.3F, 0.3F).play(block);
      }
      return true;
    }
    return false;
  }

  private void displayLavaDisk() {
    damageBlock(location.toBlock(user.world()));
    int angle = user.yaw() + 90;
    double cos = Math.cos(-angle);
    double sin = Math.sin(-angle);
    int offset = 0;
    int index = 0;
    float size = 0.8F;
    for (int i = 1; i <= 8; i++) {
      for (int j = 0; j <= 288; j += 72) {
        int rotAngle = rotationAngle + j + offset;
        double length = 0.1 * i;
        Vector3d temp = new Vector3d(length * Math.cos(rotAngle), 0, length * Math.sin(rotAngle));
        Vector3d loc = location.add(VectorUtil.rotateAroundAxisY(temp, cos, sin));
        ParticleUtil.rgb(loc, colors[index], size).spawn(user.world());
        if (length > 0.5) {
          damageBlock(loc.toBlock(user.world()));
        }
      }
      offset += 4;
      index = Math.min(colors.length - 1, ++index);
      size -= 0.05;
    }
  }

  private boolean isLocationSafe() {
    Block block = location.toBlock(user.world());
    if (MaterialUtil.isWater(block)) {
      WorldUtil.playLavaExtinguishEffect(block);
      return false;
    }
    return MaterialUtil.isTransparent(block) || damageBlock(block);
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 9000;
    @Modifiable(Attribute.DAMAGE)
    private double minDamage = 1;
    @Modifiable(Attribute.DAMAGE)
    private double maxDamage = 4;
    @Modifiable(Attribute.RANGE)
    private double range = 18;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 6;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.75;
    @Modifiable(Attribute.STRENGTH)
    private double power = 20;
    private double powerDiminishPerEntity = 7.5;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "lavadisk");
    }
  }
}