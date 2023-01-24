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
import java.util.function.Supplier;

import me.moros.bending.event.BendingDamageEvent;
import me.moros.bending.event.EventBus;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.functional.Suppliers;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.AbilityDamageSource;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.item.FabricInventory;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.property.BooleanProperty;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.entity.projectile.Arrow;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FabricLivingEntity extends FabricEntity implements LivingEntity {
  private final Supplier<Map<BooleanProperty, Boolean>> properties;

  public FabricLivingEntity(net.minecraft.world.entity.LivingEntity handle) {
    super(handle);
    this.properties = Suppliers.lazy(IdentityHashMap::new);
  }

  @Override
  public net.minecraft.world.entity.LivingEntity handle() {
    return (net.minecraft.world.entity.LivingEntity) super.handle();
  }

  @Override
  public double health() {
    return handle().getHealth();
  }

  @Override
  public double maxHealth() {
    return handle().getMaxHealth();
  }

  @Override
  public boolean damage(double damage) {
    return handle().hurt(DamageSource.GENERIC, (float) damage);
  }

  @Override
  public boolean damage(double damage, Entity source) {
    DamageSource damageSource;
    if (source instanceof FabricPlayer fabricPlayer) {
      damageSource = DamageSource.playerAttack(fabricPlayer.handle());
    } else if (source instanceof FabricLivingEntity fabricLiving) {
      damageSource = DamageSource.mobAttack(fabricLiving.handle());
    } else {
      damageSource = DamageSource.indirectMobAttack(((FabricEntity) source).handle(), null);
    }
    return handle().hurt(damageSource, (float) damage);
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    BendingDamageEvent event = EventBus.INSTANCE.postAbilityDamageEvent(source, desc, this, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    return handle().hurt(AbilityDamageSource.create(source, desc), (float) dmg);
  }

  @Override
  public boolean ai() {
    return handle() instanceof Mob mob && !mob.isNoAi();
  }

  @Override
  public void ai(boolean value) {
    if (handle() instanceof Mob mob) {
      mob.setNoAi(!value);
    }
  }

  @Override
  public double eyeHeight() {
    return handle().getEyeHeight();
  }

  @Override
  public @Nullable Inventory inventory() {
    return new FabricInventory(handle()); // All living entities can equip armor but is only visible for few
  }

  @Override
  public boolean addPotion(Potion potion) {
    return handle().addEffect(PlatformAdapter.toFabricPotion(potion));
  }

  @Override
  public boolean hasPotion(PotionEffect effect) {
    return handle().hasEffect(PlatformAdapter.toFabricPotion(effect));
  }

  @Override
  public @Nullable Potion potion(PotionEffect effect) {
    var potion = handle().getEffect(PlatformAdapter.toFabricPotion(effect));
    return potion == null ? null : PlatformAdapter.fromFabricPotion(potion);
  }

  @Override
  public void removePotion(PotionEffect effect) {
    handle().removeEffect(PlatformAdapter.toFabricPotion(effect));
  }

  @Override
  public Collection<Potion> activePotions() {
    return handle().getActiveEffects().stream().map(PlatformAdapter::fromFabricPotion).toList();
  }

  @Override
  public int airCapacity() {
    return handle().getMaxAirSupply();
  }

  @Override
  public int remainingAir() {
    return handle().getAirSupply();
  }

  @Override
  public void remainingAir(int amount) {
    handle().setAirSupply(amount);
  }

  @Override
  public Entity shootArrow(Position origin, Vector3d direction, double power) {
    var w = handle().getLevel();
    var arrow = new Arrow(w, origin.x(), origin.y(), origin.z());
    arrow.shoot(direction.x(), direction.y(), direction.z(), (float) power, 0);
    arrow.setOwner(handle());
    arrow.setNoGravity(true);
    arrow.setInvulnerable(true);
    arrow.pickup = Pickup.DISALLOWED;
    w.addFreshEntity(arrow);
    return PlatformAdapter.fromFabricEntity(arrow);
  }

  @Override
  public TriState checkProperty(BooleanProperty property) {
    var vanillaProperty = PropertyMapper.PROPERTIES.get(property);
    if (vanillaProperty == null) {
      return TriState.byBoolean(properties.get().get(property));
    }
    return vanillaProperty.get(handle());
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    var vanillaProperty = PropertyMapper.PROPERTIES.get(property);
    if (vanillaProperty != null) {
      vanillaProperty.set(handle(), value);
    } else {
      properties.get().put(property, value);
    }
  }
}