package com.firefly.net.tcp.codec.model;

/**
 * @author Pengtao Qiu
 */
public class Request extends MetaInfo {

    protected String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
