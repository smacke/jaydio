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

import java.io.File;
import java.io.IOException;
import java.nio.channels.NonWritableChannelException;
import java.util.Arrays;

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.AlignedDirectByteBuffer;
import net.smacke.jaydio.channel.DirectIoByteChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class which extends {@link BufferedChannelAbstractTester}.
 * This does actual file I/O with the O_DIRECT flag (through JNA). </p>
 *
 * @author smacke
 *
 */
public class TestDirectIoByteChannel extends BufferedChannelAbstractTester<AlignedDirectByteBuffer> {

    private File tempDir;
    private File tempFile;
    private static DirectIoLib lib;
    
    private static File getTempDirectory(String prefix, String suffix) throws IOException {
        File temp = File.createTempFile(prefix, suffix);
        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        } else if (!temp.mkdir()) {
            throw new IOException("could not create temporary directory");
        }
        return temp;
    }

    @BeforeClass public static void setupClass() {
        lib = DirectIoLib.getLibForPath(System.getProperty("java.io.tmpdir"));
    }


    private void subSetup(boolean readOnly) throws IOException {
        channel = DirectIoByteChannel.getChannel(tempFile, readOnly);
        buffer = AlignedDirectByteBuffer.allocate(lib, 2*lib.blockSize());
        testPosition = lib.blockSize();
        int startFilelength = 2*lib.blockSize();
        byte[] fileContents = new byte[startFilelength];
        Arrays.fill(fileContents, (byte)7);
        buffer.put(fileContents);
        channel.write(buffer, 0);
        buffer.clear();
    }

	@Override
    @Before public void setUp() throws IOException {
        tempDir = getTempDirectory("temp", Long.toString(System.nanoTime()));
        tempFile = new File(tempDir, "channel");
        subSetup(false);
    }

    @Override
    @After
    public void tearDown() throws IOException {
        super.tearDown();
        tempFile.delete();
        if (!tempDir.delete()) {
            throw new IOException("could not delete temp directory " + tempDir.getAbsolutePath());
        }
    }

    @Override
    @Test(expected = NonWritableChannelException.class)
    public void testCannotWriteToReadOnlyChannel() throws IOException {
        super.tearDown(); // super method doesn't delete file 
        subSetup(true);
        super.testCannotWriteToReadOnlyChannel();
    }

    public void testCannotOpenNewFileInReadOnlyMode() throws IOException {
        tearDown(); // delete temp file
        try{
            subSetup(true);
            fail("trying to open file in read only mode should throw exception");
        } catch (IOException expected) {
        	// expected
        }
        setUp(); // recreate temp file
    }
}
