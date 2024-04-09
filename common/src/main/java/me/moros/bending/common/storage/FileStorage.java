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

package me.moros.bending.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.storage.file.IOFunction;
import me.moros.bending.common.storage.file.loader.Loader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.reference.ConfigurationReference;

final class FileStorage extends AbstractStorage {
  private static final String SUFFIX = ".json";

  private final Path dataPath;
  private final Loader<?> loader;
  private final LoadingCache<Path, ReentrantLock> locks;
  private final Semaphore semaphore;

  FileStorage(Logger logger, Path directory, Loader<?> loader) {
    super(logger);
    this.dataPath = directory;
    this.loader = loader;
    try {
      Files.createDirectories(dataPath);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    this.locks = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).build(key -> new ReentrantLock());
    this.semaphore = new Semaphore(Math.max(Runtime.getRuntime().availableProcessors(), 2));
  }

  private Path filePath(UUID uuid) {
    return dataPath.resolve(uuid + SUFFIX);
  }

  @Override
  public Set<UUID> loadUuids() {
    try (var stream = Files.list(dataPath)) {
      return stream.map(p -> p.getFileName().toString())
        .filter(name -> name.endsWith(SUFFIX))
        .map(name -> name.substring(0, name.length() - SUFFIX.length()))
        .map(TextUtil::parseUUID)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public @Nullable BenderProfile loadProfile(UUID uuid) {
    Path path = filePath(uuid);
    return loadDataFile(path, ref -> Files.exists(path) ? ref.node().get(BenderProfile.class) : null, null);
  }

  @Override
  public boolean saveProfile(BenderProfile profile) {
    return loadDataFile(filePath(profile.uuid()), ref -> {
      ref.set(NodePath.path(), profile);
      ref.save();
      return true;
    }, false);
  }

  @Override
  public boolean isRemote() {
    return false;
  }

  private <R> R loadDataFile(Path path, IOFunction<ConfigurationReference<?>, R> function, R onException) {
    ReentrantLock lock = locks.get(path);
    lock.lock();
    semaphore.acquireUninterruptibly();
    try (var ref = loader.load(path)) {
      return function.apply(ref);
    } catch (IOException e) {
      logger.warn(e.getMessage(), e);
      return onException;
    } finally {
      semaphore.release();
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return "Json";
  }

  @Override
  public void close() {
  }
}
