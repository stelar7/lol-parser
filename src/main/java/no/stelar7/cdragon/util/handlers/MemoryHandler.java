package no.stelar7.cdragon.util.handlers;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32.ByReference;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MemoryHandler
{
    public static void readProcessMemory(String handleName) throws IOException
    {
        Path output = UtilHandler.CDRAGON_FOLDER.resolve("processMemory.bin");
        if (!Files.exists(output))
        {
            Files.createFile(output);
        }
        
        Files.write(output, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
        SeekableByteChannel ch = Files.newByteChannel(output, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        
        int processId = findProcessID(handleName);
        if (processId == 0)
        {
            System.err.println("Unable to find process with name: " + handleName);
            System.exit(0);
        }
        
        int     accessFlags   = 0x0020 + 0x0010 + 0x0008; // PROCESS_VM_WRITE + PROCESS_VM_READ + PROCESS_VM_OPERATION
        HANDLE  processHandle = Kernel32.INSTANCE.OpenProcess(accessFlags, true, processId);
        HMODULE processModule = findModule(processHandle, handleName);
        int     pageSize      = getPageSize();
        
        ByteBuffer dst    = ByteBuffer.allocateDirect(pageSize);
        Pointer    dstPtr = Native.getDirectBufferPointer(dst);
        
        IntByReference bytesRead = new IntByReference(1);
        Pointer        pointer   = processModule.getPointer();
        while (bytesRead.getValue() > 0)
        {
            Kernel32.INSTANCE.ReadProcessMemory(processHandle, pointer, dstPtr, pageSize, bytesRead);
            ch.write(dst);
            dst.position(0);
            
            pointer = pointer.share(pageSize);
        }
        
        ch.close();
    }
    
    private static int getPageSize()
    {
        SYSTEM_INFO si = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(si);
        return si.dwPageSize.intValue();
    }
    
    private static int getRegionSize()
    {
        SYSTEM_INFO si = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(si);
        return si.dwPageSize.intValue();
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
            return 0;
        } finally
        {
            Kernel32.INSTANCE.CloseHandle(processSnapshot);
        }
    }
}
