/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.jaeger;

import com.fasterxml.jackson.jr.ob.JSON;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.internal.marshal.MarshalerUtil;
import io.opentelemetry.exporter.internal.marshal.MarshalerWithSize;
import io.opentelemetry.exporter.internal.marshal.ProtoEnumInfo;
import io.opentelemetry.exporter.internal.marshal.Serializer;
import io.opentelemetry.exporter.jaeger.proto.api_v2.internal.KeyValue;
import io.opentelemetry.exporter.jaeger.proto.api_v2.internal.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings({
    "checkstyle:LocalVariableName",
    "checkstyle:MemberName",
    "checkstyle:ParameterName",
})
final class KeyValueMarshaler extends MarshalerWithSize {

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final byte[] keyUtf8;
    private final ProtoEnumInfo valueType;
    private final byte[] vStrUtf8;
    private final boolean vBool;
    private final long vInt64;
    private final double vFloat64;

    static List<KeyValueMarshaler> createRepeated(Attributes attributes) {
        if (attributes.isEmpty()) {
            return new ArrayList<>();
        }

        List<KeyValueMarshaler> marshalers = new ArrayList<>(attributes.size());
        attributes.forEach((attributeKey, o) -> marshalers.add(create(attributeKey, o)));
        return marshalers;
    }

    static KeyValueMarshaler create(AttributeKey<?> key, Object value) {
        byte[] keyUtf8 = MarshalerUtil.toBytes(key.getKey());

        // Default is the 0 value, string in this case
        ProtoEnumInfo valueType = ValueType.STRING;
        byte[] vStrUtf8 = EMPTY_BYTES;
        boolean vBool = false;
        long vInt64 = 0;
        double vFloat64 = 0;

        if (value instanceof Supplier) {
            value = ((Supplier<?>) value).get();
        }

        switch (key.getType()) {
            case STRING:
                valueType = ValueType.STRING;

                vStrUtf8 = MarshalerUtil.toBytes(String.valueOf(value));
                break;
            case BOOLEAN:
                valueType = ValueType.BOOL;
                vBool = (boolean) value;
                break;
            case LONG:
                valueType = ValueType.INT64;
                vInt64 = (long) value;
                break;
            case DOUBLE:
                valueType = ValueType.FLOAT64;
                vFloat64 = (double) value;
                break;
            case STRING_ARRAY:
            case BOOLEAN_ARRAY:
            case LONG_ARRAY:
            case DOUBLE_ARRAY:
                valueType = ValueType.STRING;
                try {
                    vStrUtf8 = JSON.std.asBytes(value);
                } catch (IOException e) {
                    // Can't happen, just ignore it.
                }
                break;
        }

        return new KeyValueMarshaler(keyUtf8, valueType, vStrUtf8, vBool, vInt64, vFloat64);
    }

    KeyValueMarshaler(
        byte[] keyUtf8,
        ProtoEnumInfo valueType,
        byte[] vStrUtf8,
        boolean vBool,
        long vInt64,
        double vFloat64) {
        super(calculateSize(keyUtf8, valueType, vStrUtf8, vBool, vInt64, vFloat64));
        this.keyUtf8 = keyUtf8;
        this.valueType = valueType;
        this.vStrUtf8 = vStrUtf8;
        this.vBool = vBool;
        this.vInt64 = vInt64;
        this.vFloat64 = vFloat64;
    }

    @Override
    protected void writeTo(Serializer output) throws IOException {
        output.serializeString(KeyValue.KEY, keyUtf8);
        output.serializeEnum(KeyValue.V_TYPE, valueType);
        output.serializeString(KeyValue.V_STR, vStrUtf8);
        output.serializeBool(KeyValue.V_BOOL, vBool);
        output.serializeInt64(KeyValue.V_INT64, vInt64);
        output.serializeDouble(KeyValue.V_FLOAT64, vFloat64);
    }

    private static int calculateSize(
        byte[] keyUtf8,
        ProtoEnumInfo valueType,
        byte[] vStrUtf8,
        boolean vBool,
        long vInt64,
        double vFloat64) {
        int size = 0;
        size += MarshalerUtil.sizeBytes(KeyValue.KEY, keyUtf8);
        size += MarshalerUtil.sizeEnum(KeyValue.V_TYPE, valueType);
        size += MarshalerUtil.sizeBytes(KeyValue.V_STR, vStrUtf8);
        size += MarshalerUtil.sizeBool(KeyValue.V_BOOL, vBool);
        size += MarshalerUtil.sizeInt64(KeyValue.V_INT64, vInt64);
        size += MarshalerUtil.sizeDouble(KeyValue.V_FLOAT64, vFloat64);
        return size;
    }
}
