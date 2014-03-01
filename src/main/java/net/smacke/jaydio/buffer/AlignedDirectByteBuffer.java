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

import net.smacke.jaydio.DirectIoLib;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * Implementation of {@link Buffer} which uses JNA to get access to properly aligned
 * native memory, for use with the <tt>O_DIRECT</tt> flag. It is called "AlignedDIRECTByteBuffer"
 * after <tt>java.nio.DirectByteBuffer</tt>, as it uses "direct" memory. </p>
 *
 * @author smacke
 *
 */
public final class AlignedDirectByteBuffer extends AbstractBuffer implements JaydioByteBuffer {

    private Pointer pointer;
    private DirectIoLib lib;

    /**
     * Allocate <tt>capacity</tt> bytes of native memory for use as a buffer, and
     * return a {@link AlignedDirectByteBuffer} which gives an interface to this memory. The
     * memory is allocated with
     * {@link DirectIoLib#posix_memalign(PointerByReference,NativeLong,NativeLong) DirectIoLib#posix_memalign()}
     * to ensure that the buffer can be used with <tt>O_DIRECT</tt>.
     * 
     * IT IS VERY IMPORTANT TO CALL {@link #close()} ONCE FINISHED TO FREE MEMORY.
     *
     * @param capacity The requested number of bytes to allocate
     *
     * @return A new JnaMemAlignedBuffer of <tt>capacity</tt> bytes aligned in native memory.
     */
    public static AlignedDirectByteBuffer allocate(DirectIoLib lib, int capacity) {
        if (capacity % lib.blockSize() > 0) {
            throw new IllegalArgumentException("Capacity (" + capacity + ") must be a multiple"
            		+ "of the block size (" + lib.blockSize() + ")");
        }
        NativeLong blockSize = new NativeLong(lib.blockSize());
        PointerByReference pointerToPointer = new PointerByReference();

        // align memory for use with O_DIRECT
        DirectIoLib.posix_memalign(pointerToPointer, blockSize, new NativeLong(capacity));
        return new AlignedDirectByteBuffer(lib, pointerToPointer.getValue(), 0, capacity, capacity);
    }

    private AlignedDirectByteBuffer(DirectIoLib lib, Pointer pointer, int pos, int lim, int cap) {
        super(pos, lim, cap);
        this.lib = lib;
        this.pointer = pointer;
    }





    @Override
    public AlignedDirectByteBuffer get(byte[] dst, int offset, int length) {
        checkWithinBounds(offset, length, dst.length);
        if (length > remaining()) {
            throw new BufferUnderflowException();
        }
        pointer.read(position, dst, offset, length);
        this.position(position + length);
        return this;
    }

    @Override
    public AlignedDirectByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }
    
    @Override
    public AlignedDirectByteBuffer get(ByteBuffer dst) {
    	final int length = Math.min(this.remaining(), dst.remaining());
    	this.get(dst.array(), dst.position(), length);
    	dst.position(dst.position()+length);
    	return this;
    }



    @Override
    public AlignedDirectByteBuffer put(byte[] src, int offset, int length) {
        checkWithinBounds(offset, length, src.length);
        if (length > remaining()) {
            throw new BufferOverflowException();
        }
        pointer.write(position, src, offset, length);
        this.position(position + length);
        return this;
    }

    @Override
    public AlignedDirectByteBuffer put(byte[] src) {
        return this.put(src, 0, src.length);
    }
    
    @Override
    public AlignedDirectByteBuffer put(ByteBuffer src) {
    	final int length = Math.min(this.remaining(), src.remaining());
    	this.put(src.array(), src.position(), length);
    	src.position(src.position()+length);
    	return this;
    }


    @Override
    public byte get() {
        return pointer.getByte(safeIncrementForGet());
    }

    @Override
    public AlignedDirectByteBuffer put(byte b) {
        pointer.setByte(safeIncrementForPut(), b);
        return this;
    }
    

    @Override
    public AlignedDirectByteBuffer copy() {
        AlignedDirectByteBuffer copy = AlignedDirectByteBuffer.allocate(lib, this.capacity());
        int oldPos = this.position();
        int oldLim = this.limit();
        this.clear();
        byte[] temp = new byte[this.capacity()];
        this.get(temp);
        copy.put(temp);
        this.position(oldPos);
        copy.position(oldPos);
        this.limit(oldLim);
        copy.limit(oldLim);
        return copy;
    }

    /**
     * @return A view of the native memory which backs this buffer
     */
    public Pointer pointer() {
        return pointer.share(0); // share view rather than actual pointer
    }

    @Override
    public void close() {
        if (!isOpen) {
            return;
        }
        isOpen = false;
        DirectIoLib.free(pointer); // native free
        pointer = null;
    }
}
