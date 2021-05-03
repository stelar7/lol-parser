package no.stelar7.cdragon.util.readers;


import com.google.common.collect.EvictingQueue;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.*;
import java.nio.file.Path;
import java.util.*;

public class RandomAccessReader implements AutoCloseable, BinaryReader
{
    private final ByteBuffer       buffer;
    private       MappedByteBuffer mappedBuffer;
    private       boolean          preventLockedFile = false;
    
    private Path      path;
    private byte[]    rawBytes;
    private ByteOrder byteOrder;
    
    public static RandomAccessReader create(RandomAccessReader raf)
    {
        if (raf.getPath() != null)
        {
            return new RandomAccessReader(raf.getPath(), raf.byteOrder, raf.preventLockedFile);
        } else
        {
            return new RandomAccessReader(raf.getRawBytes(), raf.byteOrder);
        }
    }
    
    public static RandomAccessReader create(File file, ByteOrder order, boolean preventLockedFile)
    {
        return new RandomAccessReader(file.toPath(), order, preventLockedFile);
    }
    
    
    public RandomAccessReader(Path path, ByteOrder order, boolean preventLockedFile)
    {
        this.path = path;
        this.preventLockedFile = preventLockedFile;
        
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r"))
        {
            if (this.preventLockedFile)
            {
                this.mappedBuffer = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.getChannel().size());
                this.mappedBuffer.order(order);
                if (this.mappedBuffer.isDirect())
                {
                    this.buffer = ByteBuffer.allocateDirect(this.mappedBuffer.capacity());
                } else
                {
                    this.buffer = ByteBuffer.allocate(this.mappedBuffer.capacity());
                }
                
                this.buffer.order(this.mappedBuffer.order());
                
                this.mappedBuffer.position(0);
                this.buffer.put(this.mappedBuffer);
                this.buffer.position(0);
            } else
            {
                this.buffer = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.getChannel().size());
                this.buffer.order(order);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Invalid file?");
        }
    }
    
    public RandomAccessReader(Path path, ByteOrder order)
    {
        this(path, order, true);
    }
    
    public RandomAccessReader(Path path)
    {
        this(path, ByteOrder.LITTLE_ENDIAN, true);
    }
    
    public RandomAccessReader(byte[] dataBytes, ByteOrder order)
    {
        this.rawBytes = dataBytes;
        this.buffer = ByteBuffer.wrap(dataBytes);
        this.buffer.order(order);
    }
    
    public RandomAccessReader(byte[] dataBytes)
    {
        this.rawBytes = dataBytes;
        this.buffer = ByteBuffer.wrap(dataBytes);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public byte[] getRawBytes()
    {
        return rawBytes;
    }
    
    public void setEndian(ByteOrder order)
    {
        this.buffer.order(order);
    }
    
    @Override
    public void align()
    {
        int current = pos();
        int newPos  = (current + 3) & -4;
        
        if (newPos > current)
        {
            this.seekFromCurrentPosition(newPos - current);
        }
    }
    
    @Override
    public void close()
    {
        if (this.preventLockedFile)
        {
            try
            {
                Class unsafeClass;
                try
                {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch (Exception ex)
                {
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                }
                Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                clean.setAccessible(true);
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object theUnsafe = theUnsafeField.get(null);
                clean.invoke(theUnsafe, this.mappedBuffer);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public int pos()
    {
        return buffer.position();
    }
    
    public int remaining()
    {
        return buffer.remaining();
    }
    
    
    /**
     * Sets the current position of the reader
     */
    public void seek(int pos)
    {
        buffer.position(pos);
    }
    
    /**
     * to go backwards, you still need a negative input to this function
     */
    public void seekFromEnd(int pos)
    {
        buffer.position(buffer.limit() + pos);
    }
    
    public void seekFromCurrentPosition(int pos)
    {
        buffer.position(this.pos() + pos);
    }
    
    public String readShortString()
    {
        return readString(readShort());
    }
    
    public String readIntString()
    {
        return readString(readInt());
    }
    
    public String readString(int length)
    {
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }
    
    public String readString(int length, Charset charset)
    {
        return new String(readBytes(length), charset).trim();
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
        byte[] temp  = new byte[65536];
        byte   b;
        int    index = 0;
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
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }
    
    public long readLong()
    {
        return buffer.getLong();
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
        return buffer.getInt();
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
        return buffer.getShort();
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
        return buffer.get();
    }
    
    @Override
    public char readChar()
    {
        return (char) buffer.get();
    }
    
    public byte[] readBytes(int length)
    {
        if (length > remaining())
        {
            length = remaining();
        }
        
        byte[] tempData = new byte[length];
        buffer.get(tempData, 0, length);
        return Arrays.copyOf(tempData, length);
    }
    
    public Path getPath()
    {
        return path;
    }
    
    public double readDouble()
    {
        return buffer.getDouble();
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
        return buffer.getFloat();
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
        return buffer.get() > 0;
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
        int pos = buffer.position();
        buffer.position(0);
        byte[] tempData = new byte[buffer.remaining()];
        buffer.get(tempData, 0, buffer.remaining());
        buffer.position(pos);
        
        byte[] temp  = new byte[65536];
        byte   b;
        int    index = offset;
        while ((b = tempData[index++]) != 0)
        {
            temp[index - offset] = b;
        }
        
        return new String(temp, StandardCharsets.UTF_8).trim();
    }
    
    public void printBuffer()
    {
        int pos = buffer.position();
        while (buffer.hasRemaining())
        {
            System.out.print(buffer.get() + ", ");
        }
        System.out.println();
        buffer.position(pos);
    }
    
    public Vector3f readVec3F()
    {
        Vector3f vector = new Vector3f();
        vector.x = buffer.getFloat();
        vector.y = buffer.getFloat();
        vector.z = buffer.getFloat();
        return vector;
    }
    
    public Vector3i readVec3I()
    {
        Vector3i vector = new Vector3i();
        vector.x = (buffer.getInt());
        vector.y = (buffer.getInt());
        vector.z = (buffer.getInt());
        return vector;
    }
    
    public Vector3s readVec3S()
    {
        Vector3s vector = new Vector3s();
        vector.setX((buffer.getShort()));
        vector.setY((buffer.getShort()));
        vector.setZ((buffer.getShort()));
        return vector;
    }
    
    public Vector3b readVec3B()
    {
        Vector3b vector = new Vector3b();
        vector.setX((buffer.get()));
        vector.setY((buffer.get()));
        vector.setZ((buffer.get()));
        return vector;
    }
    
    public Quaternionf readQuaternion()
    {
        Quaternionf vector = new Quaternionf();
        vector.x = (buffer.getFloat());
        vector.y = (buffer.getFloat());
        vector.z = (buffer.getFloat());
        vector.w = (buffer.getFloat());
        return vector;
    }
    
    public Vector2i readVec2I()
    {
        Vector2i vector = new Vector2i();
        vector.x = (buffer.getInt());
        vector.y = (buffer.getInt());
        return vector;
    }
    
    public Vector2f readVec2F()
    {
        Vector2f vector = new Vector2f();
        vector.x = (buffer.getFloat());
        vector.y = (buffer.getFloat());
        return vector;
    }
    
    public Vector4b readVec4B()
    {
        Vector4b vector = new Vector4b();
        vector.setX((buffer.get()));
        vector.setY((buffer.get()));
        vector.setZ((buffer.get()));
        vector.setW((buffer.get()));
        return vector;
    }
    
    public Vector4f readVec4F()
    {
        Vector4f vector = new Vector4f();
        vector.x = (buffer.getFloat());
        vector.y = (buffer.getFloat());
        vector.z = (buffer.getFloat());
        vector.w = (buffer.getFloat());
        return vector;
    }
    
    public Matrix4f readMatrix4x4()
    {
        Matrix4f mat = new Matrix4f();
        
        mat.m00(buffer.getFloat());
        mat.m01(buffer.getFloat());
        mat.m02(buffer.getFloat());
        mat.m03(buffer.getFloat());
        
        mat.m10(buffer.getFloat());
        mat.m11(buffer.getFloat());
        mat.m12(buffer.getFloat());
        mat.m13(buffer.getFloat());
        
        mat.m20(buffer.getFloat());
        mat.m21(buffer.getFloat());
        mat.m22(buffer.getFloat());
        mat.m23(buffer.getFloat());
        
        mat.m30(buffer.getFloat());
        mat.m31(buffer.getFloat());
        mat.m32(buffer.getFloat());
        mat.m33(buffer.getFloat());
        
        return mat;
    }
    
    public Matrix4x3f readMatrix4x3()
    {
        Matrix4x3f mat = new Matrix4x3f();
        
        mat.m00(buffer.getFloat());
        mat.m01(buffer.getFloat());
        mat.m02(buffer.getFloat());
        
        mat.m10(buffer.getFloat());
        mat.m11(buffer.getFloat());
        mat.m12(buffer.getFloat());
        
        mat.m20(buffer.getFloat());
        mat.m21(buffer.getFloat());
        mat.m22(buffer.getFloat());
        
        mat.m30(buffer.getFloat());
        mat.m31(buffer.getFloat());
        mat.m32(buffer.getFloat());
        
        return mat;
    }
    
    public boolean isEOF()
    {
        return !buffer.hasRemaining();
    }
    
    public boolean readUntillString(String data)
    {
        int    pos     = pos();
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
        EvictingQueue<String> queue      = EvictingQueue.create(data.length());
        String                dataString = "";
        do
        {
            dataString = new String(readBytes(data.length() - 1), StandardCharsets.UTF_8);
        } while (!dataString.contains(data.substring(0, 3)));
        
        for (char c : dataString.toCharArray())
        {
            queue.add("" + c);
        }
        
        while (!isEOF())
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
    
    public boolean containsString(String data)
    {
        int     pos   = pos();
        boolean found = false;
        
        EvictingQueue<String> queue = EvictingQueue.create(data.length());
        while (!isEOF())
        {
            queue.add(new String(readBytes(1), StandardCharsets.UTF_8));
            String internal = String.join("", queue);
            if (internal.equalsIgnoreCase(data))
            {
                found = true;
                break;
            }
        }
        seek(pos);
        return found;
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
    
    public byte[] readBytesReverse(int length)
    {
        if (pos() < length)
        {
            length = pos();
        }
        
        byte[] tempData = new byte[length];
        buffer.position(buffer.position() - length);
        buffer.get(tempData);
        buffer.position(buffer.position() - length);
        return Arrays.copyOf(tempData, length);
    }
    
    public byte[] getBufferArray()
    {
        return this.buffer.array();
    }
    
    public byte[] getBufferData()
    {
        int pos = buffer.position();
        
        buffer.position(0);
        int    size = buffer.limit();
        byte[] data = new byte[size];
        buffer.get(data);
        
        buffer.position(pos);
        return data;
    }
    
    public int readIntReverse()
    {
        ByteBuffer wrapped = ByteBuffer.wrap(readBytesReverse(4));
        wrapped.order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getInt();
    }
    
    public long readLongReverse()
    {
        ByteBuffer wrapped = ByteBuffer.wrap(readBytesReverse(8));
        wrapped.order(ByteOrder.LITTLE_ENDIAN);
        return wrapped.getLong();
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
