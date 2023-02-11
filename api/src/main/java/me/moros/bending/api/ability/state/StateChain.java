/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.api.ability.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.platform.block.Block;

/**
 * Represents a chain of states that are evaluated sequentially.
 * @see State
 */
public class StateChain implements Updatable {
  private final Collection<Block> chainStore;
  private final Queue<State> chainQueue;
  private State currentState = DummyState.INSTANCE;

  private boolean started = false;
  private boolean finished = false;

  public StateChain() {
    chainStore = new ArrayList<>();
    chainQueue = new ArrayDeque<>();
  }

  public StateChain(Collection<Block> store) {
    this();
    chainStore.addAll(store);
  }

  /**
   * Add a new state to this chain.
   * @param state the state to add
   * @return this chain
   * @throws RuntimeException if chain has already started
   */
  public StateChain addState(State state) {
    if (started) {
      throw new RuntimeException("State is executing");
    }
    chainQueue.offer(state);
    return this;
  }

  /**
   * Attempt to start this chain.
   * @return this chain
   * @throws RuntimeException if chain has already started or there are no states queued
   */
  public StateChain start() {
    if (started) {
      throw new RuntimeException("State is executing");
    }
    if (chainQueue.isEmpty()) {
      throw new RuntimeException("Chain is empty");
    }
    started = true;
    nextState();
    return this;
  }

  /**
   * Get the state that is currently evaluated.
   * @return the current state
   */
  public State current() {
    return currentState;
  }

  /**
   * Manually move to the next state or end the chain if there's no other state queued.
   */
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
    if (!started || finished) {
      return UpdateResult.REMOVE;
    }
    return currentState.update();
  }

  /**
   * Abort this chain.
   */
  public void abort() {
    finished = true;
  }

  /**
   * Check if this chain has completed evaluating all its states.
   * @return the result
   */
  public boolean completed() {
    return finished && chainQueue.isEmpty();
  }

  /**
   * Check if this chain has finished, either completed or aborted.
   * @return the result
   */
  public boolean finished() {
    return finished;
  }

  /**
   * Return this chain's store of blocks.
   * @return a mutable collection of this chain's stored blocks
   */
  public Collection<Block> chainStore() {
    return chainStore;
  }
}
