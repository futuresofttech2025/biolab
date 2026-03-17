package com.biolab.auth.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TLS / Encryption-in-Transit configuration.
 *
 * <h3>Encryption in Transit — layered approach</h3>
 * <ol>
 *   <li><strong>External (Internet → API Gateway):</strong>
 *       Handled by Nginx/AWS ALB. Spring Boot only sees decrypted traffic.
 *       The {@code SecurityHeadersFilter} adds {@code Strict-Transport-Security}
 *       so clients enforce HTTPS for future requests.</li>
 *
 *   <li><strong>Internal (service-to-service mTLS):</strong>
 *       Enabled when {@code SERVER_SSL_ENABLED=true}. This bean adds an HTTP
 *       connector on {@code app.tls.http-port} that redirects all traffic to
 *       the HTTPS port, preventing accidental plain-text internal calls.</li>
 * </ol>
 *
 * <h3>Activation</h3>
 * <pre>
 *   SERVER_SSL_ENABLED=true
 *   KEYSTORE_PATH=/run/secrets/biolab-auth.p12
 *   KEYSTORE_PASSWORD=&lt;secret&gt;
 *   KEY_ALIAS=biolab-auth
 * </pre>
 *
 * <h3>Certificate generation (dev)</h3>
 * <pre>
 *   keytool -genkeypair -alias biolab-auth -keyalg RSA -keysize 2048 \
 *     -validity 365 -keystore src/main/resources/keystore/biolab-auth-dev.p12 \
 *     -storetype PKCS12 -dname "CN=localhost,O=BioLabs,C=US"
 * </pre>
 *
 * @author BioLab Engineering Team
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class TlsConfig implements WebMvcConfigurer {

    @Value("${server.port:8081}")
    private int httpsPort;

    @Value("${app.tls.http-port:8080}")
    private int httpRedirectPort;

    /**
     * Adds a plain HTTP connector that immediately redirects to HTTPS.
     * Only active when {@code server.ssl.enabled=true}.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpToHttpsRedirect() {
        return factory -> factory.addAdditionalTomcatConnectors(httpConnector());
    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpRedirectPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
