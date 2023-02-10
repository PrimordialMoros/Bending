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

package me.moros.bending.common.storage.file.serializer;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.leangen.geantyref.TypeToken;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

final class PlayerBenderProfileSerializer extends AbstractSerializer<PlayerBenderProfile> {
  static final PlayerBenderProfileSerializer INSTANCE = new PlayerBenderProfileSerializer();

  private static final String UUID = "uuid";
  private static final String BOARD = "board";
  private static final String SLOTS = "slots";
  private static final String ELEMENTS = "elements";
  private static final String PRESETS = "presets";

  private static final TypeToken<Map<Integer, String>> ABILITY_MAP_TOKEN = new TypeToken<>() {
  };
  private static final TypeToken<Set<Element>> ELEMENT_SET_TOKEN = new TypeToken<>() {
  };
  private static final TypeToken<Map<String, Preset>> PRESET_MAP_TOKEN = new TypeToken<>() {
  };

  private PlayerBenderProfileSerializer() {
  }

  @Override
  public PlayerBenderProfile deserialize(Type type, ConfigurationNode source) throws SerializationException {
    ConfigurationNode uuidNode = nonVirtualNode(source, UUID);
    UUID uuid = uuidNode.get(UUID.class);
    if (uuid == null) {
      throw new SerializationException(uuidNode, UUID.class, "Invalid uuid!");
    }
    boolean board = getSafe(nonVirtualNodeOrNull(source, BOARD), n -> n.getBoolean(true)).orElse(true);
    var slotsMap = getSafe(nonVirtualNodeOrNull(source, SLOTS), n -> n.get(ABILITY_MAP_TOKEN)).orElseGet(Map::of);
    var slots = mapToList(slotsMap, Registries.ABILITIES::fromString, 9);
    var elements = getSafe(nonVirtualNodeOrNull(source, ELEMENTS), n -> n.get(ELEMENT_SET_TOKEN)).orElseGet(Set::of);
    var presetMap = getSafe(nonVirtualNodeOrNull(source, PRESETS), n -> n.get(PRESET_MAP_TOKEN)).orElseGet(Map::of);
    var data = BenderProfile.of(slots, elements, new HashSet<>(presetMap.values()));
    return BenderProfile.of(uuid.hashCode(), uuid, board, data);
  }

  @Override
  public void serialize(Type type, @Nullable PlayerBenderProfile profile, ConfigurationNode target) throws SerializationException {
    if (profile == null) {
      target.raw(null);
      return;
    }
    target.node(UUID).set(profile.uuid());
    target.node(BOARD).set(profile.board());
    target.node(SLOTS).set(ABILITY_MAP_TOKEN, listToMap(profile.slots(), AbilityDescription::name, 9));
    target.node(ELEMENTS).set(ELEMENT_SET_TOKEN, profile.elements());
    target.node(PRESETS).set(PRESET_MAP_TOKEN, profile.presets().stream().collect(Collectors.toMap(Preset::name, Function.identity())));
  }

  private <T> Optional<T> getSafe(@Nullable ConfigurationNode node, ThrowableFunction<ConfigurationNode, T> mapper) {
    if (node != null) {
      try {
        return Optional.ofNullable(mapper.apply(node));
      } catch (Throwable ignore) {
      }
    }
    return Optional.empty();
  }

  @FunctionalInterface
  public interface ThrowableFunction<T, R> {
    R apply(T t) throws Throwable;
  }
}
