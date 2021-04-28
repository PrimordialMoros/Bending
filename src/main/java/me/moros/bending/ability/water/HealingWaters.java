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

package me.moros.bending.ability.water;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HealingWaters extends AbilityInstance implements Ability {
  private static final org.bukkit.attribute.Attribute healthAttribute = org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH;
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private LivingEntity target;

  private long nextTime;

  public HealingWaters(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, HealingWaters.class)) {
      return false;
    }
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    nextTime = System.currentTimeMillis();
    target = user.getEntity();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, getDescription())) {
      return UpdateResult.REMOVE;
    }
    long time = System.currentTimeMillis();
    if (time > nextTime) {
      nextTime = time + 250;
      if (!tryHeal()) {
        return UpdateResult.REMOVE;
      }
    } else {
      ParticleUtil.createRGB(user.getMainHandSide().toLocation(user.getWorld()), "00ffff").spawn();
    }
    return UpdateResult.CONTINUE;
  }

  private boolean isValidEntity(LivingEntity entity) {
    if (entity == null || !entity.isValid()) {
      return false;
    }
    if (!entity.getWorld().equals(user.getWorld())) {
      return false;
    }
    if (EntityMethods.getEntityCenter(entity).distanceSq(user.getEyeLocation()) > userConfig.range * userConfig.range) {
      return false;
    }
    return user.getEntity().hasLineOfSight(entity);
  }

  private boolean tryHeal() {
    if (!user.getEntity().equals(target) && !isValidEntity(target)) {
      target = user.getEntity();
    }
    if (!MaterialUtil.isWater(target.getLocation().getBlock()) && !InventoryUtil.hasFullBottle(user)) {
      return false;
    }

    ParticleUtil.createRGB(EntityMethods.getEntityCenter(target).toLocation(user.getWorld()), "00ffff")
      .count(6).offset(0.35, 0.35, 0.35).spawn();
    target.getActivePotionEffects().stream().map(PotionEffect::getType).filter(PotionUtil::isNegative)
      .forEach(target::removePotionEffect);
    AttributeInstance attributeInstance = target.getAttribute(healthAttribute);
    if (attributeInstance != null && target.getHealth() < attributeInstance.getValue()) {
      PotionUtil.tryAddPotion(target, PotionEffectType.REGENERATION, 60, userConfig.power);
      return true;
    }
    return false;
  }

  public static void setTarget(User user, LivingEntity entity) {
    if (user.getSelectedAbilityName().equals("HealingWaters")) {
      Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, HealingWaters.class)
        .ifPresent(hw -> hw.setTarget(entity));
    }
  }

  private void setTarget(LivingEntity entity) {
    if (!target.equals(entity) && !user.getEntity().equals(entity) && isValidEntity(entity)) {
      target = entity;
    }
  }

  @Override
  public void onDestroy() {
    user.setCooldown(getDescription(), userConfig.cooldown);
  }

  @Override
  public @NonNull User getUser() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.STRENGTH)
    public int power;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "healingwaters");

      cooldown = abilityNode.node("cooldown").getLong(3000);
      range = abilityNode.node("range").getDouble(5.0);
      power = abilityNode.node("power").getInt(2) - 1;
    }
  }
}
