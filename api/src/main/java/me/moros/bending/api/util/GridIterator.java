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

package me.moros.bending.api.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import me.moros.math.Position;
import me.moros.math.Vector3d;

/**
 * Iterator for travelling a line in a grid.
 */
public final class GridIterator implements Iterator<Vector3d> {
  private static final double BIG_DELTA = 1e30;
  private static final double EPSILON = 1e-10;

  private final short[] signums = new short[3];
  private final Position end;

  private boolean foundEnd;

  //length of ray from current position to next x or y-side
  double sideDistX;
  double sideDistY;
  double sideDistZ;

  //length of ray from one x or y-side to next x or y-side
  private final double deltaDistX;
  private final double deltaDistY;
  private final double deltaDistZ;

  //which box of the map we're in
  int mapX;
  int mapY;
  int mapZ;

  private final Deque<Vector3d> extraPoints = new ArrayDeque<>();

  private GridIterator(Vector3d origin, Vector3d dir, double maxDistance) {
    end = origin.add(dir.multiply(maxDistance)).toVector3i();

    //which box of the map we're in
    mapX = origin.blockX();
    mapY = origin.blockY();
    mapZ = origin.blockZ();

    signums[0] = (short) Math.signum(dir.x());
    signums[1] = (short) Math.signum(dir.y());
    signums[2] = (short) Math.signum(dir.z());

    deltaDistX = (dir.x() == 0) ? BIG_DELTA : Math.abs(1 / dir.x());
    deltaDistY = (dir.y() == 0) ? BIG_DELTA : Math.abs(1 / dir.y());
    deltaDistZ = (dir.z() == 0) ? BIG_DELTA : Math.abs(1 / dir.z());

    //calculate step and initial sideDist
    if (dir.x() < 0) {
      sideDistX = (origin.x() - mapX) * deltaDistX;
    } else {
      sideDistX = (mapX + 1.0 - origin.x()) * deltaDistX;
    }
    if (dir.y() < 0) {
      sideDistY = (origin.y() - mapY) * deltaDistY;
    } else {
      sideDistY = (mapY + 1.0 - origin.y()) * deltaDistY;
    }
    if (dir.z() < 0) {
      sideDistZ = (origin.z() - mapZ) * deltaDistZ;
    } else {
      sideDistZ = (mapZ + 1.0 - origin.z()) * deltaDistZ;
    }
  }

  private boolean reachedEnd(Position p) {
    return p.blockX() == end.blockX() && p.blockY() == end.blockY() && p.blockZ() == end.blockZ();
  }

  @Override
  public boolean hasNext() {
    return !foundEnd;
  }

  @Override
  public Vector3d next() {
    if (foundEnd) {
      throw new NoSuchElementException();
    }
    if (!extraPoints.isEmpty()) {
      Vector3d res = extraPoints.poll();
      if (reachedEnd(res)) {
        foundEnd = true;
      }
      return res;
    }

    Vector3d current = Vector3d.of(mapX, mapY, mapZ);
    if (reachedEnd(current)) {
      foundEnd = true;
    }

    double closest = Math.min(sideDistX, Math.min(sideDistY, sideDistZ));
    boolean needsX = sideDistX - closest < EPSILON;
    boolean needsY = sideDistY - closest < EPSILON;
    boolean needsZ = sideDistZ - closest < EPSILON;

    if (needsZ) {
      sideDistZ += deltaDistZ;
      mapZ += signums[2];
    }
    if (needsX) {
      sideDistX += deltaDistX;
      mapX += signums[0];
    }
    if (needsY) {
      sideDistY += deltaDistY;
      mapY += signums[1];
    }

    if (needsX && needsY && needsZ) {
      extraPoints.add(Vector3d.of(signums[0] + current.x(), current.y(), current.z()));
      extraPoints.add(Vector3d.of(current.x(), signums[1] + current.y(), current.z()));
      extraPoints.add(Vector3d.of(current.x(), current.y(), signums[2] + current.z()));
    } else if (needsX && needsY) {
      extraPoints.add(Vector3d.of(signums[0] + current.x(), current.y(), current.z()));
      extraPoints.add(Vector3d.of(current.x(), signums[1] + current.y(), current.z()));
    } else if (needsX && needsZ) {
      extraPoints.add(Vector3d.of(signums[0] + current.x(), current.y(), current.z()));
      extraPoints.add(Vector3d.of(current.x(), current.y(), signums[2] + current.z()));
    } else if (needsY && needsZ) {
      extraPoints.add(Vector3d.of(current.x(), signums[1] + current.y(), current.z()));
      extraPoints.add(Vector3d.of(current.x(), current.y(), signums[2] + current.z()));
    }

    return current;
  }

  /**
   * Creates an iterator that iterates through blocks on a 3d grid, until the total length exceeds the length specified.
   * @param start iterator start position
   * @param dir iterator direction
   * @param length the maximum length of the iterator
   * @return the iterator
   */
  public static GridIterator create(Vector3d start, Vector3d dir, double length) {
    return new GridIterator(start, dir.normalize(), length);
  }
}
