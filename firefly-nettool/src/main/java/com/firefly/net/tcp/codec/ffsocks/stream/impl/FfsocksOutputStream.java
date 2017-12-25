package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.tcp.codec.ffsocks.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.ffsocks.model.MetaInfo;
import com.firefly.net.tcp.codec.ffsocks.protocol.*;
import com.firefly.net.tcp.codec.ffsocks.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.codec.ByteArrayUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class FfsocksOutputStream extends OutputStream implements Callback {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected final MetaInfo metaInfo;
    protected final Stream stream;
    protected final MetaInfoGenerator metaInfoGenerator;

    protected boolean closed;
    protected boolean committed;

    private boolean isWriting;
    private boolean noContent = true;
    protected LinkedList<Frame> frames = new LinkedList<>();

    public FfsocksOutputStream(MetaInfo metaInfo, Stream stream, MetaInfoGenerator metaInfoGenerator, boolean committed) {
        Assert.notNull(metaInfo, "The meta info must be not null");
        Assert.notNull(stream, "The stream must be not null");

        this.metaInfo = metaInfo;
        this.stream = stream;
        this.metaInfoGenerator = Optional.ofNullable(metaInfoGenerator).orElse(MetaInfoGenerator.DEFAULT);
        this.committed = committed;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isCommitted() {
        return committed;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] array, int offset, int length) {
        Assert.notNull(array, "The data must be not null");
        write(ByteBuffer.wrap(array, offset, length));
    }

    public synchronized void write(ByteBuffer data) {
        Stream stream = getStream();
        Assert.state(!closed, "The stream " + stream + " output is closed.");

        noContent = false;
        commit();

        if (data.remaining() > Frame.MAX_PAYLOAD_LENGTH) {
            BufferUtils.split(data, Frame.MAX_PAYLOAD_LENGTH)
                       .forEach(buf -> writeFrame(new DataFrame(false, getStream().getId(), false,
                               BufferUtils.toArray(buf))));
        } else {
            writeFrame(new DataFrame(false, getStream().getId(), false, BufferUtils.toArray(data)));
        }
    }

    public synchronized void commit() {
        if (committed || closed) {
            return;
        }

        byte[] metaInfoData = metaInfoGenerator.generate(metaInfo);
        if (metaInfoData.length > Frame.MAX_PAYLOAD_LENGTH) {
            List<byte[]> splitData = ByteArrayUtils.splitData(metaInfoData, Frame.MAX_PAYLOAD_LENGTH);
            for (int i = 0; i < splitData.size(); i++) {
                boolean endFrame = (i == splitData.size() - 1);
                writeFrame(new ControlFrame(noContent, getStream().getId(), endFrame, splitData.get(i)));
            }
        } else {
            writeFrame(new ControlFrame(noContent, getStream().getId(), true, metaInfoData));
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }

        commit();
        writeFrame(new DataFrame(true, getStream().getId(), true, null));
    }

    protected synchronized void writeFrame(Frame frame) {
        if (isWriting) {
            frames.offer(frame);
        } else {
            _writeFrame(frame);
        }
    }

    protected synchronized void _writeFrame(Frame frame) {
        isWriting = true;
        switch (frame.getType()) {
            case CONTROL:
                ControlFrame controlFrame = (ControlFrame) frame;
                closed = controlFrame.isEndStream();
                getStream().send(controlFrame, this);
                break;
            case DATA:
                DataFrame dataFrame = (DataFrame) frame;
                closed = dataFrame.isEndStream();
                getStream().send(dataFrame, this);
                break;
        }
    }

    @Override
    public synchronized void succeeded() {
        Frame frame = frames.poll();
        if (frame != null) {
            _writeFrame(frame);
        } else {
            isWriting = false;
        }
    }

    @Override
    public synchronized void failed(Throwable x) {
        log.error("Write ffsocks frame error", x);
        DisconnectionFrame frame = new DisconnectionFrame(ErrorCode.IO_ERROR.getValue(),
                x.getMessage().getBytes(StandardCharsets.UTF_8));
        getStream().getSession().disconnect(frame);
        closed = true;
        isWriting = false;
    }

    public Stream getStream() {
        return stream;
    }
}
