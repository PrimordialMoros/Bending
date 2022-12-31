/*
 * Copyright 2020-2023 Moros
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.property.EntityProperty;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FireJet extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);
  private static final SoundEffect LOUD_EXPLOSION = SoundEffect.EXPLOSION.with(10, 0);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private boolean jetBlast;
  private TriState wasGliding = TriState.NOT_SET;
  private long duration;
  private long startTime;
  private int ticks = 3;

  public FireJet(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldUid()).destroyUserInstance(user, FireJet.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    Block block = user.block();
    boolean ignitable = MaterialUtil.isIgnitable(block);
    if (!ignitable && !MaterialUtil.isAir(block)) {
      return false;
    }

    if (ignitable) {
      igniteBlock(block);
    }

    if (user.sneaking()) {
      jetBlast = true;
      duration = userConfig.jetBlastDuration;
      jetBlastAnimation();
    } else {
      jetBlast = false;
      duration = userConfig.duration;
    }

    wasGliding = user.checkProperty(EntityProperty.GLIDING);
    user.setProperty(EntityProperty.GLIDING, true);

    removalPolicy = Policies.builder()
      .add(Policies.PARTIALLY_UNDER_WATER)
      .add(Policies.PARTIALLY_UNDER_LAVA)
      .add(ExpireRemovalPolicy.of(duration))
      .build();

    startTime = System.currentTimeMillis();
    return true;
  }

  private void igniteBlock(Block block) {
    TempBlock.fire().duration(BendingProperties.instance().fireRevertTime()).ability(this).build(block);
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  private void jetBlastAnimation() {
    Vector3d center = user.location().add(0, 0.2, 0);
    VectorUtil.circle(Vector3d.PLUS_I, Vector3d.PLUS_J, 36).forEach(v ->
      ParticleBuilder.fire(user, center.add(v.multiply(0.5))).count(0).offset(v).extra(0.09).spawn(user.world())
    );
    LOUD_EXPLOSION.play(user.world(), user.location());
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    double halfSpeed = 0.5 * (jetBlast ? userConfig.jetBlastSpeed : userConfig.speed);
    double timeFactor = (System.currentTimeMillis() - startTime) / (double) duration;
    double speed = halfSpeed + halfSpeed * Math.sin(Math.PI * timeFactor);

    user.applyVelocity(this, user.direction().multiply(speed));
    user.fallDistance(0);

    Vector3d target = user.location().add(user.velocity().negate());
    int amount = jetBlast ? 16 : 10;
    double offset = jetBlast ? 0.7 : 0.4;
    double particleSpeed = 0.05 * Math.min(1, speed);
    for (int i = 0; i < amount; i++) {
      Vector3d center = VectorUtil.gaussianOffset(user.location(), offset);
      Vector3d v = target.subtract(center);
      ParticleBuilder.fire(user, center).count(0).offset(v).extra(particleSpeed).spawn(user.world());
    }
    TempLight.builder(++ticks).build(user.block());
    if (ThreadLocalRandom.current().nextBoolean()) {
      SoundEffect.FIRE.play(user.world(), user.location());
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), jetBlast ? userConfig.jetBlastCooldown : userConfig.cooldown);
    user.setProperty(EntityProperty.GLIDING, wasGliding);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    private double speed = 0.85;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 7000;
    @Modifiable(Attribute.DURATION)
    private long duration = 2000;
    @Modifiable(Attribute.SPEED)
    private double jetBlastSpeed = 1.5;
    @Modifiable(Attribute.COOLDOWN)
    private long jetBlastCooldown = 10_000;
    @Modifiable(Attribute.DURATION)
    private long jetBlastDuration = 2000;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "firejet");
    }
  }
}
