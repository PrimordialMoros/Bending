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

package me.moros.bending.common.backup;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import io.leangen.geantyref.TypeToken;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.Bending;
import me.moros.tasker.Task;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract sealed class AbstractOperation implements Operation permits ExportOperation, ImportOperation {
  protected static final String SUFFIX = ".json.gz";

  protected static final TypeToken<Map<String, BenderProfile[]>> PROFILES_TOKEN = new TypeToken<>() {
  };

  protected final Bending plugin;
  protected final BendingStorage storage;

  protected AbstractOperation(Bending plugin, BendingStorage storage) {
    this.plugin = plugin;
    this.storage = storage;
  }

  @Override
  public final CompletableFuture<Void> execute(AtomicBoolean lock) {
    final long startTime = System.currentTimeMillis();
    return Tasker.async().submit(this::executeOperation).handle((result, t) -> {
      if (Boolean.TRUE.equals(result) && t == null) {
        onSuccess((System.currentTimeMillis() - startTime) / 1000.0);
      } else {
        onFailure(t);
      }
      lock.set(false);
      return null;
    });
  }

  protected abstract boolean executeOperation();

  protected abstract void onSuccess(double seconds);

  protected abstract void onFailure(@Nullable Throwable throwable);

  protected abstract void logProgress(int current, int total);

  protected final Task createProgressCheckingTask(LongAdder progress, int size) {
    return Tasker.async().repeat(() -> logProgress(progress.intValue(), size), 5, TimeUnit.SECONDS);
  }
}
