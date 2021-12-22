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

public final class Node<K, V> {
  private Node<K, V> prev;
  private Node<K, V> next;
  private final K key;
  private final V value;
  private int expirationTick;

  Node() {
    this(null, null);
  }

  Node(K key, V value) {
    this.key = key;
    this.value = value;
    prev = next = this;
  }

  K key() {
    return key;
  }

  V value() {
    return value;
  }

  int expirationTick() {
    return expirationTick;
  }

  void expirationTick(int expirationTick) {
    this.expirationTick = expirationTick;
  }

  Node<K, V> previous() {
    return prev;
  }

  void previous(Node<K, V> prev) {
    this.prev = prev;
  }

  Node<K, V> next() {
    return next;
  }

  void next(Node<K, V> next) {
    this.next = next;
  }

  static <K, V> void link(Node<K, V> sentinel, Node<K, V> node) {
    node.previous(sentinel.previous());
    node.next(sentinel);
    sentinel.previous().next(node);
    sentinel.previous(node);
  }

  static <K, V> void unlink(Node<K, V> node) {
    Node<K, V> next = node.next();
    if (next != null) {
      Node<K, V> prev = node.previous();
      next.previous(prev);
      prev.next(next);
    }
  }
}
