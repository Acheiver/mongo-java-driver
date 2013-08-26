/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.command;

import org.mongodb.Decoder;
import org.mongodb.codecs.DocumentCodec;
import org.mongodb.codecs.PrimitiveCodecs;

//TODO: think these warnings mean we've got our types all wrong
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommandResultWithPayloadDecoder<T> extends DocumentCodec {
    private final Decoder<T> decoder;

    public CommandResultWithPayloadDecoder(final PrimitiveCodecs primitiveCodecs, final Decoder<T> decoder) {
        super(primitiveCodecs);
        this.decoder = decoder;
    }

    @Override
    protected Decoder getDecoderForField(final String fieldName) {
        if (fieldName.equals("value")) {
            return decoder;
        }
        return new DocumentCodec(getPrimitiveCodecs());
    }
}
