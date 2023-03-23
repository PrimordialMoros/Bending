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

package me.moros.bending.sponge.platform.entity;

import java.util.Collection;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.user.User;
import me.moros.bending.sponge.platform.AbilityDamageSource;
import me.moros.bending.sponge.platform.PlatformAdapter;
import me.moros.bending.sponge.platform.item.SpongeInventory;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.PickupRules;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
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
    var ds = DamageSource.builder().type(DamageTypes.GENERIC).entity(PlatformAdapter.toSpongeEntity(source)).build();
    return handle().damage(damage, ds);
  }

  @Override
  public boolean damage(double damage, User source, AbilityDescription desc) {
    BendingDamageEvent event = source.game().eventBus().postAbilityDamageEvent(source, desc, this, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    var value = DamageTypes.registry().value(AbilityDamageSource.BENDING_DAMAGE);
    var spongeDamageSource = DamageSource.builder().type(value).entity(PlatformAdapter.toSpongeEntity(source)).build();
    return handle().damage(dmg, (DamageSource) AbilityDamageSource.wrap(spongeDamageSource, source, desc));
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
    handle().offerSingle(Keys.POTION_EFFECTS, PlatformAdapter.toSpongePotion(potion));
    return true;
  }

  @Override
  public boolean hasPotion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.toSpongePotion(effect);
    for (var p : handle().potionEffects()) {
      if (p.type().equals(spongeEffect)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable Potion potion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.toSpongePotion(effect);
    for (var p : handle().potionEffects()) {
      if (p.type().equals(spongeEffect)) {
        return PlatformAdapter.fromSpongePotion(p);
      }
    }
    return null;
  }

  @Override
  public void removePotion(PotionEffect effect) {
    var spongeEffect = PlatformAdapter.toSpongePotion(effect);
    handle().transform(Keys.POTION_EFFECTS, effects -> {
      effects.removeIf(p -> p.type().equals(spongeEffect));
      return effects;
    });
  }

  @Override
  public Collection<Potion> activePotions() {
    return handle().potionEffects().get().stream().map(PlatformAdapter::fromSpongePotion).toList();
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
    handle().offer(Keys.REMAINING_AIR, amount);
  }

  @Override
  public Entity shootArrow(Position origin, Vector3d direction, double power) {
    var w = handle().world();
    var vec = org.spongepowered.math.vector.Vector3d.from(origin.x(), origin.y(), origin.z());
    var arrow = w.createEntity(EntityTypes.ARROW, vec);
    ((net.minecraft.world.entity.projectile.Arrow) arrow).shoot(direction.x(), direction.y(), direction.z(), (float) power, 0);
    arrow.offer(Keys.SHOOTER, handle());
    arrow.offer(Keys.IS_GRAVITY_AFFECTED, false);
    arrow.offer(Keys.INVULNERABLE, true);
    arrow.offer(Keys.PICKUP_RULE, PickupRules.DISALLOWED.get());
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
