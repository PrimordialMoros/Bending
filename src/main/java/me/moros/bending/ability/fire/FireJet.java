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

package me.moros.bending.ability.fire;

import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempLight;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SoundUtil.SoundEffect;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FireJet extends AbilityInstance {
  private static final Config config = new Config();
  private static final SoundEffect LOUD_EXPLOSION = SoundUtil.EXPLOSION.with(10, 0);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private boolean jetBlast;
  private long duration;
  private long startTime;
  private int ticks = 3;

  public FireJet(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, FireJet.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block block = user.locBlock();
    boolean ignitable = MaterialUtil.isIgnitable(block);
    if (!ignitable && !MaterialUtil.isAir(block)) {
      return false;
    }

    if (ignitable) {
      Tasker.sync(() -> igniteBlock(block), 1);
    }

    if (user.sneaking()) {
      jetBlast = true;
      duration = userConfig.jetBlastDuration;
      jetBlastAnimation();
    } else {
      jetBlast = false;
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
    TempBlock.fire().duration(BendingProperties.FIRE_REVERT_TIME).build(block);
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  private void jetBlastAnimation() {
    Vector3d center = user.location().add(new Vector3d(0, 0.2, 0));
    VectorUtil.circle(Vector3d.PLUS_I, Vector3d.PLUS_J, 36).forEach(v ->
      ParticleUtil.fire(user, center.add(v.multiply(0.5))).count(0).offset(v).extra(0.09).spawn(user.world())
    );
    LOUD_EXPLOSION.play(user.world(), user.location());
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    double halfSpeed = 0.5 * (jetBlast ? userConfig.jetBlastSpeed : userConfig.speed);
    double timeFactor = (System.currentTimeMillis() - startTime) / (double) duration;
    double speed = halfSpeed + halfSpeed * Math.sin(Math.PI * timeFactor);

    EntityUtil.applyVelocity(this, user.entity(), user.direction().multiply(speed));
    user.entity().setFallDistance(0);

    Vector3d target = user.location().add(user.velocity().negate());
    int amount = jetBlast ? 16 : 10;
    double offset = jetBlast ? 0.7 : 0.4;
    double particleSpeed = 0.05 * Math.min(1, speed);
    for (int i = 0; i < amount; i++) {
      Vector3d center = VectorUtil.gaussianOffset(user.location(), offset);
      Vector3d v = target.subtract(center);
      ParticleUtil.fire(user, center).count(0).offset(v).extra(particleSpeed).spawn(user.world());
    }
    TempLight.builder(++ticks).build(user.locBlock());
    if (ThreadLocalRandom.current().nextBoolean()) {
      SoundUtil.FIRE.play(user.world(), user.location());
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), jetBlast ? userConfig.jetBlastCooldown : userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    private long duration;
    @Modifiable(Attribute.SPEED)
    public double jetBlastSpeed;
    @Modifiable(Attribute.COOLDOWN)
    public long jetBlastCooldown;
    @Modifiable(Attribute.DURATION)
    public long jetBlastDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firejet");

      speed = abilityNode.node("speed").getDouble(0.85);
      cooldown = abilityNode.node("cooldown").getLong(7000);
      duration = abilityNode.node("duration").getLong(2000);

      CommentedConfigurationNode boostedNode = abilityNode.node("boosted");

      jetBlastSpeed = boostedNode.node("speed").getDouble(1.5);
      jetBlastCooldown = boostedNode.node("cooldown").getLong(10000);
      jetBlastDuration = boostedNode.node("duration").getLong(2000);
    }
  }
}
