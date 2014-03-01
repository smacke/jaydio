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
package net.smacke.jaydio.channel;


import java.io.IOException;
import java.nio.channels.NonWritableChannelException;
import java.util.Arrays;

import net.smacke.jaydio.buffer.JaydioByteBuffer;
import net.smacke.jaydio.channel.BufferedChannel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

 /**
 * @author smacke
 *
 */
public abstract class BufferedChannelAbstractTester<T extends JaydioByteBuffer> extends Assert {
	
	protected BufferedChannel<T> channel;
    protected T buffer;
    protected long testPosition;
    
    @Before
    public abstract void setUp() throws IOException;
    
    @After
    public void tearDown() throws IOException {
    	channel.close();
        buffer.close();
    }

    @Test
    public void testClosedChannelIsNotOpen() throws IOException {
        channel.close();
        assertFalse(channel.isOpen());
    }

    @Test(expected = NonWritableChannelException.class)
    public void testCannotWriteToReadOnlyChannel() throws IOException {
        assert channel.isReadOnly();

        channel.write(buffer, testPosition);
    }

    @Test
    public void testWritesAreActuallyWritten() throws IOException {
        int size = 7;
        assert size < buffer.capacity();
        byte[] written = new byte[size];
        for (int i=0; i<size; i++) {
            written[i] = (byte)i;
        }
        buffer.clear();
        buffer.put(written);
        buffer.clear();
        channel.write(buffer, testPosition);
        channel.read(buffer, testPosition);
        byte[] read = new byte[size];
        buffer.get(read);
        assertTrue(Arrays.equals(written, read));
    }

    @Test
    public void testWritingPastEofIncreasesLength() throws IOException {
        long expected = testPosition + buffer.limit();
        assert expected > channel.size();
        channel.write(buffer, testPosition);
        assertEquals(expected, channel.size());
    }

}
