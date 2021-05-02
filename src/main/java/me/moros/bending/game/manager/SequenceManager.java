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

package me.moros.bending.game.manager;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.game.AbilityRegistry;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SequenceManager {
  private final Map<AbilityDescription, Sequence> registeredSequences = new HashMap<>();
  private final Cache<User, Deque<AbilityAction>> cache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofSeconds(10))
    .build();
  private final Game game;

  public SequenceManager(@NonNull Game game) {
    this.game = game;
  }

  public void clear() {
    cache.invalidateAll();
  }

  /**
   * Register ability sequences. This must be called after all Abilities have been registered in {@link AbilityRegistry}
   * Note: Some sequences may fail to register if they require a disabled or invalid ability.
   * @param sequences the map containing all the sequences
   * @return the amount of sequences that were registered.
   */
  public int registerSequences(@NonNull Map<@NonNull AbilityDescription, @NonNull Sequence> sequences) {
    int i = 0;
    for (Map.Entry<AbilityDescription, Sequence> entry : sequences.entrySet()) {
      AbilityDescription desc = entry.getKey();
      if (!game.abilityRegistry().registered(desc)) {
        continue;
      }
      Sequence sequence = entry.getValue();
      boolean valid = sequence.actions().stream()
        .map(AbilityAction::abilityDescription)
        .allMatch(game.abilityRegistry()::registered);
      if (valid) {
        registeredSequences.put(entry.getKey(), sequence);
        i++;
      } else {
        Bending.logger().warn(desc.name() + " sequence will be disabled as it requires an invalid ability to activate.");
      }
    }
    return i;
  }

  public @Nullable Sequence sequence(@NonNull AbilityDescription desc) {
    return registeredSequences.get(desc);
  }

  public void registerAction(@NonNull User user, @NonNull ActivationMethod action) {
    AbilityDescription desc = user.selectedAbility().orElse(null);
    if (desc == null) {
      return;
    }
    Deque<AbilityAction> buffer = cache.get(user, u -> new ArrayDeque<>(16));
    if (buffer.size() >= 16) {
      buffer.removeFirst();
    }
    buffer.addLast(new AbilityAction(desc, action));
    for (Map.Entry<AbilityDescription, Sequence> entry : registeredSequences.entrySet()) {
      AbilityDescription sequenceDesc = entry.getKey();
      Sequence sequence = entry.getValue();
      if (sequence.matches(buffer.toArray(new AbilityAction[0]))) {
        if (!user.canBend(sequenceDesc)) {
          continue;
        }
        Ability ability = sequenceDesc.createAbility();
        if (ability.activate(user, ActivationMethod.SEQUENCE)) {
          game.abilityManager(user.world()).addAbility(user, ability);
        }
        buffer.clear(); // Consume all actions in the buffer
        return;
      }
    }
  }

  /**
   * Note: this will include hidden abilities. You will need to filter them.
   * @return a stream of all the registered sequences
   */
  public @NonNull Stream<AbilityDescription> sequences() {
    return registeredSequences.keySet().stream();
  }
}
