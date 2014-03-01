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

import net.smacke.jaydio.DirectIoLib;
import net.smacke.jaydio.buffer.JaydioByteBuffer;
import net.smacke.jaydio.channel.BufferedChannel;

public class MockByteChannelAligner extends ByteChannelAligner<JaydioByteBuffer> {

	public MockByteChannelAligner(DirectIoLib lib,
			BufferedChannel<JaydioByteBuffer> channel, JaydioByteBuffer buffer) {
		super(lib, channel, buffer);
	}

}
