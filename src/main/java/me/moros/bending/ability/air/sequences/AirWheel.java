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

package me.moros.bending.ability.air.sequences;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class AirWheel extends AbilityInstance implements Ability {
  private static final AABB BOUNDS = new AABB(new Vector3(-0.4, -2, -2), new Vector3(0.4, 2, 2));
  private static final Config config = new Config();
  private static AbilityDescription scooterDesc;

  private User user;
  private Config userConfig;

  private final Map<Entity, Boolean> affectedEntities = ExpiringMap.builder()
    .expirationPolicy(ExpirationPolicy.CREATED)
    .expiration(250, TimeUnit.MILLISECONDS).build();

  private AirScooter scooter;
  private Collider collider;
  private Vector3 center;

  private long nextRenderTime;

  public AirWheel(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (scooterDesc == null) {
      scooterDesc = Bending.getGame().getAbilityRegistry().getAbilityDescription("AirScooter").orElseThrow(RuntimeException::new);
    }
    scooter = new AirScooter(scooterDesc);
    if (user.isOnCooldown(scooter.getDescription()) || !scooter.activate(user, ActivationMethod.ATTACK)) {
      return false;
    }
    scooter.canRender = false;

    this.user = user;
    recalculateConfig();

    user.setCooldown(scooter.getDescription(), 1000); // Ensures airscooter won't be activated twice

    nextRenderTime = 0;
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    long time = System.currentTimeMillis();
    center = user.getLocation().add(new Vector3(0, 0.8, 0)).add(user.getDirection().setY(0).scalarMultiply(1.2));
    collider = new Disk(new OBB(BOUNDS, Vector3.PLUS_J, FastMath.toRadians(user.getYaw())), new Sphere(center, 2));

    if (time > nextRenderTime) {
      render();
      nextRenderTime = time + 100;
    }

    Block base = center.subtract(new Vector3(0, 1.6, 0)).toBlock(user.getWorld());
    BlockMethods.tryCoolLava(user, base);
    BlockMethods.tryExtinguishFire(user, base);

    CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit);
    return scooter.update();
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.containsKey(entity)) {
      return false;
    }
    affectedEntities.put(entity, false);
    DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
    return true;
  }

  @Override
  public void onDestroy() {
    scooter.onDestroy();
    user.setCooldown(getDescription(), userConfig.cooldown);
  }

  private void render() {
    Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(user.getDirection().setY(0));
    VectorMethods.circle(user.getDirection().scalarMultiply(1.6), rotateAxis, 40).forEach(v ->
      ParticleUtil.createAir(center.add(v).toLocation(user.getWorld())).spawn()
    );
  }

  public Vector3 getCenter() {
    return center;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> getColliders() {
    return Collections.singletonList(collider);
  }

  @Override
  public @NonNull User getUser() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;

    public Config() {
      super();
    }

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "sequences", "airwheel");

      cooldown = abilityNode.node("cooldown").getLong(8000);
      damage = abilityNode.node("damage").getDouble(2.0);
    }
  }
}

