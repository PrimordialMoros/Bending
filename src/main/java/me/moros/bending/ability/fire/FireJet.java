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

import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.Flight;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireJet extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Flight flight;

  private boolean jetBlast;
  private double speed;
  private long duration;
  private long startTime;

  public FireJet(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, FireJet.class)) {
      return false;
    }

    this.user = user;
    recalculateConfig();

    Block block = user.locBlock();
    boolean ignitable = MaterialUtil.isIgnitable(block);
    if (!ignitable && !MaterialUtil.isAir(block)) {
      return false;
    }

    flight = Flight.get(user);
    if (ignitable) {
      Tasker.newChain().delay(1).sync(() -> igniteBlock(block)).execute();
    }

    if (user.sneaking()) {
      jetBlast = true;
      speed = userConfig.jetBlastSpeed;
      duration = userConfig.jetBlastDuration;
      jetBlastAnimation();
    } else {
      jetBlast = false;
      speed = userConfig.speed;
      duration = userConfig.duration;
    }

    removalPolicy = Policies.builder()
      .add(Policies.IN_LIQUID)
      .add(ExpireRemovalPolicy.of(duration))
      .build();

    startTime = System.currentTimeMillis();
    return true;
  }

  private void igniteBlock(Block block) {
    TempBlock.create(block, Material.FIRE.createBlockData(), BendingProperties.FIRE_REVERT_TIME, true);
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  private void jetBlastAnimation() {
    Vector3 center = user.location().add(new Vector3(0, 0.2, 0));
    VectorMethods.circle(Vector3.PLUS_I, Vector3.PLUS_J, 36).forEach(v ->
      ParticleUtil.createFire(user, center.add(v.multiply(0.5)).toLocation(user.world()))
        .count(0).offset(v.x, v.y, v.z).extra(0.09).spawn()
    );
    SoundUtil.playSound(user.entity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10, 0);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    FireTick.extinguish(user.entity());
    // scale down to 0.5 speed near the end
    double factor = 1 - ((System.currentTimeMillis() - startTime) / (2.0 * duration));

    user.entity().setVelocity(user.direction().multiply(speed * factor).clampVelocity());
    user.entity().setFallDistance(0);

    Vector3 target = user.location().add(user.velocity().negate());
    int amount = jetBlast ? 16 : 10;
    double offset = jetBlast ? 0.7 : 0.4;
    for (int i = 0; i < amount; i++) {
      Vector3 center = VectorMethods.gaussianOffset(user.location(), offset);
      Vector3 v = target.subtract(center).normalize();
      ParticleUtil.createFire(user, center.toLocation(user.world()))
        .count(0).offset(v.x, v.y, v.z).extra(0.05 * speed * factor).spawn();
    }

    if (ThreadLocalRandom.current().nextBoolean()) {
      SoundUtil.FIRE_SOUND.play(user.entity().getLocation());
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), jetBlast ? userConfig.jetBlastCooldown : userConfig.cooldown);
    flight.release();
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.SPEED)
    public double speed;
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute(Attribute.SPEED)
    public double jetBlastSpeed;
    @Attribute(Attribute.COOLDOWN)
    public long jetBlastCooldown;
    @Attribute(Attribute.DURATION)
    public long jetBlastDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firejet");

      speed = abilityNode.node("speed").getDouble(0.8);
      cooldown = abilityNode.node("cooldown").getLong(7000);
      duration = abilityNode.node("duration").getLong(2000);

      jetBlastSpeed = abilityNode.node("boost-speed").getDouble(1.6);
      jetBlastCooldown = abilityNode.node("boost-cooldown").getLong(10000);
      jetBlastDuration = abilityNode.node("boost-duration").getLong(2500);
    }
  }
}
