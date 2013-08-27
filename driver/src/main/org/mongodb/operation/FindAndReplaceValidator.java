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

package org.mongodb.operation;

import org.mongodb.Document;
import org.mongodb.codecs.validators.Validator;

import static java.lang.String.format;

public class FindAndReplaceValidator<T> implements Validator<T> {
    @Override
    public void validate(final T value) {
        if (value instanceof Document){
            for (String key : ((Document) value).keySet()) {
                if (key.startsWith("$")) {
                    throw new IllegalArgumentException(format("Can't use update operators (beginning with '$') in a find and replace "
                                                              + "operation (Bad Key: '%s')", value));
                }
            }
        }
    }
}
