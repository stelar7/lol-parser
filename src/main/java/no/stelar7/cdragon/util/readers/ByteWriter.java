package no.stelar7.cdragon.util.readers;

import com.google.common.io.LittleEndianDataOutputStream;
import no.stelar7.cdragon.util.types.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ByteWriter implements AutoCloseable
{
    private LittleEndianDataOutputStream stream;
    private ByteArrayOutputStream        output;
    
    public ByteWriter()
    {
        output = new ByteArrayOutputStream();
        stream = new LittleEndianDataOutputStream(output);
    }
    
    
    public byte[] toByteArray()
    {
        return output.toByteArray();
    }
    
    public void writeByte(byte value)
    {
        try
        {
            stream.writeByte(value);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeInt(int i)
    {
        try
        {
            stream.writeInt(i);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeShort(short i)
    {
        try
        {
            stream.writeShort(i);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeFloat(float i)
    {
        try
        {
            stream.writeFloat(i);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeString(String s)
    {
        try
        {
            stream.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void writeStringWithLength(String s)
    {
        writeShort((short) s.length());
        writeString(s);
    }
    
    public void writeBoolean(boolean value)
    {
        try
        {
            stream.writeBoolean(value);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeLong(Long value)
    {
        try
        {
            stream.writeLong(value);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeVec2F(Vector2f value)
    {
        try
        {
            stream.writeFloat(value.x);
            stream.writeFloat(value.y);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeVec3F(Vector3f value)
    {
        try
        {
            stream.writeFloat(value.x);
            stream.writeFloat(value.y);
            stream.writeFloat(value.z);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeVec4F(Vector4f value)
    {
        try
        {
            stream.writeFloat(value.x);
            stream.writeFloat(value.y);
            stream.writeFloat(value.z);
            stream.writeFloat(value.w);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeFloat4x4(Matrix4f value)
    {
        try
        {
            stream.writeFloat(value.m00());
            stream.writeFloat(value.m01());
            stream.writeFloat(value.m02());
            stream.writeFloat(value.m03());
            
            stream.writeFloat(value.m10());
            stream.writeFloat(value.m11());
            stream.writeFloat(value.m12());
            stream.writeFloat(value.m13());
            
            stream.writeFloat(value.m20());
            stream.writeFloat(value.m21());
            stream.writeFloat(value.m22());
            stream.writeFloat(value.m23());
            
            stream.writeFloat(value.m30());
            stream.writeFloat(value.m31());
            stream.writeFloat(value.m32());
            stream.writeFloat(value.m33());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeVec4B(Vector4b value)
    {
        try
        {
            stream.writeShort(value.getX());
            stream.writeShort(value.getY());
            stream.writeShort(value.getZ());
            stream.writeShort(value.getW());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeVec3S(Vector3s value)
    {
        try
        {
            stream.writeShort(value.getX());
            stream.writeShort(value.getY());
            stream.writeShort(value.getZ());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @Override
    public void close() throws IOException
    {
        stream.close();
        output.close();
    }
    
    public void writeRAF(RandomAccessReader raf)
    {
        try
        {
            int pos = raf.pos();
            stream.write(raf.readBytes(raf.remaining()));
            raf.seek(pos);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeByteArray(byte[] data)
    {
        try
        {
            stream.write(data);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeByteArray(byte[] data, int offset, int length)
    {
        try
        {
            stream.write(data, offset, length);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void writeByteArray(byte[] data, int length)
    {
        try
        {
            stream.write(data, 0, length);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
