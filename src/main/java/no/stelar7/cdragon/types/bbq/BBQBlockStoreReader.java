package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads a window (one file in the bundle) of a BBQBlockStore.
 * Positions are relative to the start of the window, so alignment works like it does in the file itself.
 */
public class BBQBlockStoreReader implements BinaryReader
{
    private final BBQBlockStore storage;
    private final long          base;
    private final long          length;
    private final byte[]        scratch = new byte[8];
    private       long          pos     = 0;
    private       ByteOrder     endian  = ByteOrder.BIG_ENDIAN;

    public BBQBlockStoreReader(BBQBlockStore storage)
    {
        this(storage, 0, storage.getMaxPos());
    }

    public BBQBlockStoreReader(BBQBlockStore storage, long base, long length)
    {
        if (base < 0 || length < 0 || base + length > storage.getMaxPos())
        {
            throw new ParseException("Bundle entry (offset " + base + ", size " + length + ") is outside the bundle data (size " + storage.getMaxPos() + ")", -1);
        }

        this.storage = storage;
        this.base = base;
        this.length = length;
    }

    public long size()
    {
        return length;
    }

    public long remaining()
    {
        return length - pos;
    }

    public ByteOrder getEndian()
    {
        return endian;
    }

    private void fill(int count)
    {
        if (count > remaining())
        {
            throw new ParseException("Unexpected end of data", pos);
        }

        storage.read(base + pos, scratch, 0, count);
        pos += count;
    }

    private long assemble(int count)
    {
        fill(count);
        long value = 0;
        if (endian == ByteOrder.BIG_ENDIAN)
        {
            for (int i = 0; i < count; i++)
            {
                value = (value << 8) | (scratch[i] & 0xFF);
            }
        } else
        {
            for (int i = count - 1; i >= 0; i--)
            {
                value = (value << 8) | (scratch[i] & 0xFF);
            }
        }
        return value;
    }

    @Override
    public String readString(long length)
    {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    /**
     * Reads a 0x00 terminated UTF-8 string
     */
    @Override
    public String readString()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (pos < length)
        {
            byte value = storage.get(base + pos++);
            if (value == 0)
            {
                break;
            }
            bytes.write(value);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Override
    public long readLong()
    {
        return assemble(8);
    }

    @Override
    public int readInt()
    {
        return (int) assemble(4);
    }

    @Override
    public short readShort()
    {
        return (short) assemble(2);
    }

    @Override
    public double readDouble()
    {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat()
    {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads up to i bytes, fewer if the end of the window is reached
     */
    @Override
    public byte[] readBytes(long i)
    {
        int    count = (int) Math.max(0, Math.min(i, remaining()));
        byte[] data  = new byte[count];
        storage.read(base + pos, data, 0, count);
        pos += count;
        return data;
    }

    @Override
    public byte readByte()
    {
        if (pos >= length)
        {
            throw new ParseException("Unexpected end of data", pos);
        }
        return storage.get(base + pos++);
    }

    @Override
    public char readChar()
    {
        return (char) (readByte() & 0xFF);
    }

    @Override
    public boolean readBoolean()
    {
        return readByte() != 0;
    }

    @Override
    public byte[] readRemaining()
    {
        return readBytes(remaining());
    }

    @Override
    public long pos()
    {
        return pos;
    }

    @Override
    public void seek(long pos)
    {
        this.pos = pos;
    }

    public void seek(long pos, int direction)
    {
        switch (direction)
        {
            case 0 -> this.pos = pos;
            case 1 -> this.pos += pos;
            case 2 -> this.pos = length + pos;
            default -> throw new IllegalArgumentException("Unknown seek direction " + direction);
        }
    }

    @Override
    public void setEndian(ByteOrder order)
    {
        this.endian = order;
    }

    @Override
    public void align()
    {
        align(4);
    }

    public void align(int alignment)
    {
        long rest = pos % alignment;
        if (rest != 0)
        {
            pos += alignment - rest;
        }
    }
}
