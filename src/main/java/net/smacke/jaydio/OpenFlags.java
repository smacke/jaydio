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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Constants for {@link DirectIoLib#oDirectOpen(String, boolean)}. </p>
 *
 * @author smacke
 *
 */
public final class OpenFlags {
    public static final int O_RDONLY = getOpenFlags("O_RDONLY");
    public static final int O_WRONLY = getOpenFlags("O_WRONLY");
    public static final int O_RDWR = getOpenFlags("O_RDWR");
    public static final int O_CREAT = getOpenFlags("O_CREAT");
    public static final int O_TRUNC = getOpenFlags("O_TRUNC");
    public static final int O_DIRECT = getOpenFlags("O_DIRECT");
    public static final int O_SYNC = getOpenFlags("O_SYNC");

    private OpenFlags() {}

    private static int getOpenFlags(String s) {
        try {
            Process pb = new ProcessBuilder("/bin/sh", "-c", "printf \"%d\" $(printf '#include<fcntl.h>\\n" + s + "' | gcc -D_GNU_SOURCE -E - | tail -n1)").start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(pb.getInputStream(), StandardCharsets.UTF_8))) {
                return Integer.parseInt(br.readLine());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
