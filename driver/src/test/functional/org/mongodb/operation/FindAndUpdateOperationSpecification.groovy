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

package org.mongodb.operation

import org.mongodb.Document
import org.mongodb.FunctionalSpecification
import org.mongodb.MongoClientOptions
import org.mongodb.MongoCollection
import org.mongodb.connection.Cluster
import org.mongodb.connection.ClusterableServerFactory
import org.mongodb.connection.ConnectionFactory
import org.mongodb.connection.ServerAddress
import org.mongodb.connection.impl.DefaultClusterFactory
import org.mongodb.connection.impl.DefaultClusterableServerFactory
import org.mongodb.connection.impl.DefaultConnectionFactory
import org.mongodb.connection.impl.DefaultConnectionProviderFactory
import org.mongodb.session.ClusterSession
import org.mongodb.session.Session
import org.mongodb.test.Worker
import org.mongodb.test.WorkerCodec

import static java.util.concurrent.Executors.newScheduledThreadPool
import static org.mongodb.Fixture.getBufferProvider
import static org.mongodb.Fixture.getSSLSettings

class FindAndUpdateOperationSpecification extends FunctionalSpecification {
    private final MongoClientOptions options = MongoClientOptions.builder().build();
    private final ConnectionFactory connectionFactory = new DefaultConnectionFactory(options.connectionSettings,
                                                                                     getSSLSettings(), getBufferProvider(), [])

    private final ClusterableServerFactory clusterableServerFactory = new DefaultClusterableServerFactory(
            options.serverSettings, new DefaultConnectionProviderFactory(options.connectionProviderSettings, connectionFactory),
            null, connectionFactory, newScheduledThreadPool(3), getBufferProvider())

    private final Cluster cluster = new DefaultClusterFactory().create(new ServerAddress(), clusterableServerFactory)
    private final Session session = new ClusterSession(cluster)

    private MongoCollection<Worker> workerCollection

    def setup() {
        //setup with a collection designed to store Workers not Documents
        workerCollection = database.getCollection(getCollectionName(), new WorkerCodec())
    }

    def 'should be able to specify a custom encoder and have the found value return in that type'() {
        given:
        Worker pete = new Worker('Pete', 'handyman', new Date(), 3)
        Worker sam = new Worker('Sam', 'plumber', new Date(), 7)

        workerCollection.insert(pete);
        workerCollection.insert(sam);

        when:
        FindAndUpdate findAndUpdate = new FindAndUpdate<Worker>(getCollectionName()).where(new Document('name', 'Pete'))
                .updateWith(new Document('$inc', new Document('numberOfJobs', 1)))
                .returnNew(true);

        FindAndUpdateOperation<Worker> operation = new FindAndUpdateOperation<Worker>(getBufferProvider(), session, false,
                                                                                      workerCollection.namespace, findAndUpdate,
                                                                                      new WorkerCodec())
        Worker returnedValue = operation.execute()

        then:
        Worker updatedPete = new Worker(pete.id, pete.name, pete.jobTitle, pete.dateStarted, 4)
        returnedValue == updatedPete
    }

}
