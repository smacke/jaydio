/**
 * Copyright (C) 2014 Stephen Macke (smacke@cs.stanford.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.smacke.jaydio.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;



/**
 * Skeletal implementation of {@link Buffer}. </p>
 *
 * @author smacke
 *
 */
public abstract class AbstractBuffer implements Buffer {

    protected int position = 0;
    protected int limit;
    protected int capacity;
    protected boolean isOpen;

    protected AbstractBuffer(int pos, int lim, int cap) {
        if (cap < 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = cap;
        limit(lim);
        position(pos);
        this.isOpen = true;
    }

    @Override
    public final boolean isOpen() {
        return isOpen;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public AbstractBuffer position(int newPosition) {
        if ((newPosition > limit) || (newPosition < 0)) {
            throw new IllegalArgumentException();
        }
        position = newPosition;
        return this;
    }

    @Override
    public final int position() {
        return position;
    }

    @Override
    public AbstractBuffer limit(int newLimit) {
    	// enforce the invariants:
    	// position <= limit <= capacity
        if ((newLimit > capacity) || (newLimit < 0)) {
            throw new IllegalArgumentException();
        }
        limit = newLimit;
        this.position = Math.min(this.position, limit);
        return this;
    }

    @Override
    public int limit() {
        return limit;
    }

    @Override
    public AbstractBuffer rewind() {
        position = 0;
        return this;
    }


    @Override
    public AbstractBuffer flip() {
        limit = position;
        position = 0;
        return this;
    }

    @Override
    public AbstractBuffer clear() {
        position = 0;
        limit = capacity;
        return this;
    }

    @Override
    public int remaining() {
        return limit - position;
    }

    @Override
    public boolean hasRemaining() {
        return position < limit;
    }


    // sanity checking methods
    // (package private for use with unit tests)


    static void checkWithinBounds(int off, int len, int size) {
        if ((off | len)  < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (off + len > size) {
        	throw new IndexOutOfBoundsException();
        }
    }
    int safeIncrementForGet() {
        if (position >= limit) {
            throw new BufferUnderflowException();
        }
        return position++;
    }

    int safeIncrementForPut() {
        if (position >= limit) {
            throw new BufferOverflowException();
        }
        return position++;
    }

    @Override
    public void close() {
        isOpen = false;
    }

}
