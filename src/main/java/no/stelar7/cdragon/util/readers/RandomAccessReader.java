package no.stelar7.cdragon.util.readers;


import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RandomAccessReader implements BinaryReader
{
    private final MemorySegment segment;
    private       long          pos = 0;

    private byte[]    rawBytes;
    private ByteOrder byteOrder;
    private Path      path;

    // file data is not guaranteed to be aligned, so every multi-byte read uses an unaligned layout in the current byte order
    private ValueLayout.OfShort  shortLayout;
    private ValueLayout.OfChar   charLayout;
    private ValueLayout.OfInt    intLayout;
    private ValueLayout.OfLong   longLayout;
    private ValueLayout.OfFloat  floatLayout;
    private ValueLayout.OfDouble doubleLayout;

    public static RandomAccessReader create(RandomAccessReader raf)
    {
        if (raf.getPath() != null)
        {
            return new RandomAccessReader(raf.getPath(), raf.byteOrder);
        } else
        {
            return new RandomAccessReader(raf.rawBytes, raf.byteOrder);
        }
    }

    public RandomAccessReader(File path, ByteOrder order)
    {
        this(path.toPath(), order);
    }

    public RandomAccessReader(Path path, ByteOrder order)
    {
        // copy the file into GC-managed memory and unmap it right away,
        // so the file is not left locked (on Windows) while the reader is alive
        try (Arena mapArena = Arena.ofConfined(); FileChannel channel = FileChannel.open(path, StandardOpenOption.READ))
        {
            MemorySegment mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), mapArena);
            this.segment = Arena.ofAuto().allocate(mapped.byteSize());
            this.segment.copyFrom(mapped);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        this.path = path;
        setEndian(order);
    }

    public RandomAccessReader(Path path)
    {
        this(path, ByteOrder.LITTLE_ENDIAN);
    }

    public RandomAccessReader(byte[] dataBytes)
    {
        this(dataBytes, ByteOrder.LITTLE_ENDIAN);
    }

    public RandomAccessReader(byte[] dataBytes, ByteOrder order)
    {
        this.rawBytes = dataBytes;
        this.segment = MemorySegment.ofArray(dataBytes);
        setEndian(order);
    }


    public void setEndian(ByteOrder order)
    {
        this.byteOrder = order;
        this.shortLayout = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(order);
        this.charLayout = ValueLayout.JAVA_CHAR_UNALIGNED.withOrder(order);
        this.intLayout = ValueLayout.JAVA_INT_UNALIGNED.withOrder(order);
        this.longLayout = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(order);
        this.floatLayout = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(order);
        this.doubleLayout = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(order);
    }

    public ByteOrder getEndian()
    {
        return byteOrder;
    }

    @Override
    public void align()
    {
        long current = pos();
        long newPos  = (current + 3) & -4;

        if (newPos > current)
        {
            this.seekFromCurrentPosition(newPos - current);
        }
    }

    public long pos()
    {
        return this.pos;
    }

    public long size()
    {
        return this.segment.byteSize();
    }

    public long remaining()
    {
        return this.segment.byteSize() - this.pos;
    }


    /**
     * Sets the current position of the reader
     */
    public void seek(long pos)
    {
        this.pos = pos;
        // any buffered bits belong to the old position
        this.bitsLeft = 0;
    }

    /**
     * to go backwards, you still need a negative input to this function
     */
    public void seekFromEnd(long pos)
    {
        seek(this.segment.byteSize() + pos);
    }

    public void seekFromCurrentPosition(long pos)
    {
        seek(this.pos() + pos);
    }

    /**
     * Reads a string prefixed by its length as an unsigned short
     */
    public String readShortString()
    {
        return readString(readU16());
    }

    public String readIntString()
    {
        return readString(readInt());
    }

    public String readString(long length)
    {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public String readString(long length, Charset charset)
    {
        return new String(readBytes(length), charset).trim();
    }

    /**
     * Reads a fixed size field, and returns the text up to the first 0x00 in it
     */
    public String readFixedString(long length)
    {
        byte[] data = readBytes(length);
        int    end  = 0;
        while (end < data.length && data[end] != 0)
        {
            end++;
        }
        return new String(data, 0, end, StandardCharsets.UTF_8);
    }


    private int  bitsLeft;
    private int  totalBitsRead;
    private byte bits;

    /**
     * Only counts bits read with the readBits(int) method!
     */
    public int getTotalBitsRead()
    {
        return totalBitsRead;
    }

    /**
     * Discards any bits left over from the last byte read by readBits, so the next readBits starts on a byte boundary
     */
    public void alignBits()
    {
        this.bitsLeft = 0;
    }

    private byte readBit()
    {
        if (bitsLeft == 0)
        {
            bits = readByte();
            bitsLeft = 8;
        }

        bitsLeft--;
        totalBitsRead++;
        return (((bits & 0xFF) & (0x80 >> bitsLeft)) != 0) ? (byte) 1 : (byte) 0;
    }

    public int readBits(int count)
    {
        int result = 0;
        for (int i = 0; i < count; i++)
        {
            if (readBit() == 1)
            {
                result |= (1 << i);
            }
        }
        return result;
    }


    /**
     * Reads untill 0x00 is read.
     */
    public String readString()
    {
        long start = pos();
        long end   = indexOfZero(start);
        String value = new String(slice(start, end - start), StandardCharsets.UTF_8);
        seek(Math.min(end + 1, size()));
        return value;
    }

    /**
     * Finds the next 0x00 at or after offset, or the end of the data if there is none
     */
    private long indexOfZero(long offset)
    {
        long size = size();
        long i    = offset;
        while (i < size && this.segment.get(ValueLayout.JAVA_BYTE, i) != 0)
        {
            i++;
        }
        return i;
    }

    private byte[] slice(long offset, long length)
    {
        if (length > Integer.MAX_VALUE)
        {
            throw new IllegalStateException("Cannot read more than Integer.MAX_VALUE bytes");
        }
        return this.segment.asSlice(offset, length).toArray(ValueLayout.JAVA_BYTE);
    }

    /**
     * Reads untill EOF
     */
    public String readAsString()
    {
        return new String(readRemaining(), StandardCharsets.UTF_8);
    }


    public byte[] readRemaining()
    {
        byte[] data = slice(pos(), remaining());
        seek(size());
        return data;
    }

    public long readLong()
    {
        long value = this.segment.get(longLayout, pos());
        pos += Long.BYTES;
        return value;
    }

    public List<Long> readLongs(int count)
    {
        List<Long> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readLong());
        }
        return values;
    }

    public long[] readLongArray(int count)
    {
        long[] values = new long[count];
        for (int j = 0; j < count; j++)
        {
            values[j] = readLong();
        }
        return values;
    }

    public int readInt()
    {
        int value = this.segment.get(intLayout, pos());
        pos += Integer.BYTES;
        return value;
    }

    public List<Integer> readInts(int count)
    {
        List<Integer> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readInt());
        }
        return values;
    }

    public int[] readIntArray(int count)
    {
        int[] values = new int[count];
        for (int j = 0; j < count; j++)
        {
            values[j] = readInt();
        }
        return values;
    }

    public short readShort()
    {
        short value = this.segment.get(shortLayout, pos());
        pos += Short.BYTES;
        return value;
    }

    public List<Short> readShorts(int count)
    {
        List<Short> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readShort());
        }
        return values;
    }

    public short[] readShortArray(int count)
    {
        short[] values = new short[count];
        for (int j = 0; j < count; j++)
        {
            values[j] = readShort();
        }
        return values;
    }

    /**
     * Reads unsigned shorts, widened to int
     */
    public int[] readU16Array(int count)
    {
        int[] values = new int[count];
        for (int j = 0; j < count; j++)
        {
            values[j] = readU16();
        }
        return values;
    }

    public byte readByte()
    {
        byte value = this.segment.get(ValueLayout.JAVA_BYTE, pos());
        pos += Byte.BYTES;
        return value;
    }

    public int readU8()
    {
        return Byte.toUnsignedInt(readByte());
    }

    public int readU16()
    {
        return Short.toUnsignedInt(readShort());
    }

    public long readU32()
    {
        return Integer.toUnsignedLong(readInt());
    }

    /**
     * Reads a single byte as a char
     */
    @Override
    public char readChar()
    {
        return (char) (readByte() & 0xFF);
    }

    /**
     * Reads a two byte (UTF-16) char
     */
    public char readWideChar()
    {
        char value = this.segment.get(charLayout, pos());
        pos += Character.BYTES;
        return value;
    }

    /**
     * Reads up to length bytes, fewer if the end of the data is reached first
     */
    public byte[] readBytes(long length)
    {
        if (length > remaining())
        {
            length = remaining();
        }

        byte[] data = slice(pos(), Math.max(length, 0));
        pos += data.length;
        return data;
    }

    /**
     * Reads the length bytes before the current position, and moves the position back to the start of them
     */
    public byte[] readBytesReverse(long length)
    {
        if (pos() < length)
        {
            length = pos();
        }

        seek(pos() - length);
        return slice(pos(), length);
    }

    public Path getPath()
    {
        return path;
    }

    public byte[] getRawBytes()
    {
        return rawBytes;
    }

    public double readDouble()
    {
        double value = this.segment.get(doubleLayout, pos());
        pos += Double.BYTES;
        return value;
    }

    public List<Double> readDoubles(int count)
    {
        List<Double> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readDouble());
        }
        return values;
    }

    public float readFloat()
    {
        float value = this.segment.get(floatLayout, pos());
        pos += Float.BYTES;
        return value;
    }

    public List<Float> readFloats(int count)
    {
        List<Float> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readFloat());
        }
        return values;
    }

    public float[] readFloatArray(int count)
    {
        float[] values = new float[count];
        for (int j = 0; j < count; j++)
        {
            values[j] = readFloat();
        }
        return values;
    }


    public boolean readBoolean()
    {
        byte value = readByte();
        return value != 0;
    }


    public List<Boolean> readBooleans(int count)
    {
        List<Boolean> values = new ArrayList<>(count);
        for (int j = 0; j < count; j++)
        {
            values.add(readBoolean());
        }
        return values;
    }

    /**
     * Reads the 0x00 terminated string at an absolute offset, without moving the current position
     */
    public String readFromOffset(int offset)
    {
        long end = indexOfZero(offset);
        return new String(slice(offset, end - offset), StandardCharsets.UTF_8).trim();
    }

    public Vector3f readVec3F()
    {
        Vector3f vector = new Vector3f();
        vector.x = readFloat();
        vector.y = readFloat();
        vector.z = readFloat();
        return vector;
    }

    public Vector3i readVec3I()
    {
        Vector3i vector = new Vector3i();
        vector.x = readInt();
        vector.y = readInt();
        vector.z = readInt();
        return vector;
    }

    public Vector3s readVec3S()
    {
        Vector3s vector = new Vector3s();
        vector.setX(readShort());
        vector.setY(readShort());
        vector.setZ(readShort());
        return vector;
    }

    public Vector3b readVec3B()
    {
        Vector3b vector = new Vector3b();
        vector.setX(readByte());
        vector.setY(readByte());
        vector.setZ(readByte());
        return vector;
    }

    public Quaternionf readQuaternion()
    {
        Quaternionf vector = new Quaternionf();
        vector.x = readFloat();
        vector.y = readFloat();
        vector.z = readFloat();
        vector.w = readFloat();
        return vector;
    }

    public Vector2i readVec2I()
    {
        Vector2i vector = new Vector2i();
        vector.x = readInt();
        vector.y = readInt();
        return vector;
    }

    public Vector2f readVec2F()
    {
        Vector2f vector = new Vector2f();
        vector.x = readFloat();
        vector.y = readFloat();
        return vector;
    }

    public Vector4b readVec4B()
    {
        Vector4b vector = new Vector4b();
        vector.setX(readByte());
        vector.setY(readByte());
        vector.setZ(readByte());
        vector.setW(readByte());
        return vector;
    }

    public Vector4f readVec4F()
    {
        Vector4f vector = new Vector4f();
        vector.x = readFloat();
        vector.y = readFloat();
        vector.z = readFloat();
        vector.w = readFloat();
        return vector;
    }

    public Matrix4f readMatrix4x4()
    {
        Matrix4f mat = new Matrix4f();

        mat.m00(readFloat());
        mat.m01(readFloat());
        mat.m02(readFloat());
        mat.m03(readFloat());

        mat.m10(readFloat());
        mat.m11(readFloat());
        mat.m12(readFloat());
        mat.m13(readFloat());

        mat.m20(readFloat());
        mat.m21(readFloat());
        mat.m22(readFloat());
        mat.m23(readFloat());

        mat.m30(readFloat());
        mat.m31(readFloat());
        mat.m32(readFloat());
        mat.m33(readFloat());

        return mat;
    }

    public Matrix4x3f readMatrix4x3()
    {
        Matrix4x3f mat = new Matrix4x3f();

        mat.m00(readFloat());
        mat.m01(readFloat());
        mat.m02(readFloat());

        mat.m10(readFloat());
        mat.m11(readFloat());
        mat.m12(readFloat());

        mat.m20(readFloat());
        mat.m21(readFloat());
        mat.m22(readFloat());

        mat.m30(readFloat());
        mat.m31(readFloat());
        mat.m32(readFloat());

        return mat;
    }

    public boolean hasMoreBytes()
    {
        return remaining() > 0;
    }

    /**
     * Moves to the start of the next occurrence of data (at or after the current position).
     * Returns false, and leaves the position unchanged, if there is none.
     */
    public boolean readUntillString(String data)
    {
        byte[] needle = data.getBytes(StandardCharsets.UTF_8);
        long   last   = size() - needle.length;

        outer:
        for (long i = pos(); i <= last; i++)
        {
            for (int j = 0; j < needle.length; j++)
            {
                if (this.segment.get(ValueLayout.JAVA_BYTE, i + j) != needle[j])
                {
                    continue outer;
                }
            }

            seek(i);
            return true;
        }

        return false;
    }

    public List<?> readPattern(String pattern)
    {
        char[]       chars = pattern.toCharArray();
        List<Object> data  = new ArrayList<>();

        int count = 0;
        for (char aChar : chars)
        {
            if (Character.isDigit(aChar))
            {
                count *= 10;
                count += Character.digit(aChar, 10);
                continue;
            }

            switch (aChar)
            {
                case 's':
                case 'p':
                case 'c':
                {
                    data.add(count > 1 ? readString(count) : readString(1));
                    break;
                }
                case 'b':
                case 'B':
                {
                    data.add(count > 1 ? readBytes(count) : readByte());
                    break;
                }
                case '?':
                {
                    data.add(count > 1 ? readBooleans(count) : readBoolean());
                    break;
                }
                case 'h':
                case 'H':
                {
                    data.add(count > 1 ? readShorts(count) : readShort());
                    break;
                }
                case 'i':
                case 'I':
                case 'l':
                case 'L':
                {
                    data.add(count > 1 ? readInts(count) : readInt());
                    break;
                }
                case 'q':
                case 'Q':
                {
                    data.add(count > 1 ? readLongs(count) : readLong());
                    break;
                }
                case 'f':
                case 'F':
                {
                    data.add(count > 1 ? readFloats(count) : readFloat());
                    break;
                }
                case 'd':
                case 'D':
                {
                    data.add(count > 1 ? readDoubles(count) : readDouble());
                    break;
                }

                case ' ':
                {
                    break;
                }
            }

            count = 0;
        }


        return data;
    }

    public ByteArray readToByteArray()
    {
        return new ByteArray(readRemaining());
    }

    /**
     * Reads the 4 bytes before the current position as a little endian int, and moves back past them
     */
    public int readIntReverse()
    {
        return MemorySegment.ofArray(readBytesReverse(4)).get(ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN), 0);
    }

    /**
     * Reads the 8 bytes before the current position as a little endian long, and moves back past them
     */
    public long readLongReverse()
    {
        return MemorySegment.ofArray(readBytesReverse(8)).get(ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN), 0);
    }

    public String readStringReverse(int length)
    {
        return new String(readBytesReverse(length), StandardCharsets.UTF_8);
    }

    public BoundingBox readBoundingBox()
    {
        BoundingBox box = new BoundingBox();
        box.setMin(readVec3F());
        box.setMax(readVec3F());
        return box;
    }

    public Matrix4f readMatrix3x3()
    {
        Matrix4f m = new Matrix4f();
        m.m00(readFloat());
        m.m01(readFloat());
        m.m02(readFloat());
        m.m03(0);

        m.m10(readFloat());
        m.m11(readFloat());
        m.m12(readFloat());
        m.m13(0);

        m.m20(readFloat());
        m.m21(readFloat());
        m.m22(readFloat());
        m.m23(0);

        m.m30(0);
        m.m31(0);
        m.m32(0);
        m.m33(1);

        return m;
    }
}
