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

package me.moros.bending.ability.water;

import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.KeyUtil;
import me.moros.math.FastMath;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class HealingWaters extends AbilityInstance {
  private enum Mode {SELF, OTHERS}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private boolean healed = false;
  private long nextTime;

  public HealingWaters(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldUid()).hasAbility(user, HealingWaters.class)) {
      return false;
    }
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    nextTime = System.currentTimeMillis();
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
    long time = System.currentTimeMillis();
    if (time >= nextTime) {
      nextTime = time + 250;
      if (!tryHeal()) {
        return UpdateResult.REMOVE;
      }
    } else {
      ParticleBuilder.rgb(user.mainHandSide(), "#00FFFF").spawn(user.world());
    }
    return UpdateResult.CONTINUE;
  }

  private boolean tryHeal() {
    LivingEntity target;
    Mode mode = user.store().get(KeyUtil.data("healingwaters-mode", Mode.class)).orElse(Mode.SELF);
    if (mode == Mode.OTHERS) {
      Entity entity = user.rayTrace(userConfig.range + 1).cast(user.world()).entity();
      if (entity instanceof LivingEntity && user.entity().hasLineOfSight(entity)) {
        target = (LivingEntity) entity;
      } else {
        return false;
      }
    } else {
      target = user.entity();
    }
    if (!target.inWater(false) && !InventoryUtil.hasFullBottle(user)) {
      return false;
    }
    EntityUtil.removeNegativeEffects(target);
    if (target.health() < target.maxHealth()) {
      ParticleBuilder.rgb(target.center(), "#00FFFF").count(6).offset(0.35).spawn(user.world());
      int ticks = FastMath.floor(userConfig.duration / 50.0);
      if (EntityUtil.tryAddPotion(target, PotionEffect.REGENERATION, ticks, userConfig.power - 1)) {
        healed = true;
      }
    }
    return true;
  }

  public static void switchMode(User user) {
    if (user.selectedAbilityName().equals("HealingWaters")) {
      var key = KeyUtil.data("healingwaters-mode", Mode.class);
      if (user.store().canEdit(key)) {
        Mode mode = user.store().toggle(key, Mode.SELF);
        user.sendActionBar(Component.text("Healing: " + mode.name(), ColorPalette.TEXT_COLOR));
      }
    }
  }

  @Override
  public void onDestroy() {
    if (healed) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Modifiable(Attribute.DURATION)
    private long duration = 3000;
    @Modifiable(Attribute.RANGE)
    private double range = 5;
    @Modifiable(Attribute.STRENGTH)
    private int power = 2;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "healingwaters");
    }
  }
}
