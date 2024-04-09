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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.Bending;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.storage.file.loader.JsonLoader;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

final class ImportOperation extends AbstractOperation {
  private final Audience audience;
  private final Path path;

  ImportOperation(Bending plugin, BendingStorage storage, Audience audience, String name) {
    super(plugin, storage);
    this.audience = audience;
    this.path = plugin.path().resolve(name.endsWith(SUFFIX) ? name : (name + SUFFIX));
  }

  @Override
  protected boolean executeOperation() {
    if (!plugin.path().equals(path.getParent()) || !Files.isReadable(path)) {
      logToAudience(Component.text("Can not read file " + path, ColorPalette.FAIL));
      return false;
    }
    logToAudience(Component.text("Reading data to import...", ColorPalette.NEUTRAL));
    BenderProfile[] profiles = loadFromFile(path);
    if (profiles.length == 0) {
      return false;
    }
    logToAudience(Component.text("Found %d users to import.".formatted(profiles.length), ColorPalette.NEUTRAL));
    return saveUsers(profiles);
  }

  @Override
  protected void onSuccess(double seconds) {
    Message.IMPORT_SUCCESS.send(audience, seconds);
  }

  @Override
  protected void onFailure(@Nullable Throwable throwable) {
    logToAudience(Component.text("An error occurred while importing data.", ColorPalette.FAIL));
    if (throwable != null) {
      plugin.logger().warn(throwable.getMessage(), throwable);
    }
  }

  private BenderProfile[] loadFromFile(Path path) {
    try (var fis = Files.newInputStream(path);
         var gis = new GZIPInputStream(fis);
         var isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
         var reader = new BufferedReader(isr)
    ) {
      var loader = new JsonLoader().withSerializers().lenient(false).indent(0).source(() -> reader).build();
      return loader.load().get(PROFILES_TOKEN, Map.of()).getOrDefault("users", new BenderProfile[0]);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean saveUsers(BenderProfile[] profiles) {
    LongAdder progress = new LongAdder();
    var checker = createProgressCheckingTask(progress, profiles.length);
    return storage.saveProfilesAsync(Arrays.asList(profiles), progress)
      .whenComplete((result, t) -> {
        checker.cancel();
        if (Boolean.TRUE.equals(result)) {
          updateOnline(profiles);
        }
      }).join();
  }

  private void updateOnline(BenderProfile[] profiles) {
    var map = Arrays.stream(profiles).collect(Collectors.toMap(BenderProfile::uuid, Function.identity()));
    var online = Registries.BENDERS.stream().toList();
    for (var user : online) {
      BenderProfile updatedProfile = map.get(user.uuid());
      if (updatedProfile != null) {
        user.fromProfile(updatedProfile);
      }
    }
  }

  @Override
  protected void logProgress(int current, int total) {
    int percent = current * 100 / total;
    Message.IMPORT_PROGRESS.send(audience, percent);
  }

  private void logToAudience(Component msg) {
    audience.sendMessage(Message.brand(msg));
  }
}
