package org.jetlinks.community.things.helper.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Modbus 帧与 ByteBuf 之间的简单编解码工具。
 * <p>
 * 支持功能码:
 * <ul>
 *     <li>0x01/0x02: 读线圈/离散输入 (请求与响应)</li>
 *     <li>0x03/0x04: 读保持寄存器/输入寄存器 (请求与响应)</li>
 *     <li>0x05: 写单线圈</li>
 *     <li>0x06: 写单寄存器</li>
 *     <li>0x0F: 写多个线圈</li>
 *     <li>0x10: 写多个寄存器</li>
 *     <li>0x16: 掩码写保持寄存器 (Mask Write Register)</li>
 * </ul>
 * <p>
 * <p>解码/编码行为由 {@link ModbusLinkType} 决定：
 * <ul>
 *     <li>{@link ModbusLinkType#PDU}：报文即为 UnitId + 功能码 + 数据；</li>
 *     <li>{@link ModbusLinkType#TCP}：含 6 字节 MBAP，其后为 Length 所覆盖的 UnitId + PDU；</li>
 *     <li>{@link ModbusLinkType#RTU}：PDU 末尾 2 字节为 CRC16（校验失败则解析失败）。</li>
 * </ul>
 * 不处理粘包拆包。
 */
public final class ModbusFrameCodec {

    private ModbusFrameCodec() {
    }

    /**
     * 等价于 {@link #decode(ByteBuf, ModbusLinkType)} {@link ModbusLinkType#PDU}。
     */
    public static ModbusFrame decode(ByteBuf payload) {
        return decode(payload, ModbusLinkType.PDU);
    }

    /**
     * 从上游报文中解析出单个 Modbus 帧。
     *
     * @param payload   原始缓冲区（不修改 readerIndex）
     * @param linkType  链路类型
     * @return Modbus 帧, 解析失败时返回 null
     */
    public static ModbusFrame decode(ByteBuf payload, ModbusLinkType linkType) {
        if (payload == null || !payload.isReadable()) {
            return null;
        }
        ModbusLinkType lt = linkType == null ? ModbusLinkType.PDU : linkType;
        ByteBuf pduBuf = switch (lt) {
            case TCP -> stripTcpMbap(payload);
            case RTU -> stripRtuCrc(payload);
            default -> payload;
        };
        if (pduBuf == null || !pduBuf.isReadable()) {
            return null;
        }
        return decodePdu(pduBuf);
    }

    /**
     * 剥离 Modbus TCP MBAP，返回 UnitId + 功能码 + 数据 的视图（不复制负载）。
     */
    static ByteBuf stripTcpMbap(ByteBuf raw) {
        if (raw.readableBytes() < 8) {
            return null;
        }
        int proto = raw.getUnsignedShort(raw.readerIndex() + 2);
        if (proto != 0) {
            return null;
        }
        int length = raw.getUnsignedShort(raw.readerIndex() + 4);
        if (length < 2 || raw.readableBytes() < 6 + length) {
            return null;
        }
        return raw.slice(raw.readerIndex() + 6, length);
    }

    /**
     * 校验并剥离 RTU CRC，返回 PDU 视图。
     */
    static ByteBuf stripRtuCrc(ByteBuf raw) {
        int n = raw.readableBytes();
        if (n < 4) {
            return null;
        }
        int crcRx = raw.getUnsignedShortLE(raw.readerIndex() + n - 2);
        int crcCalc = crc16Modbus(raw, raw.readerIndex(), n - 2);
        if (crcCalc != crcRx) {
            return null;
        }
        return raw.slice(raw.readerIndex(), n - 2);
    }

    /**
     * 解析裸 PDU：首字节从站/单元 ID，第二字节功能码。
     */
    private static ModbusFrame decodePdu(ByteBuf payload) {
        if (payload == null || !payload.isReadable()) {
            return null;
        }
        // 不修改原始 readerIndex, 复制一份用于解析
        ByteBuf buf = payload.slice();
        if (buf.readableBytes() < 2) {
            return null;
        }
        int slaveId = buf.readUnsignedByte();
        int functionCode = buf.readUnsignedByte();

        switch (functionCode) {
            case 0x01: // Read Coils (response)
            case 0x02: { // Read Discrete Inputs (response)
                if (!buf.isReadable()) {
                    return null;
                }
                int byteCount = buf.readUnsignedByte();
                if (buf.readableBytes() < byteCount) {
                    return null;
                }
                ByteBuf values = buf.readSlice(byteCount);
                return new ModbusFrame(slaveId, functionCode, 0, values);
            }
            case 0x03: // Read Holding Registers (response)
            case 0x04: { // Read Input Registers (response)
                if (!buf.isReadable()) {
                    return null;
                }
                int byteCount = buf.readUnsignedByte();
                if (buf.readableBytes() < byteCount) {
                    return null;
                }
                // 寄存器数据区, 直接保留为 ByteBuf
                ByteBuf values = buf.readSlice(byteCount);
                // 响应报文中并未包含起始地址, 这里将 address 设为 0,
                // 实际映射时通过 ModbusThingsMapping.properties 中每条映射的寄存器键完成定位。
                return new ModbusFrame(slaveId, functionCode, 0, values);
            }
            case 0x05: { // Write Single Coil (request/response): address(2) + value(2), 0xFF00=ON 0x0000=OFF
                if (buf.readableBytes() < 4) {
                    return null;
                }
                int address = buf.readUnsignedShort();
                ByteBuf values = buf.readSlice(2);
                return new ModbusFrame(slaveId, functionCode, address, values);
            }
            case 0x06: { // Write Single Register (request/response)
                if (buf.readableBytes() < 4) {
                    return null;
                }
                int address = buf.readUnsignedShort();
                // 后续 2 字节为寄存器值
                ByteBuf values = buf.readSlice(2);
                return new ModbusFrame(slaveId, functionCode, address, values);
            }
            case 0x0F: { // Write Multiple Coils (request): address(2) + quantity(2) + byteCount + coil bytes
                if (buf.readableBytes() < 5) {
                    return null;
                }
                int address = buf.readUnsignedShort();
                int quantity = buf.readUnsignedShort();
                int byteCount = buf.readUnsignedByte();
                if (buf.readableBytes() < byteCount) {
                    return null;
                }
                ByteBuf values = buf.readSlice(byteCount);
                return new ModbusFrame(slaveId, functionCode, address, values);
            }
            case 0x10: { // Write Multiple Registers (request)
                if (buf.readableBytes() < 5) {
                    return null;
                }
                int address = buf.readUnsignedShort();
                int quantity = buf.readUnsignedShort();
                int byteCount = buf.readUnsignedByte();
                if (buf.readableBytes() < byteCount) {
                    return null;
                }
                // 写多个寄存器的数据区
                ByteBuf values = buf.readSlice(byteCount);
                return new ModbusFrame(slaveId, functionCode, address, values);
            }
            case 0x16: { // Mask Write Register (request/response echo): address(2) + AND_Mask(2) + OR_Mask(2)
                if (buf.readableBytes() < 6) {
                    return null;
                }
                int address = buf.readUnsignedShort();
                int andMask = buf.readUnsignedShort();
                int orMask = buf.readUnsignedShort();
                ByteBuf values = Unpooled.buffer(4).writeShort(andMask).writeShort(orMask);
                return new ModbusFrame(slaveId, functionCode, address, values);
            }
            default:
                // 不支持的功能码, 交由上层处理
                return null;
        }
    }

    /**
     * 将一批 Modbus 帧编码为字节流（默认 {@link ModbusLinkType#PDU}）。
     */
    public static Flux<ByteBuf> encode(List<ModbusFrame> frames) {
        return encode(frames, ModbusLinkType.PDU);
    }

    /**
     * 将一批 Modbus 帧按链路类型编码。
     */
    public static Flux<ByteBuf> encode(List<ModbusFrame> frames, ModbusLinkType linkType) {
        if (frames == null || frames.isEmpty()) {
            return Flux.empty();
        }
        ModbusLinkType lt = linkType == null ? ModbusLinkType.PDU : linkType;
        return Flux.fromIterable(frames)
                   .map(f -> encodeFrame(f, lt));
    }

    /**
     * 将单个帧编码为 PDU（无 MBAP、无 CRC）。
     */
    public static ByteBuf encodeFrame(ModbusFrame frame) {
        return encodeFrame(frame, ModbusLinkType.PDU);
    }

    /**
     * 按链路类型封装：TCP 增加 MBAP，RTU 增加 CRC，PDU 仅输出 UnitId+功能码+数据。
     */
    public static ByteBuf encodeFrame(ModbusFrame frame, ModbusLinkType linkType) {
        if (frame == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        ModbusLinkType lt = linkType == null ? ModbusLinkType.PDU : linkType;
        ByteBuf pdu = encodePduInternal(frame);
        if (pdu == null || !pdu.isReadable()) {
            return pdu == null ? Unpooled.EMPTY_BUFFER : pdu;
        }
        switch (lt) {
            case TCP:
                return wrapTcpMbap(pdu);
            case RTU:
                return appendRtuCrc(pdu);
            case PDU:
            default:
                return pdu;
        }
    }

    private static ByteBuf wrapTcpMbap(ByteBuf pdu) {
        try {
            int n = pdu.readableBytes();
            ByteBuf out = Unpooled.buffer(6 + n);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(n);
            out.writeBytes(pdu);
            return out;
        } finally {
            ReferenceCountUtil.safeRelease(pdu);
        }
    }

    private static ByteBuf appendRtuCrc(ByteBuf pdu) {
        try {
            int n = pdu.readableBytes();
            ByteBuf out = Unpooled.buffer(n + 2);
            out.writeBytes(pdu);
            int crc = crc16Modbus(out, 0, n);
            out.writeByte(crc & 0xFF);
            out.writeByte((crc >> 8) & 0xFF);
            return out;
        } finally {
            ReferenceCountUtil.safeRelease(pdu);
        }
    }

    /**
     * Modbus RTU CRC16（多项式 0xA001，初值 0xFFFF）。
     */
    public static int crc16Modbus(ByteBuf buf, int offset, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= (buf.getByte(offset + i) & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private static ByteBuf encodePduInternal(ModbusFrame frame) {
        if (frame == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        int functionCode = frame.getFunctionCode();
        ByteBuf values = frame.getValues();
        if (values == null) {
            values = Unpooled.EMPTY_BUFFER;
        }
        ByteBuf buf;
        switch (functionCode) {
            case 0x01:
            case 0x02: {
                // 读线圈/离散输入请求: slaveId + functionCode + address(2) + quantity(2)
                int quantity;
                if (values.readableBytes() == 2) {
                    quantity = values.getUnsignedShort(values.readerIndex());
                } else {
                    quantity = values.readableBytes() / 2;
                }
                if (quantity <= 0) {
                    quantity = 1;
                }
                buf = Unpooled.buffer(1 + 1 + 2 + 2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                buf.writeShort(quantity);
                return buf;
            }
            case 0x03:
            case 0x04: {
                // 读寄存器请求: slaveId + functionCode + address(2) + quantity(2)
                int quantity;
                if (values.readableBytes() == 2) {
                    quantity = values.getUnsignedShort(values.readerIndex());
                } else {
                    quantity = values.readableBytes() / 2;
                }
                if (quantity <= 0) {
                    quantity = 1;
                }
                buf = Unpooled.buffer(1 + 1 + 2 + 2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                buf.writeShort(quantity);
                return buf;
            }
            case 0x05: {
                // 写单线圈: slaveId + functionCode + address(2) + value(2), 0xFF00=ON 0x0000=OFF
                buf = Unpooled.buffer(1 + 1 + 2 + 2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                int v = values.readableBytes() >= 2 ? values.getUnsignedShort(values.readerIndex()) : 0;
                buf.writeShort((v == 1 || v == 0xFF00) ? 0xFF00 : 0x0000);
                return buf;
            }
            case 0x06: {
                // 写单寄存器: slaveId + functionCode + address(2) + value(2)
                buf = Unpooled.buffer(1 + 1 + 2 + 2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                if (values.readableBytes() >= 2) {
                    // 不修改源 ByteBuf 的 readerIndex
                    buf.writeBytes(values, values.readerIndex(), 2);
                } else {
                    buf.writeShort(0);
                }
                return buf;
            }
            case 0x0F: {
                // 写多个线圈: slaveId + functionCode + address(2) + quantity(2) + byteCount + coil bytes
                // values 格式: 每个线圈占 2 字节(short), 0/1 表示 off/on, 需打包为线圈字节(每字节8位,LSB优先)
                int coilCount = values.readableBytes() / 2;
                int byteCount = (coilCount + 7) / 8;
                if (coilCount == 0) {
                    byteCount = 1;
                    coilCount = 1;
                }
                ByteBuf coilBytes = Unpooled.buffer(byteCount);
                for (int i = 0; i < byteCount; i++) {
                    int b = 0;
                    for (int bit = 0; bit < 8 && i * 8 + bit < coilCount; bit++) {
                        int idx = (i * 8 + bit) * 2;
                        if (values.readableBytes() >= idx + 2) {
                            int v = values.getUnsignedShort(values.readerIndex() + idx);
                            if ((v & 1) != 0) {
                                b |= (1 << bit);
                            }
                        }
                    }
                    coilBytes.writeByte(b);
                }
                buf = Unpooled.buffer(1 + 1 + 2 + 2 + 1 + byteCount);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                buf.writeShort(coilCount);
                buf.writeByte(byteCount);
                buf.writeBytes(coilBytes);
                return buf;
            }
            case 0x10: {
                // 写多个寄存器: slaveId + functionCode + address(2) + quantity(2) + byteCount + values*2
                int byteCount = values.readableBytes();
                int quantity = byteCount / 2;
                buf = Unpooled.buffer(1 + 1 + 2 + 2 + 1 + byteCount);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                buf.writeShort(quantity);
                buf.writeByte(byteCount);
                if (byteCount > 0) {
                    buf.writeBytes(values, values.readerIndex(), byteCount);
                }
                return buf;
            }
            case 0x16: {
                // 掩码写: slaveId + functionCode + address(2) + AND_Mask(2) + OR_Mask(2)
                buf = Unpooled.buffer(1 + 1 + 2 + 2 + 2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                buf.writeShort(frame.getAddress());
                int andMask = values.readableBytes() >= 2 ? values.getUnsignedShort(values.readerIndex()) : 0;
                int orMask = values.readableBytes() >= 4 ? values.getUnsignedShort(values.readerIndex() + 2) : 0;
                buf.writeShort(andMask);
                buf.writeShort(orMask);
                return buf;
            }
            default:
                // 未知功能码, 仅写入 slaveId 和 functionCode
                buf = Unpooled.buffer(2);
                buf.writeByte(frame.getSlaveId());
                buf.writeByte(functionCode);
                return buf;
        }
    }

}

