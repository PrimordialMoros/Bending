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

final class TimerWheel {
  private static final int[] BUCKETS = {40, 30, 15, 4, 4}; // 2s, 1m, 15m, 1h, 4h
  private static final int[] SPANS = {40, 1200, 18000, 72000, 288000, 288000};

  private final TemporaryBase[][] wheel;
  private final int[] index = {0, 0, 0, 0, 0};
  private final int length;

  TimerWheel() {
    length = BUCKETS.length;
    wheel = new TemporaryBase[length][];
    for (int i = 0; i < length; i++) {
      int innerLength = BUCKETS[i];
      wheel[i] = new TemporaryBase[innerLength];
      for (int j = 0; j < innerLength; j++) {
        wheel[i][j] = TemporaryBase.EMPTY;
      }
    }
  }

  void advance(int currentTick) {
    for (int i = 0; i < length; i++) {
      boolean end = !increment(i);
      expire(wheel[i][index[i]], currentTick);
      if (end) {
        return;
      }
    }
  }

  private void expire(TemporaryBase sentinel, int currentTick) {
    TemporaryBase prev = sentinel.previous();
    TemporaryBase node = sentinel.next();
    sentinel.previous(sentinel);
    sentinel.next(sentinel);
    while (node != sentinel) {
      TemporaryBase next = node.next();
      node.previous(null);
      node.next(null);
      try {
        if (node.expirationTick() > currentTick || !node.revert()) {
          schedule(node, currentTick);
        }
        node = next;
      } catch (Throwable t) {
        node.previous(sentinel.previous());
        node.next(next);
        sentinel.previous().next(node);
        sentinel.previous(prev);
        return;
      }
    }
  }

  void schedule(TemporaryBase node, int currentTick) {
    TemporaryBase sentinel = findBucket(node.expirationTick() - currentTick);
    TemporaryBase.link(sentinel, node);
  }

  void reschedule(TemporaryBase node, int currentTick) {
    if (node.next() != null) {
      TemporaryBase.unlink(node);
      schedule(node, currentTick);
    }
  }

  void deschedule(TemporaryBase node) {
    TemporaryBase.unlink(node);
    node.next(null);
    node.previous(null);
  }

  private TemporaryBase findBucket(int ticks) {
    for (int i = 0; i < length; i++) {
      int tickCapacity = SPANS[i + 1];
      if (ticks <= tickCapacity) {
        return add(i, ticks);
      }
    }
    return add(wheel.length - 1, Math.min(ticks, SPANS[SPANS.length - 1]));
  }

  private TemporaryBase add(int idx, int ticks) {
    return wheel[idx][(index[idx] + ticks % SPANS[idx]) % BUCKETS[idx]];
  }

  private boolean increment(int idx) {
    if (++index[idx] > BUCKETS[idx] - 1) {
      index[idx] = 0;
      return true;
    }
    return false;
  }
}
