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

package me.moros.bending.model;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Based on Apache Collections CircularFifoQueue
 */
public final class CircularQueue<E> extends AbstractCollection<E> implements Queue<E> {
	private final E[] elements;
	private int start = 0;
	private int end = 0;
	private boolean full = false;
	private final int maxElements;

	public CircularQueue() {
		this(16);
	}

	@SuppressWarnings("unchecked")
	public CircularQueue(int size) {
		if (size <= 0) throw new IllegalArgumentException("The size must be greater than 0");
		elements = (E[]) new Object[size];
		maxElements = elements.length;
	}

	@Override
	public int size() {
		int size;
		if (end < start) {
			size = maxElements - start + end;
		} else if (end == start) {
			size = full ? maxElements : 0;
		} else {
			size = end - start;
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public void clear() {
		full = false;
		start = 0;
		end = 0;
		Arrays.fill(elements, null);
	}

	@Override
	public boolean add(E element) {
		if (element == null) throw new NullPointerException("Attempted to add null object to queue");
		if (size() == maxElements) poll();
		elements[end++] = element;
		if (end >= maxElements) end = 0;
		if (end == start) full = true;
		return true;
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public E remove() {
		if (isEmpty()) throw new NoSuchElementException("Queue is empty");
		E element = elements[start];
		if (element != null) {
			elements[start++] = null;
			if (start >= maxElements) start = 0;
			full = false;
		}
		return element;
	}

	@Override
	public E poll() {
		if (isEmpty()) return null;
		return remove();
	}

	@Override
	public E element() {
		if (isEmpty()) throw new NoSuchElementException("Queue is empty");
		return peek();
	}

	@Override
	public E peek() {
		if (isEmpty()) return null;
		return elements[start];
	}

	public E get(int index) {
		if (index < 0 || index >= size()) throw new NoSuchElementException();
		int i = (start + index) % maxElements;
		return elements[i];
	}

	private int increment(int index) {
		return index > maxElements ? 0 : ++index;
	}

	private int decrement(int index) {
		return index <= 0 ? maxElements - 1 : --index;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private int index = start;
			private int lastReturnedIndex = -1;
			private boolean isFirst = full;

			@Override
			public boolean hasNext() {
				return isFirst || index != end;
			}

			@Override
			public E next() {
				if (!hasNext()) throw new NoSuchElementException();
				isFirst = false;
				lastReturnedIndex = index;
				index = increment(index);
				return elements[lastReturnedIndex];
			}

			@Override
			public void remove() {
				if (lastReturnedIndex == -1) throw new IllegalStateException();
				if (lastReturnedIndex == start) {
					CircularQueue.this.remove();
					lastReturnedIndex = -1;
					return;
				}
				int pos = lastReturnedIndex + 1;
				if (start < lastReturnedIndex && pos < end) {
					System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos);
				} else {
					while (pos != end) {
						if (pos >= maxElements) {
							elements[pos - 1] = elements[0];
							pos = 0;
						} else {
							elements[decrement(pos)] = elements[pos];
							pos = increment(pos);
						}
					}
				}
				lastReturnedIndex = -1;
				end = decrement(end);
				elements[end] = null;
				full = false;
				index = decrement(index);
			}
		};
	}
}
