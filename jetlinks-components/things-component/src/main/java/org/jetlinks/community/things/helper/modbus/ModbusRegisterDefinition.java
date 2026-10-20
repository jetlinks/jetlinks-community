package org.jetlinks.community.things.helper.modbus;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.layout.ByteLayout;
import org.jetlinks.core.codec.layout.ByteLayouts;
import org.jetlinks.core.utils.NumberUtils;

import static org.jetlinks.core.codec.layout.ByteLayout.BIG_ENDIAN;

/**
 * 描述 Modbus 寄存器的基础定义信息。
 * <p>
 * 通过 {@link Codec} 与 {@link ByteLayout}
 * 定义数值的解析规则与字节布局, 避免在此处单独配置有符号、浮点、小端等细节。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModbusRegisterDefinition {

    /**
     * 寄存器键
     */
    private ModbusRegisterKey key;

    /**
     * 寄存器数量(单位: 寄存器, 一个寄存器为2字节)
     */
    private int registerCount = 1;

    /**
     * 使用的编解码器标识, 对应 {@link Codec#getId()}.
     * <p>
     * 当配置此字段时, 将优先使用 Codec 进行数值编解码。
     */
    private String codec;

    /**
     * 字节布局标识, 对应 {@link ByteLayout#getId()}.
     * <p>
     * 仅在使用 Codec 时生效, 用于描述大小端等字节重排规则。
     */
    private String layout;

    /**
     * 缩放因子, 解码时: 原始值 * scaleFactor（经 {@link #applyScaleAfterDecode} 应用）。
     */
    private double scaleFactor = 1D;

    /**
     * 小数位, &lt;0 表示不处理精度（经 {@link #applyScaleAfterDecode} 应用）。
     */
    private int scale = -1;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 使用当前寄存器定义, 从给定的 ByteBuf 中解码出完整数值。
     * <p>
     * - 若配置了 codec, 则直接使用 codec + ByteLayout 进行解码;
     * - 否则按无符号 16 位整型读取原始值（缩放由上层在按位/元素提取之后或之前统一处理）。
     *
     * @param source 寄存器对应的原始字节(通常长度为 registerCount * 2)
     * @return 解码后的结果, 可能是标量、数组或集合; 若无法解码则返回 null
     */
    public Object decode(ByteBuf source) {
        if (source == null) {
            return null;
        }

        // 优先使用 Codec + ByteLayout 进行解析
        if (codec != null && !codec.isEmpty()) {
            Codec<?> c = Codecs.getNow(codec);
            ByteLayout layoutObj = resolveLayout(layout);
            ByteBuf buf;
            if (layoutObj != null
                && layoutObj.byteLength() > 0
                && layoutObj.byteLength() <= source.readableBytes()
                && layoutObj != BIG_ENDIAN
                && layoutObj != ByteLayout.AB
                && layoutObj != ByteLayout.AB_CD
                && layoutObj != ByteLayout.AB_CD_EF_GH) {
                // 仅在需要进行字节重排时才复制, 避免修改上游缓存内容
                buf = source.copy();
                ByteBuf slice = buf.slice(0, layoutObj.byteLength());
                buf = layoutObj.reorder(slice);
            } else {
                // 无需字节重排时直接 duplicate, 避免不必要的 copy
                buf = source.duplicate();
            }
            return c.decode(buf);
        }

        // 兼容旧配置: 未配置 codec 时读取无符号 16 位原始值
        if (source.readableBytes() < 2) {
            return null;
        }
        return source.getUnsignedShort(source.readerIndex());
    }

    /**
     * 对 Codec/原始整型解码结果应用 {@link #scaleFactor} / {@link #scale}。
     * <p>字内按位映射应在调用本方法<b>之前</b>使用原始解码值做位提取（见 {@link ModbusThingsMapping#decode}）。</p>
     */
    public Object applyScaleAfterDecode(Object decoded) {
        if (decoded == null) {
            return null;
        }
        if (scaleFactor == 1D && scale < 0) {
            return decoded;
        }
        if (decoded instanceof Number) {
            return scaleOne(((Number) decoded).doubleValue());
        }
        if (decoded instanceof int[]) {
            int[] arr = (int[]) decoded;
            if (arr.length == 0) {
                return arr;
            }
            double[] out = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = scaleOneDouble(arr[i]);
            }
            return out;
        }
        if (decoded instanceof long[]) {
            long[] arr = (long[]) decoded;
            double[] out = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = scaleOneDouble(arr[i]);
            }
            return out;
        }
        if (decoded instanceof float[]) {
            float[] arr = (float[]) decoded;
            double[] out = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = scaleOneDouble(arr[i]);
            }
            return out;
        }
        if (decoded instanceof double[]) {
            double[] arr = (double[]) decoded;
            double[] out = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                out[i] = scaleOneDouble(arr[i]);
            }
            return out;
        }
        return decoded;
    }

    private Object scaleOne(double v) {
        if (scaleFactor != 1D) {
            v = v * scaleFactor;
        }
        if (scale >= 0) {
            return NumberUtils.convertEffectiveScale(v, scale);
        }
        return v;
    }

    private double scaleOneDouble(double v) {
        if (scaleFactor != 1D) {
            v = v * scaleFactor;
        }
        if (scale >= 0) {
            Object o = NumberUtils.convertEffectiveScale(v, scale);
            return o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(String.valueOf(o));
        }
        return v;
    }

    /**
     * 根据布局标识解析为 {@link ByteLayout}, 当前简单支持 2 字节场景(AB/BA)。
     */
    private ByteLayout resolveLayout(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return ByteLayouts.getNow(id);
    }

}

