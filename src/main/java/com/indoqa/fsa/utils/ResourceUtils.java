/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.fsa.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.util.stream.Stream;

public final class ResourceUtils {

    private ResourceUtils() {
        // hide utility class constructor
    }

    public static Runnable close(Closeable closeable) {
        return () -> {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static Stream<String> getLines(String resourcePath) {
        InputStream inputStream = ResourceUtils.class.getResourceAsStream(resourcePath);
        return new BufferedReader(new InputStreamReader(inputStream, UTF_8))
            .lines()
            .map(line -> line.trim())
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .onClose(ResourceUtils.close(inputStream));
    }
}
