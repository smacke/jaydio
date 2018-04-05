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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.align.ByteChannelAligner;
import net.smacke.jaydio.buffer.AlignedDirectByteBuffer;
import net.smacke.jaydio.buffer.JavaHeapByteBuffer;

/**
 * An {@link BufferedChannel} implementation which uses {@link DirectIoLib}
 * for JNA hooks to native Linux methods. Particular, the O_DIRECT flag is used. </p>
 * 
 * <p> One might wonder why the functionality in this class is not directly subsumed by
 * {@link ByteChannelAligner}. For testing purposes, it made sense to separate out the
 * alignment logic from the actual I/O logic. For example, it is possible to test the
 * alignment logic completely in-memory using {@link MockByteChannel}s and
 * {@link JavaHeapByteBuffer}s thanks to this abstraction. </p>
 *
 * @author smacke
 *
 */
public final class DirectIoByteChannel implements BufferedChannel<AlignedDirectByteBuffer> {

    private DirectIoLib lib;
    private int fd;
    private boolean isOpen;
    private long fileLength;
    private boolean isReadOnly;

    public static DirectIoByteChannel getChannel(File file, boolean readOnly) throws IOException {
    	DirectIoLib lib = DirectIoLib.getLibForPath(file.toString());
    	if (lib == null)
    	    throw new IllegalStateException("Could not get lib for path (DirectIoLib.getLibForPath(...)): " + file.toString());
    	return getChannel(lib, file, readOnly);
    }

    public static DirectIoByteChannel getChannel(DirectIoLib lib, File file, boolean readOnly) throws IOException {
        int fd = lib.oDirectOpen(file.toString(), readOnly);
        long length = file.length();
        return new DirectIoByteChannel(lib, fd, length, readOnly);
    }

    private DirectIoByteChannel(DirectIoLib lib, int fd, long fileLength, boolean readOnly) {
    	this.lib = lib;
        this.fd = fd;
        this.isOpen = true;
        this.isReadOnly = readOnly;
        this.fileLength = fileLength;
    }

    private void ensureOpen() throws ClosedChannelException {
    	if (!isOpen()) {
    		throw new ClosedChannelException();
    	}
    }
    
    private void ensureWritable() {
    	if (isReadOnly()) {
    		throw new NonWritableChannelException();
    	}
    }


    @Override
    public int read(AlignedDirectByteBuffer dst, long position) throws IOException {
    	ensureOpen();
        return lib.pread(fd, dst, position);
    }

    @Override
    public int write(AlignedDirectByteBuffer src, long position) throws IOException {
    	ensureOpen();
    	ensureWritable();
        assert src.position() == lib.blockStart(src.position());

        int written = lib.pwrite(fd, src, position);

        // update file length if we wrote past it
        fileLength = Math.max(position + written, fileLength);
        return written;
    }
    
    @Override
    public DirectIoByteChannel truncate(final long length) throws IOException {
    	ensureOpen();
    	ensureWritable();
        if (DirectIoLib.ftruncate(fd, length) < 0) {
            throw new IOException("Error during truncate on descriptor " + fd + ": " +
            		DirectIoLib.getLastError());
        }
        fileLength = length;
    	return this;
    }

    @Override
    public long size() {
        return fileLength;
    }
    
    @Override
    public int getFD() {
    	return fd;
    }
    
    
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public void close() throws IOException {
        if (!isOpen()) {
            return;
        }
        try {
            if (!isReadOnly()) {
            	truncate(fileLength);
            }
        } finally {
        	isOpen = false;
            if (lib.close(fd) < 0) {
                throw new IOException("Error closing file with descriptor " + fd + ": " +
                                        DirectIoLib.getLastError());
            }
        }
    }
}
