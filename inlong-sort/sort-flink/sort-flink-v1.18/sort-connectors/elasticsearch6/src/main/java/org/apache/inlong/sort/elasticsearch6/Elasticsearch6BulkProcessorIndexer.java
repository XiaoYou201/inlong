/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.elasticsearch6;

import org.apache.inlong.sort.elasticsearch.RequestIndexer;

import org.apache.flink.annotation.Internal;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

import java.util.concurrent.atomic.AtomicLong;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Implementation of a {@link RequestIndexer}, using a {@link BulkProcessor}. {@link ActionRequest
 * ActionRequests} will be buffered before sending a bulk request to the Elasticsearch cluster.
 *
 * <p>Note: This class is binary compatible to Elasticsearch 6.
 * Modify from {@link org.apache.flink.streaming.connectors.elasticsearch6.Elasticsearch6BulkProcessorIndexer}
 */
@Internal
class Elasticsearch6BulkProcessorIndexer implements RequestIndexer {

    private final BulkProcessor bulkProcessor;
    private final boolean flushOnCheckpoint;
    private final AtomicLong numPendingRequestsRef;

    Elasticsearch6BulkProcessorIndexer(
            BulkProcessor bulkProcessor,
            boolean flushOnCheckpoint,
            AtomicLong numPendingRequestsRef) {
        this.bulkProcessor = checkNotNull(bulkProcessor);
        this.flushOnCheckpoint = flushOnCheckpoint;
        this.numPendingRequestsRef = checkNotNull(numPendingRequestsRef);
    }

    @Override
    public void add(DeleteRequest... deleteRequests) {
        for (DeleteRequest deleteRequest : deleteRequests) {
            if (flushOnCheckpoint) {
                numPendingRequestsRef.getAndIncrement();
            }
            this.bulkProcessor.add(deleteRequest);
        }
    }

    @Override
    public void add(IndexRequest... indexRequests) {
        for (IndexRequest indexRequest : indexRequests) {
            if (flushOnCheckpoint) {
                numPendingRequestsRef.getAndIncrement();
            }
            this.bulkProcessor.add(indexRequest);
        }
    }

    @Override
    public void add(UpdateRequest... updateRequests) {
        for (UpdateRequest updateRequest : updateRequests) {
            if (flushOnCheckpoint) {
                numPendingRequestsRef.getAndIncrement();
            }
            this.bulkProcessor.add(updateRequest);
        }
    }
}
