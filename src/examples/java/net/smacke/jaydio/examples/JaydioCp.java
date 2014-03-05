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
package net.smacke.jaydio.examples;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import net.smacke.jaydio.DirectRandomAccessFile;

public class JaydioCp {
	
	public static void main(String[] args) throws IOException {
		
		int bufferSize = 
				// use 8 MiB buffers by default
				args.length >= 3 ? Integer.parseInt(args[3]) : (1<<23);
		
		byte[] buf = new byte[bufferSize];

		DirectRandomAccessFile fin = 
				new DirectRandomAccessFile(new File(args[0]), "r", bufferSize);
		
		DirectRandomAccessFile fout =
				new DirectRandomAccessFile(new File(args[1]), "rw", bufferSize);
		
		while (fin.getFilePointer() < fin.length()) {
			int remaining = (int)Math.min(bufferSize, fin.length()-fin.getFilePointer());
			fin.read(buf,0,remaining);
			fout.write(buf,0,remaining);
		}
		
		fin.close();
		fout.close();
	}
}
