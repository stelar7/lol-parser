package no.stelar7.cdragon.util.writers;

import com.google.common.io.LittleEndianDataOutputStream;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.math.*;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ByteWriter implements AutoCloseable
{
    private LittleEndianDataOutputStream stream;
    private ByteArrayOutputStream        output;
    
    public ByteWriter()
    {
        output = new ByteArrayOutputStream();
        stream = new LittleEndianDataOutputStream(output);
    }
    
    public int getBytesWrittenCount()
    {
        return output.size();
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
    
    public void writeColor(Color c)
    {
        try
        {
            stream.write(c.getBlue());
            stream.write(c.getGreen());
            stream.write(c.getRed());
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
    
    public void remove(int offset, int count)
    {
        try
        {
            
            byte[] data       = output.toByteArray();
            byte[] outputData = new byte[offset + (data.length - (offset + count))];
            
            byte[] preData  = new byte[offset];
            byte[] postData = new byte[data.length - (offset + count)];
            
            System.arraycopy(data, 0, preData, 0, preData.length);
            System.arraycopy(data, offset + count, postData, 0, postData.length);
            
            System.arraycopy(preData, 0, outputData, 0, preData.length);
            System.arraycopy(postData, 0, outputData, preData.length, postData.length);
            
            output = new ByteArrayOutputStream();
            stream = new LittleEndianDataOutputStream(output);
            stream.write(outputData);
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void save(Path output)
    {
        try
        {
            Files.createDirectories(output.getParent());
            Files.write(output, toByteArray(), StandardOpenOption.CREATE);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
