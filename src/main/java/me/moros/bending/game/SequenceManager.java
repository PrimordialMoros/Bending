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

package me.moros.bending.game;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.ability.description.SequenceStep;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class SequenceManager {
  private final LoadingCache<UUID, Deque<SequenceStep>> cache;

  SequenceManager() {
    cache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofSeconds(10))
      .build(u -> new ArrayDeque<>(16));
  }

  public void clear() {
    cache.invalidateAll();
  }

  public void registerStep(@NonNull User user, @NonNull Activation action) {
    AbilityDescription desc = user.selectedAbility();
    if (desc == null) {
      return;
    }
    Deque<SequenceStep> buffer = cache.get(user.uuid());
    if (buffer.size() >= 16) {
      buffer.removeFirst();
    }
    buffer.addLast(new SequenceStep(desc, action));
    List<SequenceStep> bufferSteps = new ArrayList<>(buffer);
    for (Sequence sequence : Registries.SEQUENCES) {
      if (sequence.matches(bufferSteps)) {
        if (Bending.game().activationController().activateAbility(user, Activation.SEQUENCE, sequence) != null) {
          buffer.clear(); // Consume all actions in the buffer
          return;
        }
      }
    }
  }
}
