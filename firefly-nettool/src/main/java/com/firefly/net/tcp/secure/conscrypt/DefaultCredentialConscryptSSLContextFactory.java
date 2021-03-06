package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;

/**
 * @author Pengtao Qiu
 */
public class DefaultCredentialConscryptSSLContextFactory extends AbstractConscryptSSLContextFactory {
    @Override
    public SSLContext getSSLContext() {
        try {
            return getSSLContext(new ByteArrayInputStream(SecureUtils.DEFAULT_CREDENTIAL), "ptmima1234", "ptmima4321");
        } catch (Throwable e) {
            log.error("get SSL context error", e);
            return null;
        }
    }
}
