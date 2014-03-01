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

import net.smacke.jaydio.buffer.JavaHeapByteBuffer;
import net.smacke.jaydio.buffer.JaydioByteBuffer;
import net.smacke.jaydio.channel.MockByteChannel;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class which composes a {@link BufferedChannelAbstractTester}, passing in a
 * {@link MockByteChannel} to test. Since this class is using a mock channel, it
 * does not do anything with JNA or any actual file I/O and is therefore fast-testable. </p>
 *
 * @author smacke
 *
 */
public class TestMockByteChannel extends BufferedChannelAbstractTester<JaydioByteBuffer> {
    private static final int BLOCK_SIZE = 512;
    private static final int BUFFER_SIZE = 2*BLOCK_SIZE;

    @Override
	@Before public void setUp() {
        channel = MockByteChannel.getChannel(BUFFER_SIZE, BLOCK_SIZE, false);
        buffer = JavaHeapByteBuffer.allocate(512);
        testPosition = BLOCK_SIZE;
    }

    @Override
    @Test(expected = NonWritableChannelException.class)
    public void testCannotWriteToReadOnlyChannel() throws IOException {
        super.tearDown();
        //repoen for readonly
        channel = MockByteChannel.getChannel(BUFFER_SIZE, BLOCK_SIZE, true);
        buffer = JavaHeapByteBuffer.allocate(BLOCK_SIZE);
        super.testCannotWriteToReadOnlyChannel();
    }
}
