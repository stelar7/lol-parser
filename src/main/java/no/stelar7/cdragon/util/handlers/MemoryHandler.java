package no.stelar7.cdragon.util.handlers;

import com.sun.jna.*;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32.ByReference;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.*;
import com.sun.jna.ptr.IntByReference;

import java.nio.ByteBuffer;
import java.util.*;

import static com.sun.jna.platform.win32.WinNT.*;

public class MemoryHandler
{
    public static void readProcessMemory(String handleName)
    {
        int     processId     = findProcessID(handleName);
        HANDLE  processHandle = openProcess(processId);
        Pointer memoryPage    = scanMemoryPages(processHandle);
        readGameObjects(memoryPage);
        
    }
    
    private static void readGameObjects(Pointer startAddress)
    {
        // invalid memory access?
        Pointer start = startAddress.getPointer(8);
        Pointer end   = startAddress.getPointer(2);
        
        /*
        
        for (long i = start; i < end; i += 4)
        {
            Pointer objectPointer = new Pointer(i);
            String  name          = objectPointer.getString(0x60);
            System.out.println(name);
        }
        
         */
    }
    
    private static Pointer scanMemoryPages(HANDLE handle)
    {
        byte[] dataPattern = new byte[]{(byte) 0x8B, 0x3D, 0x00, 0x00, 0x00, 0x00, (byte) 0x8B, 0x35, 0x00, 0x00, 0x00, 0x00, 0x3B, (byte) 0xF7, 0x0F, (byte) 0x84, 0x00, 0x00, 0x00, 0x00, 0x66, 0x66};
        byte[] mask        = new byte[]{1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1};
        
        SYSTEM_INFO si             = getSystemInfo();
        long        scanAddress    = Pointer.nativeValue(si.lpMinimumApplicationAddress);
        long        maxScanAddress = Pointer.nativeValue(si.lpMaximumApplicationAddress);
        while (scanAddress < maxScanAddress)
        {
            MEMORY_BASIC_INFORMATION info = getMemoryInfo(handle, Pointer.createConstant(scanAddress));
            if (memoryHasFlags(info, PAGE_READWRITE, MEM_COMMIT))
            {
                if (scanForPattern(handle, info, dataPattern, mask))
                {
                    return info.baseAddress;
                }
            }
            
            scanAddress += info.regionSize.longValue();
        }
        
        System.err.println("Unable to find game objects byte sequence");
        System.exit(0);
        
        return null;
    }
    
    private static boolean scanForPattern(HANDLE processHandle, MEMORY_BASIC_INFORMATION info, byte[] dataPattern, byte[] mask)
    {
        int        pageSize = getSystemInfo().dwPageSize.intValue();
        ByteBuffer ret      = ByteBuffer.allocateDirect(info.regionSize.intValue());
        ByteBuffer dst      = ByteBuffer.allocateDirect(pageSize);
        Pointer    dstPtr   = Native.getDirectBufferPointer(dst);
        
        IntByReference bytesRead   = new IntByReference(1);
        long           readAddress = Pointer.nativeValue(info.baseAddress);
        while (bytesRead.getValue() > 0)
        {
            Kernel32.INSTANCE.ReadProcessMemory(processHandle, Pointer.createConstant(readAddress), dstPtr, pageSize, bytesRead);
            transferData(dst, ret);
            dst.position(0);
            
            readAddress += pageSize;
        }
        
        byte[] data = bytebufferToArray(ret);
        return indexOf(data, dataPattern, mask) > 0;
    }
    
    private static void transferData(ByteBuffer src, ByteBuffer dst)
    {
        while (src.remaining() > 0 && dst.remaining() > 0)
        {
            dst.put(src.get());
        }
    }
    
    private static int indexOf(byte[] outer, byte[] inner)
    {
        byte[] mask = new byte[inner.length];
        Arrays.fill(mask, (byte) 1);
        return indexOf(outer, inner, mask);
    }
    
    private static int indexOf(byte[] outer, byte[] inner, byte[] mask)
    {
        for (int i = 0; i < outer.length - inner.length + 1; i++)
        {
            boolean found = true;
            for (int j = 0; j < inner.length; j++)
            {
                if (mask[j] == 1 && outer[i + j] != inner[j])
                {
                    found = false;
                    break;
                }
            }
            if (found)
            {
                return i;
            }
        }
        
        return -1;
    }
    
    
    private static void printBuffer(ByteBuffer data)
    {
        StringBuilder result = new StringBuilder();
        
        data.mark();
        
        while (data.remaining() > 0)
        {
            String hex = Integer.toHexString(Byte.toUnsignedInt(data.get())).toUpperCase(Locale.ENGLISH);
            if (hex.length() != 2)
            {
                hex = "0" + hex;
            }
            result.append(hex).append(" ");
        }
        
        data.reset();
        
        result.reverse().deleteCharAt(0).reverse();
        System.out.println(result);
    }
    
    private static SYSTEM_INFO getSystemInfo()
    {
        SYSTEM_INFO si = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(si);
        return si;
    }
    
    private static MEMORY_BASIC_INFORMATION getMemoryInfo(HANDLE handle, Pointer p)
    {
        MEMORY_BASIC_INFORMATION info = new MEMORY_BASIC_INFORMATION();
        Kernel32.INSTANCE.VirtualQueryEx(handle, p, info, new SIZE_T(info.size()));
        return info;
    }
    
    private static byte[] bytebufferToArray(ByteBuffer buffer)
    {
        int oldPos = buffer.position();
        
        byte[] data = new byte[buffer.limit()];
        buffer.position(0);
        buffer.get(data);
        
        buffer.position(oldPos);
        return data;
    }
    
    private static boolean memoryHasFlags(MEMORY_BASIC_INFORMATION info, int protect, int state)
    {
        return (info.protect.intValue() == protect && info.state.intValue() == state);
    }
    
    private static HANDLE openProcess(int processId)
    {
        int    accessFlags   = 0x0020 + 0x0010 + 0x0008; // PROCESS_VM_WRITE + PROCESS_VM_READ + PROCESS_VM_OPERATION
        HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(accessFlags, true, processId);
        if (processHandle == null)
        {
            System.err.println("Unable to open process with id: " + processHandle);
            System.exit(0);
        }
        
        return processHandle;
    }
    
    
    private static int findProcessID(String processName)
    {
        Tlhelp32.PROCESSENTRY32.ByReference processInfo     = new ByReference();
        HANDLE                              processSnapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new DWORD(0));
        try
        {
            Kernel32.INSTANCE.Process32First(processSnapshot, processInfo);
            String name = Native.toString(processInfo.szExeFile);
            if (name.equals(processName))
            {
                return processInfo.th32ProcessID.intValue();
            }
            
            while (Kernel32.INSTANCE.Process32Next(processSnapshot, processInfo))
            {
                name = Native.toString(processInfo.szExeFile);
                if (name.equals(processName))
                {
                    return processInfo.th32ProcessID.intValue();
                }
            }
            
            System.err.println("Unable to find process with name: " + processName);
            System.exit(0);
        } finally
        {
            Kernel32.INSTANCE.CloseHandle(processSnapshot);
        }
        
        return 0;
    }
}
