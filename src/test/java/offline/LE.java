package offline;

import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.nio.charset.StandardCharsets;

/**
 * Tiny little-endian byte builder used by the offline tests to build fixtures in code
 */
public final class LE
{
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteOrder             order;

    public LE()
    {
        this(ByteOrder.LITTLE_ENDIAN);
    }

    public LE(ByteOrder order)
    {
        this.order = order;
    }

    private ByteBuffer buf(int size)
    {
        return ByteBuffer.allocate(size).order(order);
    }

    public LE u8(int v)
    {
        out.write(v & 0xFF);
        return this;
    }

    public LE i16(int v)
    {
        out.writeBytes(buf(2).putShort((short) v).array());
        return this;
    }

    public LE i32(int v)
    {
        out.writeBytes(buf(4).putInt(v).array());
        return this;
    }

    public LE i64(long v)
    {
        out.writeBytes(buf(8).putLong(v).array());
        return this;
    }

    public LE f32(float v)
    {
        out.writeBytes(buf(4).putFloat(v).array());
        return this;
    }

    public LE f32(float... v)
    {
        for (float f : v)
        {
            f32(f);
        }
        return this;
    }

    public LE bytes(byte[] v)
    {
        out.writeBytes(v);
        return this;
    }

    public LE zeros(int count)
    {
        out.writeBytes(new byte[count]);
        return this;
    }

    /**
     * Raw ASCII/UTF-8 bytes, no length and no terminator
     */
    public LE ascii(String v)
    {
        out.writeBytes(v.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * UTF-8 bytes followed by a 0 terminator
     */
    public LE cstr(String v)
    {
        ascii(v);
        return u8(0);
    }

    /**
     * UTF-8 bytes padded with zeros (or truncated) to the given length
     */
    public LE fixed(String v, int length)
    {
        byte[] b = v.getBytes(StandardCharsets.UTF_8);
        byte[] f = new byte[length];
        System.arraycopy(b, 0, f, 0, Math.min(b.length, length));
        return bytes(f);
    }

    /**
     * u16 length prefixed UTF-8 string
     */
    public LE str16(String v)
    {
        byte[] b = v.getBytes(StandardCharsets.UTF_8);
        i16(b.length);
        return bytes(b);
    }

    /**
     * i32 length prefixed UTF-8 string
     */
    public LE str32(String v)
    {
        byte[] b = v.getBytes(StandardCharsets.UTF_8);
        i32(b.length);
        return bytes(b);
    }

    public int size()
    {
        return out.size();
    }

    public byte[] toBytes()
    {
        return out.toByteArray();
    }
}
