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

package org.apache.inlong.sort.iceberg.sink;

import com.codahale.metrics.SlidingWindowReservoir;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.dropwizard.metrics.DropwizardHistogramWrapper;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.metrics.MetricGroup;
import org.apache.iceberg.io.WriteResult;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.SinkExactlyMetric;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Copy from iceberg-flink:iceberg-flink-1.15:1.3.1
 */
@Slf4j
class IcebergStreamWriterMetrics {

    // 1,024 reservoir size should cost about 8KB, which is quite small.
    // It should also produce good accuracy for histogram distribution (like percentiles).
    private static final int HISTOGRAM_RESERVOIR_SIZE = 1024;

    private final MetricGroup metrics;
    private SinkExactlyMetric sinkExactlyMetric;

    private final Counter flushedDataFiles;
    private final Counter flushedDeleteFiles;
    private final Counter flushedReferencedDataFiles;
    private final AtomicLong lastFlushDurationMs;
    private final Histogram dataFilesSizeHistogram;
    private final Histogram deleteFilesSizeHistogram;

    IcebergStreamWriterMetrics(MetricGroup metrics, String fullTableName) {
        this.metrics = metrics;
        MetricGroup writerMetrics =
                metrics.addGroup("IcebergStreamWriter").addGroup("table", fullTableName);
        this.flushedDataFiles = writerMetrics.counter("flushedDataFiles");
        this.flushedDeleteFiles = writerMetrics.counter("flushedDeleteFiles");
        this.flushedReferencedDataFiles = writerMetrics.counter("flushedReferencedDataFiles");
        this.lastFlushDurationMs = new AtomicLong();
        writerMetrics.gauge("lastFlushDurationMs", lastFlushDurationMs::get);

        com.codahale.metrics.Histogram dropwizardDataFilesSizeHistogram =
                new com.codahale.metrics.Histogram(new SlidingWindowReservoir(HISTOGRAM_RESERVOIR_SIZE));
        this.dataFilesSizeHistogram =
                writerMetrics.histogram(
                        "dataFilesSizeHistogram",
                        new DropwizardHistogramWrapper(dropwizardDataFilesSizeHistogram));
        com.codahale.metrics.Histogram dropwizardDeleteFilesSizeHistogram =
                new com.codahale.metrics.Histogram(new SlidingWindowReservoir(HISTOGRAM_RESERVOIR_SIZE));
        this.deleteFilesSizeHistogram =
                writerMetrics.histogram(
                        "deleteFilesSizeHistogram",
                        new DropwizardHistogramWrapper(dropwizardDeleteFilesSizeHistogram));
    }

    public void registerMetrics(MetricOption metricOption) {
        if (metricOption != null) {
            sinkExactlyMetric = new SinkExactlyMetric(metricOption, metrics);
        } else {
            log.warn("failed to init sourceMetricData since the metricOption is null");
        }
    }

    void updateFlushResult(WriteResult result) {
        flushedDataFiles.inc(result.dataFiles().length);
        flushedDeleteFiles.inc(result.deleteFiles().length);
        flushedReferencedDataFiles.inc(result.referencedDataFiles().length);

        // For file size distribution histogram, we don't have to update them after successful commits.
        // This should works equally well and we avoided the overhead of tracking the list of file sizes
        // in the {@link CommitSummary}, which currently stores simple stats for counters and gauges
        // metrics.
        Arrays.stream(result.dataFiles())
                .forEach(
                        dataFile -> {
                            dataFilesSizeHistogram.update(dataFile.fileSizeInBytes());
                        });
        Arrays.stream(result.deleteFiles())
                .forEach(
                        deleteFile -> {
                            deleteFilesSizeHistogram.update(deleteFile.fileSizeInBytes());
                        });
    }

    void flushDuration(long flushDurationMs) {
        lastFlushDurationMs.set(flushDurationMs);
    }

    void outputMetricsWithEstimate(int size, long time) {
        if (sinkExactlyMetric != null) {
            sinkExactlyMetric.invokeWithId(1, size, time);
        }
    }

    void flushAudit() {
        if (sinkExactlyMetric != null) {
            sinkExactlyMetric.flushAudit();
        }
    }

    void updateCurrentCheckpointId(long checkpointId) {
        if (sinkExactlyMetric != null) {
            sinkExactlyMetric.updateCurrentCheckpointId(checkpointId);
        }
    }

    void updateLastCheckpointId(long checkpointId) {
        if (sinkExactlyMetric != null) {
            sinkExactlyMetric.updateLastCheckpointId(checkpointId);
        }
    }
}
