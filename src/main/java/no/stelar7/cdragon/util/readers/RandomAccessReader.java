package no.stelar7.cdragon.util.readers;


import com.google.common.collect.EvictingQueue;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
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
    private long pos = 0;

    private byte[] rawBytes;
    private ByteOrder byteOrder;
    private Path path;

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
        try (Arena arena = Arena.ofShared())
        {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ))
            {
                this.segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), arena);
                this.byteOrder = order;
                this.path = path;
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
        this.byteOrder = order;
    }


    public void setEndian(ByteOrder order)
    {
        this.byteOrder = order;
    }

    @Override
    public void align()
    {
        long current = pos();
        long newPos = (current + 3) & -4;

        if (newPos > current)
        {
            this.seekFromCurrentPosition(newPos - current);
        }
    }

    public long pos()
    {
        return this.pos;
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

    public String readShortString()
    {
        return readString(readShort());
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


    private int bitsLeft;
    private int totalBitsRead;
    private byte bits;

    /**
     * Only counts bits read with the readBits(int) method!
     */
    public int getTotalBitsRead()
    {
        return totalBitsRead;
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
        byte[] temp = new byte[65536];
        byte b;
        int index = 0;
        while ((b = readByte()) != 0)
        {
            temp[index++] = b;
        }

        return new String(temp, 0, index, StandardCharsets.UTF_8);
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
        long remainingSize = remaining();
        if (remainingSize > Integer.MAX_VALUE)
        {
            throw new IllegalStateException("Cannot read more than Integer.MAX_VALUE bytes");
        }

        return this.segment.asSlice(pos(), remainingSize).toArray(ValueLayout.JAVA_BYTE);
    }

    public long readLong()
    {
        long value = this.segment.get(ValueLayout.JAVA_LONG, pos());
        pos += Long.BYTES;
        return value;
    }

    public List<Long> readLongs(int count)
    {
        List<Long> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readLong());
        }
        return values;
    }

    public int readInt()
    {
        int value = this.segment.get(ValueLayout.JAVA_INT, pos());
        pos += Integer.BYTES;
        return value;
    }

    public List<Integer> readInts(int count)
    {
        List<Integer> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readInt());
        }
        return values;
    }

    public short readShort()
    {
        short value = this.segment.get(ValueLayout.JAVA_SHORT, pos());
        pos += Short.BYTES;
        return value;
    }

    public List<Short> readShorts(int count)
    {
        List<Short> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readShort());
        }
        return values;
    }

    public byte readByte()
    {
        byte value = this.segment.get(ValueLayout.JAVA_BYTE, pos());
        pos += Byte.BYTES;
        return value;
    }

    @Override
    public char readChar()
    {
        char value = this.segment.get(ValueLayout.JAVA_CHAR, pos());
        pos += Character.BYTES;
        return value;
    }

    public byte[] readBytes(long length)
    {
        if (length > remaining())
        {
            length = remaining();
        }

        return this.segment.asSlice(pos(), length).toArray(ValueLayout.JAVA_BYTE);
    }

    public Path getPath()
    {
        return path;
    }

    public double readDouble()
    {
        double value = this.segment.get(ValueLayout.JAVA_DOUBLE, pos());
        pos += Double.BYTES;
        return value;
    }

    public List<Double> readDoubles(int count)
    {
        List<Double> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readDouble());
        }
        return values;
    }

    public float readFloat()
    {
        float value = this.segment.get(ValueLayout.JAVA_FLOAT, pos());
        pos += Float.BYTES;
        return value;
    }

    public List<Float> readFloats(int count)
    {
        List<Float> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readFloat());
        }
        return values;
    }


    public boolean readBoolean()
    {
        byte value = readByte();
        return value > 0;
    }


    public List<Boolean> readBooleans(int count)
    {
        List<Boolean> values = new ArrayList<>();
        for (int j = 0; j < count; j++)
        {
            values.add(readBoolean());
        }
        return values;
    }

    /**
     * Reads untill 0x00 is read.
     */
    public String readFromOffset(int offset)
    {
        long currentPos = pos();
        byte[] tempData = readRemaining();
        seek(currentPos);

        byte[] temp = new byte[65536];
        byte b;
        int index = offset;
        while ((b = tempData[index++]) != 0)
        {
            temp[index - offset] = b;
        }

        return new String(temp, StandardCharsets.UTF_8).trim();
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
        return pos() < remaining();
    }

    public boolean readUntillString(String data)
    {
        long pos = pos();
        String content = new String(readRemaining(), StandardCharsets.UTF_8);

        int index = content.indexOf(data);
        if (index != -1)
        {
            seek(index);
            // indexOf doesnt count the null bytes in the string, so the posision can be way earlier than what it really is..
            readUntillStringAccurate(data);
            return true;
        }

        return false;
    }

    private void readUntillStringAccurate(String data)
    {
        // Is there a better way to do this?
        // String.join() is really slow, so we want to avoid calling it...
        EvictingQueue<String> queue = EvictingQueue.create(data.length());
        String dataString = "";
        do
        {
            dataString = new String(readBytes(data.length() - 1), StandardCharsets.UTF_8);
        } while (!dataString.contains(data.substring(0, 3)));

        for (char c : dataString.toCharArray())
        {
            queue.add("" + c);
        }

        while (hasMoreBytes())
        {
            queue.add(new String(readBytes(1), StandardCharsets.UTF_8));
            String internal = String.join("", queue);
            if (internal.equalsIgnoreCase(data))
            {
                seek(pos() - data.length());
                return;
            }
        }
    }

    public List<?> readPattern(String pattern)
    {
        char[] chars = pattern.toCharArray();
        List<Object> data = new ArrayList<>();

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

    public int readIntReverse()
    {
        ByteBuffer wrapped = ByteBuffer.wrap(readBytes(4));
        wrapped.order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }

    public long readLongReverse()
    {
        ByteBuffer wrapped = ByteBuffer.wrap(readBytes(8));
        wrapped.order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getLong();
    }

    public String readStringReverse(int length)
    {
        return new String(readBytes(length), StandardCharsets.UTF_8);
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
