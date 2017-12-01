package com.firefly.codec.http2.stream;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHTTPHandler implements Handler {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected final HTTP2Configuration config;

    public AbstractHTTPHandler(HTTP2Configuration config) {
        this.config = config;
    }

    @Override
    public void messageReceived(Session session, Object message) {
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("HTTP handler exception", t);
        if (session.getAttachment() != null && session.getAttachment() instanceof AbstractHTTPConnection) {
            try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment()) {
                if (httpConnection.getExceptionListener() != null) {
                    httpConnection.getExceptionListener().call(httpConnection, t);
                }
            } catch (Exception e) {
                log.error("http connection exception listener error", e);
            }
        } else {
            session.close();
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("session {} closed", session.getSessionId());
        if (session.getAttachment() != null && session.getAttachment() instanceof AbstractHTTPConnection) {
            try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment()) {
                if (httpConnection.getClosedListener() != null) {
                    httpConnection.getClosedListener().call(httpConnection);
                }
            } catch (Exception e) {
                log.error("http2 connection close exception", e);
            }
        }
    }

}
