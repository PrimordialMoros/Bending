/*
 * Copyright 2020-2026 Moros
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.common.Bending;
import net.kyori.adventure.audience.Audience;

public sealed interface Operation permits AbstractOperation {
  CompletableFuture<Void> execute(AtomicBoolean lock);

  static Operation ofExport(Bending plugin, BendingStorage storage, Audience audience, String name) {
    return new ExportOperation(plugin, storage, audience, name);
  }

  static Operation ofImport(Bending plugin, BendingStorage storage, Audience audience, String name) {
    return new ImportOperation(plugin, storage, audience, name);
  }
}
