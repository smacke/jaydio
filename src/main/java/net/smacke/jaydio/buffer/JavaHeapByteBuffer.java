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
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A mock implementation of {@link JaydioByteBuffer}, used for testing.
 * This class uses Java heap rather than manually malloc'd memory using JNA. </p>
 *
 * @author smacke
 *
 */
public class JavaHeapByteBuffer extends AbstractBuffer implements JaydioByteBuffer {

    private byte[] backing;

    public static JavaHeapByteBuffer allocate(int capacity) {
        return new JavaHeapByteBuffer(0, capacity, capacity);
    }

    protected JavaHeapByteBuffer(int pos, int lim, int cap) {
        super(pos, lim, cap);
        backing = new byte[cap];
    }

    @Override
    public JavaHeapByteBuffer put(byte[] src) {
        return this.put(src, 0, src.length);
    }

    @Override
    public JavaHeapByteBuffer put(byte[] src, int offset, int length) {
        checkWithinBounds(offset, length, src.length);
        if (length > remaining()) {
            throw new BufferOverflowException();
        }
        System.arraycopy(src, offset, backing, position, length);
        this.position(position + length);
        return this;
    }

	@Override
	public JavaHeapByteBuffer put(ByteBuffer src) {
    	final int length = Math.min(this.remaining(), src.remaining());
    	this.put(src.array(), src.position(), length);
    	src.position(src.position()+length);
    	return this;
	}

    @Override
    public JavaHeapByteBuffer put(byte b) {
        backing[safeIncrementForPut()] = b;
        return this;
    }

    @Override
    public byte get() {
        return backing[safeIncrementForGet()];
    }

    @Override
    public JavaHeapByteBuffer get(byte[] dst, int offset, int length) {
        checkWithinBounds(offset, length, dst.length);
        if (length > remaining()) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(backing, position, dst, offset, length);
        this.position(position + length);
        return this;
    }

	@Override
	public JavaHeapByteBuffer get(ByteBuffer dst) {
    	final int length = Math.min(this.remaining(), dst.remaining());
    	this.get(dst.array(), dst.position(), length);
    	dst.position(dst.position()+length);
    	return this;
	}

    @Override
    public JavaHeapByteBuffer get(byte[] dst) {
        return this.get(dst, 0, dst.length);
    }

    @Override
    public JavaHeapByteBuffer copy() {
        JavaHeapByteBuffer copy = new JavaHeapByteBuffer(position, limit, capacity);
        copy.backing = Arrays.copyOf(backing, backing.length);
        return copy;
    }

    @Override
    public void close() {
        if (!isOpen) {
            return;
        }
        isOpen = false;
        backing = null;
    }


}
