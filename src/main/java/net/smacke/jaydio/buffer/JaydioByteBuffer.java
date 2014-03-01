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

public interface JaydioByteBuffer extends Buffer {
    
    /**
     * @see java.nio.ByteBuffer#get()
     */
    public byte get();


    /**
     * @see java.nio.ByteBuffer#get(byte[])
     */
    public JaydioByteBuffer get(byte[] dst);


    /**
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     */
    public JaydioByteBuffer get(byte[] dst, int offset, int length);
    
    /**
     * Reads <tt>min(this.remaining(),dst.remaining())</tt> bytes from this buffer into <tt>dst</tt>.
     * Note the subtle difference from the corresponding method {@link java.nio.ByteBuffer} -- that
     * one does not take the min of the two remaining capacities.
     * 
     * @param dst
     * 		  The buffer into which to read.
     * 
     * @return This buffer
     */
    public JaydioByteBuffer get(java.nio.ByteBuffer dst);


    /**
     * @see java.nio.ByteBuffer#put(byte)
     */
    public JaydioByteBuffer put(byte b);


    /**
     * @see java.nio.ByteBuffer#put(byte[])
     */
    public JaydioByteBuffer put(byte[] src);


    /**
     * @see java.nio.ByteBuffer#put(byte[], int, int)
     */
    public JaydioByteBuffer put(byte[] src, int offset, int length);
    
    
    /**
     * Writes <tt>min(this.remaining(),src.remaining())</tt> bytes into this buffer.
     * 
     * @param src
     * 		  The ByteBuffer from which to write into this buffer
     * 
     * @return This buffer
     */
    public JaydioByteBuffer put(java.nio.ByteBuffer src);
    
    
    // override return type
    @Override
    public JaydioByteBuffer copy();

}
