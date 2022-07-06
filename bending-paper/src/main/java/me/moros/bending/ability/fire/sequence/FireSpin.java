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

package me.moros.bending.ability.fire.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempLight;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FireSpin extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final Collection<FireStream> streams = new ArrayList<>();

  public FireSpin(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();

    Vector3d origin = user.location().add(Vector3d.PLUS_J);
    VectorUtil.circle(Vector3d.PLUS_I, Vector3d.PLUS_J, 40).forEach(
      v -> streams.add(new FireStream(new Ray(origin, v.multiply(userConfig.range))))
    );

    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class FireStream extends ParticleStream {
    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed / 2, 0.5);
      canCollide = Block::isLiquid;
      steps = 2;
    }

    @Override
    public void render() {
      ParticleUtil.fire(user, location).extra(0.01).spawn(user.world());
      TempLight.builder(++ticks).build(location.toBlock(user.world()));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        BendingEffect.FIRE_TICK.apply(user, entity);
        EntityUtil.applyVelocity(FireSpin.this, entity, ray.direction.normalize().multiply(userConfig.knockback));
      }
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(List.of(block), 3);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "sequences", "firespin");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      damage = abilityNode.node("damage").getDouble(1.0);
      range = abilityNode.node("range").getDouble(6.0);
      speed = abilityNode.node("speed").getDouble(0.5);
      knockback = abilityNode.node("knockback").getDouble(1.8);

      abilityNode.node("speed").comment("How many blocks the streams advance with each tick.");
    }
  }
}
