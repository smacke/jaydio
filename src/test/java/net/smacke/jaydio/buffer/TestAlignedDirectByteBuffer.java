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

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.AlignedDirectByteBuffer;

import org.junit.BeforeClass;

/**
 * Concrete {@link AbstractBufferTester} used to test
 * {@link AlignedDirectByteBuffer}. This requires access to native
 * memory and will only work on Linux. </p>
 * 
 * @author smacke
 *
 */
public class TestAlignedDirectByteBuffer extends AbstractBufferTester {
	
	private static DirectIoLib lib;

    @BeforeClass public static void setupClass() {
        lib = DirectIoLib.getLibForPath(System.getProperty("java.io.tmpdir"));
    }

	@Override
	protected AlignedDirectByteBuffer createInstance() {
        return AlignedDirectByteBuffer.allocate(lib, lib.defaultBufferSize());
	}
}
