/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.ability.water;

import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityUtil;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.InventoryUtil;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.math.FastMath;
import net.kyori.adventure.text.Component;

public class HealingWaters extends AbilityInstance {
  private enum Mode {SELF, OTHERS}

  private static final DataKey<Mode> KEY = KeyUtil.data("healingwaters-mode", Mode.class);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private boolean healed = false;
  private long nextTime;

  public HealingWaters(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, HealingWaters.class)) {
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
    }
    ParticleBuilder.rgb(user.mainHandSide(), "#00FFFF").spawn(user.world());
    return UpdateResult.CONTINUE;
  }

  private boolean tryHeal() {
    LivingEntity target;
    Mode mode = user.store().get(KEY).orElse(Mode.SELF);
    if (mode == Mode.OTHERS) {
      Entity entity = user.rayTrace(userConfig.range + 1).cast(user.world()).entity();
      if (entity instanceof LivingEntity living) {
        target = living;
      } else {
        return false;
      }
    } else {
      target = user;
    }
    if (!target.inWater() && !InventoryUtil.hasFullBottle(user)) {
      return false;
    }
    EntityUtil.removeNegativeEffects(target);
    if (target.propertyValue(EntityProperties.HEALTH) < target.propertyValue(EntityProperties.MAX_HEALTH)) {
      ParticleBuilder.rgb(target.center(), "#00FFFF").count(6).offset(0.35).spawn(user.world());
      int ticks = FastMath.floor(userConfig.duration / 50.0);
      if (EntityUtil.tryAddPotion(target, PotionEffect.REGENERATION, ticks, userConfig.power - 1)) {
        healed = true;
      }
    }
    return true;
  }

  public static void switchMode(User user) {
    if (user.hasAbilitySelected("healingwaters")) {
      if (user.store().canEdit(KEY)) {
        Mode mode = user.store().toggle(KEY, Mode.SELF);
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

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Modifiable(Attribute.DURATION)
    private long duration = 3000;
    @Modifiable(Attribute.RANGE)
    private double range = 5;
    @Modifiable(Attribute.STRENGTH)
    private int power = 2;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "healingwaters");
    }
  }
}
