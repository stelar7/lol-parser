package no.stelar7.cdragon.util.handlers;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32.ByReference;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.*;
import com.sun.jna.ptr.IntByReference;
import no.stelar7.cdragon.util.types.Pointer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static com.sun.jna.platform.win32.WinNT.*;

public class MemoryHandler
{
    public static void readProcessMemory(String handleName)
    {
        byte[] dataPattern = new byte[]{(byte) 0x8B, 0x3D, 0x00, 0x00, 0x00, 0x00, (byte) 0x8B, 0x35, 0x00, 0x00, 0x00, 0x00, 0x3B, (byte) 0xF7, 0x0F, (byte) 0x84, 0x00, 0x00, 0x00, 0x00, 0x66, 0x66};
        byte[] mask        = new byte[]{1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1};
        
        int     processId         = findProcessID(handleName);
        HANDLE  processHandle     = openProcess(processId);
        Pointer memoryPagePointer = scanMemoryPages(processHandle, dataPattern, mask);
        readGameObjects(processHandle, memoryPagePointer, dataPattern);
        
    }
    
    private static void readGameObjects(HANDLE processHandle, Pointer startAddress, byte[] pattern)
    {
        byte[] data = startAddress.readArray(22);
        byte[] mask = new byte[]{1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1};
        
        for (int i = 0; i < data.length; i++)
        {
            if (mask[i] == 1 && pattern[i] != data[i])
            {
                System.out.println("invalid pattern");
            }
        }
        
        Pointer startPointer = startAddress.readPointer(8);
        Pointer endPointer   = startAddress.readPointer(2);
        long    startValue   = startPointer.readInt();
        long    endValue     = endPointer.readInt();
        
        for (long i = startValue; i < endValue; i += 4)
        {
            Pointer index          = new Pointer(processHandle, i);
            Pointer objectLocation = index.readPointer();
            String  name           = objectLocation.readAString(0x60);
            System.out.println(name);
        }
    }
    
    static int count = 0;
    
    private static void dumpMemory(HANDLE processHandle, long startAddress)
    {
        try
        {
            Path output = UtilHandler.CDRAGON_FOLDER.resolve("dumps/processMemory" + (count++) + ".bin");
            Files.createDirectories(output.getParent());
            if (!Files.exists(output))
            {
                Files.createFile(output);
            }
            Files.write(output, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            SeekableByteChannel ch = Files.newByteChannel(output, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            
            ByteBuffer     dst       = ByteBuffer.allocateDirect(getSystemInfo().dwPageSize.intValue());
            Pointer        dstPtr    = new Pointer(processHandle, Native.getDirectBufferPointer(dst));
            IntByReference bytesRead = new IntByReference(1);
            Pointer        pointer   = new Pointer(processHandle, startAddress);
            
            while (bytesRead.getValue() > 0)
            {
                Kernel32.INSTANCE.ReadProcessMemory(processHandle, pointer.asJNAPointer(), dstPtr.asJNAPointer(), dst.limit(), bytesRead);
                ch.write(dst);
                dst.position(0);
                
                pointer.move(dst.limit());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    private static Pointer scanMemoryPages(HANDLE handle, byte[] pattern, byte[] mask)
    {
        SYSTEM_INFO si             = getSystemInfo();
        Pointer     scanAddress    = new Pointer(handle, si.lpMinimumApplicationAddress);
        Pointer     maxScanAddress = new Pointer(handle, si.lpMaximumApplicationAddress);
        while (scanAddress.getAddress() < maxScanAddress.getAddress())
        {
            MEMORY_BASIC_INFORMATION info = getMemoryInfo(handle, scanAddress);
            if (memoryHasFlags(info, PAGE_READWRITE, MEM_COMMIT))
            {
                int patternIndex = scanForPattern(handle, info, pattern, mask);
                if (patternIndex > 0)
                {
                    return new Pointer(handle, info.baseAddress, patternIndex);
                }
            }
            scanAddress.move(info.regionSize.intValue());
        }
        
        System.err.println("Unable to find game objects byte sequence");
        System.exit(0);
        
        return null;
    }
    
    private static int scanForPattern(HANDLE processHandle, MEMORY_BASIC_INFORMATION info, byte[] dataPattern, byte[] mask)
    {
        int        pageSize = getSystemInfo().dwPageSize.intValue();
        ByteBuffer ret      = ByteBuffer.allocateDirect(info.regionSize.intValue());
        ByteBuffer dst      = ByteBuffer.allocateDirect(pageSize);
        Pointer    dstPtr   = new Pointer(processHandle, Native.getDirectBufferPointer(dst));
        
        IntByReference bytesRead   = new IntByReference(1);
        Pointer        readAddress = new Pointer(processHandle, info.baseAddress);
        while (bytesRead.getValue() > 0)
        {
            Kernel32.INSTANCE.ReadProcessMemory(processHandle, readAddress.asJNAPointer(), dstPtr.asJNAPointer(), pageSize, bytesRead);
            transferData(dst, ret);
            dst.position(0);
            
            readAddress.move(pageSize);
        }
        
        byte[] data = UtilHandler.bytebufferToArray(ret);
        return indexOf(data, dataPattern, mask);
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
    
    
    private static SYSTEM_INFO getSystemInfo()
    {
        SYSTEM_INFO si = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(si);
        return si;
    }
    
    private static MEMORY_BASIC_INFORMATION getMemoryInfo(HANDLE handle, Pointer p)
    {
        MEMORY_BASIC_INFORMATION info = new MEMORY_BASIC_INFORMATION();
        Kernel32.INSTANCE.VirtualQueryEx(handle, p.asJNAPointer(), info, new SIZE_T(info.size()));
        return info;
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
    
    private static HMODULE findModule(HANDLE processHandle, String processName)
    {
        List<HMODULE>  mods     = new ArrayList<>();
        HMODULE[]      hmodules = new HMODULE[1024 * 4];
        IntByReference iref     = new IntByReference();
        if (!Psapi.INSTANCE.EnumProcessModules(processHandle, hmodules, hmodules.length, iref))
        {
            System.err.println("Failed to retrive process modules");
            throw new Win32Exception(Native.getLastError());
        }
        
        for (int i = 0; i < iref.getValue() / 4; i++)
        {
            byte[] data = new byte[1024];
            Psapi.INSTANCE.GetModuleFileNameExA(processHandle, hmodules[i], data, data.length);
            if (new String(data, StandardCharsets.UTF_8).contains(processName))
            {
                return hmodules[i];
            }
        }
        
        System.err.println("No process module found??");
        System.exit(0);
        return null;
    }
    
}
