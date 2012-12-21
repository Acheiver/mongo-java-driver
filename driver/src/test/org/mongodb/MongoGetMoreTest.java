/*
 * Copyright (c) 2008 - 2012 10gen, Inc. <http://10gen.com>
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
 *
 */

package org.mongodb;

import org.bson.types.Document;
import org.junit.Test;
import org.mongodb.operation.MongoFind;
import org.mongodb.operation.MongoInsert;
import org.mongodb.operation.MongoKillCursor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MongoGetMoreTest extends MongoClientTestBase {
    @Test
    public void shouldThrowCursorNotFoundException() {
        collection.insert(new MongoInsert<Document>(new Document()));
        collection.insert(new MongoInsert<Document>(new Document()));
        collection.insert(new MongoInsert<Document>(new Document()));

        MongoCursor<Document> cursor = collection.find(new MongoFind().batchSize(2));
        getClient().getOperations().killCursors(new MongoKillCursor(cursor.getCursorId()));
        cursor.next();
        cursor.next();
        try {
            cursor.next();
            fail("Should throw exception");
        } catch (MongoCursorNotFoundException e) {
            assertEquals(cursor.getCursorId(), e.getCursorId());
        }
    }
}