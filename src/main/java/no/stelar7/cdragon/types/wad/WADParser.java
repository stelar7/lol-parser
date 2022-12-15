package no.stelar7.cdragon.types.wad;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wad.data.*;
import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.types.wad.data.header.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class WADParser implements Parseable<WADFile>
{
    public WADParser()
    {
        FileTypeHandler.getMagicNumbers();
    }
    
    /**
     * Downloads and parses the latest WAD file;
     * if the file already exists, it parses that file
     *
     * @param path path to store the file
     * @return WADFile
     */
    public WADFile parseLatest(String pluginName, Path path, boolean pbe)
    {
        return parseHidden(pluginName, path, pbe, false, 0);
    }
    
    public WADFile parseVersion(String pluginName, Path path, String version, boolean pbe)
    {
        return parseHiddenVersion(pluginName, path, pbe, version, false, 0);
    }
    
    private WADFile parseHidden(String pluginName, Path path, boolean pbe, boolean assetDefault, int count)
    {
        String url       = "http://l3cdn.riotgames.com/releases/%s/projects/league_client/releases/%s/files/Plugins/" + pluginName;
        String pbeString = pbe ? "pbe" : "live";
        url = String.format(url, pbeString, "%s");
        
        String cacheKey = String.format("lastGoodVersion-%s-%s", pbeString, pluginName);
        int    version  = UtilHandler.getPreferences().getInt(cacheKey, 390);
        
        String type = assetDefault ? "/default-assets.wad.compressed" : "/assets.wad.compressed";
        int    use  = version;
        int    next = WebHandler.getMaxVersion(url, type, version);
        
        if (count >= 3)
        {
            return null;
        }
        
        if (next == -1)
        {
            return parseHidden(pluginName, path, pbe, !assetDefault, ++count);
        }
        
        if (next > version)
        {
            use = next;
            UtilHandler.getPreferences().putInt(cacheKey, use);
        }
        
        return handleAll(pluginName, String.format(url + "%s", "%s", type), UtilHandler.getIPFromLong(use), path);
    }
    
    private WADFile parseHiddenVersion(String pluginName, Path path, boolean pbe, String version, boolean assetDefault, int count)
    {
        String url       = "http://l3cdn.riotgames.com/releases/%s/projects/league_client/releases/%s/files/Plugins/" + pluginName;
        String pbeString = pbe ? "pbe" : "live";
        url = String.format(url, pbeString, "%s");
        
        String cacheKey = String.format("lastGoodVersion-%s-%s", pbeString, pluginName);
        
        String type = assetDefault ? "/default-assets.wad.compressed" : "/assets.wad.compressed";
        
        if (count >= 3)
        {
            return null;
        }
        
        return handleAll(pluginName, String.format(url + "%s", "%s", type), version, path);
    }
    
    
    private WADFile handleAll(String pluginName, String urlWithFormatTokens, String version, Path path)
    {
        String filename          = String.format("%s-%s", pluginName, version);
        Path   fileLocation      = path.resolve(filename);
        Path   noCompressionPath = path.resolve(filename + ".nocompress");
        
        
        if (Files.exists(noCompressionPath))
        {
            deleteOld(fileLocation);
            return parse(noCompressionPath);
        }
        
        if (Files.exists(fileLocation))
        {
            uncompress(pluginName, fileLocation, noCompressionPath);
            deleteOld(fileLocation);
            return parse(noCompressionPath);
        }
        
        download(pluginName, fileLocation, urlWithFormatTokens, version);
        uncompress(pluginName, fileLocation, noCompressionPath);
        deleteOld(fileLocation);
        return parse(noCompressionPath);
    }
    
    
    private void uncompress(String pluginName, Path fileLocation, Path noCompressionPath)
    {
        System.out.println("Uncompressing WAD: " + pluginName);
        CompressionHandler.uncompressDEFLATE(fileLocation, noCompressionPath);
    }
    
    private void deleteOld(Path fileLocation)
    {
        try
        {
            System.out.println("Trying to delete file");
            Files.deleteIfExists(fileLocation);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void download(String pluginName, Path save, String url, String version)
    {
        System.out.printf("Downloading %s-%s%n", pluginName, version);
        String finalUrl = String.format(url, version);
        WebHandler.downloadFile(save, finalUrl);
    }
    
    
    /**
     * Parses the specified WAD file
     *
     * @param path path to the file
     * @return WADFile
     */
    @Override
    public WADFile parse(Path path)
    {
        try (RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN))
        {
            return parse(raf);
        }
    }
    
    /**
     * Parses the specified WAD, locking it for writing.
     * This is much faster than the other options, but blocks the file from being written to, deleted, and moved.
     *
     * @param path path to the file
     * @return WADFile
     */
    public WADFile parseReadOnly(Path path)
    {
        try (RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN, false))
        {
            return parse(raf);
        }
    }
    
    public WADFile parseCompressed(Path path)
    {
        try
        {
            byte[] dataBytes = CompressionHandler.uncompress(Files.readAllBytes(path));
            return parse(new ByteArray(dataBytes));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public WADFile parse(ByteArray data)
    {
        try (RandomAccessReader raf = new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN))
        {
            return parse(raf);
        }
    }
    
    @Override
    public WADFile parse(RandomAccessReader raf)
    {
        WADFile wadFile = new WADFile(raf);
        
        wadFile.setHeader(parseHeader(raf));
        wadFile.setContentHeaders(parseContent(raf, wadFile.getHeader()));
        
        return wadFile;
    }
    
    /**
     * Read the content headers, and put them in a list
     *
     * @param raf  the filereader
     * @param base header containing the major version, and filecount
     * @return {@code List<WADContentHeaderV1>}
     */
    private List<WADContentHeaderV1> parseContent(RandomAccessReader raf, WADHeaderBase base)
    {
        List<WADContentHeaderV1> content = new ArrayList<>();
        
        try
        {
            Field field = WADHeaderBase.class.getDeclaredField("fileCount");
            field.setAccessible(true);
            long fileCount = (long) field.get(base);
            
            for (int i = 0; i < fileCount; i++)
            {
                WADContentHeaderV1 header = new WADContentHeaderV1();
                header.setPathHash(HashHandler.toHex(raf.readLong(), 16));
                header.setOffset(raf.readInt());
                header.setCompressedFileSize(raf.readInt());
                header.setFileSize(raf.readInt());
                
                if (base.getMajor() > 1)
                {
                    if (base.getMajor() >= 3 && base.getMinor() >= 3)
                    {
                        byte dataAndChunkCount = raf.readByte();
                        int  type              = dataAndChunkCount & 0xF;
                        int  count             = dataAndChunkCount >> 4;
                        header.setCompressionType(WADCompressionType.valueOf(type));
                        header.setSubChunkCount(count);
                    } else
                    {
                        header.setCompressionType(WADCompressionType.valueOf(raf.readByte()));
                    }
                } else
                {
                    header.setCompressionType(WADCompressionType.valueOf((byte) raf.readInt()));
                }
                
                if (base.getMajor() == 2 || base.getMajor() == 3)
                {
                    WADContentHeaderV2 headerv2 = new WADContentHeaderV2(header);
                    headerv2.setDuplicate(raf.readByte() > 0);
                    headerv2.setSubChunkOffset(raf.readShort());
                    headerv2.setSha256(raf.readLong());
                    
                    content.add(headerv2);
                    continue;
                }
                
                if (header.getCompressionType() == WADCompressionType.REFERENCE)
                {
                    System.out.println("Found file reference in " + raf.getPath());
                }
                
                content.add(header);
            }
        } catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
        
        return content;
    }
    
    /**
     * Parse the WAD header by version number
     *
     * @param raf wad file
     * @return WADHeaderBase
     */
    
    private WADHeaderBase parseHeader(RandomAccessReader raf)
    {
        WADHeaderBase base = new WADHeaderBase();
        base.setMagic(raf.readString(2));
        base.setMajor(raf.readByte());
        base.setMinor(raf.readByte());
        
        switch (base.getMajor())
        {
            case 1:
            {
                WADHeaderV1 head = new WADHeaderV1(base);
                head.setEntryHeaderOffset(raf.readShort());
                head.setEntryHeaderCellSize(raf.readShort());
                head.setFileCount(raf.readInt());
                return head;
            }
            case 2:
            {
                WADHeaderV2 head = new WADHeaderV2(base);
                head.setECDSALength(raf.readByte());
                head.setECDSA(raf.readBytes(head.getECDSALength()));
                head.setECDSAPadding(raf.readBytes(83 - head.getECDSALength()));
                head.setFileChecksum(raf.readLong());
                head.setEntryHeaderOffset(raf.readShort());
                head.setEntryHeaderCellSize(raf.readShort());
                head.setFileCount(raf.readInt());
                return head;
            }
            case 3:
            {
                WADHeaderV3 head = new WADHeaderV3(base);
                head.setECDSA(raf.readBytes(256));
                head.setChecksum(raf.readLong());
                head.setFileCount(raf.readInt());
                return head;
            }
            default:
                throw new RuntimeException("Invalid major version! " + base.getMajor());
        }
    }
}
