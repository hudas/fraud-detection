package org.ignas.frauddetection.shared;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

/**
 * Custom codec used for any message that can be serialised to Json and does not require custom serialisation.
 *      Must be used strictly only for ImmutableObjects.
 *
 *      If non immutable object will be used undefined behaviour will occur!
 *
 *      In case messages are sent locally trough event bus,
 *      consumer vertices will receive reference to the same object,
 *      therefore any mutations by one of the vertices will in turn cause changes affecting all vertices!
 *
 *
 * @param <T>
 */
public class ImmutableObjectCodec<T> implements MessageCodec<T, T> {

    private Class<T> type;

    public ImmutableObjectCodec(Class<T> type) {
        this.type = type;
    }

    @Override
    public void encodeToWire(Buffer buffer, T statisticsRequest) {
        Buffer encoded = Json.encodeToBuffer(statisticsRequest);

        buffer.appendInt(encoded.length());
        buffer.appendBuffer(encoded);
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        int payloadLength = buffer.getInt(pos);
        int endPosition = payloadLength + 4;

        return buffer.slice(pos, endPosition).toJsonObject().mapTo(type);
    }


    /**
     * In order to prevent unexpected modifications of object state
     *      affecting other receivers or publisher itself deep copy of object should be returned.
     *
     * TODO: Pretty unsafe solution, requires further refactoring
     * @param request
     * @return
     */
    @Override
    public T transform(T request) {
        return request;
    }

    @Override
    public String name() {
        return type.getCanonicalName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
