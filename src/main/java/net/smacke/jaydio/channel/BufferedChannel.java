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
import java.nio.channels.Channel;

import net.smacke.jaydio.buffer.Buffer;


/**
 * Channel which supports positional {@link #read(Buffer,long) read}s and
 * {@link #write(Buffer, long) write}s from/to a buffer to an underlying resource.
 * 
 * @author smacke
 *
 */
public interface BufferedChannel <T extends Buffer> extends Channel {


    /**
     * Writes from the <tt>src</tt> buffer into this channel at <tt>position</tt>. </p>
     *
     * @param src
     *        The {@link Buffer} to write from
     *
     * @param position
     *        The position within the file at which to start writing
     *
     * @return How many bytes were written from <tt>src</tt> into the file
     * @throws IOException
     */
    public int write(T src, long position) throws IOException;

    
    
    
    /**
     * Reads from this channel into the <tt>dst</tt> buffer from <tt>position</tt>. </p>
     *
     * @param dst
     *        The {@link Buffer} to read into 
     *
     * @param position
     *        The position within the file at which to start reading
     *
     * @return How many bytes were placed into <tt>dst</tt>
     * @throws IOException
     */
    public int read(T dst, long position) throws IOException;
    
    

    /**
     * @return The file size for this channel
     */
    public long size();


    /**
     * @return <tt>true</tt> if this channel is read only, <tt>false</tt> otherwise
     */
    public boolean isReadOnly();
    

    /**
     * Truncates this file's length to <tt>fileLength</tt>. </p>
     * 
     * @param fileLength The length to which to truncate
     * 
     * @return This UnsafeByteAlignedChannel
     * 
     * @throws IOException
     */
    public BufferedChannel<T> truncate(long fileLength) throws IOException;

    
    /**
     * @return The file descriptor for this channel
     */
    public int getFD();
}