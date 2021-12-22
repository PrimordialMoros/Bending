/*
 * Copyright 2020-2021 Moros
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

final class TimerWheel<K, V extends Temporary> {
  private static final int[] BUCKETS = {40, 30, 15, 4, 4}; // 2s, 1m, 15m, 1h, 4h
  private static final int[] SPANS = {40, 1200, 18000, 72000, 288000, 288000};

  private final TemporalManager<K, V> manager;
  private final Node<K, V>[][] wheel;
  private final int[] index = {0, 0, 0, 0, 0};

  @SuppressWarnings("unchecked")
  TimerWheel(TemporalManager<K, V> manager) {
    this.manager = manager;
    wheel = new Node[BUCKETS.length][];
    for (int i = 0; i < wheel.length; i++) {
      wheel[i] = new Node[BUCKETS[i]];
      for (int j = 0; j < wheel[i].length; j++) {
        wheel[i][j] = new Node<>();
      }
    }
  }

  void advance(int currentTick) {
    for (int i = 0; i < wheel.length; i++) {
      boolean end = !increment(i);
      expire(wheel[i][index[i]], currentTick);
      if (end) {
        return;
      }
    }
  }

  private void expire(Node<K, V> sentinel, int currentTick) {
    Node<K, V> prev = sentinel.previous();
    Node<K, V> node = sentinel.next();
    sentinel.previous(sentinel);
    sentinel.next(sentinel);
    while (node != sentinel) {
      Node<K, V> next = node.next();
      node.previous(null);
      node.next(null);
      try {
        if (node.expirationTick() <= currentTick && node.value().revert()) {
          manager.removeEntry(node.key());
        } else {
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

  void schedule(Node<K, V> node, int currentTick) {
    Node<K, V> sentinel = findBucket(node.expirationTick() - currentTick);
    Node.link(sentinel, node);
  }

  void reschedule(Node<K, V> node, int currentTick) {
    if (node.next() != null) {
      Node.unlink(node);
      schedule(node, currentTick);
    }
  }

  void deschedule(Node<K, V> node) {
    Node.unlink(node);
    node.next(null);
    node.previous(null);
  }

  private Node<K, V> findBucket(int ticks) {
    for (int i = 0; i < wheel.length; i++) {
      int tickCapacity = SPANS[i + 1];
      if (ticks <= tickCapacity) {
        return add(i, ticks);
      }
    }
    return add(wheel.length - 1, Math.min(ticks, SPANS[SPANS.length - 1]));
  }

  private Node<K, V> add(int idx, int ticks) {
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
