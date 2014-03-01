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

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.AlignedDirectByteBuffer;
import net.smacke.jaydio.channel.BufferedChannel;
import net.smacke.jaydio.channel.DirectIoByteChannel;

public class DirectIoByteChannelAligner extends ByteChannelAligner<AlignedDirectByteBuffer> {
	
	// TODO (smacke): a builder would be good here

	public DirectIoByteChannelAligner(DirectIoLib lib,
			BufferedChannel<AlignedDirectByteBuffer> channel,
			AlignedDirectByteBuffer buffer) {
		super(lib, channel, buffer);
	}

    public static DirectIoByteChannelAligner open(File path) throws IOException {
    	DirectIoLib lib = DirectIoLib.getLibForPath(path.toString());
        return open(lib, path, lib.defaultBufferSize(), false);
    }
    
    public static DirectIoByteChannelAligner open(File path, int bufferSize) throws IOException {
    	DirectIoLib lib = DirectIoLib.getLibForPath(path.toString());
        return open(lib, path, bufferSize, false);
    }
    
    public static DirectIoByteChannelAligner open(File path, boolean readOnly) throws IOException {
    	DirectIoLib lib = DirectIoLib.getLibForPath(path.toString());
        return open(lib, path, lib.defaultBufferSize(), readOnly);
    }
    
    public static DirectIoByteChannelAligner open(File path, int bufferSize, boolean readOnly) throws IOException {
    	DirectIoLib lib = DirectIoLib.getLibForPath(path.toString());
        return open(lib, path, bufferSize, readOnly);
    }

    public static DirectIoByteChannelAligner open(DirectIoLib lib, File path, int bufferSize, boolean readOnly) throws IOException {
        if (bufferSize < 0 || (bufferSize % lib.blockSize() != 0)) {
            throw new IllegalArgumentException("The buffer capacity must be a multiple of the file system block size");
        }
        BufferedChannel<AlignedDirectByteBuffer> channel = DirectIoByteChannel.getChannel(lib, path, readOnly);
        AlignedDirectByteBuffer buffer = AlignedDirectByteBuffer.allocate(lib, bufferSize);
        return new DirectIoByteChannelAligner(lib, channel, buffer);
    }

}
