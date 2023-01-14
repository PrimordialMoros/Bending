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

package me.moros.bending.platform.entity;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.DamageUtil;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.item.BukkitInventory;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.property.BooleanProperty;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitLivingEntity extends BukkitEntity implements LivingEntity {
  private AttributeInstance maxHealth;
  private final Supplier<Map<BooleanProperty, Boolean>> properties;

  public BukkitLivingEntity(org.bukkit.entity.LivingEntity handle) {
    super(handle);
    this.properties = Suppliers.lazy(IdentityHashMap::new);
  }

  @Override
  public org.bukkit.entity.LivingEntity handle() {
    return (org.bukkit.entity.LivingEntity) super.handle();
  }

  @Override
  public double health() {
    return handle().getHealth();
  }

  @Override
  public double maxHealth() {
    if (maxHealth == null) {
      maxHealth = Objects.requireNonNull(handle().getAttribute(Attribute.GENERIC_MAX_HEALTH));
    }
    return maxHealth.getValue();
  }

  @Override
  public boolean damage(double damage) {
    handle().damage(damage);
    return true;
  }

  @Override
  public boolean damage(double damage, Entity source) {
    handle().damage(damage, ((BukkitEntity) source).handle());
    return true;
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    return DamageUtil.damageEntity(handle(), source, damage, desc);
  }

  @Override
  public boolean ai() {
    return handle().hasAI();
  }

  @Override
  public void ai(boolean value) {
    handle().setAI(value);
  }

  @Override
  public double eyeHeight() {
    return handle().getEyeHeight();
  }

  @Override
  public @Nullable Inventory inventory() {
    var equipment = handle().getEquipment();
    return equipment == null ? null : new BukkitInventory(equipment);
  }

  @Override
  public boolean addPotion(Potion potion) {
    var bukkitPotion = PlatformAdapter.toBukkitPotion(potion);
    return bukkitPotion != null && handle().addPotionEffect(bukkitPotion);
  }

  @Override
  public boolean hasPotion(PotionEffect effect) {
    var bukkitEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    return bukkitEffect != null && handle().hasPotionEffect(bukkitEffect);
  }

  @Override
  public @Nullable Potion potion(PotionEffect effect) {
    var bukkitEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    return bukkitEffect == null ? null : PlatformAdapter.fromBukkitPotion(handle().getPotionEffect(bukkitEffect));
  }

  @Override
  public void removePotion(PotionEffect effect) {
    var bukkitEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    if (bukkitEffect != null) {
      handle().removePotionEffect(bukkitEffect);
    }
  }

  @Override
  public Collection<Potion> activePotions() {
    return handle().getActivePotionEffects().stream().map(PlatformAdapter::fromBukkitPotion).filter(Objects::nonNull).toList();
  }

  @Override
  public boolean hasLineOfSight(Entity other) {
    return other instanceof BukkitEntity ent && handle().hasLineOfSight(ent.handle());
  }

  @Override
  public int airCapacity() {
    return handle().getMaximumAir();
  }

  @Override
  public int remainingAir() {
    return handle().getRemainingAir();
  }

  @Override
  public void remainingAir(int amount) {
    handle().setRemainingAir(amount);
  }

  @Override
  public Entity shootArrow(Position origin, Vector3d direction, double power) {
    var w = handle().getWorld();
    var loc = origin.to(Location.class, w);
    var arrow = w.spawnArrow(loc, direction.to(Vector.class), (float) power, 0);
    arrow.setShooter(handle());
    arrow.setGravity(false);
    arrow.setInvulnerable(true);
    arrow.setPickupStatus(PickupStatus.DISALLOWED);
    return PlatformAdapter.fromBukkitEntity(arrow);
  }

  @Override
  public TriState checkProperty(BooleanProperty property) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    if (bukkitProperty == null) {
      return TriState.byBoolean(properties.get().get(property));
    }
    return bukkitProperty.get(handle());
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    var bukkitProperty = PropertyMapper.PROPERTIES.get(property);
    if (bukkitProperty != null) {
      bukkitProperty.set(handle(), value);
    } else {
      properties.get().put(property, value);
    }
  }
}
