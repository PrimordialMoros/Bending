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

package me.moros.bending.common.collision;

import java.util.Arrays;
import java.util.Comparator;

import me.moros.bending.api.collision.geometry.AABB;
import org.checkerframework.checker.nullness.qual.Nullable;

//https://developer.nvidia.com/blog/thinking-parallel-part-iii-tree-construction-gpu/
public class LBVH<E extends Boundable> {
  private final Node<E>[] treeNodes;
  private final Node<E>[] leafNodes;

  private LBVH(Node<E>[] treeNodes, Node<E>[] leafNodes) {
    this.treeNodes = treeNodes;
    this.leafNodes = leafNodes;
  }

  public int size() {
    return leafNodes.length;
  }

  private Node<E> root() {
    return treeNodes[0];
  }

  public CollisionQuery<E> queryAll() {
    CollisionQueryImpl<E> result = new CollisionQueryImpl<>();
    Node<E> root = root();
    for (var leaf : leafNodes) {
      recursiveQuery(leaf.element, root, result);
    }
    return result;
  }

  public CollisionQuery<E> query(E element) {
    CollisionQueryImpl<E> result = new CollisionQueryImpl<>();
    Node<E> root = root();
    recursiveQuery(element, root, result);
    return result;
  }

  private void recursiveQuery(E toCheck, Node<E> node, CollisionQueryImpl<E> potential) {
    if (node.element == toCheck) {
      return;
    }
    if (toCheck.box().intersects(node.box)) {
      if (node.element != null) {
        potential.add(toCheck, node.element);
      } else {
        recursiveQuery(toCheck, node.left, potential);
        recursiveQuery(toCheck, node.right, potential);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Boundable & MortonEncoded> LBVH<E> buildTree(E[] elements) {
    Arrays.sort(elements, Comparator.comparingInt(MortonEncoded::morton));
    int length = elements.length;
    int leafLength = length - 1;
    final Node<E>[] treeNodes = new Node[leafLength];
    final Node<E>[] leafNodes = new Node[length];
    for (int i = 0; i < length; i++) {
      if (i < leafLength) {
        treeNodes[i] = new Node<>();
      }
      Node<E> node = new Node<>();
      node.element = elements[i];
      node.box = node.element.box();
      leafNodes[i] = node;
    }
    for (int i = 0; i < treeNodes.length; i++) {
      generateNode(elements, treeNodes, leafNodes, i);
    }
    calculateVolumeHierarchy(treeNodes[0]);
    return new LBVH<>(treeNodes, leafNodes);
  }

  private static void calculateVolumeHierarchy(Node<?> node) {
    if (node.element != null) { // Skip leaf node
      return;
    }
    calculateVolumeHierarchy(node.left);
    calculateVolumeHierarchy(node.right);
    node.box = AABBUtil.combine(node.left.box, node.right.box);
  }

  private static <E> void generateNode(final MortonEncoded[] sorted, final Node<E>[] treeNodes, final Node<E>[] leafNodes, final int idx) {
    final Range range = determineRange(sorted, idx);
    final int split = findSplit(sorted, range.start(), range.end());
    final Node<E> left;
    final Node<E> right;
    if (split == range.start()) {
      left = leafNodes[split];
    } else {
      left = treeNodes[split];
    }
    if (split + 1 == range.end()) {
      right = leafNodes[split + 1];
    } else {
      right = treeNodes[split + 1];
    }
    final Node<E> node = treeNodes[idx];
    node.left = left;
    node.right = right;
    left.parent = node;
    right.parent = node;
  }

  private static Range determineRange(final MortonEncoded[] sorted, int index) {
    final int lastIndex = sorted.length - 1;
    if (index == 0) {
      return new Range(0, lastIndex);
    }
    final int initialIndex = index;
    final int prevMorton = sorted[index - 1].morton();
    final int currMorton = sorted[index].morton();
    final int nextMorton = sorted[index + 1].morton();
    if (prevMorton == currMorton && nextMorton == currMorton) {
      while (index > 0 && index < lastIndex) {
        index += 1;
        if (index >= lastIndex || sorted[index].morton() != sorted[index + 1].morton()) {
          break;
        }
      }
      return new Range(initialIndex, index);
    } else {
      final int dir;
      final int d_min;
      final int tempLeft = Integer.numberOfLeadingZeros(currMorton ^ prevMorton);
      final int tempRight = Integer.numberOfLeadingZeros(currMorton ^ nextMorton);
      if (tempLeft > tempRight) {
        dir = -1;
        d_min = tempRight;
      } else {
        dir = 1;
        d_min = tempLeft;
      }
      int l_max = 2;
      int testIndex = index + l_max * dir;
      while (testIndex <= lastIndex && testIndex >= 0 && (Integer.numberOfLeadingZeros(currMorton ^ sorted[testIndex].morton()) > d_min)) {
        l_max *= 2;
        testIndex = index + l_max * dir;
      }
      int l = 0;
      for (int div = 2; l_max / div >= 1; div *= 2) {
        int t = l_max / div;
        int newTest = index + (l + t) * dir;
        if (newTest <= lastIndex && newTest >= 0) {
          int splitPrefix = Integer.numberOfLeadingZeros(currMorton ^ sorted[newTest].morton());
          if (splitPrefix > d_min)
            l = l + t;
        }
      }
      if (dir == 1) {
        return new Range(index, index + l * dir);
      } else {
        return new Range(index + l * dir, index);
      }
    }
  }

  private static int findSplit(final MortonEncoded[] sorted, final int first, final int last) {
    final int firstCode = sorted[first].morton();
    final int lastCode = sorted[last].morton();
    if (firstCode == lastCode) {
      return first;
    }
    final int commonPrefix = Integer.numberOfLeadingZeros(firstCode ^ lastCode);
    int split = first;
    int step = last - first;
    do {
      step = (step + 1) >> 1;
      final int newSplit = split + step;
      if (newSplit < last) {
        final int splitCode = sorted[newSplit].morton();
        final int splitPrefix = Integer.numberOfLeadingZeros(firstCode ^ splitCode);
        if (splitPrefix > commonPrefix) {
          split = newSplit;
        }
      }
    } while (step > 1);
    return split;
  }

  private record Range(int start, int end) {
  }

  private static final class Node<E> {
    Node<E> left = null;
    Node<E> right = null;
    Node<E> parent = null;
    @Nullable E element = null;
    AABB box;
  }
}
