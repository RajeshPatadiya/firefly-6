package com.firefly.net.tcp.codec;

import static com.firefly.utils.lang.ArrayUtils.EMPTY_BYTE_ARRAY;

/**
 * The length based parser. The format: length (int) + data (byte[])
 *
 * @author Pengtao Qiu
 */
public class LengthBasedParser extends AbstractByteBufferMessageHandler<byte[]> {

    @Override
    protected void parse() {
        parsing:
        while (true) {
            State state = getState();
            switch (state) {
                case UNDERFLOW:
                    break parsing;
                case OVERFLOW:
                    readData();
                    break;
                case COMPLETE:
                    readData();
                    break parsing;
            }
        }
    }

    protected State getState() {
        if (buffer.remaining() < 4) {
            return State.UNDERFLOW;
        }

        buffer.mark();
        int length = buffer.getInt();

        State state;
        if (length <= 0) {
            state = State.COMPLETE;
        } else if (buffer.remaining() < length) {
            state = State.UNDERFLOW;
        } else if (buffer.remaining() > length) {
            state = State.OVERFLOW;
        } else {
            state = State.COMPLETE;
        }
        buffer.reset();
        return state;
    }

    protected void readData() {
        int length = buffer.getInt();
        if (length > 0) {
            byte[] data = new byte[length];
            buffer.get(data);
            action.call(data);
        } else {
            action.call(EMPTY_BYTE_ARRAY);
        }
    }

    public enum State {
        UNDERFLOW,
        OVERFLOW,
        COMPLETE
    }
}
