/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.user;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.protection.ProtectionCache;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.GridIterator;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataContainer;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.jspecify.annotations.Nullable;

/**
 * Represents a user that can bend.
 */
public sealed interface User extends LivingEntity, ElementUser, AttributeUser, PresetUser permits BendingUser {
  /**
   * Get the game object that this user belongs to.
   * @return the game
   */
  Game game();

  /**
   * Get the data store for this user.
   * @return the data store object
   */
  DataContainer store();

  /**
   * Check if this user is in spectator mode.
   * @return whether this user is a player in spectator mode
   */
  default boolean isSpectator() {
    return false;
  }

  /**
   * Check if the user has the specified ability on cooldown.
   * @param desc the ability to check
   * @return true if the ability is on cooldown for this user, false otherwise
   */
  boolean onCooldown(AbilityDescription desc);

  /**
   * Attempts to put the specified ability on cooldown for the given duration.
   * @param desc the ability to put on cooldown
   * @param duration the duration of the cooldown
   * @return true if cooldown was added successfully, false otherwise
   */
  boolean addCooldown(AbilityDescription desc, long duration);

  /**
   * Create a snapshot of this user's current slots as a preset.
   * @return the preset
   */
  Preset slots();

  /**
   * Bind a preset to slots.
   * @param preset the preset of abilities to bind
   * @return true is at least 1 ability from the preset was bound, false otherwise
   */
  boolean bindPreset(Preset preset);

  /**
   * Assigns an ability to the specified slot.
   * @param slot the slot number in the range [1, 9] (inclusive)
   * @param desc the ability to bind
   */
  void bindAbility(int slot, @Nullable AbilityDescription desc);

  /**
   * Retrieve the ability assigned to the specified slot.
   * @param slot the slot number to check, slot must be in range [1, 9] (inclusive)
   * @return the ability bound to given slot if found, null otherwise
   */
  @Nullable AbilityDescription boundAbility(int slot);

  /**
   * Get the currently selected slot.
   * @return a slot index in the 1-9 range (inclusive)
   */
  int currentSlot();

  /**
   * Changes the currently selected slot.
   * <p>Note: This has no effect on players.
   * @param slot the slot number in the range [1, 9] (inclusive)
   */
  void currentSlot(int slot);

  /**
   * Get the currently selected ability for the user.
   * @return the ability in the currently selected slot for the user if found, null otherwise
   */
  default @Nullable AbilityDescription selectedAbility() {
    return boundAbility(currentSlot());
  }

  /**
   * Utility method to easily check if the currently selected ability matches the given key.
   * If key doesn't have a namespace, the default one will be used ({@value KeyUtil#BENDING_NAMESPACE}).
   * If there's no ability bound in the selected slot then false will be returned.
   * @return true if the currently selected ability matches the specified key
   */
  default boolean hasAbilitySelected(String key) {
    AbilityDescription selected = selectedAbility();
    return selected != null && selected.key().equals(KeyUtil.BENDING_KEY_MAPPER.apply(key.toLowerCase(Locale.ROOT)));
  }

  /**
   * Clears the specified slot.
   * @param slot the slot number to clear, slot must be in range [1, 9] (inclusive)
   */
  default void clearSlot(int slot) {
    bindAbility(slot, null);
  }

  /**
   * Check whether this user can bend the specified ability.
   * @param desc the ability to check
   * @return true if the user can bend the given ability, false otherwise
   */
  boolean canBend(AbilityDescription desc);

  /**
   * Check whether this user can bend.
   * @return true if the user can bend, false otherwise
   */
  boolean canBend();

  /**
   * Toggle this user's bending.
   * @return true if the user can bend after the toggle, false otherwise
   */
  boolean toggleBending();

  /**
   * Gets the board for this user.
   * @return the board instance
   */
  Board board();

  /**
   * Check if the user has all required permissions for the specified ability.
   * @param desc the ability to check
   * @return true if the user has all permissions for the ability, false otherwise
   * @see #hasPermission(String)
   */
  default boolean hasPermission(AbilityDescription desc) {
    return desc.permissions().stream().allMatch(this::hasPermission);
  }

  /**
   * Check if the user has the specified permission.
   * If the user is a non-player, this will return true unless a virtual node is set.
   * @param permission the permission to check
   * @return true if the user has the given permission, false otherwise
   * @see #setPermission(String, TriState)
   */
  boolean hasPermission(String permission);

  /**
   * Set a virtual permission node (in memory) for a user.
   * <p>Note: This has no effect if the user is a player.
   * @param permission the permission node
   * @param state the permission state
   * @return the previous state of the permission node
   */
  TriState setPermission(String permission, TriState state);

  /**
   * Checks if the user can build at its current location.
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild() {
    return canBuild(world().blockAt(location()));
  }

  /**
   * Checks if the user can build at a location.
   * @param position the position to check in the user's current world
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild(Vector3d position) {
    return canBuild(world().blockAt(position));
  }

  /**
   * Checks if the user can build at a block location.
   * @param block the block to check
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild(Block block) {
    return ProtectionCache.INSTANCE.canBuild(this, block);
  }

  /**
   * Attempt to find a possible block source that matches the given predicate.
   * @param range the max range to check
   * @param predicate the predicate to check
   * @return the source block if one was found, null otherwise
   */
  default @Nullable Block find(double range, Predicate<Block> predicate) {
    GridIterator it = GridIterator.create(eyeLocation(), direction(), Math.clamp(FastMath.ceil(range), 1, 100));
    while (it.hasNext()) {
      Block block = world().blockAt(it.next());
      if (block.type().isAir()) {
        continue;
      }
      if (predicate.test(block) && TempBlock.isBendable(block) && canBuild(block)) {
        return block;
      }
      if (block.type().isCollidable()) {
        break;
      }
    }
    return null;
  }

  BenderProfile toProfile();

  boolean fromProfile(BenderProfile profile);

  static Optional<User> create(Game game, LivingEntity entity, BenderProfile profile) {
    return create(game, entity, CompletableFuture.completedFuture(profile));
  }

  static Optional<User> create(Game game, LivingEntity entity, CompletableFuture<BenderProfile> profileFuture) {
    Objects.requireNonNull(game);
    Objects.requireNonNull(profileFuture);
    if (!Registries.BENDERS.containsKey(entity.uuid())) {
      User user = entity instanceof Player player ? new BendingPlayer(game, player) : new BendingUser(game, entity);
      if (Registries.BENDERS.register(user)) {
        profileFuture.thenAccept(user::fromProfile);
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }
}
