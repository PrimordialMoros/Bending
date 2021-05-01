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

import java.util.Collections;
import java.util.Optional;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Bolt extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3 targetLocation;

  private boolean struck = false;
  private long startTime;

  public Bolt(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, description())) {
      return false;
    }
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder()
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
      if (user.sneaking()) {
        ParticleUtil.createRGB(user.mainHandSide().toLocation(user.world()), "01E1FF").spawn();
        return UpdateResult.CONTINUE;
      } else {
        strike();
      }
    } else if (user.sneaking()) {
      return UpdateResult.CONTINUE;
    }
    return UpdateResult.REMOVE;
  }

  private boolean onEntityHit(Entity entity) {
    if (entity instanceof Creeper) {
      ((Creeper) entity).setPowered(true);
    }
    double distance = EntityMethods.entityCenter(entity).distance(targetLocation);
    boolean hitWater = MaterialUtil.isWater(targetLocation.toBlock(user.world()));

    boolean vulnerable = (entity instanceof LivingEntity && InventoryUtil.hasMetalArmor((LivingEntity) entity));

    double damage = (vulnerable || hitWater) ? userConfig.damage * 2 : userConfig.damage;
    if (distance > (0.3 * userConfig.radius)) {
      damage -= (hitWater ? distance / 3 : distance / 2);
    }
    DamageUtil.damageEntity(entity, user, damage, description());
    return true;
  }

  private boolean isNearbyChannel() {
    Optional<Bolt> instance = Bending.game().abilityManager(user.world()).instances(Bolt.class)
      .filter(b -> !b.user().equals(user))
      .filter(b -> b.user().location().distanceSq(targetLocation) < userConfig.radius * userConfig.radius)
      .findAny();
    instance.ifPresent(bolt -> bolt.startTime = 0);
    return instance.isPresent();
  }

  private void dealDamage() {
    Collider collider = new Sphere(targetLocation, userConfig.radius);
    CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true, true);
    FragileStructure.tryDamageStructure(Collections.singletonList(targetLocation.toBlock(user.world())), 8);
  }

  private void strike() {
    targetLocation = user.rayTraceEntity(userConfig.range)
      .map(EntityMethods::entityCenter).orElseGet(() -> user.rayTrace(userConfig.range));

    for (int i = 0; i < 2; i++) {
      Block target = targetLocation.toBlock(user.world()).getRelative(BlockFace.DOWN);
      if (MaterialUtil.isTransparent(target)) {
        targetLocation = targetLocation.add(Vector3.MINUS_J);
      } else {
        break;
      }
    }

    if (!Bending.game().protectionSystem().canBuild(user, targetLocation.toBlock(user.world()))) {
      return;
    }
    user.world().spigot().strikeLightningEffect(targetLocation.toLocation(user.world()), true);
    SoundUtil.playSound(targetLocation.toLocation(user.world()), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 5, 1.2F);
    user.addCooldown(description(), userConfig.cooldown);
    struck = true;
    if (!isNearbyChannel()) {
      dealDamage();
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public void onDestroy() {
    if (!struck && userConfig.duration > 0 && System.currentTimeMillis() > startTime + userConfig.duration) {
      DamageUtil.damageEntity(user.entity(), user, userConfig.damage, description());
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.RADIUS)
    public double radius;
    @Attribute(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Attribute(Attribute.DURATION)
    public long duration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "bolt");
      cooldown = abilityNode.node("cooldown").getLong(6000);
      damage = abilityNode.node("damage").getDouble(3.0);
      range = abilityNode.node("range").getDouble(30.0);
      radius = abilityNode.node("radius").getDouble(4.0);
      chargeTime = abilityNode.node("charge-time").getLong(2000);
      duration = abilityNode.node("duration").getLong(8000);
    }
  }
}

