package no.stelar7.cdragon.types.rofl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.rofl.data.*;
import no.stelar7.cdragon.util.handlers.JsonHandler;
import no.stelar7.cdragon.util.readers.*;

import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ROFL (v1): https://github.com/robertabcd/lol-ob/wiki/ROFL-Container-Notes
 * <p>
 * ROFL2 ("RIOT\2\0"): the header game version and the trailing metadata json (https://github.com/fraxiinus/roflxd.cs Rofl2Reader),
 * and the payload chunks ({@link ROFLFile#getChunks()}: zstd-compressed, split into packets by {@link ROFL2Chunk#getBlocks()})
 * as reverse engineered by https://github.com/Toastaspiring/ROFL-X / https://github.com/Mowokuma/ROFL.
 * {@link ROFLFile#getPayloadHeader()} is null and {@link ROFLFile#getEntries()} is empty for those files.
 */
public class ROFLParser implements Parseable<ROFLFile>
{
    private static final byte[] MAGIC_V1 = {'R', 'I', 'O', 'T', 0, 0};
    private static final byte[] MAGIC_V2 = {'R', 'I', 'O', 'T', 2, 0};

    private static final int V1_HEADER_SIZE = 6 + 256 + 2 + 4 * 6;

    private static final int V2_VERSION_LENGTH_OFFSET = 0x0E;
    private static final int V2_SIGNATURE_SIZE        = 0x100;

    @Override
    public ROFLFile parse(RandomAccessReader raf)
    {
        raf.setEndian(ByteOrder.LITTLE_ENDIAN);

        if (raf.size() < 6)
        {
            throw new ParseException("File is too small to be a ROFL file", 0);
        }

        raf.seek(0);
        byte[] magic = raf.readBytes(6);
        if (Arrays.equals(magic, MAGIC_V1))
        {
            return parseV1(raf);
        }

        if (Arrays.equals(magic, MAGIC_V2))
        {
            return parseV2(raf);
        }

        throw new ParseException("Not a ROFL file, or an unsupported ROFL version (magic " + Arrays.toString(magic) + ")", 0);
    }

    private ROFLFile parseV2(RandomAccessReader raf)
    {
        if (raf.size() < 14 + 15 + 4)
        {
            throw new ParseException("File is too small to be a ROFL2 file", 0);
        }

        ROFLHeader header = new ROFLHeader();
        header.setMagic(new String(MAGIC_V2, StandardCharsets.UTF_8));
        header.setVersion(2);

        // u8 length at 0x0E, followed by the version string
        raf.seek(V2_VERSION_LENGTH_OFFSET);
        int  versionLength = raf.readU8();
        long payloadStart;
        byte[] versionBytes = raf.readBytes(Math.min(versionLength, 32));
        if (versionLength > 0 && versionLength <= 32 && versionBytes.length == versionLength && isPrintable(versionBytes))
        {
            header.setGameVersion(new String(versionBytes, StandardCharsets.US_ASCII));
            payloadStart = V2_VERSION_LENGTH_OFFSET + 1 + versionLength;
        } else
        {
            raf.seek(V2_VERSION_LENGTH_OFFSET);
            header.setGameVersion(raf.readFixedString(15).trim());
            payloadStart = -1;
        }

        raf.seekFromEnd(-4);
        long metadataLength = raf.readU32();
        long metadataOffset = raf.size() - 4 - metadataLength;
        if (metadataOffset < 29)
        {
            throw new ParseException("Invalid ROFL2 metadata length " + metadataLength, raf.size() - 4);
        }

        header.setFileSize((int) Math.min(Integer.MAX_VALUE, raf.size()));
        header.setMetadataOffset((int) metadataOffset);
        header.setMetadataLength((int) metadataLength);

        raf.seek(metadataOffset);
        ROFLMetadata metadata = parseMetadata(raf.readString(metadataLength), metadataOffset);
        if (metadata.getGameVersion() == null)
        {
            metadata.setGameVersion(header.getGameVersion());
        }
        header.setMetadata(metadata);

        ROFLFile file = new ROFLFile();
        file.setHeader(header);
        file.setEntries(new ArrayList<>());
        parseV2Chunks(raf, file, payloadStart, metadataOffset - V2_SIGNATURE_SIZE);
        return file;
    }

    /**
     * Splits the ROFL2 payload (between the header and the 256 byte signature before the metadata) into its chunks.
     * The payload layout is not documented by Riot; this follows https://github.com/Toastaspiring/ROFL-X (src/rofl/mod.rs, chunk.rs).
     * Problems are recorded in {@link ROFLFile#getPayloadError()} instead of failing, so the metadata stays usable.
     */
    private void parseV2Chunks(RandomAccessReader raf, ROFLFile file, long start, long end)
    {
        List<ROFL2Chunk> chunks = new ArrayList<>();
        file.setChunks(chunks);

        if (start < 0 || end < start)
        {
            file.setPayloadError("The file has no room for a payload and signature (payload " + start + " to " + end + ")");
            return;
        }

        raf.seek(end);
        file.setSignature(raf.readBytes(V2_SIGNATURE_SIZE));

        long pos = start;
        while (pos < end)
        {
            if (end - pos < ROFL2Chunk.HEADER_SIZE)
            {
                file.setPayloadError("Truncated chunk header at " + pos);
                return;
            }

            raf.seek(pos);
            ROFL2Chunk chunk = new ROFL2Chunk();
            chunk.setOffset(pos);
            chunk.setChunkId(raf.readU32());
            chunk.setChunkType(raf.readU8());
            chunk.setChunkId2(raf.readU32());
            chunk.setUncompressedLength(raf.readU32());
            chunk.setCompressedLength(raf.readU32());

            long bodyLength = chunk.isCompressed() ? chunk.getCompressedLength() : chunk.getUncompressedLength();
            long bodyEnd    = pos + ROFL2Chunk.HEADER_SIZE + bodyLength;
            if (bodyEnd > end)
            {
                file.setPayloadError("Chunk " + chunk.getChunkId() + " at " + pos + " (" + bodyLength + " bytes) runs past the end of the payload (" + end + ")");
                return;
            }

            chunk.setBody(raf.readBytes(bodyLength));
            if (!chunks.isEmpty())
            {
                chunk.setDictionarySource(chunks.getFirst());
            }
            chunks.add(chunk);
            pos = bodyEnd;
        }
    }

    private static boolean isPrintable(byte[] data)
    {
        for (byte b : data)
        {
            if (b < 0x20 || b > 0x7E)
            {
                return false;
            }
        }
        return true;
    }

    private ROFLFile parseV1(RandomAccessReader raf)
    {
        if (raf.size() < V1_HEADER_SIZE)
        {
            throw new ParseException("File is too small to be a ROFL file", 0);
        }

        ROFLFile file = new ROFLFile();
        file.setHeader(parseHeader(raf));
        file.setPayloadHeader(parsePayloadHeader(raf, file.getHeader()));
        file.setEntries(parseEntries(raf, file));
        return file;
    }

    private void checkRange(RandomAccessReader raf, long offset, long length, String what)
    {
        if (offset < 0 || length < 0 || offset + length > raf.size())
        {
            throw new ParseException("ROFL " + what + " (offset " + offset + ", length " + length + ") is outside the file (size " + raf.size() + ")", offset);
        }
    }

    private List<ROFLPayloadEntry> parseEntries(RandomAccessReader raf, ROFLFile file)
    {
        ROFLPayloadHeader payloadHeader = file.getPayloadHeader();

        long payloadOffset = Integer.toUnsignedLong(file.getHeader().getPayloadOffset());
        long maxEntries    = (long) payloadHeader.getChunkCount() + payloadHeader.getKeyframeCount();
        if (payloadHeader.getChunkCount() < 0 || payloadHeader.getKeyframeCount() < 0)
        {
            throw new ParseException("Invalid ROFL chunk/keyframe count", Integer.toUnsignedLong(file.getHeader().getPayloadHeaderOffset()));
        }
        checkRange(raf, payloadOffset, maxEntries * ROFLPayloadEntry.HEADER_SIZE, "payload entry table");

        // the key is the same for every chunk, so only decrypt it once
        byte[] chunkKey = ROFLPayloadEntry.decryptChunkKey(payloadHeader.getGameId(), payloadHeader.getEncryptionKey());

        long                   entryDataOffset = payloadOffset + ROFLPayloadEntry.HEADER_SIZE * maxEntries;
        List<ROFLPayloadEntry> entries         = new ArrayList<>();
        for (int i = 0; i < maxEntries; i++)
        {
            raf.seek(payloadOffset + ((long) ROFLPayloadEntry.HEADER_SIZE * i));

            ROFLPayloadEntry entry = new ROFLPayloadEntry();
            entry.setId(raf.readInt());
            entry.setType(raf.readByte());
            entry.setLength(raf.readInt());
            entry.setNextChunkId(raf.readInt());
            entry.setOffset(raf.readInt());

            long dataOffset = entryDataOffset + Integer.toUnsignedLong(entry.getOffset());
            checkRange(raf, dataOffset, Integer.toUnsignedLong(entry.getLength()), "chunk " + entry.getId());

            raf.seek(dataOffset);
            entry.setEncryptedData(raf.readBytes(entry.getLength()), chunkKey);
            entries.add(entry);
        }

        return entries;
    }

    private ROFLPayloadHeader parsePayloadHeader(RandomAccessReader raf, ROFLHeader fileHeader)
    {
        long offset = Integer.toUnsignedLong(fileHeader.getPayloadHeaderOffset());
        long length = Integer.toUnsignedLong(fileHeader.getPayloadHeaderLength());
        checkRange(raf, offset, length, "payload header");
        if (length < 34)
        {
            throw new ParseException("ROFL payload header is too small (" + length + " bytes)", offset);
        }

        raf.seek(offset);
        ROFLPayloadHeader header = new ROFLPayloadHeader();
        header.setGameId(raf.readLong());
        header.setGameLength(raf.readInt());
        header.setKeyframeCount(raf.readInt());
        header.setChunkCount(raf.readInt());
        header.setEndStartupChunkId(raf.readInt());
        header.setStartGameChunkId(raf.readInt());
        header.setKeyframeInterval(raf.readInt());
        header.setEncryptionKeyLength(raf.readShort());

        int keyLength = Short.toUnsignedInt(header.getEncryptionKeyLength());
        if (34 + keyLength > length)
        {
            throw new ParseException("ROFL encryption key length " + keyLength + " exceeds the payload header", raf);
        }

        try
        {
            header.setEncryptionKey(Base64.getDecoder().decode(raf.readString(keyLength, StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e)
        {
            throw new ParseException("Invalid ROFL encryption key", raf.pos(), e);
        }
        return header;
    }

    private ROFLHeader parseHeader(RandomAccessReader raf)
    {
        raf.seek(0);

        ROFLHeader header = new ROFLHeader();
        header.setMagic(raf.readString(6));
        header.setVersion(1);
        header.setSignature(raf.readBytes(256));
        header.setHeaderSize(raf.readShort());
        header.setFileSize(raf.readInt());
        header.setMetadataOffset(raf.readInt());
        header.setMetadataLength(raf.readInt());
        header.setPayloadHeaderOffset(raf.readInt());
        header.setPayloadHeaderLength(raf.readInt());
        header.setPayloadOffset(raf.readInt());

        long offset = Integer.toUnsignedLong(header.getMetadataOffset());
        long length = Integer.toUnsignedLong(header.getMetadataLength());
        checkRange(raf, offset, length, "metadata");

        raf.seek(offset);
        header.setMetadata(parseMetadata(raf.readString(length), offset));
        return header;
    }

    private ROFLMetadata parseMetadata(String json, long offset)
    {
        JsonObject object;
        try
        {
            object = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e)
        {
            throw new ParseException("Invalid ROFL metadata json", offset, e);
        }

        ROFLMetadata metadata = new ROFLMetadata();
        String       rawStats;
        try
        {
            metadata.setGameLength(getLong(object, "gameLength"));
            metadata.setGameVersion(object.has("gameVersion") && !object.get("gameVersion").isJsonNull() ? object.get("gameVersion").getAsString() : null);
            metadata.setLastGameChunkId((int) getLong(object, "lastGameChunkId"));
            metadata.setLastKeyframeId((int) getLong(object, "lastKeyFrameId"));
            rawStats = object.has("statsJson") && !object.get("statsJson").isJsonNull() ? object.get("statsJson").getAsString() : null;
        } catch (RuntimeException e)
        {
            throw new ParseException("Invalid ROFL metadata field: " + e.getMessage(), offset, e);
        }

        if (rawStats != null)
        {
            metadata.setRawStatsJson(rawStats);

            try
            {
                JsonElement             stats     = JsonParser.parseString(rawStats);
                Type                    statsType = new TypeToken<List<ROFLMetadataStats>>() {}.getType();
                List<ROFLMetadataStats> statList  = JsonHandler.getGson().fromJson(stats, statsType);
                metadata.setStatsJson(statList);
            } catch (RuntimeException e)
            {
                // the stats change between patches, keep the raw json available instead of failing the whole replay
                metadata.setStatsJson(null);
            }
        }

        return metadata;
    }

    private long getLong(JsonObject object, String key)
    {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull())
        {
            return 0;
        }

        // some writers store numbers in scientific notation
        return element.getAsBigDecimal().longValue();
    }
}
