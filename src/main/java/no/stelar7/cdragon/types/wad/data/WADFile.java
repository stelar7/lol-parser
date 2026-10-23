package no.stelar7.cdragon.types.wad.data;

import no.stelar7.cdragon.types.atlas.AtlasParser;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.tex.TEXParser;
import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.types.wad.data.header.WADHeaderBase;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class WADFile
{
    private final RandomAccessReader fileReader;
    private       WADHeaderBase      header;
    
    private List<SubChunkInfo> subChunkContent;
    private boolean            subChunkSearched   = false;
    private int                totalSubChunkCount = -1;

    private List<WADContentHeaderV1> contentHeaders = new ArrayList<>();
    
    public WADFile(RandomAccessReader raf)
    {
        this.fileReader = raf;
    }
    
    public RandomAccessReader getFileReader()
    {
        return fileReader;
    }
    
    public WADHeaderBase getHeader()
    {
        return header;
    }
    
    public void setHeader(WADHeaderBase header)
    {
        this.header = header;
    }
    
    public List<WADContentHeaderV1> getContentHeaders()
    {
        return contentHeaders;
    }
    
    public void setContentHeaders(List<WADContentHeaderV1> contentHeaders)
    {
        this.contentHeaders = contentHeaders;
        this.subChunkContent = null;
        this.subChunkSearched = false;
        this.totalSubChunkCount = -1;
    }
    
    public boolean hasSubChunks()
    {
        return getContentHeaders().stream().anyMatch(h -> h.getSubChunkCount() > 0);
    }
    
    /**
     * The number of subchunks referenced by the entries in this WAD (computed once)
     */
    public synchronized int getTotalSubChunkCount()
    {
        if (totalSubChunkCount < 0)
        {
            totalSubChunkCount = getContentHeaders().stream()
                                                    .filter(c -> c instanceof WADContentHeaderV2)
                                                    .mapToInt(c -> ((WADContentHeaderV2) c).getSubChunkOffset() + c.getSubChunkCount())
                                                    .max()
                                                    .orElse(0);
        }
        return totalSubChunkCount;
    }

    /**
     * Returns the parsed .subchunktoc of this WAD.
     *
     * @throws ParseException if the subchunk TOC can not be found
     */
    public synchronized List<SubChunkInfo> getSubChunkMap(int expectedChunkCount)
    {
        List<SubChunkInfo> toc = findSubChunkMap(expectedChunkCount);
        if (toc == null)
        {
            throw new ParseException("Unable to determine subchunk file", -1);
        }
        return toc;
    }

    /**
     * Finds and parses the .subchunktoc of this WAD, or returns null if there is none.
     * The result (also a miss) is cached.
     */
    private synchronized List<SubChunkInfo> findSubChunkMap(int expectedChunkCount)
    {
        if (subChunkSearched)
        {
            return subChunkContent;
        }

        long startPos = fileReader.pos();
        try
        {
            // the toc is stored next to the wad entries, as "<wad path without .client>.subchunktoc"
            WADContentHeaderV1 byName = findSubChunkTocByPath();
            if (byName != null)
            {
                subChunkContent = parseSubChunkToc(byName);
                if (subChunkContent == null)
                {
                    throw new ParseException("Subchunk TOC " + byName.getPathHash() + " is corrupt", -1);
                }
                return subChunkContent;
            }

            // we don't know the path of the wad, so guess it from the file sizes
            List<WADContentHeaderV1> possibleChoices = getContentHeaders().stream()
                                                                          .filter(c -> c.getCompressionType() != WADCompressionType.ZSTD_MULTI)
                                                                          .filter(c -> c.getCompressionType() != WADCompressionType.REFERENCE)
                                                                          .filter(c -> c.getFileSize() % 16 == 0)
                                                                          .filter(c -> c.getFileSize() / 16 == expectedChunkCount)
                                                                          .toList();

            List<List<SubChunkInfo>> chunkLists = new ArrayList<>();
            for (WADContentHeaderV1 possibleChoice : possibleChoices)
            {
                List<SubChunkInfo> chunks = parseSubChunkToc(possibleChoice);
                if (chunks != null)
                {
                    chunkLists.add(chunks);
                }
            }

            if (chunkLists.size() == 1)
            {
                subChunkContent = chunkLists.get(0);
            }

            return subChunkContent;
        } finally
        {
            subChunkSearched = true;
            fileReader.seek(startPos);
        }
    }

    /**
     * Looks for an entry named like this wad, with the extension .subchunktoc.
     * The path of the wad relative to the game folder is not known, so every suffix of the path is tried
     * (eg. "aatrox.wad.subchunktoc", "champions/aatrox.wad.subchunktoc", "final/champions/aatrox.wad.subchunktoc", ...)
     */
    private WADContentHeaderV1 findSubChunkTocByPath()
    {
        Path path = fileReader.getPath();
        if (path == null || path.getFileName() == null)
        {
            return null;
        }

        Map<String, WADContentHeaderV1> byHash = new HashMap<>();
        for (WADContentHeaderV1 header : getContentHeaders())
        {
            byHash.put(header.getPathHash().toLowerCase(Locale.ENGLISH), header);
        }

        String       filename = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
        List<String> names    = new ArrayList<>();
        int          wadIndex = filename.lastIndexOf(".wad.");
        if (wadIndex >= 0)
        {
            // x.wad.client -> x.wad.subchunktoc
            names.add(filename.substring(0, wadIndex + ".wad".length()) + ".subchunktoc");
        }
        names.add(filename + ".subchunktoc");

        List<String> parents = new ArrayList<>();
        for (Path parent = path.getParent(); parent != null && parent.getFileName() != null; parent = parent.getParent())
        {
            parents.add(parent.getFileName().toString().toLowerCase(Locale.ENGLISH));
        }

        StringBuilder prefix = new StringBuilder();
        for (int depth = 0; depth <= parents.size(); depth++)
        {
            if (depth > 0)
            {
                prefix.insert(0, parents.get(depth - 1) + "/");
            }

            for (String name : names)
            {
                long               hash  = HashHandler.computeXXHash64AsLong(prefix + name);
                WADContentHeaderV1 found = byHash.get(HashHandler.toHex(hash, 16).toLowerCase(Locale.ENGLISH));
                if (found != null)
                {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Parses a subchunk toc entry, returns null if the data does not look like a subchunk toc
     */
    private List<SubChunkInfo> parseSubChunkToc(WADContentHeaderV1 tocHeader)
    {
        if (tocHeader.getCompressionType() == WADCompressionType.ZSTD_MULTI || tocHeader.getCompressionType() == WADCompressionType.REFERENCE)
        {
            return null;
        }

        byte[] bytes;
        try
        {
            bytes = readContentFromHeaderData(tocHeader);
        } catch (RuntimeException e)
        {
            return null;
        }

        if (bytes == null || bytes.length % 16 != 0)
        {
            return null;
        }

        RandomAccessReader reader = new RandomAccessReader(bytes);
        List<SubChunkInfo> chunks = new ArrayList<>(bytes.length / 16);
        while (reader.hasMoreBytes())
        {
            long compressed   = reader.readU32();
            long uncompressed = reader.readU32();
            long hash         = reader.readLong();

            // a compressed chunk is never stored if it is bigger than the raw data
            if (compressed > uncompressed)
            {
                return null;
            }

            chunks.add(new SubChunkInfo(uncompressed, compressed, hash));
        }

        return chunks;
    }
    
    public void extractFiles(Path path, String wadName)
    {
        final int interval = (int) Math.floor(getContentHeaders().size() / 10f);
        for (int index = 0; index < getContentHeaders().size(); index++)
        {
            WADContentHeaderV1 fileHeader = getContentHeaders().get(index);
            
            if (contentHeaders.size() > 1500)
            {
                if (index % interval == 0)
                {
                    System.out.println(index + "/" + getContentHeaders().size());
                }
            }
            
            saveFile(fileHeader, path, wadName);
        }
    }
    
    public void saveFile(WADContentHeaderV1 header, Path savePath, String wadName)
    {
        String unhashed = HashHandler.getWadHash(header.getPathHash());
        String filename = unhashed.equals(header.getPathHash()) ? header.getPathHash() : unhashed;
        Path   self     = savePath.resolve(filename);
        
        if (self.toString().length() > 255)
        {
            self = self.resolveSibling("too_long_filename_" + header.getPathHash() + "." + UtilHandler.getEnding(self));
        }
        
        self.getParent().toFile().mkdirs();
        String parentName = self.getParent().getFileName().toString();
        byte[] data       = readContentFromHeaderData(header);
        
        if (filename.endsWith("json"))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        try
        {
            if (header.getPathHash().equals(filename))
            {
                findFileTypeAndRename(data, filename, savePath, wadName);
            } else
            {
                Files.createDirectories(self.getParent());
                if (!filename.contains("."))
                {
                    saveFileWithNoExtension(filename, savePath, data);
                } else
                {
                    writeWithConversions(self, data);
                }
            }
            
        } catch (IOException e)
        {
            System.out.println("Failed to write " + self);
            e.printStackTrace();
        }
    }
    
    private void saveFileWithNoExtension(String filename, Path savePath, byte[] data) throws IOException
    {
        if (new AtlasParser().canParse(data))
        {
            Files.write(savePath.resolve(filename + ".atlas"), data);
            return;
        }
        
        if (new BINParser().canParse(data))
        {
            Files.write(savePath.resolve(filename + ".bin"), data);
            return;
        }
        
        Files.write(savePath.resolve(filename + ".unknown"), data);
        System.out.println("Unable to determine the type of file " + savePath + "/" + filename);
    }
    
    
    public synchronized byte[] readContentFromHeaderData(WADContentHeaderV1 header)
    {
        long offset = header.getOffset();
        if (offset + header.getCompressedFileSize() > fileReader.size())
        {
            throw new ParseException("WAD entry " + header.getPathHash() + " points outside the file", offset);
        }

        fileReader.seek(offset);

        switch (header.getCompressionType())
        {
            case NONE:
                return fileReader.readBytes(header.getFileSize());
            case GZIP:
                return checkDecompressed(header, CompressionHandler.uncompressGZIP(fileReader.readBytes(header.getCompressedFileSize())));
            case REFERENCE:
                return fileReader.readFixedString(fileReader.readU32()).getBytes(StandardCharsets.UTF_8);
            case ZSTD:
                return checkDecompressed(header, CompressionHandler.uncompressZSTD(fileReader.readBytes(header.getCompressedFileSize()), toArraySize(header.getFileSize(), header)));
            case ZSTD_MULTI:
                return readSubChunked(header);
            default:
                throw new ParseException(header.getCompressionType().name() + " NOT HANDLED in readContentFromHeaderData", offset);
        }
    }

    private byte[] readSubChunked(WADContentHeaderV1 header)
    {
        long               offset = header.getOffset();
        List<SubChunkInfo> chunks = findSubChunkMap(getTotalSubChunkCount());

        // findSubChunkMap may have read other entries
        fileReader.seek(offset);

        if (chunks == null)
        {
            // No subchunk TOC; if every chunk is zstd compressed, the entry is just a series of zstd frames
            byte[] data = CompressionHandler.uncompressZSTD(fileReader.readBytes(header.getCompressedFileSize()), toArraySize(header.getFileSize(), header));
            if (data == null || data.length != header.getFileSize())
            {
                throw new ParseException("Unable to determine subchunk file, and " + header.getPathHash() + " is not plain zstd", offset);
            }
            return data;
        }

        WADContentHeaderV2 realHeader = (WADContentHeaderV2) header;
        int                start      = realHeader.getSubChunkOffset();
        int                end        = start + realHeader.getSubChunkCount();
        if (end > chunks.size())
        {
            throw new ParseException("WAD entry " + header.getPathHash() + " references subchunk " + (end - 1) + ", but the toc only has " + chunks.size(), offset);
        }

        byte[] output   = new byte[toArraySize(header.getFileSize(), header)];
        int    writePos = 0;
        for (int i = start; i < end; i++)
        {
            SubChunkInfo chunk = chunks.get(i);
            if (writePos + chunk.uncompressed > output.length)
            {
                throw new ParseException("Subchunks of " + header.getPathHash() + " are bigger than the file size", fileReader);
            }

            byte[] data = fileReader.readBytes(chunk.compressed);
            if (data.length != chunk.compressed)
            {
                throw new ParseException("Unexpected end of file in subchunk " + i + " of " + header.getPathHash(), fileReader);
            }

            if (chunk.compressed == chunk.uncompressed)
            {
                // chunks that would not shrink are stored raw
                System.arraycopy(data, 0, output, writePos, data.length);
            } else
            {
                byte[] uncompressed = CompressionHandler.uncompressZSTD(data, (int) chunk.uncompressed);
                if (uncompressed == null || uncompressed.length != chunk.uncompressed)
                {
                    throw new ParseException("Failed to decompress subchunk " + i + " of " + header.getPathHash(), fileReader);
                }
                System.arraycopy(uncompressed, 0, output, writePos, uncompressed.length);
            }
            writePos += (int) chunk.uncompressed;
        }

        if (writePos != output.length)
        {
            throw new ParseException("Subchunks of " + header.getPathHash() + " contain " + writePos + " bytes, expected " + output.length, offset);
        }

        return output;
    }

    private static int toArraySize(long size, WADContentHeaderV1 header)
    {
        if (size > Integer.MAX_VALUE - 8)
        {
            throw new ParseException("WAD entry " + header.getPathHash() + " is too big to load into memory (" + size + " bytes)", header.getOffset());
        }
        return (int) size;
    }

    private static byte[] checkDecompressed(WADContentHeaderV1 header, byte[] data)
    {
        if (data == null)
        {
            throw new ParseException("Failed to decompress WAD entry " + header.getPathHash(), header.getOffset());
        }
        return data;
    }
    
    
    private String findFileTypeAndRename(byte[] data, String filename, Path parent, String wadName) throws IOException
    {
        ByteArray magic    = new ByteArray(Arrays.copyOf(data, data.length));
        String    fileType = FileTypeHandler.findFileType(magic);
        
        if ("unknown".equals(fileType))
        {
            System.err.format("Unknown filetype: %s\\%s %s%n", parent, filename, magic.copyOfRange(0, 4));
        }
        
        StringBuilder sb    = new StringBuilder(filename).append(".").append(fileType);
        Path          other = parent.resolve("unknown").resolve(wadName).resolve(sb.toString());
        
        if ("json".equals(fileType))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        Files.createDirectories(other.getParent());
        writeWithConversions(other, data);

        return fileType;
    }

    /**
     * Writes the file. TEX textures are converted instead of written as-is (like CommunityDragon's exports):
     * a .png if the texture can be decoded, else a .dds (which other tools can open), and only if both fail the original .tex.
     * The converted file is named by replacing a trailing ".tex", or by appending the new extension.
     */
    private static void writeWithConversions(Path output, byte[] data) throws IOException
    {
        if (!TEXParser.isTEX(data))
        {
            Files.write(output, data);
            return;
        }

        String name = output.getFileName().toString();
        String base = name.toLowerCase(Locale.ENGLISH).endsWith(".tex") ? name.substring(0, name.length() - ".tex".length()) : name;

        String pngError;
        try
        {
            BufferedImage image = new TEXParser().parse(new ByteArray(data));
            if (ImageIO.write(image, "png", output.resolveSibling(base + ".png").toFile()))
            {
                return;
            }
            pngError = "no png writer for the decoded image";
        } catch (ParseException e)
        {
            pngError = e.getMessage();
        }

        try
        {
            Files.write(output.resolveSibling(base + ".dds"), TEXParser.toDDS(data));
        } catch (ParseException unsupported)
        {
            System.out.println("Unable to convert " + output + " (png: " + pngError + ", dds: " + unsupported.getMessage() + "), writing the original tex");
            Files.write(output, data);
        }
    }
    
    public void printUnknownFiles(String wadfilename, Path outputFile)
    {
        try
        {
            StandardOpenOption[] flags   = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            List<String>         outputs = new ArrayList<>();
            this.getContentHeaders().forEach(header -> {
                String filename = HashHandler.getWadHash(header.getPathHash());
                if (!filename.equals(header.getPathHash()))
                {
                    return;
                }
                
                byte[] data     = readContentFromHeaderData(header);
                String filetype = FileTypeHandler.findFileType(new ByteArray(data));
                String output   = String.format("%s : %-15s : %s", filename, filetype, wadfilename);
                outputs.add(output);
            });
            
            Files.write(outputFile, outputs, StandardCharsets.UTF_8, flags);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
