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

package me.moros.bending.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import me.moros.bending.common.storage.file.loader.Loader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.reference.ConfigurationReference;

final class FileStorage extends AbstractStorage {
  private static final String SUFFIX = ".json";

  private final Path dataPath;
  private final Loader<?> loader;

  FileStorage(Logger logger, Path directory, Loader<?> loader) {
    super(logger);
    this.dataPath = directory;
    this.loader = loader;
    try {
      Files.createDirectories(dataPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ConfigurationReference<? extends ConfigurationNode> load(UUID uuid) throws ConfigurateException {
    return loader.load(dataPath.resolve(uuid + SUFFIX));
  }

  @Override
  public void init() {
  }

  @Override
  public void close() {
  }

  @Override
  protected int createNewProfileId(UUID uuid) {
    return uuid.hashCode();
  }

  @Override
  protected @Nullable PlayerBenderProfile loadProfile(UUID uuid) {
    try (var ref = load(uuid)) {
      return ref.node().get(PlayerBenderProfile.class);
    } catch (ConfigurateException e) {
      logger.warn(e.getMessage(), e);
      return null;
    }
  }

  @Override
  protected void saveProfile(PlayerBenderProfile profile) {
    edit(profile.uuid(), ref -> ref.set(NodePath.path(), profile));
  }

  @Override
  protected int savePreset(Identifiable user, Preset preset) {
    edit(user.uuid(), ref -> ref.set(NodePath.path("presets", preset.name()), preset));
    return preset.name().hashCode() & 0x7FFFFFFF;
  }

  @Override
  protected void deletePreset(Identifiable user, Preset preset) {
    edit(user.uuid(), ref -> ref.get("presets", preset.name()).raw(null));
  }

  private void edit(UUID uuid, ThrowableConsumer<ConfigurationReference<? extends ConfigurationNode>> consumer) {
    try (var ref = load(uuid)) {
      consumer.accept(ref);
      ref.save();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface ThrowableConsumer<T> {
    void accept(T t) throws Throwable;
  }
}
