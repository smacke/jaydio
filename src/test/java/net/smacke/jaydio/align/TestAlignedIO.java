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
package net.smacke.jaydio.align;

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.JavaHeapByteBuffer;
import net.smacke.jaydio.buffer.JaydioByteBuffer;
import net.smacke.jaydio.channel.BufferedChannel;
import net.smacke.jaydio.channel.MockByteChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


/**
 * Tests which use MockJnaBuffers and MockJnaChannels to fuzz
 * DirectIndexInputs/Outputs, which will do buffered "I/O" totally
 * in-memory, but in an aligned fashion. These tests hit most of the
 * nasty error-prone code paths but are quite fast. </p>
 *
 * @author smacke
 *
 */
public class TestAlignedIO extends Assert {
    private int smallWriteSize;
    private int seekTrials;
    private long randomSeed;
    private int alignedFileSize;
    private int unalignedFileSize;
    private int bufferSize;
	private DirectIoLib mockLib;

    private Random rand;


    public TestAlignedIO() {
        this.smallWriteSize = 42;
        this.seekTrials = 10; // number of times to seek in tests which do so
        this.randomSeed = 42; // constant so that the fuzzer does the same thing each time
        this.alignedFileSize = 1024*2; // chosen so that FILE_SIZE % 512 == 0
        this.unalignedFileSize = this.alignedFileSize + 217; // chosen so that FILE_SIZE_UNALIGNED is not a power of 2
    	int blockSize = 4096;
        this.bufferSize = 2*blockSize;
    	this.mockLib = new MockDirectIoLib(blockSize);
    }

    @Before
    public void setUp() {
    	// so that it doesn't matter what order tests execute in
        rand = new Random(randomSeed);
    }


    @Test
    public void testWritingWithoutSeeking() throws IOException {
        performWriteTest(unalignedFileSize, false, false);
        performWriteTest(alignedFileSize, false, false);
    }

    @Test
    public void testSeekingWithSmallWrites() throws IOException {
        performWriteTest(unalignedFileSize, true, true);
        performWriteTest(alignedFileSize, true, true);
    }

    @Test
    public void testSeekingWithLargeWrites() throws IOException {
        performWriteTest(unalignedFileSize, false, true);
        performWriteTest(alignedFileSize, false, true);
    }


    private void performWriteTest(final int fileSize, final boolean smallWrites, final boolean seek) throws IOException {
        // the gold standard, what we expect to see
        byte[] gold = getGoldBytes(fileSize);

        // "write" bytes to the mock channel
        BufferedChannel<JaydioByteBuffer> channel = MockByteChannel.getChannel(fileSize, mockLib.blockSize(), false);
        MockByteChannelAligner aligned = getMockAlignedChannel(mockLib, channel, bufferSize, fileSize);
        aligned.writeBytes(gold, 0, fileSize);

        if (seek) {
            // now seek around a bit and write random bytes
            for (int i=0; i<seekTrials; i++) {
                final int newpos = rand.nextInt(fileSize);
                final int remaining = fileSize - newpos;
                final int nBytesToWrite = rand.nextInt(smallWrites ? Math.min(smallWriteSize, remaining) : remaining);
                byte[] newBytes = new byte[nBytesToWrite];
                rand.nextBytes(newBytes);
                aligned.position(newpos);
                aligned.writeBytes(newBytes, 0, nBytesToWrite);
                System.arraycopy(newBytes, 0, gold, newpos, nBytesToWrite);
            }
        }

        aligned.truncate(fileSize);
        checkConsistency(mockLib, channel, bufferSize, gold, fileSize);
        // underlying channel gets closed in checkConsistency
    }

    @Test
    public void testIdempotentFlush() throws IOException {
        // unaligned file size less than one buffer long
        int fileSize = bufferSize/2 + Math.min(bufferSize/2 - 7, 7);

        byte[] gold = getGoldBytes(fileSize);

        // seed the mock channel with some starter bytes
        BufferedChannel<JaydioByteBuffer> channel = MockByteChannel.getChannel(fileSize, mockLib.blockSize(), false);
        MockByteChannelAligner aligned = getMockAlignedChannel(mockLib, channel, bufferSize, fileSize);
        aligned.writeBytes(gold, 0, fileSize);

        aligned.position(0);
        aligned.writeBytes(gold, 0, fileSize);

        // second flush should do nothing
        aligned.flush();
        aligned.flush();

        assertEquals(fileSize, aligned.size());

        aligned.truncate(fileSize);
        checkConsistency(mockLib, channel, bufferSize, gold, fileSize);
        // underlying channel gets closed in checkConsistency
    }

    @Test
    public void testStreamReadingAndWriting() throws IOException {
        // unaligned file size less than one buffer long
        int fileSize = bufferSize/2 + bufferSize/4;

        byte[] gold = getGoldBytes(fileSize);


        // "write" bytes to the mock channel
        BufferedChannel<JaydioByteBuffer> channel = MockByteChannel.getChannel(fileSize, mockLib.blockSize(), false);
        MockByteChannelAligner aligned = getMockAlignedChannel(mockLib, channel, bufferSize, fileSize);
        
        // copy gold to file
        aligned.writeBytes(gold, 0, fileSize);
        aligned.position(0);

        // seek to start of file
        aligned.position(0);
        
        // now go stream through the file, alternating
        // between reading and writing.
        int remaining = fileSize;
        int position = 0;
        boolean reading = false;
        while (remaining > 0) {
            final int forward = 1+rand.nextInt(remaining);
            remaining -= forward;
            byte[] forwardBuf = new byte[forward];
            if (reading) {
                // don't actually do anything; just move file pointer forward
                // using read semantics
                aligned.readBytes(forwardBuf, 0, forward);
            } else {
                rand.nextBytes(forwardBuf);
                aligned.writeBytes(forwardBuf, 0, forward);
                System.arraycopy(forwardBuf, 0, gold, position, forward);
            }
            position += forward;
            reading = !reading;
        }

        assertEquals(fileSize, aligned.size());
        
        aligned.truncate(fileSize);
        checkConsistency(mockLib, channel, bufferSize, gold, fileSize);
        // underlying channel gets closed in checkConsistency
    }

    private static MockByteChannelAligner getMockAlignedChannel(DirectIoLib mockLib, BufferedChannel<JaydioByteBuffer> channel, int bufferSize, long fileSize) throws IOException {
        JaydioByteBuffer buffer = JavaHeapByteBuffer.allocate(bufferSize);
        return new MockByteChannelAligner(mockLib, channel, buffer);
    }

    private static MockByteChannelAligner getMockAlignedChannel(DirectIoLib lib, BufferedChannel<JaydioByteBuffer> channel, int bufferSize) {
        JaydioByteBuffer buffer = JavaHeapByteBuffer.allocate(bufferSize);
        return new MockByteChannelAligner(lib, channel, buffer);
    }

    private byte[] getGoldBytes(int fileSize) {
        byte[] expected = new byte[fileSize];
        rand.nextBytes(expected);
        return expected;
    }

    /* Make sure we wrote the same stuff
     * 
     * NOTE: In general it does not make sense of the same ByteAlignedChannels to
     * share the same underlying UnsafeByteAlignedChannels. This is because of the
     * internal bookkeeping that the safe channel does to keep track of length. The
     * only reason this works is because we instantiate the safe channel after the
     * bytes have been flushed to the unsafe channel -- the unsafe channel will think
     * that the file length is longer than it actually will eventually end up being,
     * so the safe channel that we instantiate here doesn't think we're reading past
     * EOF when we get the bytes we're reading.
     */
    private static void checkConsistency(DirectIoLib mockLib, BufferedChannel<JaydioByteBuffer> channel, int bufferSize, byte[] gold, int fileSize) throws IOException {
        // this is where we'll read back in from the files
        byte[] directRead = new byte[fileSize];

        // Now make sure we didn't fail
        // (use same channel as the one we wrote to; it stored the bytes in RAM)
        MockByteChannelAligner aligned = getMockAlignedChannel(mockLib, channel, bufferSize);
        aligned.readBytes(directRead, 0, fileSize);
        aligned.close();

        // make sure the byte arrays are the same
        assertTrue(Arrays.equals(gold, directRead));
    }
}
