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


/**
 * A container used to buffer some arbitrary type. The actual storage
 * mechanism is flexible. </p>
 *
 * <p> The API is similar to that of {@link java.nio.Buffer},
 * but this <tt>Buffer</tt> is actually extensible and whatnot.
 * {@link java.nio.Buffer} has package private methods, preventing its
 * use as a superinterface. Also, this buffer impl has no concept of
 * a <tt>mark</tt> like {@link java.nio.Buffer}.</p>
 *
 * @see java.nio.Buffer
 *
 * @author smacke
 *
 */
public interface Buffer {

    /**
     * @see java.nio.Buffer#position()
     */
    public int position();


    /**
     * @see java.nio.Buffer#limit()
     * 
     * @throws java.nio.BufferUnderflowException
     * @throws java.nio.BufferOverflowException
     */
    public int limit();


    /**
     * @see java.nio.Buffer#capacity()
     */
    public int capacity();


    /**
     * @see java.nio.Buffer#remaining()
     */
    public int remaining();


    /**
     * @see java.nio.Buffer#hasRemaining()
     */
    public boolean hasRemaining();




    /**
     * @see java.nio.Buffer#position(int)
     */
    public Buffer position(int newPosition);


    /**
     * @see java.nio.Buffer#limit(int)
     */
    public Buffer limit(int newLimit);


    /**
     * @see java.nio.Buffer#clear()
     */
    public Buffer clear();


    /**
     * @see java.nio.Buffer#flip()
     */
    public Buffer flip();


    /**
     * @see java.nio.Buffer#rewind()
     */
    public Buffer rewind();




    /**
     * Returns a deep copy of this {@link Buffer}. </p>
     */
    public Buffer copy();

    
    
    /**
     * Free any resources associated with this buffer. </p>
     */
    public void close();



    /**
     * @return whether the resources associated with this buffer have not yet been freed. </p>
     */
    public boolean isOpen();

}
