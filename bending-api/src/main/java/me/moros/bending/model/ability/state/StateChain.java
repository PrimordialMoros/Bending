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

package me.moros.bending.model.ability.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

import me.moros.bending.model.ability.Updatable;
import org.bukkit.block.Block;

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
    if (chainQueue.isEmpty()) {
      throw new RuntimeException("Chain is empty");
    }
    started = true;
    nextState();
    return this;
  }

  public State current() {
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
    if (!started || finished) {
      return UpdateResult.REMOVE;
    }
    return currentState.update();
  }

  public void abort() {
    finished = true;
  }

  public boolean completed() {
    return finished && chainQueue.isEmpty();
  }

  public boolean finished() {
    return finished;
  }

  public Collection<Block> chainStore() {
    return chainStore;
  }

  private static final class DummyState implements State {
    private static final State INSTANCE = new DummyState();

    private DummyState() {
    }

    @Override
    public void start(StateChain chain) {
    }

    @Override
    public void complete() {
    }

    @Override
    public UpdateResult update() {
      return UpdateResult.REMOVE;
    }
  }
}
