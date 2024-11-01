/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2022 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.network.internal;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utilities related to SSL/TLS protocols. */
public final class TlsUtils {

    public static final String SSL_V2_HELLO = "SSLv2Hello";
    public static final String SSL_V3 = "SSLv3";
    public static final String TLS_V1 = "TLSv1";
    public static final String TLS_V1_1 = "TLSv1.1";
    public static final String TLS_V1_2 = "TLSv1.2";
    public static final String TLS_V1_3 = "TLSv1.3";

    public static final String APPLICATION_PROTOCOL_HTTP_1_1 = "http/1.1";
    public static final String APPLICATION_PROTOCOL_HTTP_2 = "h2";

    private static final Logger LOGGER = LogManager.getLogger(TlsUtils.class);

    private static final String PROTOCOL_TLS = "TLS";
    private static final String FILTER_TLS = "SSLContext.TLS";

    /**
     * Try to use something if not able to read the supported protocols.
     *
     * <p>Older versions are disabled by default in newer JREs, so try with a newer protocol
     * version.
     *
     * @see <a
     *     href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8202343">JDK-8202343</a>
     */
    private static final List<String> FALLBACK_TLS_PROTOCOLS = Arrays.asList(TLS_V1_2);

    private static final List<String> SUPPORTED_TLS_PROTOCOLS = readSupportedTlsProtocols();

    private static final List<String> SUPPORTED_APPLICATION_PROTOCOLS =
            List.of(APPLICATION_PROTOCOL_HTTP_1_1, APPLICATION_PROTOCOL_HTTP_2);

    private static List<String> readSupportedTlsProtocols() {
        Provider[] tlsProviders = Security.getProviders(FILTER_TLS);
        if (tlsProviders == null) {
            LOGGER.warn(
                    "No security providers for {}, ZAP will not be able to establish TLS/SSL connections.",
                    FILTER_TLS);
            return List.of();
        }

        LOGGER.debug("Reading supported SSL/TLS protocols...");
        List<String> tempSupportedProtocols;
        try {
            SSLContext sslContext = SSLContext.getInstance(PROTOCOL_TLS);
            sslContext.init(null, null, null);
            tempSupportedProtocols =
                    Arrays.asList(sslContext.getDefaultSSLParameters().getProtocols());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error(
                    "Failed to read the SSL/TLS supported protocols (Available Providers: {}). Using fallback protocol versions: {}",
                    Stream.of(tlsProviders).map(Provider::getName).collect(Collectors.joining(",")),
                    FALLBACK_TLS_PROTOCOLS,
                    e);
            tempSupportedProtocols = FALLBACK_TLS_PROTOCOLS;
        }
        Collections.sort(tempSupportedProtocols);
        LOGGER.info("Using supported SSL/TLS protocols: {}", tempSupportedProtocols);
        return Collections.unmodifiableList(tempSupportedProtocols);
    }

    private TlsUtils() {}

    /**
     * Gets the SSL/TLS protocols that are supported (and enabled) by the JRE.
     *
     * @return the protocols.
     */
    public static List<String> getSupportedTlsProtocols() {
        return SUPPORTED_TLS_PROTOCOLS;
    }

    /**
     * Gets the application protocols that are supported by ZAP.
     *
     * @return the protocols.
     */
    public static List<String> getSupportedApplicationProtocols() {
        return SUPPORTED_APPLICATION_PROTOCOLS;
    }

    /**
     * Filters the unsupported SSL/TLS protocols from the given list.
     *
     * @param protocols the protocols to filter.
     * @return the filtered protocols.
     * @throws IllegalArgumentException if the given list is {@code null} or empty and if no
     *     protocols are supported or not in a valid configuration.
     */
    public static List<String> filterUnsupportedTlsProtocols(List<String> protocols) {
        List<String> enabledSupportedProtocols = filter(protocols, SUPPORTED_TLS_PROTOCOLS);

        if (enabledSupportedProtocols.size() == 1
                && enabledSupportedProtocols.contains(SSL_V2_HELLO)) {
            throw new IllegalArgumentException(
                    "Only SSLv2Hello set, must have at least one SSL/TLS version enabled.");
        }

        return enabledSupportedProtocols;
    }

    private static List<String> filter(List<String> protocols, List<String> supportedProtocols) {
        if (protocols == null || protocols.isEmpty()) {
            throw new IllegalArgumentException("Protocol(s) required but no protocol set.");
        }

        List<String> filteredProtocols =
                protocols.stream()
                        .filter(Objects::nonNull)
                        .filter(supportedProtocols::contains)
                        .collect(Collectors.toUnmodifiableList());

        if (filteredProtocols.isEmpty()) {
            throw new IllegalArgumentException("No supported protocol(s) set.");
        }

        return filteredProtocols;
    }

    /**
     * Filters the unsupported application protocols from the given list.
     *
     * @param protocols the protocols to filter.
     * @return the filtered protocols.
     * @throws IllegalArgumentException if the given list is {@code null} or empty and if no
     *     protocols are supported.
     */
    public static List<String> filterUnsupportedApplicationProtocols(List<String> protocols) {
        return filter(protocols, SUPPORTED_APPLICATION_PROTOCOLS);
    }
}
