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
package net.smacke.jaydio;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.smacke.jaydio.align.DirectIoByteChannelAligner;

/**
 * Class to emulate the behavior of {@link RandomAccessFile}, but using direct I/O.
 * @author smacke
 *
 */
public class DirectRandomAccessFile implements DataInput, DataOutput, Closeable {
	
	// "\uFEFF" doesn't seem to work for some reason... hacky workaround
	private static final String UTF8_BOM = Character.toString((char)0xEF) +
			Character.toString((char)0xBB) +
			Character.toString((char)0xBF);
	
	private DirectIoByteChannelAligner channel;
	
	/**
	 * @param name The name of the file to open
	 * 
	 * @param mode Either "rw" or "r", depending on whether this file is read only
	 * 
	 * @throws IOException
	 */
	public DirectRandomAccessFile(String name, String mode) throws IOException {
		this(new File(name), mode);
	}
	
	/**
	 * @param file The file to open
	 * 
	 * @param mode Either "rw" or "r", depending on whether this file is read only
	 * 
	 * @throws IOException
	 */
	public DirectRandomAccessFile(File file, String mode) throws IOException {
		this(file, mode, -1);
	}

	/**
	 * @param file The file to open
	 * 
	 * @param mode Either "rw" or "r", depending on whether this file is read only
	 * 
	 * @param bufferSize The size of the buffer used to manually buffer I/O
	 * 		  If -1 the default buffer size is used, which depends on how
	 * 		  {@link DirectIoLib} is implemented.
	 * 
	 * @throws IOException
	 */
	public DirectRandomAccessFile(File file, String mode, int bufferSize)
		throws IOException {
		
		boolean readOnly = false;
		if (mode.equals("r")) {
			readOnly = true;
		} else if (!mode.equals("rw")) {
			throw new IllegalArgumentException("only r and rw modes supported");
		}
		
		if (readOnly && !file.isFile()) {
			throw new FileNotFoundException("couldn't find file " + file);
		}
		
		this.channel = bufferSize!=-1 ? 
				DirectIoByteChannelAligner.open(file, bufferSize, readOnly) :
				DirectIoByteChannelAligner.open(file, readOnly);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	@Override
	public void write(int v) throws IOException {
		channel.write(v);
	}

	@Override
	public void write(byte[] src) throws IOException {
		write(src, 0, src.length);
	}

	@Override
	public void write(byte[] src, int offset, int length) throws IOException {
		channel.writeBytes(src, offset, length);
	}

	@Override
	public void writeBoolean(boolean b) throws IOException {
		write(b ? 1 : 0);
	}

	@Override
	public void writeByte(int b) throws IOException {
		write(b);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		write(s.getBytes());
	}

	@Override
	public void writeChar(int c) throws IOException {
		write((c >>> 8) & 0xFF);
		write((c >>> 0) & 0xFF);
	}

	@Override
	public void writeChars(String s) throws IOException {
		int numChars = s.length();
		int numBytes = 2*numChars;
		byte[] sBytes = new byte[numBytes];
		char[] sChars = new char[numChars];
		s.getChars(0, numChars, sChars, 0);
		int bPos = 0;
		for (int i = 0; i < numChars; i++) {
            sBytes[bPos++] = (byte)(sChars[i] >>> 8);
            sBytes[bPos++] = (byte)(sChars[i] >>> 0);
		}
		write(sBytes, 0, numBytes);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToLongBits(v));
	}

	@Override
	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToIntBits(v));
	}

	@Override
	public void writeInt(int v) throws IOException {
		write((v >>> 24) & 0xFF);
		write((v >>> 16) & 0xFF);
		write((v >>> 8) & 0xFF);
		write(v & 0xFF);
	}

	@Override
	public void writeLong(long v) throws IOException {
		write((int)(v >>> 56) & 0xFF);
		write((int)(v >>> 48) & 0xFF);
		write((int)(v >>> 40) & 0xFF);
		write((int)(v >>> 32) & 0xFF);
		write((int)(v >>> 24) & 0xFF);
		write((int)(v >>> 16) & 0xFF);
		write((int)(v >>> 8) & 0xFF);
		write((int)v & 0xFF);
	}

	@Override
	public void writeShort(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
		
	}

	@Override
	public void writeUTF(String s) throws IOException {
		// TODO TODO TODO
		throw new UnsupportedOperationException("not implemented yet!");
	}
	
	/**
	 * Reads one byte, and returns cast as an int.
	 * @return one byte from the underlying channel
	 * @throws IOException
	 */
	public int read() throws IOException {
		return channel.read();
	}
	
	private int readDetectEOF() throws IOException {
		int v = read();
		if (v==-1) {
			throw new EOFException();
		}
		return v;
	}

	@Override
	public boolean readBoolean() throws IOException {
		int b = this.read();
		if (b==-1) {
			throw new EOFException();
		}
		return b!=0;
	}

	@Override
	public byte readByte() throws IOException {
		int b = this.read();
		if (b==-1) {
			throw new EOFException();
		}
		return (byte) b;
	}

	@Override
	public char readChar() throws IOException {
		int ch1 = this.read();
		int ch2 = this.read();
		if (ch1==-1 || ch2==-1) {
			throw new EOFException();
		}
		return (char)((ch1<<8) + ch2);
	}

	@Override
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	@Override
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}
	
	/**
	 * Bulk read bytes from channel into dst.
	 * 
	 * @param dst The destination byte array
	 * 
	 * @param offset The offset within dst to start reading
	 * 
	 * @param length The number of bytes to read
	 * 
	 * @throws IOException
	 */
	public void read(byte[] dst, int offset, int length) throws IOException {
		channel.readBytes(dst, offset, length);
	}

	/**
	 * Bulk read bytes from channel into dst.
	 * 
	 * @param dst The destination byte array
	 * 
	 * @throws IOException
	 */
	public void read(byte[] dst) throws IOException {
		channel.readBytes(dst, 0, dst.length);
	}

	@Override
	public void readFully(byte[] src) throws IOException {
		readFully(src, 0, src.length);
	}

	@Override
	public void readFully(byte[] src, int offset, int length) throws IOException {
		read(src,offset,length);
	}

	@Override
	public int readInt() throws IOException {
		int b1 = this.readDetectEOF();
		int b2 = this.readDetectEOF();
		int b3 = this.readDetectEOF();
		int b4 = this.readDetectEOF();
		return ((b1<<24) + (b2<<16) + (b3<<8) + b4);
	}
	
	private static String sanitizeUtf8Bom(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(UTF8_BOM.length());
        }
        return s;
    }

	@Override
	public String readLine() throws IOException {
//		boolean sanitizeBOM = getFilePointer()==0;
		StringBuilder instr = new StringBuilder();
		int c;
		while ((c=read()) != -1) {
			if (c=='\n') {
				break;
			} else if (c=='\r') {
				// advance if next char is '\n'
				long pos = getFilePointer();
				if (read()!='\n') {
					seek(pos);
				}
				break;
			} else {
				instr.append((char)c);
			}
		}
		
		if (c==-1 && instr.length()==0) {
			return null;
		}
		
		String ret = instr.toString();
//		if (sanitizeBOM) {
		return sanitizeUtf8Bom(ret);
//		}
//		return ret;
	}

	@Override
	public long readLong() throws IOException {
		return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}

	@Override
	public short readShort() throws IOException {
		int b1 = this.readDetectEOF();
		int b2 = this.readDetectEOF();
		return (short)((b1<<8) + b2);
	}

	@Override
	public String readUTF() throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not implemented yet!");
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return readDetectEOF();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		int b1 = this.readDetectEOF();
		int b2 = this.readDetectEOF();
		return (b1<<8) + b2;
	}

	@Override
	public int skipBytes(int n) throws IOException {
		if (n <= 0) {
			return 0;
		}
		
		long pos = getFilePointer();
		long newpos = Math.min(pos + n, length());
		
		seek(newpos);
		
		return (int)(newpos - pos);
	}
	
	/**
	 * Seeks to position <tt>pos</tt> within the file.
	 * @param pos The position to which to seek
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		channel.position(pos);
	}
	
	/**
	 * @return The current position in the file
	 */
	public long getFilePointer() {
		return channel.position();
	}
	
	/**
	 * @return The current length of the file
	 */
	public long length() {
		return channel.size();
	}

}
