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

package me.moros.bending.fabric.platform.entity;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.fabric.platform.PlatformAdapter;
import me.moros.bending.fabric.platform.item.FabricInventory;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
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
    return handle().hurt(handle().damageSources().generic(), (float) damage);
  }

  @Override
  public boolean damage(double damage, Entity source) {
    DamageSource damageSource;
    var entity = PlatformAdapter.toFabricEntity(source);
    var sources = handle().damageSources();
    if (entity instanceof ServerPlayer player) {
      damageSource = sources.playerAttack(player);
    } else if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
      damageSource = sources.mobAttack(living);
    } else {
      damageSource = sources.mobProjectile(entity, null);
    }
    return handle().hurt(damageSource, (float) damage);
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    BendingDamageEvent event = source.game().eventBus().postAbilityDamageEvent(source, desc, this, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    return Platform.instance().nativeAdapter().damage(event);
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
    var w = handle().level();
    var arrow = new Arrow(w, origin.x(), origin.y(), origin.z(), ItemStack.EMPTY);
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
