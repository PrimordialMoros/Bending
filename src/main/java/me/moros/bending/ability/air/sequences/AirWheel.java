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
import java.util.List;
import java.util.Set;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.AirScooter;
import me.moros.bending.config.Configurable;
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
import me.moros.bending.util.ExpiringSet;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirWheel extends AbilityInstance {
  private static final AABB BOUNDS = new AABB(new Vector3(-0.4, -2, -2), new Vector3(0.4, 2, 2));
  private static final Config config = new Config();
  private static AbilityDescription scooterDesc;

  private User user;
  private Config userConfig;

  private final Set<Entity> affectedEntities = new ExpiringSet<>(250);

  private AirScooter scooter;
  private Collider collider;
  private Vector3 center;

  private long nextRenderTime;

  public AirWheel(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    if (scooterDesc == null) {
      scooterDesc = Bending.game().abilityRegistry().abilityDescription("AirScooter").orElseThrow(RuntimeException::new);
    }
    scooter = new AirScooter(scooterDesc);
    if (user.onCooldown(scooterDesc) || !scooter.activate(user, ActivationMethod.ATTACK)) {
      return false;
    }
    scooter.canRender = false;

    this.user = user;
    recalculateConfig();

    center = user.location().add(new Vector3(0, 0.8, 0));
    nextRenderTime = 0;
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    long time = System.currentTimeMillis();
    center = user.location().add(new Vector3(0, 0.8, 0)).add(user.direction().setY(0).multiply(1.2));
    collider = new Disk(new OBB(BOUNDS, Vector3.PLUS_J, Math.toRadians(user.yaw())), new Sphere(center, 2));

    if (time >= nextRenderTime) {
      render();
      nextRenderTime = time + 100;
    }

    Block base = center.subtract(new Vector3(0, 1.6, 0)).toBlock(user.world());
    BlockMethods.tryCoolLava(user, base);
    BlockMethods.tryExtinguishFire(user, base);

    CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit);
    return scooter.update();
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.contains(entity)) {
      return false;
    }
    affectedEntities.add(entity);
    DamageUtil.damageEntity(entity, user, userConfig.damage, description());
    return true;
  }

  @Override
  public void onDestroy() {
    scooter.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private void render() {
    Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(user.direction().setY(0));
    VectorMethods.circle(user.direction().multiply(1.6), rotateAxis, 40).forEach(v ->
      ParticleUtil.createAir(center.add(v).toLocation(user.world())).spawn()
    );
  }

  public @NonNull Vector3 center() {
    return center;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(collider);
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "sequences", "airwheel");

      cooldown = abilityNode.node("cooldown").getLong(8000);
      damage = abilityNode.node("damage").getDouble(1.0);
    }
  }
}

