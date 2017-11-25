package no.stelar7.cdragon.wad;

import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.data.WADFile;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.*;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class WADParser
{
    public WADParser()
    {
        UtilHandler.getMagicNumbers();
    }
    
    /**
     * Downloads and parses the latest WAD file;
     * if the file already exists, it parses that file
     *
     * @param path path to store the file
     * @return WADFile
     * @throws Exception yes :kappa:
     */
    public WADFile parseLatest(String pluginName, Path path) throws Exception
    {
        String urlNoWAD            = "http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/%s/files/Plugins/" + pluginName;
        String urlWithFormatTokens = urlNoWAD + "/default-assets.wad.compressed";
        String version             = UtilHandler.getMaxVersion(urlWithFormatTokens, 340, 360);
        if (version == null)
        {
            urlWithFormatTokens = urlNoWAD + "/assets.wad.compressed";
            version = UtilHandler.getMaxVersion(urlWithFormatTokens, 340, 360);
        }
        
        String filename          = String.format("%s-%s", pluginName, version);
        Path   fileLocation      = path.resolve(filename);
        Path   noCompressionPath = path.resolve(filename + ".nocompress");
        
        if (Files.exists(noCompressionPath))
        {
            System.out.println("Found uncompressed WAD");
            return parse(noCompressionPath);
        }
        
        if (Files.exists(fileLocation))
        {
            System.out.println("Uncompressing WAD: " + pluginName);
            CompressionHandler.uncompressDEFLATE(fileLocation, noCompressionPath);
            return parse(noCompressionPath);
        }
        
        System.out.println("Downloading " + pluginName);
        UtilHandler.tryDownloadVersion(fileLocation, urlWithFormatTokens, version);
        System.out.println("Uncompressing WAD: " + pluginName);
        CompressionHandler.uncompressDEFLATE(fileLocation, noCompressionPath);
        
        return parse(noCompressionPath);
    }
    
    
    /**
     * Parses the specified WAD file
     *
     * @param path path to the file
     * @return WADFile
     */
    public WADFile parse(Path path)
    {
        RAFReader raf     = new RAFReader(path, ByteOrder.LITTLE_ENDIAN);
        WADFile   wadFile = new WADFile(raf);
        
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
    private List<WADContentHeaderV1> parseContent(RAFReader raf, WADHeaderBase base)
    {
        System.out.println("Parsing content headers");
        List<WADContentHeaderV1> content = new ArrayList<>();
        
        try
        {
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
                header.setCompressed(raf.readByte());
                
                if (base.getMajor() == 2 || base.getMajor() == 3)
                {
                    WADContentHeaderV2 headerv2 = new WADContentHeaderV2(header);
                    headerv2.setDuplicate(raf.readByte() > 0);
                    headerv2.setPadding(raf.readShort());
                    headerv2.setSha256(raf.readLong());
                    
                    content.add(headerv2);
                    continue;
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
