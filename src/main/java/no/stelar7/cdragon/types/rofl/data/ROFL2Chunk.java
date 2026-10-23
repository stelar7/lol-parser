package no.stelar7.cdragon.types.rofl.data;

import com.github.luben.zstd.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * One chunk record of a ROFL2 replay payload. All fields are little-endian:
 * <pre>
 * 0x00 u32 chunk id
 * 0x04 u8  chunk type
 * 0x05 u32 chunk id 2          high byte is the stream: 1 = game chunk, 2 = keyframe, 3 = start keyframe, 4 = start marker
 * 0x09 u32 uncompressed length
 * 0x0D u32 compressed length   0 = the body is stored uncompressed
 * 0x11 body                    one zstd frame (compressed length bytes), or uncompressed length raw bytes
 * </pre>
 * See src/rofl/chunk.rs and docs/ROFL_FORMAT.md in https://github.com/Toastaspiring/ROFL-X
 */
public class ROFL2Chunk
{
    public static final int HEADER_SIZE = 0x11;

    public static final int STREAM_GAME_CHUNK     = 1;
    public static final int STREAM_KEYFRAME       = 2;
    public static final int STREAM_START_KEYFRAME = 3;
    public static final int STREAM_START_MARKER   = 4;

    private long   offset;
    private long   chunkId;
    private int    chunkType;
    private long   chunkId2;
    private long   uncompressedLength;
    private long   compressedLength;
    private byte[] body;

    private transient ROFL2Chunk dictionarySource;
    private transient byte[]     data;

    /**
     * Offset of the chunk header in the file
     */
    public long getOffset()
    {
        return offset;
    }

    public void setOffset(long offset)
    {
        this.offset = offset;
    }

    public long getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(long chunkId)
    {
        this.chunkId = chunkId;
    }

    public int getChunkType()
    {
        return chunkType;
    }

    public void setChunkType(int chunkType)
    {
        this.chunkType = chunkType;
    }

    public long getChunkId2()
    {
        return chunkId2;
    }

    public void setChunkId2(long chunkId2)
    {
        this.chunkId2 = chunkId2;
    }

    /**
     * The high byte of chunk id 2, see the STREAM_ constants
     */
    public int getStream()
    {
        return (int) ((chunkId2 >>> 24) & 0xFF);
    }

    public long getUncompressedLength()
    {
        return uncompressedLength;
    }

    public void setUncompressedLength(long uncompressedLength)
    {
        this.uncompressedLength = uncompressedLength;
    }

    public long getCompressedLength()
    {
        return compressedLength;
    }

    public void setCompressedLength(long compressedLength)
    {
        this.compressedLength = compressedLength;
    }

    public boolean isCompressed()
    {
        return compressedLength > 0;
    }

    /**
     * The body as stored in the file
     */
    public byte[] getBody()
    {
        return body;
    }

    public void setBody(byte[] body)
    {
        this.body = body;
        this.data = null;
    }

    /**
     * The chunk whose decompressed data is used as a zstd dictionary if this chunk does not decompress on its own
     * (one public parser reports that some replays need the first chunk as a dictionary, others decompress every chunk on its own)
     */
    public void setDictionarySource(ROFL2Chunk dictionarySource)
    {
        this.dictionarySource = dictionarySource;
    }

    /**
     * Returns the decompressed chunk data
     */
    public byte[] getData()
    {
        if (data == null)
        {
            data = decompress();
        }
        return data;
    }

    // replay chunks are a few MB at most
    private static final long MAX_UNCOMPRESSED_LENGTH = 256L * 1024 * 1024;

    private byte[] decompress()
    {
        if (!isCompressed())
        {
            return body;
        }

        // the length comes straight from the file, so check it before allocating the output
        if (uncompressedLength < 0 || uncompressedLength > MAX_UNCOMPRESSED_LENGTH)
        {
            throw new ParseException("ROFL2 chunk " + chunkId + " is too large (" + uncompressedLength + " bytes)", offset);
        }

        long frameSize = Zstd.getFrameContentSize(body);
        if (frameSize >= 0 && frameSize != uncompressedLength)
        {
            throw new ParseException("ROFL2 chunk " + chunkId + " says it is " + uncompressedLength + " bytes, but its zstd frame is " + frameSize, offset);
        }

        int    size   = (int) uncompressedLength;
        byte[] output = new byte[size];
        String error  = attempt(() -> Zstd.decompressByteArray(output, 0, size, body, 0, body.length), size);
        if (error == null)
        {
            return output;
        }

        if (dictionarySource != null && dictionarySource != this)
        {
            byte[] dictionary = dictionarySource.getData();
            String dictError  = attempt(() -> Zstd.decompressUsingDict(output, 0, body, 0, body.length, dictionary), size);
            if (dictError == null)
            {
                return output;
            }
            error += ", with the first chunk as dictionary: " + dictError;
        }

        throw new ParseException("Unable to decompress ROFL2 chunk " + chunkId + " (" + error + ")", offset);
    }

    /**
     * Runs one decompression; returns null on success, else the reason it failed
     */
    private static String attempt(java.util.function.LongSupplier decompression, int expectedSize)
    {
        try
        {
            long result = decompression.getAsLong();
            if (Zstd.isError(result))
            {
                return Zstd.getErrorName(result);
            }
            return result == expectedSize ? null : "got " + result + " of " + expectedSize + " bytes";
        } catch (RuntimeException e)
        {
            return e.getMessage();
        }
    }

    /**
     * Splits the decompressed data into its blocks (packets)
     */
    public List<ROFL2Block> getBlocks()
    {
        byte[]             bytes  = getData();
        RandomAccessReader reader = new RandomAccessReader(bytes, ByteOrder.LITTLE_ENDIAN);
        List<ROFL2Block>   blocks = new ArrayList<>();

        float time     = 0;
        int   packetId = 0;
        long  param    = 0;
        while (reader.remaining() > 0)
        {
            int start = (int) reader.pos();
            need(reader, 1, start);
            int marker = reader.readU8();

            if ((marker & ROFL2Block.MARKER_TS_RELATIVE) != 0)
            {
                need(reader, 1, start);
                time += reader.readU8() * 0.001f;
            } else
            {
                need(reader, 4, start);
                time = reader.readFloat();
            }

            long length;
            if ((marker & ROFL2Block.MARKER_LEN_U8) != 0)
            {
                need(reader, 1, start);
                length = reader.readU8();
            } else
            {
                need(reader, 4, start);
                length = reader.readU32();
            }

            if ((marker & ROFL2Block.MARKER_REUSE_PACKET_ID) == 0)
            {
                need(reader, 2, start);
                packetId = reader.readU16();
            }

            if ((marker & ROFL2Block.MARKER_PARAM_RELATIVE) != 0)
            {
                need(reader, 1, start);
                param = (param + reader.readByte()) & 0xFFFFFFFFL;
            } else
            {
                need(reader, 4, start);
                param = reader.readU32();
            }

            need(reader, length, start);

            ROFL2Block block = new ROFL2Block();
            block.setOffset(start);
            block.setMarker(marker);
            block.setTimestamp(time);
            block.setPacketId(packetId);
            block.setParam(param);
            block.setPayload(reader.readBytes(length));
            blocks.add(block);
        }
        return blocks;
    }

    private void need(RandomAccessReader reader, long count, int blockStart)
    {
        if (reader.remaining() < count)
        {
            throw new ParseException("ROFL2 chunk " + chunkId + ": block at " + blockStart + " needs " + count + " more bytes, only " + reader.remaining() + " left", reader.pos());
        }
    }

    @Override
    public String toString()
    {
        return "ROFL2Chunk{" +
               "offset=" + offset +
               ", chunkId=" + chunkId +
               ", chunkType=" + chunkType +
               ", stream=" + getStream() +
               ", uncompressedLength=" + uncompressedLength +
               ", compressedLength=" + compressedLength +
               '}';
    }
}
