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

package me.moros.bending;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import me.moros.bending.CollisionUtil.CachedAbility;
import me.moros.bending.CollisionUtil.CollectionType;
import me.moros.bending.api.collision.geometry.Collider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class CollisionBenchmark {
  @Param({"1", "10"})
  int size;

  @Param({
    "HashSet",
    "IdentityHashSet",
    "ConcurrentHashSet",
    "LinkedHashSet",
    "ArrayList",
  })
  CollectionType type;

  Collection<CachedAbility> colliders;

  @Setup
  public void setup() {
    colliders = CollisionUtil.generateColliders(size);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void processCollisions(Blackhole bh) {
    int amount = colliders.size() / 2;
    Collection<CachedAbility> pruned = type.create(amount);
    for (var first : colliders) {
      if (pruned.contains(first)) {
        continue;
      }
      for (var second : colliders) {
        if (first.uuid().equals(second.uuid()) || pruned.contains(second)) {
          continue;
        }
        Entry<Collider, Collider> collision = checkCollision(first.colliders(), second.colliders());
        if (collision != null) {
          bh.consume(collision.getKey());
          bh.consume(collision.getValue());
          Blackhole.consumeCPU(512);
          int idx = pruned.size() % 3;
          if (idx == 0) {
            pruned.add(first);
            break;
          } else if (idx == 1) {
            pruned.add(second);
          }
        }
      }
    }
    bh.consume(pruned);
  }

  private @Nullable Entry<Collider, Collider> checkCollision(Collection<Collider> firstColliders, Collection<Collider> secondColliders) {
    for (Collider firstCollider : firstColliders) {
      for (Collider secondCollider : secondColliders) {
        if (firstCollider.intersects(secondCollider)) {
          return Map.entry(firstCollider, secondCollider);
        }
      }
    }
    return null;
  }
}
