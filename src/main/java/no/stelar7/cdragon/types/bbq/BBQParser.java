package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for Unity asset bundles ("UnityFS", and the older "UnityRaw" / "UnityWeb" containers).
 * UnityCN encrypted bundles can be read after setting the game's key with {@link #setUnityCNKey(byte[])}.
 * See https://github.com/K0lb3/UnityPy/blob/master/UnityPy/files/BundleFile.py
 */
public class BBQParser implements Parseable<BBQFile>
{
    private byte[] unityCNKey;

    public BBQParser()
    {
    }

    /**
     * @param unityCNKey the 16 byte per-game key used to decrypt UnityCN encrypted bundles
     */
    public BBQParser(byte[] unityCNKey)
    {
        setUnityCNKey(unityCNKey);
    }

    /**
     * Sets the 16 byte per-game key used to decrypt UnityCN encrypted bundles (null to clear it)
     */
    public void setUnityCNKey(byte[] key)
    {
        if (key != null && key.length != 16)
        {
            throw new IllegalArgumentException("UnityCN keys are 16 bytes, got " + key.length);
        }
        this.unityCNKey = key == null ? null : key.clone();
    }

    /**
     * Sets the UnityCN key from its text form (16 characters, as the keys are usually published)
     */
    public void setUnityCNKey(String key)
    {
        setUnityCNKey(key == null ? null : key.getBytes(StandardCharsets.UTF_8));
    }

    private static final int FLAG_COMPRESSION_MASK          = 0x3F;
    private static final int FLAG_BLOCKS_AND_DIR_COMBINED   = 0x40;
    private static final int FLAG_BLOCKS_INFO_AT_END        = 0x80;
    // before 2020.3.34 / 2021.3.2 / 2022.1.1, 0x200 meant the bundle was encrypted (UnityCN)
    private static final int FLAG_OLD_ENCRYPTION            = 0x200;
    private static final int FLAG_BLOCK_INFO_NEEDS_PADDING  = 0x200;
    private static final int FLAG_ENCRYPTION                = 0x1400;

    @Override
    public BBQFile parse(RandomAccessReader raf)
    {
        // the bundle header and block info are big-endian
        raf.setEndian(ByteOrder.BIG_ENDIAN);
        raf.seek(0);

        try
        {
            BBQFile        file   = new BBQFile();
            BBQHeader      header = parseHeader(raf);
            List<BBQAsset> assets = usesFSLayout(header) ? parseEntryList(raf, header) : parseWebRaw(raf, header);

            file.setHeader(header);
            file.setEntries(assets);
            return file;
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of bundle data", raf.pos(), e);
        }
    }

    /**
     * Reads an entry count and checks that that many entries (of at least minEntrySize bytes) fit in the rest of the data
     */
    private static int readCount(RandomAccessReader raf, int minEntrySize)
    {
        int count = raf.readInt();
        if (count < 0 || (long) count * minEntrySize > raf.remaining())
        {
            throw new ParseException("Invalid entry count " + count + " (" + raf.remaining() + " bytes left)", raf.pos() - 4);
        }
        return count;
    }

    private static void align16(RandomAccessReader raf)
    {
        raf.seek((raf.pos() + 15) & ~15L);
    }

    private List<BBQAsset> parseEntryList(RandomAccessReader raf, BBQHeader header)
    {
        long   start = raf.pos();
        byte[] data;
        if (header.isMetadataAtEnd())
        {
            raf.seek(raf.size() - header.getMetadataCompressedSize());
            data = raf.readBytes(header.getMetadataCompressedSize());
            raf.seek(start);
        } else
        {
            data = raf.readBytes(header.getMetadataCompressedSize());
        }

        if (data.length != header.getMetadataCompressedSize())
        {
            throw new ParseException("Bundle block info is truncated", start);
        }

        // like UnityPy, the block info is decrypted (as block 0) if the header has flag 0x100 set
        if (header.getUnityCN() != null && (header.getFlags() & BBQUnityCN.BLOCK_FLAG) != 0 && header.getCompressionMode() != BBQCompressionType.NONE && header.getMetadataUncompressedSize() != 0)
        {
            data = header.getUnityCN().decryptBlock(data, 0);
        }

        data = BBQBlockInfo.decompress(header.getCompressionMode(), data, header.getMetadataUncompressedSize());

        RandomAccessReader metaReader = new RandomAccessReader(data, ByteOrder.BIG_ENDIAN);
        byte[]             guid       = metaReader.readBytes(16);

        int                blocks    = readCount(metaReader, header.getFormatVersion() >= 9 ? 18 : 10);
        List<BBQBlockInfo> blockList = new ArrayList<>(blocks);
        for (int i = 0; i < blocks; i++)
        {
            BBQBlockInfo block = new BBQBlockInfo();
            block.setUncompressedSize(metaReader.readInt());
            block.setCompressedSize(metaReader.readInt());
            block.setFlags(metaReader.readU16());
            if (header.getFormatVersion() >= 9)
            {
                block.setOffset(metaReader.readLong());
            }
            blockList.add(block);
        }

        int                  nodeCount = readCount(metaReader, 8 + 8 + 4 + 1);
        List<BBQBundleEntry> nodes     = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++)
        {
            nodes.add(parseMetaEntry(metaReader));
        }

        if (!usesOldArchiveFlags(header) && (header.getFlags() & FLAG_BLOCK_INFO_NEEDS_PADDING) != 0)
        {
            align16(raf);
        }

        // the block data starts right after the header (and the block info, if it is not at the end)
        long blockDataStart = raf.pos();
        header.setHeaderSize((int) blockDataStart);
        BBQBlockStore storage = new BBQBlockStore(blockList, raf, blockDataStart, header.getUnityCN());

        nodes.sort(Comparator.comparing(BBQBundleEntry::getOffset));

        List<BBQAsset> assets = new ArrayList<>(nodes.size());
        for (BBQBundleEntry node : nodes)
        {
            BBQAsset asset = BBQAsset.fromBundle(storage, header, node);
            asset.load();
            assets.add(asset);
        }

        return assets;
    }

    private BBQBundleEntry parseMetaEntry(RandomAccessReader meta)
    {
        BBQBundleEntry entry = new BBQBundleEntry();
        entry.setOffset(meta.readLong());
        entry.setSize(meta.readLong());
        entry.setFlags(meta.readInt());
        entry.setName(meta.readString());
        return entry;
    }

    private BBQHeader parseHeader(RandomAccessReader raf)
    {
        BBQHeader header = new BBQHeader();

        header.setSignature(raf.readString());
        if (!header.getSignature().startsWith("Unity"))
        {
            throw new ParseException("Invalid file signature \"" + header.getSignature() + "\"", 0);
        }

        header.setFormatVersion(raf.readInt());
        header.setUnityVersion(raf.readString());
        header.setGeneratorVersion(raf.readString());

        if (!header.isUnityFS() && !header.isRAW() && !header.isWEB())
        {
            throw new ParseException("Unable to parse \"" + header.getSignature() + "\" bundles, only UnityFS, UnityRaw and UnityWeb are supported", 0);
        }

        if (usesFSLayout(header))
        {
            loadUnityFS(header, raf);
        }
        return header;
    }

    /**
     * UnityFS bundles, and UnityRaw / UnityWeb bundles of format 6, use the block based layout
     */
    private static boolean usesFSLayout(BBQHeader header)
    {
        return header.isUnityFS() || header.getFormatVersion() == 6;
    }

    /**
     * Reads the old UnityRaw (uncompressed) and UnityWeb (one LZMA stream) containers.
     * See BundleFile.read_web_raw in UnityPy.
     */
    private List<BBQAsset> parseWebRaw(RandomAccessReader raf, BBQHeader header)
    {
        int version = header.getFormatVersion();
        if (version >= 4)
        {
            // hash, crc
            raf.readBytes(16);
            raf.readInt();
        }

        long minimumStreamedBytes = raf.readU32();
        long headerSize           = raf.readU32();
        long levelsBeforeStream   = raf.readU32();
        int  levelCount           = raf.readInt();
        if (levelCount < 1)
        {
            throw new ParseException("Invalid level count " + levelCount, raf.pos() - 4);
        }

        // only the sizes of the last level matter, as they include the earlier ones
        raf.seek(raf.pos() + 8L * (levelCount - 1));
        long compressedSize   = raf.readU32();
        long uncompressedSize = raf.readU32();
        long completeFileSize = raf.size();
        if (version >= 2)
        {
            completeFileSize = raf.readU32();
        }
        if (version >= 3)
        {
            // file info header size
            raf.readU32();
        }

        if (headerSize + compressedSize > raf.size() || compressedSize > Integer.MAX_VALUE)
        {
            throw new ParseException("Bundle data (offset " + headerSize + ", size " + compressedSize + ") is outside the file (size " + raf.size() + ")", headerSize);
        }

        raf.seek(headerSize);
        byte[] data = raf.readBytes(compressedSize);
        if (header.isWEB())
        {
            data = CompressionHandler.uncompressLZMA(data);
        }

        header.setTotalFileSize(completeFileSize);
        header.setHeaderSize((int) headerSize);
        header.setMetadataCompressedSize((int) compressedSize);
        header.setMetadataUncompressedSize((int) uncompressedSize);
        header.setCompressionMode(header.isWEB() ? BBQCompressionType.LZMA : BBQCompressionType.NONE);

        RandomAccessReader   reader    = new RandomAccessReader(data, ByteOrder.BIG_ENDIAN);
        int                  nodeCount = readCount(reader, 1 + 4 + 4);
        List<BBQBundleEntry> nodes     = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++)
        {
            BBQBundleEntry entry = new BBQBundleEntry();
            entry.setName(reader.readString());
            entry.setOffset(reader.readU32());
            entry.setSize(reader.readU32());
            nodes.add(entry);
        }

        // the directory offsets are relative to the start of the (decompressed) data, so present it as one uncompressed block
        BBQBlockInfo block = new BBQBlockInfo();
        block.setUncompressedSize(data.length);
        block.setCompressedSize(data.length);
        block.setFlags(0);
        BBQBlockStore storage = new BBQBlockStore(List.of(block), new RandomAccessReader(data, ByteOrder.BIG_ENDIAN), 0);

        List<BBQAsset> assets = new ArrayList<>(nodes.size());
        for (BBQBundleEntry node : nodes)
        {
            BBQAsset asset = BBQAsset.fromBundle(storage, header, node);
            asset.load();
            assets.add(asset);
        }
        return assets;
    }

    /**
     * Parses the first three numbers of a version like "2019.4.40f1"; returns null if it can not be parsed
     */
    private static int[] parseVersion(String version)
    {
        if (version == null)
        {
            return null;
        }

        String[] parts  = version.split("[^0-9]+");
        int[]    result = new int[3];
        int      found  = 0;
        for (String part : parts)
        {
            if (part.isEmpty())
            {
                continue;
            }
            if (found == 3)
            {
                break;
            }

            try
            {
                result[found++] = Integer.parseInt(part);
            } catch (NumberFormatException e)
            {
                return null;
            }
        }

        return (found == 3 && result[0] != 0) ? result : null;
    }

    private static boolean isBefore(int[] version, int major, int minor, int patch)
    {
        return Arrays.compare(version, new int[]{major, minor, patch}) < 0;
    }

    /**
     * Unity changed the meaning of flag 0x200 in 2020.3.34, 2021.3.2 and 2022.1.1
     */
    private static boolean usesOldArchiveFlags(BBQHeader header)
    {
        int[] v = parseVersion(header.getGeneratorVersion());
        if (v == null)
        {
            // stripped version strings are only used by new versions
            return false;
        }

        return v[0] < 2020 ||
               (v[0] == 2020 && isBefore(v, 2020, 3, 34)) ||
               (v[0] == 2021 && isBefore(v, 2021, 3, 2)) ||
               (v[0] == 2022 && isBefore(v, 2022, 1, 1));
    }

    private void loadUnityFS(BBQHeader header, RandomAccessReader raf)
    {
        header.setTotalFileSize(raf.readLong());
        header.setMetadataCompressedSize(raf.readInt());
        header.setMetadataUncompressedSize(raf.readInt());
        header.setFlags(raf.readInt());

        // UnityRaw / UnityWeb format 6 have an extra byte here
        if (!header.isUnityFS())
        {
            raf.readByte();
        }
        header.setCompressionMode(BBQCompressionType.from(header.getFlags() & FLAG_COMPRESSION_MASK));
        header.setBlockAndDirCombined((header.getFlags() & FLAG_BLOCKS_AND_DIR_COMBINED) != 0);
        header.setMetadataAtEnd((header.getFlags() & FLAG_BLOCKS_INFO_AT_END) != 0);

        int encryptionFlag = usesOldArchiveFlags(header) ? FLAG_OLD_ENCRYPTION : FLAG_ENCRYPTION;
        if ((header.getFlags() & encryptionFlag) != 0)
        {
            header.setUnityCN(new BBQUnityCN(raf, unityCNKey));
        }

        // format 7+ (and some 2019.4.15+ bundles that still say 6) align the block info to 16 bytes
        int[] version = parseVersion(header.getGeneratorVersion());
        if (header.getFormatVersion() >= 7 || (version != null && version[0] == 2019 && !isBefore(version, 2019, 4, 15)))
        {
            align16(raf);
        }

        header.setHeaderSize((int) raf.pos() + (header.isMetadataAtEnd() ? 0 : header.getMetadataCompressedSize()));
    }
}
