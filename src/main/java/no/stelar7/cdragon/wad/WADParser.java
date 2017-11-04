package no.stelar7.cdragon.wad;

import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.data.WADFile;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class WADParser
{
    
    /**
     * Downloads and parses the latest WAD file;
     * if the file already exists, it parses that file
     *
     * @param path path to store the file
     * @return WADFile
     * @throws Exception yes :kappa:
     */
    public WADFile parseLatest(Path path) throws Exception
    {
        if (Files.exists(path))
        {
            return parse(path);
        }
        
        String urlWithFormatTokens = "http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/0.0.1.%s/files/Plugins/rcp-be-lol-game-data/default-assets.wad.compressed";
        
        UtilHandler.tryDownloadVersion(path, urlWithFormatTokens, 60, 100);
        
        return parse(path);
    }
    
    
    /**
     * Parses the specified WAD file
     *
     * @param path path to the file
     * @return WADFile
     * @throws Exception yes :kappa:
     */
    public WADFile parse(Path path) throws Exception
    {
        Path   parsePath = path;
        String filename  = path.getFileName().toString();
        
        if (filename.endsWith(".compressed"))
        {
            parsePath = uncompressWAD(path);
        }
        
        RandomAccessFile accessFile = new RandomAccessFile(parsePath.toFile(), "r");
        RAFReader        raf        = new RAFReader(accessFile, ByteOrder.LITTLE_ENDIAN);
        WADFile          wadFile    = new WADFile(raf);
        
        wadFile.setHeader(parseHeader(raf));
        wadFile.setContentHeaders(parseContent(raf, wadFile.getHeader()));
        
        return wadFile;
        
    }
    
    private Path uncompressWAD(Path compressedPath) throws IOException
    {
        String filename = compressedPath.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf(".compressed"));
        Path uncompressPath = compressedPath.getParent().resolve(filename);
        
        System.out.println("Uncompressing WAD");
        
        CompressionHandler.uncompressDEFLATE(Files.readAllBytes(compressedPath), uncompressPath);
        
        return uncompressPath;
    }
    
    /**
     * Read the content headers, and put them in a list
     *
     * @param raf  the filereader
     * @param base header containing the major version, and filecount
     * @return {@code List<WADContentHeaderV1>}
     * @throws Exception yes :kappa:
     */
    private List<WADContentHeaderV1> parseContent(RAFReader raf, WADHeaderBase base) throws Exception
    {
        System.out.println("Parsing content headers");
        
        List<WADContentHeaderV1> content = new ArrayList<>();
        
        Field field = WADHeaderBase.class.getDeclaredField("fileCount");
        field.setAccessible(true);
        long fileCount = (long) field.get(base);
        
        for (int i = 0; i < fileCount; i++)
        {
            WADContentHeaderV1 header = new WADContentHeaderV1();
            header.setPathHash(raf.readULong());
            header.setOffset(raf.readUInt());
            header.setCompressedFileSize((int) raf.readUInt());
            header.setFileSize((int) raf.readUInt());
            header.setCompressed(raf.readUByte() > 0);
            
            if (base.getMajor() == 2 || base.getMajor() == 3)
            {
                WADContentHeaderV2 headerv2 = new WADContentHeaderV2(header);
                headerv2.setDuplicate(raf.readUByte() > 0);
                headerv2.setUnknown(raf.readUByte());
                headerv2.setUnknown2(raf.readUByte());
                headerv2.setSha256(raf.readULong());
                
                content.add(headerv2);
                continue;
            }
            
            content.add(header);
        }
        
        return content;
    }
    
    /**
     * Parse the WAD header by version number
     *
     * @param raf wad file
     * @return WADHeaderBase
     */
    
    private WADHeaderBase parseHeader(RAFReader raf) throws IOException
    {
        System.out.println("Parsing WAD header");
        
        
        WADHeaderBase base = new WADHeaderBase();
        base.setMagic(raf.readString(2));
        base.setMajor(raf.readUByte());
        base.setMinor(raf.readUByte());
        
        switch (base.getMajor())
        {
            case 1:
            {
                WADHeaderV1 head = new WADHeaderV1(base);
                head.setEntryHeaderOffset(raf.readUShort());
                head.setEntryHeaderCellSize(raf.readUShort());
                head.setFileCount(raf.readUInt());
                return head;
            }
            case 2:
            {
                WADHeaderV2 head = new WADHeaderV2(base);
                head.setECDSALength(raf.readUByte());
                head.setECDSA(raf.readBytes(head.getECDSALength()));
                head.setECDSAPadding(raf.readBytes(83 - head.getECDSALength()));
                head.setFileChecksum(raf.readULong());
                head.setEntryHeaderOffset(raf.readUShort());
                head.setEntryHeaderCellSize(raf.readUShort());
                head.setFileCount(raf.readUInt());
                return head;
            }
            case 3:
            {
                WADHeaderV3 head = new WADHeaderV3(base);
                head.setECDSA(raf.readBytes(256));
                head.setChecksum(raf.readULong());
                head.setFileCount(raf.readUInt());
                return head;
            }
            default:
                throw new RuntimeException("Invalid major version! " + base.getMajor());
        }
    }
}
