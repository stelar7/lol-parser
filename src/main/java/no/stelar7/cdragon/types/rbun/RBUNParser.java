package no.stelar7.cdragon.types.rbun;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * A bundle is the compressed chunks back to back, followed by a table of contents and a footer (see moonshadow565/rman rbundle.hpp):
 * <pre>
 * toc    : entryCount * { u64 chunkId, u32 uncompressedSize, u32 compressedSize }, in the same order as the chunk data
 * footer : u8[8] checksum (the bundle id in version 1, xxhash64 of the toc in version 0xFFFFFFFF), u32 entryCount, u32 version, "RBUN"
 * </pre>
 */
public class RBUNParser implements Parseable<RBUNFile>
{
    public static final int FOOTER_SIZE = 8 + 4 + 4 + 4;
    public static final int ENTRY_SIZE  = 8 + 4 + 4;

    @Override
    public RBUNFile parse(RandomAccessReader raf)
    {
        raf.setEndian(ByteOrder.LITTLE_ENDIAN);

        long size = raf.size();
        if (size < FOOTER_SIZE)
        {
            throw new ParseException("File is too small to be a RBUN", 0);
        }

        raf.seek(size - FOOTER_SIZE);
        long checksum   = raf.readLong();
        long chunkCount = raf.readU32();
        int  version    = raf.readInt();

        RBUNFile file = new RBUNFile();
        file.setMagic(raf.readString(4));
        file.setVersion(version);
        file.setBundleId(HashHandler.toHex(checksum, 16));

        if (!"RBUN".equals(file.getMagic()))
        {
            throw new ParseException("Invalid RBUN magic: " + file.getMagic(), size - 4);
        }

        if (version != 1 && version != 0xFFFFFFFF)
        {
            throw new ParseException("Unsupported RBUN version " + Integer.toUnsignedString(version), size - 8);
        }

        long tocOffset = size - FOOTER_SIZE - (chunkCount * ENTRY_SIZE);
        if (tocOffset < 0)
        {
            throw new ParseException("RBUN chunk table (" + chunkCount + " entries) is larger than the file", size - 12);
        }
        file.setChunkCount((int) chunkCount);

        raf.seek(tocOffset);
        List<RBUNChunkInfo> chunkInfos = new ArrayList<>((int) chunkCount);
        for (long i = 0; i < chunkCount; i++)
        {
            RBUNChunkInfo ci = new RBUNChunkInfo();
            ci.setChunkId(HashHandler.toHex(raf.readLong(), 16));
            ci.setUncompressedSize(raf.readU32());
            ci.setCompressedSize(raf.readU32());
            chunkInfos.add(ci);
        }

        file.setChunks(chunkInfos);
        return file;
    }
}
