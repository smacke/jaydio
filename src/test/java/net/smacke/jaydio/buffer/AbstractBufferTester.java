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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Abstract base class to test various implementations of {@link Buffer}.
 *
 * @author smacke
 *
 */
public abstract class AbstractBufferTester extends Assert {

	protected JaydioByteBuffer buffer;
	
	protected abstract JaydioByteBuffer createInstance();

    @Before
	public void setUp() {
    	buffer = createInstance();
	}

    @After
    public void tearDown() {
    	buffer.close();
    }

	@Test
	public void testGetAdvancesPosition() {
		assert buffer.position() < buffer.limit();

		int oldPos = buffer.position();
		buffer.get();

		assertEquals(buffer.position(), oldPos+1);
	}

	@Test
	public void testPutAdvancesPosition() {
		assert buffer.position() < buffer.limit();

		int oldPos = buffer.position();
		buffer.put((byte)1);

		assertEquals(buffer.position(), oldPos+1);
	}

	@Test
	public void testBulkGetAdvancesPosition() {
		int size = 5;
		assert buffer.position() + size < buffer.limit();
		byte[] dummy = new byte[size];
		int oldPos = buffer.position();
		buffer.put(dummy, 0, size);
		assertEquals(oldPos + size, buffer.position());
	}

	@Test
	public void testBulkPutAdvancesPosition() {
		int size = 5;
		assert buffer.position() + size < buffer.limit();
		byte[] dummy = new byte[size];
		int oldPos = buffer.position();
		buffer.get(dummy, 0, size);
		assertEquals(oldPos + size, buffer.position());
	}

    @Test(expected = IndexOutOfBoundsException.class)
	public void testReadPastArrayBoundFails() {
		int size = 10;
		assert buffer.position()+size < buffer.limit();
		byte[] anything = new byte[10];
		buffer.get(anything, size/2, size);
	}

    @Test(expected = IndexOutOfBoundsException.class)
	public void testWritePastArrayBoundFails() {
		int size = 10;
		assert buffer.position()+size < buffer.limit();
		byte[] anything = new byte[10];
		buffer.put(anything, size/2, size);
	}

	@Test(expected = BufferUnderflowException.class)
	public void testReadPastBufferLimitUnderflows() {
		int size = buffer.limit() - buffer.position() + 7;
		byte[] dummy = new byte[size];
		buffer.get(dummy);
	}

    @Test(expected = BufferOverflowException.class)
	public void testWritePastBufferLimitOverflows() {
		int size = buffer.limit() - buffer.position() + 7;
		byte[] dummy = new byte[size];
		buffer.put(dummy);
	}

	@Test
	public void testPosition() {
		int pos = 5;
		buffer.position(pos);
		assertEquals(pos, buffer.position());
	}

	@Test
	public void testLimit() {
		int lim = 5;
		buffer.limit(lim);
		assertEquals(lim, buffer.limit());
	}

	@Test
	public void testFlip() {
		int oldPos = buffer.position();
		buffer.flip();
		assertEquals(oldPos, buffer.limit());
		assertEquals(0, buffer.position());
	}

	@Test
	public void testClear() {
		buffer.clear();
		assertEquals(buffer.capacity(), buffer.limit());
		assertEquals(0, buffer.position());
	}

	@Test
	public void testCopyIsIndependent() {
		JaydioByteBuffer copy = buffer.copy();
		try {
			buffer.clear();
			copy.clear();
			buffer.put((byte)1);
			assertNotEquals(buffer.position(), copy.position());
			copy.put((byte)2);
			buffer.flip();
			copy.flip();
			byte bufferByte = buffer.get();
			byte copyByte = copy.get();
			assertNotEquals(bufferByte, copyByte);
		} finally {
			copy.close();
		}
	}
}