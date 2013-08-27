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

package org.mongodb.management;

import org.mongodb.management.jmx.JMXMBeanServer;

/**
 * This class is NOT part of the public API.  It may change at any time without notification.
 * <p/>
 * This class is used to insulate the rest of the driver from the possibility that JMX is not available, as currently is
 * the case on Android VM
 */
public final class MBeanServerFactory {
    private MBeanServerFactory() {
    }

    static {
        MBeanServer tmp;
        try {
            tmp = new JMXMBeanServer();
        } catch (Throwable e) {
            tmp = new NullMBeanServer();
        }

        M_BEAN_SERVER = tmp;
    }

    public static MBeanServer getMBeanServer() {
        return M_BEAN_SERVER;
    }

    private static final MBeanServer M_BEAN_SERVER;
}
