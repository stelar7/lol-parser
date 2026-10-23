package no.stelar7.cdragon.types.wad;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.wad.data.*;
import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.types.wad.data.header.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.*;
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
        
        return handleAll(pluginName, String.format(url + "%s", "%s", type), ByteHandler.getIPFromLong(use), path);
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
     * Parses the specified WAD.
     * Kept for compatibility; this is the same as {@link #parse(Path)}, the file is not kept locked.
     *
     * @param path path to the file
     * @return WADFile
     */
    public WADFile parseReadOnly(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        return parse(raf);
    }
    
    /**
     * Parses the specified WAD without reading the whole file into memory; entries are read from disk when they are accessed.
     * Much faster when only some entries are needed, but the file stays locked (on Windows) until the WADFile is garbage collected.
     *
     * @param path path to the file
     * @return WADFile
     */
    public WADFile parseMapped(Path path)
    {
        return parse(RandomAccessReader.mapped(path, ByteOrder.LITTLE_ENDIAN));
    }

    /**
     * Parses a compressed (eg. .wad.compressed) WAD
     *
     * @throws ParseException       if the file can not be decompressed, or is not a valid WAD
     * @throws UncheckedIOException if the file can not be read
     */
    public WADFile parseCompressed(Path path)
    {
        byte[] compressed;
        try
        {
            compressed = Files.readAllBytes(path);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        byte[] dataBytes = compressed.length < 4 ? null : CompressionHandler.uncompress(compressed);
        if (dataBytes == null)
        {
            throw new ParseException("Unable to decompress " + path, 0);
        }
        return parse(new ByteArray(dataBytes));
    }

    /**
     * True if the data starts with "RW" and a supported version (1.x, 2.x, 3.0-3.4)
     */
    @Override
    public boolean canParse(byte[] data)
    {
        if (data == null || data.length < 4 || data[0] != 'R' || data[1] != 'W')
        {
            return false;
        }
        int major = data[2] & 0xFF;
        int minor = data[3] & 0xFF;
        return major == 1 || major == 2 || (major == 3 && minor <= 4);
    }

    @Override
    public WADFile parse(RandomAccessReader raf)
    {
        raf.setEndian(ByteOrder.LITTLE_ENDIAN);
        WADFile wadFile = new WADFile(raf);
        
        wadFile.setHeader(parseHeader(raf));
        wadFile.setContentHeaders(parseContent(raf, wadFile.getHeader()));
        
        return wadFile;
    }
    
    /**
     * Read the content headers, and put them in a list
     *
     * Entry layouts (all little endian), matching CDTB's wad.py:
     * <pre>
     * 1.x      : u64 pathHash, u32 offset, u32 compressedSize, u32 size, u32 type
     * 2.x-3.3  : u64 pathHash, u32 offset, u32 compressedSize, u32 size, u8 type|subChunkCount<<4, u8 duplicate, u16 firstSubChunk, u64 checksum
     * 3.4      : u64 pathHash, u32 offset, u32 compressedSize, u32 size, u8 type|subChunkCount<<4, u8 firstSubChunkHigh, u16 firstSubChunkLow, u64 checksum
     * </pre>
     *
     * @param raf  the filereader
     * @param base header containing the major version, and filecount
     * @return {@code List<WADContentHeaderV1>}
     */
    private List<WADContentHeaderV1> parseContent(RandomAccessReader raf, WADHeaderBase base)
    {
        long fileCount = base.getFileCount();
        int  major     = base.getMajor();
        int  minor     = base.getMinor();

        int  entrySize = major == 1 ? 24 : 32;
        if (fileCount * entrySize > raf.remaining())
        {
            throw new ParseException("WAD entry table (" + fileCount + " entries) is larger than the file", raf);
        }

        List<WADContentHeaderV1> content = new ArrayList<>((int) fileCount);
        for (long i = 0; i < fileCount; i++)
        {
            String pathHash           = HashHandler.toHex(raf.readLong(), 16);
            long   offset             = raf.readU32();
            long   compressedFileSize = raf.readU32();
            long   fileSize           = raf.readU32();

            if (major == 1)
            {
                content.add(new WADContentHeaderV1(pathHash, offset, compressedFileSize, fileSize, WADCompressionType.valueOf(raf.readInt()), 0));
                continue;
            }

            int                typeAndChunkCount = raf.readU8();
            WADCompressionType compressionType   = WADCompressionType.valueOf(typeAndChunkCount & 0xF);
            int                subChunkCount     = typeAndChunkCount >>> 4;

            boolean duplicate;
            int     subChunkOffset;
            if (major == 3 && minor == 4)
            {
                // 3.4 dropped the duplicate flag, and uses its byte to extend the subchunk index to 24 bits
                int chunkIndexHigh = raf.readU8();
                int chunkIndexLow  = raf.readU16();
                duplicate = false;
                subChunkOffset = chunkIndexLow | (chunkIndexHigh << 16);
            } else
            {
                duplicate = raf.readByte() != 0;
                subChunkOffset = raf.readU16();
            }
            long checksum = raf.readLong();

            content.add(new WADContentHeaderV2(pathHash, offset, compressedFileSize, fileSize, compressionType, subChunkCount, duplicate, subChunkOffset, checksum));
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
        if (raf.remaining() < 4)
        {
            throw new ParseException("File is too small to be a WAD", raf);
        }

        String magic = raf.readString(2);
        int    major = raf.readU8();
        int    minor = raf.readU8();

        if (!"RW".equals(magic))
        {
            throw new ParseException("Invalid WAD magic: " + magic, 0);
        }

        switch (major)
        {
            case 1:
            {
                int  entryHeaderOffset   = raf.readU16();
                int  entryHeaderCellSize = raf.readU16();
                long fileCount           = raf.readU32();
                return new WADHeaderV1(magic, major, minor, entryHeaderOffset, entryHeaderCellSize, fileCount);
            }
            case 2:
            {
                int ecdsaLength = raf.readU8();
                if (ecdsaLength > 83)
                {
                    throw new ParseException("Invalid WAD signature length: " + ecdsaLength, raf);
                }
                byte[] ecdsa               = raf.readBytesExact(ecdsaLength);
                byte[] ecdsaPadding        = raf.readBytesExact(83 - ecdsaLength);
                long   fileChecksum        = raf.readLong();
                int    entryHeaderOffset   = raf.readU16();
                int    entryHeaderCellSize = raf.readU16();
                long   fileCount           = raf.readU32();
                return new WADHeaderV2(magic, major, minor, ecdsaLength, ecdsa, ecdsaPadding, fileChecksum, entryHeaderOffset, entryHeaderCellSize, fileCount);
            }
            case 3:
            {
                if (minor > 4)
                {
                    throw new ParseException("Unsupported WAD version " + major + "." + minor, 2);
                }

                byte[] ecdsa     = raf.readBytesExact(256);
                long   checksum  = raf.readLong();
                long   fileCount = raf.readU32();
                return new WADHeaderV3(magic, major, minor, ecdsa, checksum, fileCount);
            }
            default:
                throw new ParseException("Unsupported WAD version " + major + "." + minor, 2);
        }
    }
}
