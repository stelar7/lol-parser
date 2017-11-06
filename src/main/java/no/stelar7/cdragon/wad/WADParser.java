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
        if (Files.exists(getUncompressedPath(path)))
        {
            System.out.println("Found already decompressed file");
            return parse(getUncompressedPath(path));
        }
        if (Files.exists(path))
        {
            System.out.println("Found already existing file");
            return parse(path);
        }
        
        String urlWithFormatTokens = "http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/0.0.1.%s/files/Plugins/rcp-be-lol-game-data/default-assets.wad.compressed";
        
        UtilHandler.tryDownloadVersion(path, urlWithFormatTokens, 60, 100);
        
        return parse(path);
    }
    
    private Path getUncompressedPath(Path compressedPath)
    {
        String filename = compressedPath.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf(".compressed"));
        return compressedPath.getParent().resolve(filename);
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
        
        RAFReader raf     = new RAFReader(parsePath, ByteOrder.LITTLE_ENDIAN);
        WADFile   wadFile = new WADFile(raf);
        
        wadFile.setHeader(parseHeader(raf));
        wadFile.setContentHeaders(parseContent(raf, wadFile.getHeader()));
        
        return wadFile;
        
    }
    
    private Path uncompressWAD(Path compressedPath) throws IOException
    {
        System.out.println("Uncompressing WAD");
        
        Path uncompressed = getUncompressedPath(compressedPath);
        CompressionHandler.uncompressDEFLATE(compressedPath, uncompressed);
        
        return uncompressed;
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
            header.setPathHash(raf.readLong());
            header.setOffset(raf.readInt());
            header.setCompressedFileSize(raf.readInt());
            header.setFileSize(raf.readInt());
            header.setCompressed(raf.readByte() > 0);
            
            if (base.getMajor() == 2 || base.getMajor() == 3)
            {
                WADContentHeaderV2 headerv2 = new WADContentHeaderV2(header);
                headerv2.setDuplicate(raf.readByte() > 0);
                headerv2.setUnknown(raf.readByte());
                headerv2.setUnknown2(raf.readByte());
                headerv2.setSha256(raf.readLong());
                
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
    
    private WADHeaderBase parseHeader(RAFReader raf)
    {
        System.out.println("Parsing WAD header");
        
        
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
