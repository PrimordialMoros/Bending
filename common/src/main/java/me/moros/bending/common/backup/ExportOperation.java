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

package me.moros.bending.common.backup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.GZIPOutputStream;

import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.Bending;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.storage.file.loader.JsonLoader;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

final class ExportOperation extends AbstractOperation {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
    .withZone(ZoneId.systemDefault());

  private final Audience audience;
  private final Path path;

  ExportOperation(Bending plugin, BendingStorage storage, Audience audience, String name) {
    super(plugin, storage);
    this.audience = audience;
    this.path = plugin.path().resolve(fileName(name));
  }

  private String fileName(String name) {
    return (name.isEmpty() ? "bending-" + DATE_FORMAT.format(Instant.now()) : name) + SUFFIX;
  }

  @Override
  protected boolean executeOperation() {
    if (Files.exists(path)) {
      logToAudience(Component.text("File already exists!", ColorPalette.FAIL));
      return false;
    }
    logToAudience(Component.text("Discovering users to export...", ColorPalette.NEUTRAL));
    BenderProfile[] profiles = loadUsers();
    if (profiles.length == 0) {
      return false;
    }
    // Sort to ensure reproducible order and checksum
    Arrays.parallelSort(profiles, Comparator.comparing(BenderProfile::uuid));
    saveToFile(profiles);
    return true;
  }

  @Override
  protected void onSuccess(double seconds) {
    Message.EXPORT_SUCCESS.send(audience, path.toString(), seconds);
  }

  @Override
  protected void onFailure(@Nullable Throwable throwable) {
    logToAudience(Component.text("An error occurred while exporting data.", ColorPalette.FAIL));
    if (throwable != null) {
      plugin.logger().warn(throwable.getMessage(), throwable);
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignore) {
    }
  }

  private void saveToFile(BenderProfile[] data) {
    try (var fos = Files.newOutputStream(path);
         var gos = new GZIPOutputStream(fos);
         var osw = new OutputStreamWriter(gos, StandardCharsets.UTF_8);
         var writer = new BufferedWriter(osw)
    ) {
      var loader = new JsonLoader().withSerializers().lenient(false).indent(0).sink(() -> writer).build();
      loader.save(loader.createNode(n -> n.set(PROFILES_TOKEN, Map.of("users", data))));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BenderProfile[] loadUsers() {
    final Set<UUID> uuids = storage.loadUuids();
    final int size = uuids.size();
    logToAudience(Component.text("Found %d users to export.".formatted(size), ColorPalette.NEUTRAL));

    LongAdder progress = new LongAdder();
    var checker = createProgressCheckingTask(progress, size);
    return storage.loadProfilesAsync(uuids, progress)
      .handle((result, t) -> {
        checker.cancel();
        return result == null ? new BenderProfile[0] : result.values().toArray(BenderProfile[]::new);
      }).join();
  }

  @Override
  protected void logProgress(int current, int total) {
    int percent = current * 100 / total;
    Message.EXPORT_PROGRESS.send(audience, percent);
  }

  private void logToAudience(Component msg) {
    audience.sendMessage(Message.brand(msg));
  }
}
