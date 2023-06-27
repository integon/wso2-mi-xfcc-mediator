package ch.integon;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class XfccMediator extends AbstractMediator {

    private static final String TRUSTSTORE_PATH = System.getenv("TRUSTSTORE_PATH");
    private static final String TRUSTSTORE_PASSWORD = System.getenv("TRUSTSTORE_PASSWORD");
    private String HEADER_NAME = System.getenv("HEADER_NAME");
    private List<String> ALLOWED_CNS = new ArrayList<>();

    public XfccMediator() {
        if (TRUSTSTORE_PATH == null || TRUSTSTORE_PATH.isEmpty()) {
            throw new IllegalStateException("TRUSTSTORE_PATH environment variable is not set");
        }

        if (TRUSTSTORE_PASSWORD == null || TRUSTSTORE_PASSWORD.isEmpty()) {
            throw new IllegalStateException("TRUSTSTORE_PASSWORD environment variable is not set");
        }

        ALLOWED_CNS = getAllowedCNs();

        if (ALLOWED_CNS.isEmpty()) {
            throw new IllegalStateException("ALLOWED_CNS environment variable is not set");
        }

        if (HEADER_NAME == null || HEADER_NAME.isEmpty()) {
            HEADER_NAME = "X-Client-Cert";
            log.info("XfccMediator: HEADER_NAME environment variable is not set, using default value: " + HEADER_NAME);
        }
    }

    public boolean mediate(final org.apache.synapse.MessageContext context) {

        final Axis2MessageContext axis2MessageContext = (Axis2MessageContext) context;

        final String clientCert = getHeaderValue(axis2MessageContext, HEADER_NAME);

        if (clientCert == null || clientCert.isEmpty()) {
            log.error("XfccMediator: missing client certificate");
            throw errorResponse(context, 401, "missing client certificate");
        }

        // base64 decode the client cert
        final byte[] decodedClientCert = java.util.Base64.getDecoder().decode(clientCert);

        try {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedClientCert);

            final X509Certificate clientCertificate = (X509Certificate) certificateFactory
                    .generateCertificate(byteArrayInputStream);

            if (clientCertificate == null) {
                log.error("XfccMediator: Client certificate is not a valid X509 certificate");
                throw errorResponse(axis2MessageContext, 401,
                        "certicate in header X-Client-Cert is not a valid X509 certificate");
            }

            if (!validateCertPath(clientCertificate, TRUSTSTORE_PATH, TRUSTSTORE_PASSWORD)) {
                log.error("XfccMediator: Client certificate validation failed invalid certificate path");
                throw errorResponse(context, 401,
                        "Client certificate validation failed invalid certificate path");
            }

            if (!validateCommonName(clientCertificate)) {
                log.error("XfccMediator: Client certificate validation failed invalid common name");
                throw errorResponse(context, 403,
                        "Client certificate validation failed invalid common name");
            }

        } catch (final SynapseException e) {
            throw e;
        } catch (final Exception e) {
            throw errorResponse(context, 500, "Client certificate validation failed");
        }

        log.info("XfccMediator: Client certificate validation successful");
        return true;

    }

    private static boolean validateCertPath(final X509Certificate clientCertificate, final String truststorePath,
            final String truststorePassword)
            throws Exception {
        // Load the custom truststore
        final KeyStore truststore = KeyStore.getInstance("JKS");
        final FileInputStream truststoreFile = new FileInputStream(truststorePath);
        truststore.load(truststoreFile, truststorePassword.toCharArray());

        // Create a TrustManagerFactory and initialize it with the truststore
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(truststore);

        // Get the X509TrustManager from the TrustManagerFactory
        final X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];

        final X509Certificate[] certs = new X509Certificate[1];

        certs[0] = clientCertificate;

        try {
            trustManager.checkClientTrusted(certs, "RSA");
        } catch (final Exception e) {
            return false;
        }

        return true;

    }

    private boolean validateCommonName(final X509Certificate clientCertificate) {
        final String commonName = clientCertificate.getSubjectX500Principal().getName().split(",")[0].split("=")[1];
        if (!ALLOWED_CNS.contains(commonName)) {
            log.error("XfccMediator: Common Name: " + commonName + " is not allowed");
            return false;
        }
        return true;
    }

    private static List<String> getAllowedCNs() {
        final String allowedCNsEnv = System.getenv("ALLOWED_CNS");
        if (allowedCNsEnv == null || allowedCNsEnv.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(allowedCNsEnv.split(","));
    }

    private SynapseException errorResponse(final org.apache.synapse.MessageContext messageContext, Integer statusCode,
            final String message) {
        if (statusCode == null) {
            statusCode = 500;
        }
        final Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;

        axis2MessageContext.getAxis2MessageContext().setProperty(SynapseConstants.HTTP_SC, statusCode);
        messageContext.setProperty(SynapseConstants.ERROR_CODE, statusCode);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, message);

        return new SynapseException(message);
    }

    private static String getHeaderValue(final Axis2MessageContext axis2MessageContext, final String headerName) {
        final Object headers = axis2MessageContext.getAxis2MessageContext()
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headers == null) {
            return null;
        }

        final Map headersMap = (Map) headers;

        if (!headersMap.containsKey(headerName)) {
            return null;
        }

        return (String) headersMap.get(headerName);
    }
}
