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
import java.util.Objects;

import me.moros.bending.event.BendingDamageEvent;
import me.moros.bending.event.EventBus;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.AbilityDamageSource;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.item.SpongeInventory;
import me.moros.bending.platform.potion.Potion;
import me.moros.bending.platform.potion.PotionEffect;
import me.moros.bending.platform.property.BooleanProperty;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.PickupRules;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.item.inventory.Equipable;

public class SpongeLivingEntity extends SpongeEntity implements LivingEntity {

  public SpongeLivingEntity(Living handle) {
    super(handle);
  }

  @Override
  public Living handle() {
    return (Living) super.handle();
  }

  @Override
  protected net.minecraft.world.entity.LivingEntity nmsEntity() {
    return (net.minecraft.world.entity.LivingEntity) super.nmsEntity();
  }

  @Override
  public double health() {
    return handle().health().get();
  }

  @Override
  public double maxHealth() {
    return handle().maxHealth().get();
  }

  @Override
  public boolean damage(double damage) {
    return handle().damage(damage, DamageSources.GENERIC);
  }

  @Override
  public boolean damage(double damage, Entity source) {
    var ds = EntityDamageSource.builder().type(DamageTypes.CUSTOM).entity(((SpongeEntity) source).handle()).build();
    return handle().damage(damage, ds);
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    BendingDamageEvent event = EventBus.INSTANCE.postAbilityDamageEvent(source, desc, this, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    return handle().damage(dmg, AbilityDamageSource.builder(source, desc).build());
  }

  @Override
  public boolean ai() {
    return handle().get(Keys.IS_AI_ENABLED).orElse(false);
  }

  @Override
  public void ai(boolean value) {
    handle().offer(Keys.IS_AI_ENABLED, value);
  }

  @Override
  public double eyeHeight() {
    return handle().eyeHeight().get();
  }

  @Override
  public @Nullable Inventory inventory() {
    if (handle() instanceof Equipable holder) {
      return new SpongeInventory(holder.equipment());
    }
    return null;
  }

  @Override
  public boolean addPotion(Potion potion) {
    var spongePotion = PlatformAdapter.toSpongePotion(potion);
    if (spongePotion != null) {
      handle().potionEffects().add(spongePotion);
      return true;
    }
    return false;
  }

  @Override
  public boolean hasPotion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    if (spongeEffect != null) {
      for (var p : handle().potionEffects()) {
        if (p.type().equals(spongeEffect)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public @Nullable Potion potion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    if (spongeEffect != null) {
      for (var p : handle().potionEffects()) {
        if (p.type().equals(spongeEffect)) {
          return PlatformAdapter.fromSpongePotion(p);
        }
      }
    }
    return null;
  }

  @Override
  public void removePotion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.POTION_EFFECT_INDEX.key(effect);
    handle().potionEffects().transform(l -> {
      l.removeIf(p -> p.type().equals(spongeEffect));
      return l;
    });
  }

  @Override
  public Collection<Potion> activePotions() {
    return handle().potionEffects().get().stream().map(PlatformAdapter::fromSpongePotion).filter(Objects::nonNull).toList();
  }

  @Override
  public boolean hasLineOfSight(Entity other) {
    return other instanceof SpongeEntity ent && nmsEntity().hasLineOfSight(ent.nmsEntity());
  }

  @Override
  public int airCapacity() {
    return handle().maxAir().get();
  }

  @Override
  public int remainingAir() {
    return handle().remainingAir().get();
  }

  @Override
  public void remainingAir(int amount) {
    handle().remainingAir().set(amount);
  }

  @Override
  public Entity shootArrow(Position origin, Vector3d direction, double power) {
    var w = handle().world();
    var arrow = w.createEntity(EntityTypes.ARROW, origin.to(org.spongepowered.math.vector.Vector3d.class));
    ((net.minecraft.world.entity.projectile.Arrow) arrow).shoot(direction.x(), direction.y(), direction.z(), (float) power, 0);
    arrow.offer(Keys.SHOOTER, handle());
    arrow.gravityAffected().set(false);
    arrow.invulnerable().set(true);
    arrow.pickupRule().set(PickupRules.DISALLOWED.get());
    w.spawnEntity(arrow);
    return PlatformAdapter.fromSpongeEntity(arrow);
  }


  @Override
  public TriState checkProperty(BooleanProperty property) {
    var spongeProperty = PropertyMapper.PROPERTIES.get(property);
    if (spongeProperty == null) {
      return TriState.NOT_SET;
    }
    return TriState.byBoolean(handle().getOrNull(spongeProperty));
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    var spongeProperty = PropertyMapper.PROPERTIES.get(property);
    if (spongeProperty != null) {
      handle().offer(spongeProperty, value);
    }
  }
}
