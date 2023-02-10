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

package me.moros.bending.common.ability.air.passive;

import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.entity.EntityUtil;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.common.config.ConfigManager;
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      onDestroy();
      return UpdateResult.CONTINUE;
    }
    EntityUtil.tryAddPotion(user, PotionEffect.JUMP_BOOST, 100, userConfig.jumpAmplifier - 1);
    EntityUtil.tryAddPotion(user, PotionEffect.SPEED, 100, userConfig.speedAmplifier - 1);
    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    EntityUtil.tryRemovePotion(user, PotionEffect.JUMP_BOOST, 100, userConfig.jumpAmplifier - 1);
    EntityUtil.tryRemovePotion(user, PotionEffect.SPEED, 100, userConfig.speedAmplifier - 1);
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
    public List<String> path() {
      return List.of("abilities", "air", "passives", "airagility");
    }
  }
}
