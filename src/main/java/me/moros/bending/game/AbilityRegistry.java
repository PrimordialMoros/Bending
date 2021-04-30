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

package me.moros.bending.game;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.ability.util.ActivationMethod;

/**
 * Holds all the registered AbilityDescriptions for the current session.
 */
public final class AbilityRegistry {
  private final Map<String, AbilityDescription> abilities = new HashMap<>();
  private final Map<Element, Set<AbilityDescription>> passives = new EnumMap<>(Element.class);

  protected int registerAbilities(@NonNull Collection<@NonNull AbilityDescription> abilities) {
    int counter = 0;
    for (AbilityDescription desc : abilities) {
      if (registerAbility(desc)) {
        counter++;
      }
    }
    return counter;
  }

  private boolean registerAbility(@NonNull AbilityDescription desc) {
    abilities.put(desc.getName().toLowerCase(), desc);
    if (desc.isActivatedBy(ActivationMethod.PASSIVE)) {
      passives.computeIfAbsent(desc.getElement(), e -> new HashSet<>()).add(desc);
    }
    return true;
  }

  /**
   * Check if an ability is enabled and registered.
   * @param desc the ability to check
   * @return result
   */
  public boolean isRegistered(@NonNull AbilityDescription desc) {
    return abilities.containsKey(desc.getName().toLowerCase());
  }

  /**
   * Note: this will include hidden abilities. You will need to filter them.
   * @return a stream of all the abilities in this registry
   */
  public Stream<AbilityDescription> getAbilities() {
    return abilities.values().stream();
  }

  /**
   * Note: this will include hidden passives. You will need to filter them.
   * @return a stream of all the passives in this registry
   */
  public Stream<AbilityDescription> getPassives(Element element) {
    return passives.getOrDefault(element, Collections.emptySet()).stream();
  }

  /**
   * @param name the name to match
   * @return Optional ability description
   */
  public Optional<AbilityDescription> getAbilityDescription(String name) {
    if (name == null || name.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(abilities.get(name.toLowerCase()));
  }

  public static class AddonRegistry {
    private static final Set<AbilityDescription> addonAbilities = new HashSet<>();
    private static final Map<AbilityDescription, Sequence> addonSequences = new ConcurrentHashMap<>();

    public static boolean registerAddonAbility(@NonNull AbilityDescription desc) {
      return addonAbilities.add(desc);
    }

    public static void registerAddonSequence(@NonNull AbilityDescription desc, @NonNull Sequence sequence) {
      addonSequences.put(desc, sequence);
    }

    public static @NonNull Collection<AbilityDescription> getAddonAbilities() {
      return List.copyOf(addonAbilities);
    }

    public static @NonNull Map<AbilityDescription, Sequence> getAddonSequences() {
      return Map.copyOf(addonSequences);
    }
  }
}
