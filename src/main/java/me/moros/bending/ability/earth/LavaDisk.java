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

import com.destroystokyo.paper.MaterialSetTag;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ExpiringSet;
import me.moros.bending.util.FireTick;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class LavaDisk extends AbilityInstance {
  private static final String[] colors = {"2F1600", "5E2C00", "8C4200", "B05300", "C45D00", "F05A00", "F0A000", "F0BE00"};
  private static final Config config = new Config();

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

  public LavaDisk(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (!user.hasPermission("bending.lava")) {
      return false;
    }

    if (Bending.game().abilityManager(user.world()).hasAbility(user, LavaDisk.class)) {
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
    for (Block block : WorldMethods.nearbyBlocks(user.world(), aabb.at(location))) {
      if (MaterialUtil.isWater(block) || MaterialUtil.isWater(block.getRelative(BlockFace.UP))) {
        return false;
      }
      if (!MaterialUtil.isLava(block)) {
        TempBlock.createAir(block, BendingProperties.EARTHBENDING_REVERT_TIME);
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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
    CollisionUtil.handleEntityCollisions(user, collider, e -> damageEntity(e, damage));
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    Location center = location.toLocation(user.world());
    ParticleUtil.create(Particle.BLOCK_CRACK, center)
      .count(16).offset(0.1, 0.1, 0.1).extra(0.01)
      .data(Material.MAGMA_BLOCK.createBlockData()).spawn();
    ParticleUtil.create(Particle.LAVA, center)
      .count(2).offset(0.1, 0.1, 0.1).extra(0.01).spawn();
    SoundUtil.playSound(center, Sound.BLOCK_STONE_BREAK, 1, 1.5F);
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(collider);
  }

  private boolean damageEntity(Entity entity, double damage) {
    if (!affectedEntities.contains(entity)) {
      affectedEntities.add(entity);
      FireTick.ignite(user, entity);
      DamageUtil.damageEntity(entity, user, damage, description());
      currentPower -= userConfig.powerDiminishPerEntity;
      ParticleUtil.create(Particle.LAVA, entity.getLocation()).count(4)
        .offset(0.5, 0.5, 0.5).extra(0.1).spawn();
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
      TempBlock.createAir(block, BendingProperties.EARTHBENDING_REVERT_TIME);
      ParticleUtil.create(Particle.LAVA, block.getLocation())
        .offset(0.5, 0.5, 0.5).extra(0.05).spawn();
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        SoundUtil.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.3F, 0.3F);
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
        Location loc = location.add(VectorMethods.rotateAroundAxisY(temp, cos, sin)).toLocation(user.world());
        ParticleUtil.createRGB(loc, colors[index], size).spawn();
        if (length > 0.5) {
          damageBlock(loc.getBlock());
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
      BlockMethods.playLavaExtinguishEffect(block);
      return false;
    }
    return MaterialUtil.isTransparent(block) || damageBlock(block);
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double minDamage;
    @Modifiable(Attribute.DAMAGE)
    public double maxDamage;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double power;
    public double powerDiminishPerEntity;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "lavadisk");

      cooldown = abilityNode.node("cooldown").getLong(9000);
      minDamage = abilityNode.node("min-damage").getDouble(1.0);
      maxDamage = abilityNode.node("max-damage").getDouble(4.0);
      range = abilityNode.node("range").getDouble(18.0);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      speed = abilityNode.node("speed").getDouble(0.75);
      power = abilityNode.node("power").getDouble(20.0);
      powerDiminishPerEntity = abilityNode.node("damage-entity-power-cost").getDouble(7.5);
    }
  }
}
