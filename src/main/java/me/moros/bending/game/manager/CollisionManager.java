/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game.manager;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.RegisteredCollision;
import me.moros.bending.util.Tasker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO implement BVH and profile
public final class CollisionManager {
	private static final Collection<RegisteredCollision> collisions = new ArrayList<>();
	private final AbilityManager manager;

	public CollisionManager(@NonNull AbilityManager manager) {
		this.manager = manager;
		Tasker.createTaskTimer(this::run, 5, 1);
	}

	private void run() {
		Collection<Ability> instances = manager.getInstances().collect(Collectors.toList());
		Map<Ability, Collection<Collider>> colliderCache = new HashMap<>();
		for (RegisteredCollision registeredCollision : collisions) {
			Collection<Ability> firstAbilities = instances.stream()
				.filter(ability -> ability.getDescription().equals(registeredCollision.getFirst()))
				.collect(Collectors.toList());
			Collection<Ability> secondAbilities = instances.stream()
				.filter(ability -> ability.getDescription().equals(registeredCollision.getSecond()))
				.collect(Collectors.toList());

			for (Ability first : firstAbilities) {
				Collection<Collider> firstColliders = colliderCache.computeIfAbsent(first, Ability::getColliders);
				if (firstColliders.isEmpty()) continue;
				for (Ability second : secondAbilities) {
					if (first.getUser().equals(second.getUser())) continue;
					Collection<Collider> secondColliders = colliderCache.computeIfAbsent(second, Ability::getColliders);
					if (secondColliders.isEmpty()) continue;
					for (Collider firstCollider : firstColliders) {
						for (Collider secondCollider : secondColliders) {
							if (firstCollider.intersects(secondCollider)) {
								handleCollision(first, second, firstCollider, secondCollider, registeredCollision);
							}
						}
					}
				}
			}
		}
	}

	private void handleCollision(Ability first, Ability second, Collider collider1, Collider collider2, RegisteredCollision collision) {
		first.onCollision(new Collision(first, second, collision.shouldRemoveFirst(), collision.shouldRemoveSecond(), collider1, collider2));
		second.onCollision(new Collision(second, first, collision.shouldRemoveSecond(), collision.shouldRemoveFirst(), collider2, collider1));
	}

	public static int registerCollisions(@NonNull Collection<RegisteredCollision> newCollisions) {
		Set<RegisteredCollision> collisionSet = new HashSet<>(newCollisions);
		collisions.clear();
		collisions.addAll(collisionSet);
		//collisions.stream().map(RegisteredCollision::toString).forEach(Bending.getLog()::info);
		return collisions.size();
	}
}
