/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.impexp;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@AtlasService
@Singleton
public class MigrationProgressService {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationProgressService.class);
    public static final String MIGRATION_QUERY_CACHE_TTL    = "atlas.migration.query.cache.ttlInSecs";

    @VisibleForTesting
    static long DEFAULT_CACHE_TTL_IN_SECS            = 30 * 1000; // 30 secs

    private final long            cacheValidity;
    private final AtlasGraph      graph;
    private       MigrationStatus cachedStatus;
    private       long            cacheExpirationTime = 0;

    @Inject
    public MigrationProgressService(Configuration configuration, AtlasGraph graph) {
        this.graph = graph;
        this.cacheValidity = (configuration != null) ?
                configuration.getLong(MIGRATION_QUERY_CACHE_TTL, DEFAULT_CACHE_TTL_IN_SECS) :
                DEFAULT_CACHE_TTL_IN_SECS;
    }

    public MigrationStatus getStatus() {
        return fetchStatus();
    }

    private MigrationStatus fetchStatus() {
        long currentTime = System.currentTimeMillis();
        if(resetCache(currentTime)) {
            cachedStatus = graph.getMigrationStatus();
        }

        return cachedStatus;
    }

    private boolean resetCache(long currentTime) {
        boolean ret = cachedStatus == null || currentTime > cacheExpirationTime;
        if(ret) {
            cacheExpirationTime = currentTime + cacheValidity;
        }

        return ret;
    }
}
