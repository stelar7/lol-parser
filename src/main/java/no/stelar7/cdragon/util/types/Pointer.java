package no.stelar7.cdragon.util.types;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.nio.*;

public class Pointer
{
    private long   address;
    private HANDLE handle;
    
    public Pointer(HANDLE handle, long address)
    {
        this.handle = handle;
        this.address = address;
    }
    
    public Pointer(HANDLE handle, long address, long offset)
    {
        this.handle = handle;
        this.address = address + offset;
    }
    
    public Pointer(HANDLE handle, com.sun.jna.Pointer JNA)
    {
        this.handle = handle;
        this.address = com.sun.jna.Pointer.nativeValue(JNA);
    }
    
    public Pointer(HANDLE handle, com.sun.jna.Pointer JNA, long offset)
    {
        this.handle = handle;
        this.address = com.sun.jna.Pointer.nativeValue(JNA) + offset;
    }
    
    public long getAddress()
    {
        return address;
    }
    
    public HANDLE getHandle()
    {
        return handle;
    }
    
    public void move(long offset)
    {
        this.address += offset;
    }
    
    public Pointer clone(long offset)
    {
        return new Pointer(handle, address += offset);
    }
    
    public int readInt()
    {
        return readBytes(4).getInt();
    }
    
    public String readBytesAsString()
    {
        byte[] content = readArray(1024);
        
        int realSize = -1;
        for (int i = 0; i < content.length; i++)
        {
            if (content[i] == 0x00)
            {
                realSize = i;
                break;
            }
        }
        
        return new String(content, 0, realSize);
    }
    
    public String readString(int size)
    {
        byte[] content = readArray(size);
        return new String(content, 0, size);
    }
    
    public String readAString()
    {
        ByteBuffer buffer = readBytes(1024);
        int        size   = buffer.getInt(0x10);
        if (size > 0x0F)
        {
            Pointer loc = readPointer();
            return loc.readString(size);
        }
        
        byte[] content = UtilHandler.bytebufferToArray(buffer);
        return new String(content, 0, size);
    }
    
    public Pointer readPointer()
    {
        return new Pointer(handle, readBytes(4).getInt());
    }
    
    public byte[] readArray(int bytes)
    {
        return UtilHandler.bytebufferToArray(readBytes(bytes));
    }
    
    public ByteBuffer readBytes(int byteCount)
    {
        ByteBuffer dst = ByteBuffer.allocateDirect(byteCount);
        dst.order(ByteOrder.LITTLE_ENDIAN);
        com.sun.jna.Pointer dstPtr = Native.getDirectBufferPointer(dst);
        Kernel32.INSTANCE.ReadProcessMemory(handle, new com.sun.jna.Pointer(address), dstPtr, dst.limit(), null);
        return dst;
    }
    
    public int readInt(long offset)
    {
        return readBytes(offset, 4).getInt();
    }
    
    public String readBytesAsString(long offset)
    {
        byte[] content = readArray(offset, 1024);
        
        int realSize = -1;
        for (int i = 0; i < content.length; i++)
        {
            if (content[i] == 0x00)
            {
                realSize = i;
                break;
            }
        }
        
        return new String(content, 0, realSize);
    }
    
    public String readString(long offset, int size)
    {
        byte[] content = readArray(offset, size);
        return new String(content, 0, size);
    }
    
    public String readAString(long offset)
    {
        ByteBuffer buffer = readBytes(offset, 1024);
        int        size   = buffer.getInt(0x10);
        if (size > 0x0F)
        {
            Pointer loc = readPointer(offset);
            return loc.readString(size);
        }
        
        byte[] content = UtilHandler.bytebufferToArray(buffer);
        return new String(content, 0, size);
    }
    
    public Pointer readPointer(long offset)
    {
        return new Pointer(handle, readBytes(offset, 4).getInt());
    }
    
    public byte[] readArray(long offset, int bytes)
    {
        return UtilHandler.bytebufferToArray(readBytes(offset, bytes));
    }
    
    public ByteBuffer readBytes(long offset, int byteCount)
    {
        ByteBuffer dst = ByteBuffer.allocateDirect(byteCount);
        dst.order(ByteOrder.LITTLE_ENDIAN);
        com.sun.jna.Pointer dstPtr = Native.getDirectBufferPointer(dst);
        Kernel32.INSTANCE.ReadProcessMemory(handle, new com.sun.jna.Pointer(address + offset), dstPtr, dst.limit(), null);
        return dst;
    }
    
    public com.sun.jna.Pointer asJNAPointer()
    {
        return new com.sun.jna.Pointer(address);
    }
    
    @Override
    public String toString()
    {
        return "Pointer{" +
               "address=" + address +
               ", handle=" + handle +
               '}';
    }
}
