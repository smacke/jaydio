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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.align.DirectIoByteChannelAligner;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test class for direct I/O. It is very easy to make mistakes with block aligned I/O,
 * so these tests actually fuzz the implementation to try and break it. </p>
 *
 * <p>
 * Because this class does I/O, and because it behaves nondeterministically,
 * it can be rather slow. Use {@link TestAlignedIO} if
 * you need a test which runs quickly but still hits most of the error-prone
 * code paths. </p>
 *
 * @author smacke
 *
 */
public class TestDirectIO extends Assert {

    public static final String DIRECT_FILE_NAME = "direct_file";
    public static final String GOLD_FILE_NAME = "gold_file";
    
    private int smallWriteSize;
    private int seekTrials;
    private int alignedFileSize;
    private int unalignedFileSize;
    private int bufferSize;
    private DirectIoLib lib;

    public TestDirectIO() {
        this.smallWriteSize = 42;
        this.seekTrials = 100; // number of times to seek in tests which do so
        this.alignedFileSize = 1024*128; // chosen so that FILE_SIZE % 512 == 0
        this.unalignedFileSize = this.alignedFileSize + 217; // chosen so that FILE_SIZE_UNALIGNED is not a power of 2
        this.lib = DirectIoLib.getLibForPath(System.getProperty("java.io.tmpdir")); // since everything goes in /tmp
        this.bufferSize = 8*lib.blockSize();
    }

    private static File getTempDirectory(String prefix, String suffix) throws IOException {
        File temp = File.createTempFile(prefix, suffix);
        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        } else if (!temp.mkdir()) {
            throw new IOException("could not create temporary directory");
        }
        return temp;
    }

    @Test
    public void testWritingWithoutSeeking() throws IOException {
        performWriteTest(unalignedFileSize, false, false);
        performWriteTest(alignedFileSize, false, false);
    }

    @Test
    public void testSeekingWithSmallEdits() throws IOException {
        performWriteTest(unalignedFileSize, true, true);
        performWriteTest(alignedFileSize, true, true);
    }

    @Test
    public void testSeekingWithLargeEdits() throws IOException {
        performWriteTest(unalignedFileSize, true, false);
        performWriteTest(alignedFileSize, true, false);
    }


    private void performWriteTest(final int fileSize, final boolean seek, final boolean smallWrites) throws IOException {
        File temp = getTempDirectory("temp", Long.toString(System.nanoTime()));

        // use a RandomAccessFile as the gold standard
        // against which to compare the direct I/O channel.
        File goldFile = new File(temp, GOLD_FILE_NAME);
        RandomAccessFile gold = new RandomAccessFile(goldFile, "rw");
        
        // channel for direct i/o
        File directFile = new File(temp, DIRECT_FILE_NAME);
        DirectIoByteChannelAligner direct = DirectIoByteChannelAligner.open(lib, directFile, bufferSize, false);

        try {
            Random rand = new Random(System.nanoTime());
            byte[] writeBuffer = new byte[fileSize];
            rand.nextBytes(writeBuffer);

            // write random bytes to each file
            direct.writeBytes(writeBuffer, 0, fileSize);
            gold.write(writeBuffer);

            if (seek) {
                // now seek around a bit and write random bytes
                for (int i=0; i<seekTrials; i++) {
                    final int newpos = rand.nextInt(fileSize);
                    final int remaining = fileSize - newpos;
                    final int nBytesToWrite = rand.nextInt(smallWrites ? Math.min(smallWriteSize, remaining) : remaining);
                    rand.nextBytes(writeBuffer);
                    direct.position(newpos);
                    gold.getChannel().position(newpos);
                    direct.writeBytes(writeBuffer, 0, nBytesToWrite);
                    gold.write(writeBuffer, 0, nBytesToWrite);
                }
            }

            checkConsistency(direct, gold, directFile, fileSize);
        }
        finally {
            // cleanup
            gold.close();
            direct.close();
            goldFile.delete();
            directFile.delete();
            if (!temp.delete()) {
                throw new IOException("Error: could not delete temp directory " + temp.getAbsolutePath());
            }
        }
    }

    @Test
    public void testIdempotentFlush() throws IOException {
        // test both w/ and w/out seeking
        testIdempotentFlushHelper(false);
        testIdempotentFlushHelper(true);
    }

    private void testIdempotentFlushHelper(boolean seek) throws IOException {
        // create temp directory
        File temp = getTempDirectory("temp", Long.toString(System.nanoTime()));
        // Directory object for direct i/o
        int bufferSize = 2*lib.defaultBufferSize();

        // File for regular i/o:
        File goldFile = new File(temp, GOLD_FILE_NAME);
        RandomAccessFile gold = new RandomAccessFile(goldFile, "rw");
        
        // channel for direct i/o
        File directFile = new File(temp, DIRECT_FILE_NAME);
        DirectIoByteChannelAligner direct = DirectIoByteChannelAligner.open(lib, directFile, bufferSize, false);

        int fileSize = bufferSize/2 + bufferSize/4;

        try {
            // Get some random bytes to write to the files
            Random rand = new Random(System.nanoTime());
            byte[] writeBuffer = new byte[fileSize];
            rand.nextBytes(writeBuffer);

            // write bytes to each file
            direct.writeBytes(writeBuffer, 0, fileSize);
            gold.write(writeBuffer);

            direct.flush();
            direct.flush(); // this will DEFINITELY cause a failure later if we're not aligning properly within flush()
            if (seek) {
                for (int i=0; i<seekTrials; i++) {
                    // seek around a bit and flush the buffer
                    direct.position(rand.nextInt(fileSize));
                    direct.flush();
                    direct.flush();
                }
            }

            checkConsistency(direct, gold, directFile, fileSize);
        }
        finally {
            // cleanup
            gold.close();
            direct.close();
            goldFile.delete();
            directFile.delete();
            if (!temp.delete()) {
                throw new IOException("Error: could not delete temp directory " + temp.getAbsolutePath());
            }
        }
    }
    
    @Test
    public void testStreamReadingAndWriting() throws IOException {
        // create temp directory
        File temp = getTempDirectory("temp", Long.toString(System.nanoTime()));
        // Directory object for direct i/o

        // File for regular i/o:
        // We use a RandomAccessFile as the gold standard
        // against which to compare the direct I/O channel.
        File goldFile = new File(temp, GOLD_FILE_NAME);
        RandomAccessFile gold = new RandomAccessFile(goldFile, "rw");
        
        // channel for direct i/o
        File directFile = new File(temp, DIRECT_FILE_NAME);
        DirectIoByteChannelAligner direct = DirectIoByteChannelAligner.open(lib, directFile, bufferSize, false);
        
        int fileSize = unalignedFileSize;

        try {
            // Get some random bytes to write to the files
            Random rand = new Random(System.nanoTime());
            byte[] writeBuffer = new byte[fileSize];
            rand.nextBytes(writeBuffer);

            // write bytes to each file
            direct.writeBytes(writeBuffer, 0, fileSize);
            gold.write(writeBuffer);
            
            // seek to start of file
            direct.position(0);
            gold.seek(0);
            
            // now go stream through the file, alternating
            // between reading and writing.
            int remaining = fileSize;
            boolean reading = false;
            while (remaining > 0) {
            	final int forward = 1+rand.nextInt(remaining);
            	remaining -= forward;
            	byte[] forwardBuf = new byte[forward];
            	if (reading) {
            		direct.readBytes(forwardBuf, 0, forward);
            		gold.read(forwardBuf, 0, forward);
            	} else {
            		rand.nextBytes(forwardBuf);
            		direct.writeBytes(forwardBuf, 0, forward);
            		gold.write(forwardBuf, 0, forward);
            	}
            	reading = !reading;
            }

            checkConsistency(direct, gold, directFile, fileSize);
        }
        finally {
            // cleanup
            gold.close();
            direct.close();
            goldFile.delete();
            directFile.delete();
            if (!temp.delete()) {
                throw new IOException("Error: could not delete temp directory " + temp.getAbsolutePath());
            }
        }
    }

    // Make sure we wrote the same stuff
    // (assuming direct input works)
    private static void checkConsistency(DirectIoByteChannelAligner direct, RandomAccessFile gold, File directFile, int fileSize) throws IOException {
        // files written should have same lengths
        assertEquals(gold.length(), direct.size());

        // this is where we'll read back in from the files
        byte[] goldRead = new byte[fileSize];
        byte[] directRead = new byte[fileSize];

        direct.position(0);
        direct.readBytes(directRead, 0, fileSize);
        gold.getChannel().position(0);
        gold.read(goldRead, 0, fileSize);

        // make sure files are the same
        assertTrue(Arrays.equals(goldRead, directRead));
        
        Arrays.fill(directRead, (byte)11);
        DirectIoByteChannelAligner directNew = null;
        try {
        	direct.truncate(direct.size());
        	directNew = DirectIoByteChannelAligner.open(directFile);
        	assertEquals(gold.length(), directNew.size());
        	directNew.readBytes(directRead, 0, fileSize);
        	// make sure files are the same
        	assertTrue(Arrays.equals(goldRead, directRead));
        } finally {
        	if (directNew != null) {
        		directNew.close();
        	}
        }
    }
}
