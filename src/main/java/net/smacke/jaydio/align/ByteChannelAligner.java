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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.JaydioByteBuffer;
import net.smacke.jaydio.channel.BufferedChannel;


/**
 * Abstract class which handles reading and writing from/to an underlying byte channel in appropriate
 * increments and and at appropriate boundaries of the channel. The motivation for this class is that
 * direct I/O in Linux (and in Windows too, I believe) requires all reads and writes to obey these
 * strange alignment rules. It may seem like overkill to segment out the alignment logic necessary
 * for this into its own abstract class, separate from any actual channel, but this allows for highly
 * flexible testing with mock objects, to avoid doing actual heavy-weight I/O when possible. </p>
 * 
 * @author smacke
 * 
 */
public abstract class ByteChannelAligner <T extends JaydioByteBuffer> implements SeekableByteChannel {
	T buffer;
    BufferedChannel<T> channel;

    private DirectIoLib lib;
    private boolean isOpen;
    private long filePos;
    private long fileLength;
    
    private boolean[] dirty;
    private boolean globalDirty;

    // TODO (smacke): It may be good to support all the various options that
    // Java FileChannel does, e.g. APPEND, TRUNCATE_EXISTING, CREATE_NEW, CREATE,
    // DELETE_ON_CLOSE, etc.
    
    
    public ByteChannelAligner(DirectIoLib lib, BufferedChannel<T> channel, T buffer) {
    	this.lib = lib;
    	this.buffer = buffer;
    	this.channel = channel;
    	this.isOpen = true;
    	this.fileLength = channel.size();
    	dirty = new boolean[buffer.capacity() / lib.blockSize()];
    	Arrays.fill(dirty, false);
    	globalDirty = false;
    	positionBufferForFlushAndRefill(0);
    }
    
    private void ensureOpen() throws ClosedChannelException {
    	if (!isOpen) {
    		throw new ClosedChannelException();
    	}
    }
    
    private void ensureWritable() throws NonWritableChannelException {
    	if (channel.isReadOnly()) {
    		throw new NonWritableChannelException();
    	}
    }

    @Override
    public void close() throws IOException {
        if (isOpen) {
            try {
            	if (!channel.isReadOnly()) {
            		truncate(size());
            	}
            } finally {
            	isOpen = false;
            	try {
            		channel.close();
            	} finally {
            		buffer.close();
            	}
            }
        }
    }

    @Override
    public long position() {
        return filePos + buffer.position();
    }

    @Override
    public ByteChannelAligner<T> position(long pos) throws IOException {
    	ensureOpen();
        final long alignedPos = lib.blockStart(pos);
        if (filePos != alignedPos) { // if we need to seek() outside the current window
        	flush();
            if (alignedPos < size()) {
                positionBufferForFlushAndRefill(alignedPos);
                flushAndRefill();
            }
            else {
            	// now we're past the current channel size
            	// reads throw EOFException
            	// writes leave intermediate bytes unspecified
                filePos = alignedPos;
            }
        }
        // seek to correct place within buffer window
        final int delta = (int) (pos - alignedPos);
        buffer.position(delta);
        return this;
    }

    private void positionBufferForFlushAndRefill(long position) {
    	assert lib.blockStart(position) == position;
        buffer.clear(); // in case limit() < capacity()
        filePos = position - buffer.capacity();
        // we only refill when at capacity
        buffer.position(buffer.capacity());
    }

    @Override
    public long size() {
        return fileLength;
    }

    
    public int readBytes(byte[] dst, int offset, int length) throws IOException {
    	ensureOpen();
    	if (position() > size()) {
    		throw new EOFException("trying to read at " + position() + " , length is " + size());
    	} else if (position() == size()) {
    		return -1; //SeekableByteChannel contract
    	}
    	int total = 0;
    	if (buffer.remaining() == 0) {
    		flushAndRefill();
    	}
    	while (buffer.remaining() > 0 && length > buffer.remaining()) {
    		final int toRead = buffer.remaining();
    		total += toRead;
    		buffer.get(dst, offset, toRead);
    		offset += toRead;
    		length -= toRead;
    		if (length > 0) {
    			flushAndRefill();
    		}
    	}
        if (buffer.remaining() == 0) { // i.e. we're at EOF
    		return total;
    	} else {
    		assert length <= buffer.remaining();
    		total += length;
    		buffer.get(dst, offset, length);
    		return total;
    	}
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
    	ensureOpen();
    	final int ret = readBytes(dst.array(), dst.position(), dst.remaining());
    	dst.position(dst.position() + ret);
    	return ret;
    }
    
    public int writeBytes(byte[] src, int offset, int length) throws IOException {
    	ensureOpen();
    	ensureWritable();
    	int total = 0;
    	boolean bufferHasMoved = false;
    	if (buffer.remaining() == 0) {
    		bufferHasMoved = true;
    		flushAndForwardBufferWithoutRefill();
    	}
    	while (length > buffer.remaining()) {
            final int toWrite = buffer.remaining();
            total += toWrite;
            // set blocks which we are about to write to as being dirty
            setDirtyBlocksInRange(buffer.position(), buffer.capacity());
            buffer.put(src, offset, toWrite);
            offset += toWrite;
            length -= toWrite;
            bufferHasMoved = true;
            flushAndForwardBufferWithoutRefill();
    	}
    	assert length <= buffer.remaining();
    	// if we moved the buffer, we need to keep it in sync with the disk
    	// assuming we don't next overwrite it completely
    	if (bufferHasMoved && length < buffer.remaining()) {
    		positionBufferForFlushAndRefill(filePos);
    		flushAndRefill();
    	}
    	// set blocks which we are about to write to as being dirty
    	setDirtyBlocksInRange(buffer.position(), buffer.position() + length);
        total += length;
        buffer.put(src, offset, length);
        fileLength = Math.max(fileLength, position());
    	return total;
    }
    
    @Override
    public int write(ByteBuffer src) throws IOException {
    	ensureOpen();
    	ensureWritable();
    	final int ret = writeBytes(src.array(), src.position(), src.remaining());
    	src.position(src.position() + ret);
    	return ret;
    }
    
    public void write(int b) throws IOException {
    	ensureOpen();
    	ensureWritable();
    	if (buffer.remaining() == 0) {
    		flushAndRefill();
    	}
    	dirty[buffer.position() / lib.blockSize()] = true;
    	buffer.put((byte) b);
        fileLength = Math.max(fileLength, position());
    }
    
    public int read() throws IOException {
    	ensureOpen();
    	if (position() > size()) {
    		throw new EOFException("trying to read at " + position() + " , length is " + size());
    	} else if (position() == size()) {
    		return -1; //SeekableByteChannel contract
    	}
    	if (buffer.remaining() == 0) {
    		flushAndRefill();
    	}
    	return buffer.get() & 0xFF;
    }
    
    private void flushAndForwardBufferWithoutRefill() throws IOException {
    	assert buffer.remaining() == 0;
        flush();
        filePos += buffer.capacity();
        buffer.clear();
    }
    
    // sets blocks to dirty if buffer bytes in [start,stop)
    // have been written
    private void setDirtyBlocksInRange(int start, int stop) {
        for (int i=start / lib.blockSize(); i*lib.blockSize() < stop; i++) {
            dirty[i] = true;
        }
        // also set the global dirty bit to true
        globalDirty = true;
    }
    
    private void refill() throws IOException {
    	assert buffer.position() == 0;
        if (position() < size()) {
            assert lib.blockStart(filePos) == filePos :
            	"filePos is not a multiple of " + lib.blockSize() + ": filePos=" + filePos;
            channel.read(buffer, filePos);
            buffer.clear();
        }
    }

    private void flushAndRefill() throws IOException {
    	assert buffer.remaining() == 0;
    	flushAndForwardBufferWithoutRefill();
    	refill();
    }

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public ByteChannelAligner<T> truncate(final long size) throws IOException {
		ensureOpen();
		ensureWritable();
		flush();
		channel.truncate(size);
		fileLength = size;
		return this;
	}

    public void flush() throws IOException {
    	ensureOpen();
    	if (!globalDirty) { // nothing to do
    		return;
    	}
    	// read only channels cannot get here since there
    	// will not be any dirty bits
    	ensureWritable();
    	final int oldPos = buffer.position();
    	final int oldLim = buffer.limit();
        assert (lib.blockStart(filePos)) == filePos;
        for (int i=0; i<dirty.length; i++) {
        	int j=i;
        	while(j < dirty.length && dirty[j]) {
        		dirty[j] = false;
        		j++;
        	}
        	if (i==j) {
        		continue;
        	}
        	buffer.position(i*lib.blockSize());
        	buffer.limit(j*lib.blockSize());
        	
        	channel.write(buffer, filePos + buffer.position());
        	buffer.clear(); // so that subsequent positionings don't break
        	i=j;
        }
        buffer.position(oldPos);
        buffer.limit(oldLim);
        globalDirty = false;
    }

}