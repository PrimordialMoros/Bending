/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.paper.platform.entity;

import java.util.Collection;
import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.paper.platform.DamageUtil;
import me.moros.bending.paper.platform.PlatformAdapter;
import me.moros.bending.paper.platform.item.BukkitInventory;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitLivingEntity extends BukkitEntity implements LivingEntity {
  private AttributeInstance maxHealth;

  public BukkitLivingEntity(org.bukkit.entity.LivingEntity handle) {
    super(handle);
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
    handle().damage(damage, PlatformAdapter.toBukkitEntity(source));
    return true;
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    BendingDamageEvent event = source.game().eventBus().postAbilityDamageEvent(source, desc, this, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    if (type() == EntityType.PLAYER) {
      DamageUtil.cacheDamageSource(uuid(), DamageSource.of(source.name(), desc));
    }
    return Platform.instance().nativeAdapter().damage(event);
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
    return handle().addPotionEffect(PlatformAdapter.toBukkitPotion(potion));
  }

  @Override
  public boolean hasPotion(PotionEffect effect) {
    return handle().hasPotionEffect(PlatformAdapter.toBukkitPotion(effect));
  }

  @Override
  public @Nullable Potion potion(PotionEffect effect) {
    var potion = handle().getPotionEffect(PlatformAdapter.toBukkitPotion(effect));
    return potion == null ? null : PlatformAdapter.fromBukkitPotion(potion);
  }

  @Override
  public void removePotion(PotionEffect effect) {
    handle().removePotionEffect(PlatformAdapter.toBukkitPotion(effect));
  }

  @Override
  public Collection<Potion> activePotions() {
    return handle().getActivePotionEffects().stream().map(PlatformAdapter::fromBukkitPotion).toList();
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
    var loc = new Location(w, origin.x(), origin.y(), origin.z());
    var arrow = w.spawnArrow(loc, new Vector(direction.x(), direction.y(), direction.z()), (float) power, 0);
    arrow.setShooter(handle());
    arrow.setGravity(false);
    arrow.setInvulnerable(true);
    arrow.setPickupStatus(PickupStatus.DISALLOWED);
    return PlatformAdapter.fromBukkitEntity(arrow);
  }
}
