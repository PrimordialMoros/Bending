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

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirAgility extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  public AirAgility(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.FLYING).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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

  private static class Config extends Configurable {
    @Modifiable(Attribute.STRENGTH)
    public int speedAmplifier;
    @Modifiable(Attribute.STRENGTH)
    public int jumpAmplifier;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "passives", "airagility");

      speedAmplifier = abilityNode.node("speed-amplifier").getInt(2);
      jumpAmplifier = abilityNode.node("jump-amplifier").getInt(3);
    }
  }
}
