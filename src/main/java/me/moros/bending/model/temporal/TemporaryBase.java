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

package me.moros.bending.model.temporal;

public abstract class TemporaryBase implements Temporary {
  static TemporaryBase EMPTY = new TemporaryBase() {
    @Override
    public boolean revert() {
      return false;
    }
  };

  private TemporaryBase prev;
  private TemporaryBase next;
  private int expirationTick;

  protected TemporaryBase() {
    prev = next = this;
  }

  int expirationTick() {
    return expirationTick;
  }

  void expirationTick(int expirationTick) {
    this.expirationTick = expirationTick;
  }

  TemporaryBase previous() {
    return prev;
  }

  void previous(TemporaryBase prev) {
    this.prev = prev;
  }

  TemporaryBase next() {
    return next;
  }

  void next(TemporaryBase next) {
    this.next = next;
  }

  static void link(TemporaryBase sentinel, TemporaryBase node) {
    node.previous(sentinel.previous());
    node.next(sentinel);
    sentinel.previous().next(node);
    sentinel.previous(node);
  }

  static void unlink(TemporaryBase node) {
    TemporaryBase next = node.next();
    if (next != null) {
      TemporaryBase prev = node.previous();
      next.previous(prev);
      prev.next(next);
    }
  }
}
