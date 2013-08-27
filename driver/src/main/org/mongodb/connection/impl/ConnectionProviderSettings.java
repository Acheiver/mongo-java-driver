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

package org.mongodb.connection.impl;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mongodb.assertions.Assertions.isTrue;

/**
 * @since 3.0
 */
public class ConnectionProviderSettings {
    private final int maxSize;
    private final int minSize;
    private final int maxWaitQueueSize;
    private final long maxWaitTimeMS;
    private final long maxConnectionLifeTimeMS;
    private final long maxConnectionIdleTimeMS;
    private final long maintenanceFrequencyMS;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxSize;
        private int minSize;
        private int maxWaitQueueSize;
        private long maxWaitTimeMS;
        private long maxConnectionLifeTimeMS;
        private long maxConnectionIdleTimeMS;
        private long maintenanceFrequencyMS = MILLISECONDS.convert(1, MINUTES);

        // CHECKSTYLE:OFF
        public Builder maxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder minSize(final int minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder maxWaitQueueSize(final int maxWaitQueueSize) {
            this.maxWaitQueueSize = maxWaitQueueSize;
            return this;
        }

        public Builder maxWaitTime(final long maxWaitTime, final TimeUnit timeUnit) {
            this.maxWaitTimeMS = MILLISECONDS.convert(maxWaitTime, timeUnit);
            return this;
        }

        public Builder maxConnectionLifeTime(final long maxConnectionLifeTime, final TimeUnit timeUnit) {
            this.maxConnectionLifeTimeMS = MILLISECONDS.convert(maxConnectionLifeTime, timeUnit);
            return this;
        }

        public Builder maxConnectionIdleTime(final long maxConnectionIdleTime, final TimeUnit timeUnit) {
            this.maxConnectionIdleTimeMS = MILLISECONDS.convert(maxConnectionIdleTime, timeUnit);
            return this;
        }

        public Builder maintenanceFrequency(final long maintenanceFrequency, final TimeUnit timeUnit) {
            this.maintenanceFrequencyMS = MILLISECONDS.convert(maintenanceFrequency, timeUnit);
            return this;
        }
        // CHECKSTYLE:ON

        public ConnectionProviderSettings build() {
            return new ConnectionProviderSettings(this);
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxWaitQueueSize() {
        return maxWaitQueueSize;
    }

    public long getMaxWaitTime(final TimeUnit timeUnit) {
        return timeUnit.convert(maxWaitTimeMS, MILLISECONDS);
    }

    public long getMaxConnectionLifeTime(final TimeUnit timeUnit) {
        return timeUnit.convert(maxConnectionLifeTimeMS, MILLISECONDS);
    }

    public long getMaxConnectionIdleTime(final TimeUnit timeUnit) {
        return timeUnit.convert(maxConnectionIdleTimeMS, MILLISECONDS);
    }

    public long getMaintenanceFrequency(final TimeUnit timeUnit) {
        return timeUnit.convert(maintenanceFrequencyMS, MILLISECONDS);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConnectionProviderSettings that = (ConnectionProviderSettings) o;

        if (maxConnectionIdleTimeMS != that.maxConnectionIdleTimeMS) {
            return false;
        }
        if (maxConnectionLifeTimeMS != that.maxConnectionLifeTimeMS) {
            return false;
        }
        if (maxSize != that.maxSize) {
            return false;
        }
        if (minSize != that.minSize) {
            return false;
        }
        if (maintenanceFrequencyMS != that.maintenanceFrequencyMS) {
            return false;
        }
        if (maxWaitQueueSize != that.maxWaitQueueSize) {
            return false;
        }
        if (maxWaitTimeMS != that.maxWaitTimeMS) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = maxSize;
        result = 31 * result + minSize;
        result = 31 * result + maxWaitQueueSize;
        result = 31 * result + (int) (maxWaitTimeMS ^ (maxWaitTimeMS >>> 32));
        result = 31 * result + (int) (maxConnectionLifeTimeMS ^ (maxConnectionLifeTimeMS >>> 32));
        result = 31 * result + (int) (maxConnectionIdleTimeMS ^ (maxConnectionIdleTimeMS >>> 32));
        result = 31 * result + (int) (maintenanceFrequencyMS ^ (maintenanceFrequencyMS >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ConnectionProviderSettings{"
                + "maxSize=" + maxSize
                + ", minSize=" + minSize
                + ", maxWaitQueueSize=" + maxWaitQueueSize
                + ", maxWaitTimeMS=" + maxWaitTimeMS
                + ", maxConnectionLifeTimeMS=" + maxConnectionLifeTimeMS
                + ", maxConnectionIdleTimeMS=" + maxConnectionIdleTimeMS
                + ", maintenanceFrequencyMS=" + maintenanceFrequencyMS
                + '}';
    }

    ConnectionProviderSettings(final Builder builder) {
        isTrue("maxSize > 0", builder.maxSize > 0);
        isTrue("minSize >= 0", builder.minSize >= 0);
        isTrue("maxWaitQueueSize >= 0", builder.maxWaitQueueSize >= 0);
        isTrue("maxConnectionLifeTime >= 0", builder.maxConnectionLifeTimeMS >= 0);
        isTrue("maxConnectionIdleTime >= 0", builder.maxConnectionIdleTimeMS >= 0);
        isTrue("sizeMaintenanceFrequency >= 0", builder.maintenanceFrequencyMS >= 0);
        isTrue("maxSize >= minSize", builder.maxSize >= builder.minSize);

        maxSize = builder.maxSize;
        minSize = builder.minSize;
        maxWaitQueueSize = builder.maxWaitQueueSize;
        maxWaitTimeMS = builder.maxWaitTimeMS;
        maxConnectionLifeTimeMS = builder.maxConnectionLifeTimeMS;
        maxConnectionIdleTimeMS = builder.maxConnectionIdleTimeMS;
        maintenanceFrequencyMS = builder.maintenanceFrequencyMS;
    }
}
