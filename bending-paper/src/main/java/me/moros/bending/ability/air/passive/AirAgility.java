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

package me.moros.bending.ability.air.passive;

import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirAgility extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  public AirAgility(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.FLYING).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      user.entity().removePotionEffect(PotionEffectType.JUMP);
      user.entity().removePotionEffect(PotionEffectType.SPEED);
      return UpdateResult.CONTINUE;
    }
    handlePotionEffect(PotionEffectType.JUMP, userConfig.jumpAmplifier - 1);
    handlePotionEffect(PotionEffectType.SPEED, userConfig.speedAmplifier - 1);
    return UpdateResult.CONTINUE;
  }

  private void handlePotionEffect(PotionEffectType type, int amplifier) {
    if (amplifier < 0) {
      return;
    }
    EntityUtil.tryAddPotion(user.entity(), type, 100, amplifier);
  }

  @Override
  public void onDestroy() {
    user.entity().removePotionEffect(PotionEffectType.JUMP);
    user.entity().removePotionEffect(PotionEffectType.SPEED);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.STRENGTH)
    private int speedAmplifier = 2;
    @Modifiable(Attribute.STRENGTH)
    private int jumpAmplifier = 3;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "passives", "airagility");
    }
  }
}
