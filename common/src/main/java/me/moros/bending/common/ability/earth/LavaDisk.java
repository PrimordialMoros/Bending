/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Disk;
import me.moros.bending.api.collision.geometry.OBB;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

public class LavaDisk extends AbilityInstance {
  private static final String[] colors = {"#2F1600", "#5E2C00", "#8C4200", "#B05300", "#C45D00", "#F05A00", "#F0A000", "#F0BE00"};
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d location;
  private Vector3d direction;
  private Collider collider;

  private final ExpiringSet<UUID> affectedEntities = new ExpiringSet<>(500);

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
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, LavaDisk.class)) {
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
    location = source.center();
    direction = user.direction();
    AABB aabb = AABB.of(Vector3d.of(-r, -0.3, -r), Vector3d.of(r, 0.3, r));
    collider = Disk.of(Sphere.of(r), OBB.of(aabb)).at(location);
    for (Block block : user.world().nearbyBlocks(aabb.at(location))) {
      if (MaterialUtil.isWater(block) || MaterialUtil.isWater(block.offset(Direction.UP))) {
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
      direction = targetLocation.subtract(location).normalize();
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
    BlockType.MAGMA_BLOCK.asParticle(location).count(16).offset(0.1).extra(0.01).spawn(user.world());
    Particle.LAVA.builder(location).count(2).offset(0.1).extra(0.01).spawn(user.world());
    Sound.BLOCK_STONE_BREAK.asEffect(1, 1.5F).play(user.world(), location);
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(collider);
  }

  private boolean damageEntity(Entity entity, double damage) {
    if (affectedEntities.add(entity.uuid())) {
      Particle.LAVA.builder(entity.center()).count(4).offset(0.5).extra(0.1).spawn(user.world());
      BendingEffect.FIRE_TICK.apply(user, entity);
      entity.damage(damage, user, description());
      currentPower -= userConfig.powerDiminishPerEntity;
      return true;
    }
    return false;
  }

  private boolean damageBlock(Position position) {
    if (currentPower <= 0) {
      return false;
    }
    Block block = user.world().blockAt(position);
    FragileStructure.tryDamageStructure(block, 0, Ray.of(location, direction));
    if (!TempBlock.isBendable(block) || !user.canBuild(block)) {
      return false;
    }
    if (MaterialUtil.isLava(block)) {
      return true;
    }
    BlockType mat = block.type();
    boolean tree = BlockTag.LEAVES.isTagged(mat) || BlockTag.LOGS_THAT_BURN.isTagged(mat);
    if (tree || EarthMaterials.isEarthOrSand(block)) {
      currentPower -= block.type().hardness();
      TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
      Vector3d center = block.center();
      Particle.LAVA.builder(center).offset(0.5).extra(0.05).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        Sound.BLOCK_GRINDSTONE_USE.asEffect(0.3F, 0.3F).play(block);
      }
      return true;
    }
    return false;
  }

  private void displayLavaDisk() {
    damageBlock(location);
    float angle = user.yaw() + 90;
    double cos = Math.cos(-angle);
    double sin = Math.sin(-angle);
    int offset = 0;
    int index = 0;
    float size = 0.8F;
    for (int i = 1; i <= 8; i++) {
      for (int j = 0; j <= 288; j += 72) {
        int rotAngle = rotationAngle + j + offset;
        double length = 0.1 * i;
        Vector3d temp = Vector3d.of(length * Math.cos(rotAngle), 0, length * Math.sin(rotAngle));
        Vector3d loc = location.add(VectorUtil.rotateAroundAxisY(temp, cos, sin));
        ParticleBuilder.rgb(loc, colors[index], size).spawn(user.world());
        if (length > 0.5) {
          damageBlock(loc);
        }
      }
      offset += 4;
      index = Math.min(colors.length - 1, ++index);
      size -= 0.05F;
    }
  }

  private boolean isLocationSafe() {
    Block block = user.world().blockAt(location);
    if (MaterialUtil.isWater(block)) {
      WorldUtil.playLavaExtinguishEffect(block);
      return false;
    }
    return MaterialUtil.isTransparent(block) || damageBlock(block);
  }

  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "lavadisk");
    }
  }
}
