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

package org.apache.inlong.tubemq.manager.repository;

import org.apache.inlong.tubemq.manager.entry.ClusterEntry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<ClusterEntry, Long> {

    /**
     * find clusterEntry by clusterId
     *
     * @param clusterId
     * @return
     */
    ClusterEntry findClusterEntryByClusterId(long clusterId);

    /**
     * delete cluster by cluster id
     *
     * @param clusterId
     * @return
     */
    Integer deleteByClusterId(Long clusterId);

    /**
     * find clusterEntry by clusterName
     * @param clusterName
     * @return
     */
    ClusterEntry findClusterEntryByClusterName(String clusterName);
}
