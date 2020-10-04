/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

package me.moros.bending.model.ability.state;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.UpdateResult;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

public class StateChain implements Updatable {
	private final Collection<Block> chainStore;
	private final Queue<State> chainQueue;
	private State currentState;

	private boolean started = false;
	private boolean finished = false;

	public StateChain() {
		this(new ArrayList<>());
	}

	public StateChain(Collection<Block> store) {
		chainStore = store;
		chainQueue = new ArrayDeque<>();
	}

	public StateChain addState(State state) {
		if (started) {
			throw new RuntimeException("State is executing");
		}
		chainQueue.offer(state);
		return this;
	}

	public StateChain start() {
		if (started) {
			throw new RuntimeException("State is executing");
		}
		started = true;
		nextState();
		return this;
	}

	public State getCurrent() {
		return currentState;
	}

	public void nextState() {
		if (chainQueue.isEmpty()) {
			finished = true;
			return;
		}
		currentState = chainQueue.poll();
		currentState.start(this);
	}

	@Override
	public UpdateResult update() {
		if (!started || finished) return UpdateResult.REMOVE;
		return currentState.update();
	}

	public void abort() {
		finished = true;
	}

	public boolean isComplete() {
		return finished && chainQueue.isEmpty();
	}

	public Collection<Block> getChainStore() {
		return chainStore;
	}
}
