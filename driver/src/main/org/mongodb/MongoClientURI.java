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

package org.mongodb;

import org.mongodb.connection.Tags;
import org.mongodb.diagnostics.Loggers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.mongodb.AuthenticationMechanism.GSSAPI;
import static org.mongodb.AuthenticationMechanism.MONGODB_CR;
import static org.mongodb.AuthenticationMechanism.MONGODB_X509;
import static org.mongodb.AuthenticationMechanism.PLAIN;


/**
 * Represents a <a href="http://www.mongodb.org/display/DOCS/Connections">URI</a>
 * which can be used to create a MongoClient instance. The URI describes the hosts to
 * be used and options.
 * <p>The format of the URI is:
 * <pre>
 *   mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
 * </pre>
 * <ul>
 * <li>{@code mongodb://} is a required prefix to identify that this is a string in the standard connection format.</li>
 * <li>{@code username:password@} are optional.  If given, the driver will attempt to login to a database after
 * connecting to a database server.  For some authentication mechanisms, only the username is specified and the password is not,
 * in which case the ":" after the username is left off as well</li>
 * <li>{@code host1} is the only required part of the URI.  It identifies a server address to connect to.</li>
 * <li>{@code :portX} is optional and defaults to :27017 if not provided.</li>
 * <li>{@code /database} is the name of the database to login to and thus is only relevant if the
 * {@code username:password@} syntax is used. If not specified the "admin" database will be used by default.</li>
 * <li>{@code ?options} are connection options. Note that if {@code database} is absent there is still a {@code /}
 * required between the last host and the {@code ?} introducing the options. Options are name=value pairs and the pairs
 * are separated by "&amp;". For backwards compatibility, ";" is accepted as a separator in addition to "&amp;",
 * but should be considered as deprecated.</li>
 * </ul>
 * <p>
 * The following options are supported (case insensitive):
 * <p>
 * Replica set configuration:
 * </p>
 * <ul>
 * <li>{@code replicaSet=name}: Implies that the hosts given are a seed list, and the driver will attempt to find
 * all members of the set.</li>
 * </ul>
 * <p>Connection Configuration:</p>
 * <ul>
 * <li>{@code ssl=true|false}: Whether to connect using SSL.</li>
 * <li>{@code connectTimeoutMS=ms}: How long a connection can take to be opened before timing out.</li>
 * <li>{@code socketTimeoutMS=ms}: How long a send or receive on a socket can take before timing out.</li>
 * <li>{@code maxIdleTimeMS=ms}: Maximum idle time of a pooled connection. A connection that exceeds this limit will be closed</li>
 * <li>{@code maxLifeTimeMS=ms}: Maximum life time of a pooled connection. A connection that exceeds this limit will be closed</li>
 * </ul>
 * <p>Connection pool configuration:</p>
 * <ul>
 * <li>{@code maxPoolSize=n}: The maximum number of connections in the connection pool.</li>
 * <li>{@code waitQueueMultiple=n} : this multiplier, multiplied with the maxPoolSize setting, gives the maximum number of
 * threads that may be waiting for a connection to become available from the pool.  All further threads will get an
 * exception right away.</li>
 * <li>{@code waitQueueTimeoutMS=ms}: The maximum wait time in milliseconds that a thread may wait for a connection to
 * become available.</li>
 * </ul>
 * <p>Write concern configuration:</p>
 * <ul>
 * <li>{@code safe=true|false}
 * <ul>
 * <li>{@code true}: the driver sends a getLastError command after every update to ensure that the update succeeded
 * (see also {@code w} and {@code wtimeoutMS}).</li>
 * <li>{@code false}: the driver does not send a getLastError command after every update.</li>
 * </ul>
 * </li>
 * <li>{@code w=wValue}
 * <ul>
 * <li>The driver adds { w : wValue } to the getLastError command. Implies {@code safe=true}.</li>
 * <li>wValue is typically a number, but can be any string in order to allow for specifications like
 * {@code "majority"}</li>
 * </ul>
 * </li>
 * <li>{@code wtimeoutMS=ms}
 * <ul>
 * <li>The driver adds { wtimeout : ms } to the getlasterror command. Implies {@code safe=true}.</li>
 * <li>Used in combination with {@code w}</li>
 * </ul>
 * </li>
 * </ul>
 * <p>Read preference configuration:</p>
 * <ul>
 * <li>{@code slaveOk=true|false}: Whether a driver connected to a replica set will send reads to slaves/secondaries.</li>
 * <li>{@code readPreference=enum}: The read preference for this connection.  If set, it overrides any slaveOk value.
 * <ul>
 * <li>Enumerated values:
 * <ul>
 * <li>{@code primary}</li>
 * <li>{@code primaryPreferred}</li>
 * <li>{@code secondary}</li>
 * <li>{@code secondaryPreferred}</li>
 * <li>{@code nearest}</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li>{@code readPreferenceTags=string}.  A representation of a tag set as a comma-separated list of colon-separated
 * key-value pairs, e.g. {@code "dc:ny,rack:1}".  Spaces are stripped from beginning and end of all keys and values.
 * To specify a list of tag sets, using multiple readPreferenceTags,
 * e.g. {@code readPreferenceTags=dc:ny,rack:1;readPreferenceTags=dc:ny;readPreferenceTags=}
 * <ul>
 * <li>Note the empty value for the last one, which means match any secondary as a last resort.</li>
 * <li>Order matters when using multiple readPreferenceTags.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>Authentication configuration:</p>
 * <ul>
 * <li>{@code authMechanism=MONGO-CR|GSSAPI|PLAIN}: The authentication mechanism to use if a credential was supplied.
 * The default is MONGODB-CR, which is the native MongoDB Challenge Response mechanism.  For the GSSAPI mechanism, no password is accepted,
 * only the username.
 * </li>
 * <li>{@code authSource=string}: The source of the authentication credentials.  This is typically the database that
 * the credentials have been created.  The value defaults to the database specified in the path portion of the URI.
 * If the database is specified in neither place, the default value is "admin".  This option is only respected when using the MONGO-CR
 * mechanism (the default).
 * </li>
 * <ul>
 * <p>
 *
 * @mongodb.driver.manual reference/connection-string Connection String URI Format
 * @see MongoClientOptions for the default values for all options
 * @since 3.0.0
 */
public class MongoClientURI {

    private static final String PREFIX = "mongodb://";
    private static final String UTF_8 = "UTF-8";

    private static final Logger LOGGER = Loggers.getLogger("uri");

    private final MongoClientOptions options;
    private final MongoCredential credentials;
    private final List<String> hosts;
    private final String database;
    private final String collection;
    private final String uri;

    /**
     * Creates a MongoURI from the given string.
     *
     * @param uri the URI
     */
    public MongoClientURI(final String uri) {
        this(uri, new MongoClientOptions.Builder());
    }

    /**
     * Creates a MongoURI from the given URI string, and MongoClientOptions.Builder.  The builder can be configured
     * with default options, which may be overridden by options specified in the URI string.
     *
     * @param uri     the URI
     * @param builder a Builder
     * @see org.mongodb.MongoClientURI#getOptions()
     * @since 2.11.0
     */
    public MongoClientURI(final String uri, final MongoClientOptions.Builder builder) {
        try {
            if (!uri.startsWith(PREFIX)) {
                throw new IllegalArgumentException("uri needs to start with " + PREFIX);
            }

            this.uri = uri;

            String unprefixedURI = uri.substring(PREFIX.length());

            String serverPart;
            String nsPart;
            String optionsPart;
            String userName = null;
            char[] password = null;

            int idx = unprefixedURI.lastIndexOf("/");
            if (idx < 0) {
                if (unprefixedURI.contains("?")) {
                    throw new IllegalArgumentException("URI contains options without trailing slash");
                }
                serverPart = unprefixedURI;
                nsPart = null;
                optionsPart = "";
            }
            else {
                serverPart = unprefixedURI.substring(0, idx);
                nsPart = unprefixedURI.substring(idx + 1);

                idx = nsPart.indexOf("?");
                if (idx >= 0) {
                    optionsPart = nsPart.substring(idx + 1);
                    nsPart = nsPart.substring(0, idx);
                }
                else {
                    optionsPart = "";
                }

            }
            List<String> all = new LinkedList<String>();

            idx = serverPart.indexOf("@");

            if (idx > 0) {
                String authPart = serverPart.substring(0, idx);
                serverPart = serverPart.substring(idx + 1);

                idx = authPart.indexOf(":");
                if (idx == -1) {
                    userName = URLDecoder.decode(authPart, UTF_8);
                }
                else {
                    userName = URLDecoder.decode(authPart.substring(0, idx), UTF_8);
                    password = URLDecoder.decode(authPart.substring(idx + 1), UTF_8).toCharArray();
                }
            }

            Collections.addAll(all, serverPart.split(","));

            hosts = Collections.unmodifiableList(all);

            if (nsPart != null && !nsPart.isEmpty()) { // database,_collection
                idx = nsPart.indexOf(".");
                if (idx < 0) {
                    database = nsPart;
                    collection = null;
                }
                else {
                    database = nsPart.substring(0, idx);
                    collection = nsPart.substring(idx + 1);
                }
            }
            else {
                database = null;
                collection = null;
            }

            Map<String, List<String>> optionsMap = parseOptions(optionsPart);
            options = createOptions(optionsMap, builder);
            credentials = createCredentials(optionsMap, userName, password);
            warnOnUnsupportedOptions(optionsMap);
        } catch (UnsupportedEncodingException e) {
            throw new MongoInternalException("This should not happen", e);
        }
    }

    private static Set<String> generalOptionsKeys = new HashSet<String>();
    private static Set<String> authKeys = new HashSet<String>();
    private static Set<String> readPreferenceKeys = new HashSet<String>();
    private static Set<String> writeConcernKeys = new HashSet<String>();
    private static Set<String> allKeys = new HashSet<String>();

    static {
        generalOptionsKeys.add("minpoolsize");
        generalOptionsKeys.add("maxpoolsize");
        generalOptionsKeys.add("waitqueuemultiple");
        generalOptionsKeys.add("waitqueuetimeoutms");
        generalOptionsKeys.add("connecttimeoutms");
        generalOptionsKeys.add("maxidletimems");
        generalOptionsKeys.add("maxlifetimems");
        generalOptionsKeys.add("sockettimeoutms");
        generalOptionsKeys.add("sockettimeoutms");
        generalOptionsKeys.add("autoconnectretry");
        generalOptionsKeys.add("ssl");
        generalOptionsKeys.add("replicaset");

        readPreferenceKeys.add("slaveok");
        readPreferenceKeys.add("readpreference");
        readPreferenceKeys.add("readpreferencetags");

        writeConcernKeys.add("safe");
        writeConcernKeys.add("w");
        writeConcernKeys.add("wtimeout");
        writeConcernKeys.add("fsync");
        writeConcernKeys.add("j");

        authKeys.add("authmechanism");
        authKeys.add("authsource");

        allKeys.addAll(generalOptionsKeys);
        allKeys.addAll(authKeys);
        allKeys.addAll(readPreferenceKeys);
        allKeys.addAll(writeConcernKeys);
    }

    private void warnOnUnsupportedOptions(final Map<String, List<String>> optionsMap) {
        for (String key : optionsMap.keySet()) {
            if (!allKeys.contains(key)) {
                LOGGER.warning("Unknown or Unsupported Option '" + key + "'");
            }
        }
    }

    private MongoClientOptions createOptions(final Map<String, List<String>> optionsMap, final MongoClientOptions.Builder builder) {
        for (String key : generalOptionsKeys) {
            String value = getLastValue(optionsMap, key);
            if (value == null) {
                continue;
            }

            if (key.equals("maxpoolsize")) {
                builder.maxConnectionPoolSize(Integer.parseInt(value));
            }
            else if (key.equals("minpoolsize")) {
                builder.minConnectionPoolSize(Integer.parseInt(value));
            }
            else if (key.equals("maxidletimems")) {
                builder.maxConnectionIdleTime(Integer.parseInt(value));
            }
            else if (key.equals("maxlifetimems")) {
                builder.maxConnectionLifeTime(Integer.parseInt(value));
            }
            else if (key.equals("waitqueuemultiple")) {
                builder.threadsAllowedToBlockForConnectionMultiplier(Integer.parseInt(value));
            }
            else if (key.equals("waitqueuetimeoutms")) {
                builder.maxWaitTime(Integer.parseInt(value));
            }
            else if (key.equals("connecttimeoutms")) {
                builder.connectTimeout(Integer.parseInt(value));
            }
            else if (key.equals("sockettimeoutms")) {
                builder.socketTimeout(Integer.parseInt(value));
            }
            else if (key.equals("autoconnectretry")) {
                builder.autoConnectRetry(parseBoolean(value));
            }
            else if (key.equals("ssl") && parseBoolean(value)) {
                builder.SSLEnabled(true);
            }
            else if (key.equals("replicaset")) {
                builder.requiredReplicaSetName(value);
            }
        }

        WriteConcern writeConcern = createWriteConcern(optionsMap);
        ReadPreference readPreference = createReadPreference(optionsMap);

        if (writeConcern != null) {
            builder.writeConcern(writeConcern);
        }
        if (readPreference != null) {
            builder.readPreference(readPreference);
        }

        return builder.build();
    }

    private WriteConcern createWriteConcern(final Map<String, List<String>> optionsMap) {
        Boolean safe = null;
        String w = null;
        int wTimeout = 0;
        boolean fsync = false;
        boolean journal = false;

        for (String key : writeConcernKeys) {
            String value = getLastValue(optionsMap, key);
            if (value == null) {
                continue;
            }

            if (key.equals("safe")) {
                safe = parseBoolean(value);
            }
            else if (key.equals("w")) {
                w = value;
            }
            else if (key.equals("wtimeout")) {
                wTimeout = Integer.parseInt(value);
            }
            else if (key.equals("fsync")) {
                fsync = parseBoolean(value);
            }
            else if (key.equals("j")) {
                journal = parseBoolean(value);
            }
        }
        return buildWriteConcern(safe, w, wTimeout, fsync, journal);
    }

    private ReadPreference createReadPreference(final Map<String, List<String>> optionsMap) {
        Boolean slaveOk = null;
        String readPreferenceType = null;
        List<Tags> tagsList = new ArrayList<Tags>();

        for (String key : readPreferenceKeys) {
            String value = getLastValue(optionsMap, key);
            if (value == null) {
                continue;
            }

            if (key.equals("slaveok")) {
                slaveOk = parseBoolean(value);
            }
            else if (key.equals("readpreference")) {
                readPreferenceType = value;
            }
            else if (key.equals("readpreferencetags")) {
                for (String cur : optionsMap.get(key)) {
                    Tags tags = getTags(cur.trim());
                    tagsList.add(tags);
                }
            }
        }
        return buildReadPreference(readPreferenceType, tagsList, slaveOk);
    }

    private MongoCredential createCredentials(final Map<String, List<String>> optionsMap, final String userName,
                                              final char[] password) {
        if (userName == null) {
            return null;
        }

        AuthenticationMechanism mechanism = MONGODB_CR;
        String authSource = (database == null) ? "admin" : database;

        for (String key : authKeys) {
            String value = getLastValue(optionsMap, key);

            if (value == null) {
                continue;
            }

            if (key.equals("authmechanism")) {
                mechanism = AuthenticationMechanism.fromMechanismName(value);
            }
            else if (key.equals("authsource")) {
                authSource = value;
            }
        }

        if (mechanism == GSSAPI) {
            return MongoCredential.createGSSAPICredential(userName);
        }
        else if (mechanism == PLAIN) {
            return MongoCredential.createPlainCredential(userName, authSource, password);
        }
        else if (mechanism == MONGODB_CR) {
            return MongoCredential.createMongoCRCredential(userName, authSource, password);
        }
        else if (mechanism == MONGODB_X509) {
            return MongoCredential.createMongoX509Credential(userName);
        }
        else {
            throw new UnsupportedOperationException("Unsupported authentication mechanism in the URI: " + mechanism);
        }
    }

    private String getLastValue(final Map<String, List<String>> optionsMap, final String key) {
        List<String> valueList = optionsMap.get(key);
        if (valueList == null) {
            return null;
        }
        return valueList.get(valueList.size() - 1);
    }

    private Map<String, List<String>> parseOptions(final String optionsPart) {
        Map<String, List<String>> optionsMap = new HashMap<String, List<String>>();

        for (String part : optionsPart.split("&|;")) {
            int idx = part.indexOf("=");
            if (idx >= 0) {
                String key = part.substring(0, idx).toLowerCase();
                String value = part.substring(idx + 1);
                List<String> valueList = optionsMap.get(key);
                if (valueList == null) {
                    valueList = new ArrayList<String>(1);
                }
                valueList.add(value);
                optionsMap.put(key, valueList);
            }
        }

        return optionsMap;
    }

    private ReadPreference buildReadPreference(final String readPreferenceType,
                                               final List<Tags> tagsList, final Boolean slaveOk) {
        if (readPreferenceType != null) {
            return ReadPreference.valueOf(readPreferenceType, tagsList);
        }
        else if (slaveOk != null && slaveOk.equals(Boolean.TRUE)) {
            return ReadPreference.secondaryPreferred();
        }
        return null;
    }

    private WriteConcern buildWriteConcern(final Boolean safe, final String w,
                                           final int wTimeout, final boolean fsync, final boolean journal) {
        if (w != null || wTimeout != 0 || fsync || journal) {
            if (w == null) {
                return new WriteConcern(1, wTimeout, fsync, journal);
            }
            else {
                try {
                    return new WriteConcern(Integer.parseInt(w), wTimeout, fsync, journal);
                } catch (NumberFormatException e) {
                    return new WriteConcern(w, wTimeout, fsync, journal);
                }
            }
        }
        else if (safe != null) {
            if (safe) {
                return WriteConcern.ACKNOWLEDGED;
            }
            else {
                return WriteConcern.UNACKNOWLEDGED;
            }
        }
        return null;
    }

    private Tags getTags(final String tagSetString) {
        Tags tags = new Tags();
        if (tagSetString.length() > 0) {
            for (String tag : tagSetString.split(",")) {
                String[] tagKeyValuePair = tag.split(":");
                if (tagKeyValuePair.length != 2) {
                    throw new IllegalArgumentException("Bad read preference tags: " + tagSetString);
                }
                tags.put(tagKeyValuePair[0].trim(), tagKeyValuePair[1].trim());
            }
        }
        return tags;
    }

    boolean parseBoolean(final String input) {
        final String trimmedInput = input.trim();
        return trimmedInput != null && trimmedInput.length() > 0 && (trimmedInput.equals("1")
                || trimmedInput.toLowerCase().equals("true") || trimmedInput.toLowerCase().equals("yes"));
    }

    // ---------------------------------

    /**
     * Gets the username
     *
     * @return the username
     */
    public String getUsername() {
        return credentials != null ? credentials.getUserName() : null;
    }

    /**
     * Gets the password
     *
     * @return the password
     */
    public char[] getPassword() {
        return credentials != null ? credentials.getPassword() : null;
    }

    /**
     * Gets the list of hosts
     *
     * @return the host list
     */
    public List<String> getHosts() {
        return hosts;
    }

    /**
     * Gets the database name
     *
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }


    /**
     * Gets the collection name
     *
     * @return the collection name
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Get the unparsed URI.
     *
     * @return the URI
     */
    public String getURI() {
        return uri;
    }


    /**
     * Gets the credentials.
     *
     * @return the credentials
     */
    public List<MongoCredential> getCredentialList() {
        return credentials != null ? Arrays.asList(credentials) : new ArrayList<MongoCredential>();
    }

    /**
     * Gets the options
     *
     * @return the MongoClientOptions based on this URI.
     */
    public MongoClientOptions getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return uri;
    }
}
