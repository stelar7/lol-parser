package offline;

import com.github.luben.zstd.Zstd;
import com.google.gson.*;
import no.stelar7.cdragon.types.rofl.ROFLParser;
import no.stelar7.cdragon.types.rofl.data.*;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.ByteArray;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.paddings.*;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * League replays built in code: ROFL v1 files (header, metadata + stats json, payload header, gzip'd chunks/keyframes
 * Blowfish-encrypted with BouncyCastle the way the client does it) and ROFL2 files (version header, zstd/raw chunks, signature, trailing metadata)
 */
public class RoflTest
{
    private static final long   GAME_ID   = 6543210987L;
    private static final byte[] CHUNK_KEY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private static final String STATS = "[{\"NAME\":\"Player One\",\"SKIN\":\"Ahri\",\"TEAM\":\"100\",\"WIN\":\"Win\",\"CHAMPIONS_KILLED\":\"7\",\"ITEM0\":\"3089\"},"
                                        + "{\"NAME\":\"Player Two\",\"SKIN\":\"Garen\",\"TEAM\":\"200\",\"WIN\":\"Fail\",\"NUM_DEATHS\":\"9\"}]";

    private static final String METADATA = "{\"gameLength\":1.8e6,\"gameVersion\":\"14.1.555.1234\",\"lastGameChunkId\":3,\"lastKeyFrameId\":1,\"statsJson\":"
                                           + new Gson().toJson(STATS) + "}";

    // offsets of the v1 header fields after magic (6) + signature (256)
    private static final int V1_HEADER_SIZE      = 288;
    private static final int V1_METADATA_OFFSET  = 268;
    private static final int V1_PAYLOAD_HEADER   = 276;
    private static final int V1_PAYLOAD_OFFSET   = 284;

    /**
     * Blowfish-ECB with PKCS#5 padding, the inverse of what the parser does
     */
    private static byte[] blowfish(byte[] key, byte[] data) throws Exception
    {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new BlowfishEngine(), new PKCS7Padding());
        cipher.init(true, new KeyParameter(key));
        byte[] out     = new byte[cipher.getOutputSize(data.length)];
        int    written = cipher.processBytes(data, 0, data.length, out, 0);
        written += cipher.doFinal(out, written);
        return Arrays.copyOf(out, written);
    }

    private static byte[] gzip(byte[] data) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos))
        {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    private static byte[] text(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A v1 replay with two chunks (type 1) and one keyframe (type 2)
     */
    private static byte[] roflV1(byte[]... plain) throws Exception
    {
        String key = Base64.getEncoder().encodeToString(blowfish(text(String.valueOf(GAME_ID)), CHUNK_KEY));

        byte[] metadata      = text(METADATA);
        byte[] payloadHeader = new LE().i64(GAME_ID).i32(1_800_000).i32(1).i32(2).i32(2).i32(3).i32(60_000).str16(key).toBytes();

        LE table = new LE();
        LE data  = new LE();
        for (int i = 0; i < plain.length; i++)
        {
            byte[] encrypted = blowfish(CHUNK_KEY, gzip(plain[i]));
            table.i32(i + 1).u8(i == 2 ? 2 : 1).i32(encrypted.length).i32(i + 2).i32(data.size());
            data.bytes(encrypted);
        }

        int metadataOffset      = V1_HEADER_SIZE;
        int payloadHeaderOffset = metadataOffset + metadata.length;
        int payloadOffset       = payloadHeaderOffset + payloadHeader.length;
        int fileSize            = payloadOffset + table.size() + data.size();

        byte[] signature = new byte[256];
        Arrays.fill(signature, (byte) 0x5A);
        return new LE().ascii("RIOT").u8(0).u8(0).bytes(signature).i16(V1_HEADER_SIZE).i32(fileSize)
                       .i32(metadataOffset).i32(metadata.length).i32(payloadHeaderOffset).i32(payloadHeader.length).i32(payloadOffset)
                       .bytes(metadata).bytes(payloadHeader).bytes(table.toBytes()).bytes(data.toBytes()).toBytes();
    }

    private static byte[] defaultV1() throws Exception
    {
        return roflV1(text("chunk one"), text("chunk two, a bit longer than one block"), text("keyframe"));
    }

    private static ByteBuffer le(byte[] b)
    {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static ROFLFile parse(byte[] b)
    {
        return new ROFLParser().parse(new ByteArray(b));
    }

    @Test
    public void testV1() throws Exception
    {
        byte[]   bytes = defaultV1();
        ROFLFile file  = parse(bytes);

        ROFLHeader header = file.getHeader();
        assertEquals(1, header.getVersion());
        assertEquals(V1_HEADER_SIZE, header.getHeaderSize());
        assertEquals(bytes.length, header.getFileSize());
        assertEquals(256, header.getSignature().length);
        assertEquals(0x5A, header.getSignature()[255]);

        ROFLMetadata metadata = header.getMetadata();
        assertEquals(1_800_000, metadata.getGameLength(), "scientific notation is accepted");
        assertEquals("14.1.555.1234", metadata.getGameVersion());
        assertEquals(3, metadata.getLastGameChunkId());
        assertEquals(1, metadata.getLastKeyframeId());
        assertEquals(STATS, metadata.getRawStatsJson());
        assertEquals(2, metadata.getStatsJson().size());

        // the stats class has no getters, so look at it through its json form
        JsonObject first = new Gson().toJsonTree(metadata.getStatsJson().get(0)).getAsJsonObject();
        assertEquals("Player One", first.get("NAME").getAsString());
        assertEquals("Ahri", first.get("SKIN").getAsString());
        assertEquals(100, first.get("TEAM").getAsInt(), "the client writes numbers as strings");
        assertEquals(7, first.get("CHAMPIONS_KILLED").getAsInt());
        assertEquals(3089, first.get("ITEM0").getAsInt());
        assertEquals(9, new Gson().toJsonTree(metadata.getStatsJson().get(1)).getAsJsonObject().get("NUM_DEATHS").getAsInt());

        ROFLPayloadHeader payload = file.getPayloadHeader();
        assertEquals(GAME_ID, payload.getGameId());
        assertEquals(1_800_000, payload.getGameLength());
        assertEquals(1, payload.getKeyframeCount());
        assertEquals(2, payload.getChunkCount());
        assertEquals(2, payload.getEndStartupChunkId());
        assertEquals(3, payload.getStartGameChunkId());
        assertEquals(60_000, payload.getKeyframeInterval());
        assertEquals(32, payload.getEncryptionKeyLength(), "a 16 byte key pads to 24 bytes, 32 in base64");
        assertEquals(24, payload.getEncryptionKey().length);
        assertArrayEquals(CHUNK_KEY, ROFLPayloadEntry.decryptChunkKey(GAME_ID, payload.getEncryptionKey()));

        List<ROFLPayloadEntry> entries = file.getEntries();
        assertEquals(3, entries.size());
        assertEquals(List.of(1, 2, 3), entries.stream().map(ROFLPayloadEntry::getId).toList());
        assertEquals(List.of((byte) 1, (byte) 1, (byte) 2), entries.stream().map(ROFLPayloadEntry::getType).toList());
        assertEquals(List.of(2, 3, 4), entries.stream().map(ROFLPayloadEntry::getNextChunkId).toList());
        assertEquals(0, entries.get(0).getOffset());
        assertEquals(entries.get(0).getLength(), entries.get(1).getOffset());
        assertEquals("chunk one", new String(entries.get(0).getData(), StandardCharsets.UTF_8));
        assertEquals("chunk two, a bit longer than one block", new String(entries.get(1).getData(), StandardCharsets.UTF_8));
        assertEquals("keyframe", new String(entries.get(2).getData(), StandardCharsets.UTF_8));
        assertSame(entries.get(0).getData(), entries.get(0).getData(), "decrypted once and cached");
    }

    @Test
    public void testV1Decryption() throws Exception
    {
        // a chunk that is an exact multiple of the block size after gzip still gets a full padding block
        byte[] big = new byte[4096];
        new Random(7).nextBytes(big);
        ROFLFile file = parse(roflV1(big, new byte[0], text("k")));
        assertArrayEquals(big, file.getEntries().get(0).getData());
        assertEquals(0, file.getEntries().get(1).getData().length);

        // a different game id gives a different chunk key, which must not decrypt the chunks
        byte[] wrongId = defaultV1();
        int    phOff   = le(wrongId).getInt(V1_PAYLOAD_HEADER);
        le(wrongId).putLong(phOff, GAME_ID + 1);
        assertThrows(ParseException.class, () -> parse(wrongId).getEntries().forEach(ROFLPayloadEntry::getData));

        // flipping a byte in the last cipher block breaks the padding (or the gzip trailer)
        byte[] corrupt = defaultV1();
        corrupt[corrupt.length - 3] ^= 0x40;
        ROFLFile corruptFile = parse(corrupt);
        assertEquals("chunk one", new String(corruptFile.getEntries().get(0).getData(), StandardCharsets.UTF_8), "other chunks are unaffected");
        assertThrows(ParseException.class, () -> corruptFile.getEntries().get(2).getData());
    }

    @Test
    public void testV1Invalid() throws Exception
    {
        byte[] valid = defaultV1();

        assertThrows(ParseException.class, () -> parse(new byte[0]), "empty");
        assertThrows(ParseException.class, () -> parse(text("RIOT")), "shorter than the magic");
        assertThrows(ParseException.class, () -> parse(text("RIOX\0\0 not a replay at all, just some text")), "wrong magic");

        byte[] version = valid.clone();
        version[4] = 9;
        assertThrows(ParseException.class, () -> parse(version), "unknown version");

        // every truncation must fail cleanly: the last chunk ends exactly at the end of the file
        for (int len = 0; len < valid.length; len++)
        {
            byte[] truncated = Arrays.copyOf(valid, len);
            assertThrows(ParseException.class, () -> parse(truncated), "truncated to " + len);
        }

        byte[] metadataOffset = valid.clone();
        le(metadataOffset).putInt(V1_METADATA_OFFSET, -16);
        assertThrows(ParseException.class, () -> parse(metadataOffset), "metadata offset past the end (unsigned)");

        byte[] json = valid.clone();
        json[V1_HEADER_SIZE] = '[';
        assertThrows(ParseException.class, () -> parse(json), "metadata is not a json object");

        byte[] gameLength = valid.clone();
        System.arraycopy(text("\"gameLength\":\"abc\""), 0, gameLength, V1_HEADER_SIZE + 1, 18);
        assertThrows(ParseException.class, () -> parse(gameLength), "non numeric metadata field");

        int    phOff  = le(valid).getInt(V1_PAYLOAD_HEADER);
        byte[] base64 = valid.clone();
        base64[phOff + 34] = '*';
        assertThrows(ParseException.class, () -> parse(base64), "invalid base64 key");

        byte[] keyLength = valid.clone();
        le(keyLength).putShort(phOff + 32, (short) 31);
        assertThrows(ParseException.class, () -> parse(keyLength), "key is not a whole number of cipher blocks");

        byte[] keyTooLong = valid.clone();
        le(keyTooLong).putShort(phOff + 32, (short) 0xFFFF);
        assertThrows(ParseException.class, () -> parse(keyTooLong), "key longer than the payload header");

        byte[] chunkCount = valid.clone();
        le(chunkCount).putInt(phOff + 16, Integer.MAX_VALUE);
        assertThrows(ParseException.class, () -> parse(chunkCount), "chunk count larger than the file");

        byte[] negativeCount = valid.clone();
        le(negativeCount).putInt(phOff + 12, -1);
        assertThrows(ParseException.class, () -> parse(negativeCount), "negative keyframe count");

        int    payloadOffset = le(valid).getInt(V1_PAYLOAD_OFFSET);
        byte[] chunkLength   = valid.clone();
        le(chunkLength).putInt(payloadOffset + 5, 1 << 20);
        assertThrows(ParseException.class, () -> parse(chunkLength), "chunk runs past the end of the file");

        byte[] oddLength = valid.clone();
        le(oddLength).putInt(payloadOffset + 5, le(valid).getInt(payloadOffset + 5) - 1);
        assertThrows(ParseException.class, () -> parse(oddLength).getEntries().getFirst().getData(), "chunk is not a whole number of cipher blocks");

        // random byte corruption anywhere must only ever produce a ParseException
        Random random = new Random(1234);
        for (int i = 0; i < 2000; i++)
        {
            byte[] fuzz = valid.clone();
            for (int k = 0; k < 1 + random.nextInt(3); k++)
            {
                fuzz[V1_HEADER_SIZE - 26 + random.nextInt(fuzz.length - V1_HEADER_SIZE + 26)] = (byte) random.nextInt(256);
            }
            try
            {
                parse(fuzz).getEntries().forEach(ROFLPayloadEntry::getData);
            } catch (ParseException expected)
            {
                // fine
            }
        }
    }

    /**
     * A ROFL2 replay: magic, 8 unknown bytes, u8 length + game version, chunks, 256 byte signature, metadata json, u32 metadata length
     */
    private static byte[] roflV2(String version, byte[] payload, String metadata)
    {
        byte[] json      = text(metadata);
        byte[] signature = new byte[256];
        Arrays.fill(signature, (byte) 0xC3);
        LE out = new LE().ascii("RIOT").u8(2).u8(0).i64(0x0102030405060708L);
        if (version != null)
        {
            out.u8(version.length()).ascii(version);
        }
        return out.bytes(payload).bytes(signature).bytes(json).i32(json.length).toBytes();
    }

    private static LE chunk(LE out, int id, int type, int stream, byte[] data, boolean compress)
    {
        byte[] body = compress ? Zstd.compress(data) : data;
        return out.i32(id).u8(type).i32((stream << 24) | id).i32(data.length).i32(compress ? body.length : 0).bytes(body);
    }

    /**
     * Two blocks: one with every field written out in full, then one with every compact encoding
     */
    private static byte[] blocks()
    {
        return new LE().u8(0).f32(12.5f).i32(3).i16(0x1234).i32(0xDEADBEEF).ascii("abc")
                       .u8(0xF0).u8(250).u8(2).u8(-3).ascii("xy")
                       .toBytes();
    }

    private static byte[] defaultV2Payload()
    {
        LE payload = new LE();
        chunk(payload, 1, 1, ROFL2Chunk.STREAM_START_KEYFRAME, blocks(), false);
        chunk(payload, 2, 1, ROFL2Chunk.STREAM_GAME_CHUNK, blocks(), true);
        return payload.toBytes();
    }

    @Test
    public void testV2()
    {
        String   metadata = "{\"gameLength\":1234567,\"lastGameChunkId\":40,\"lastKeyFrameId\":20,\"statsJson\":" + new Gson().toJson(STATS) + "}";
        byte[]   bytes    = roflV2("15.20.715.1234", defaultV2Payload(), metadata);
        ROFLFile file     = parse(bytes);

        ROFLHeader header = file.getHeader();
        assertEquals(2, header.getVersion());
        assertEquals("15.20.715.1234", header.getGameVersion());
        assertEquals(bytes.length, header.getFileSize());
        assertEquals(bytes.length - 4 - metadata.length(), header.getMetadataOffset());
        assertEquals(metadata.length(), header.getMetadataLength());
        assertNull(file.getPayloadHeader());
        assertTrue(file.getEntries().isEmpty());

        ROFLMetadata meta = header.getMetadata();
        assertEquals(1234567, meta.getGameLength());
        assertEquals("15.20.715.1234", meta.getGameVersion(), "falls back to the header version");
        assertEquals(40, meta.getLastGameChunkId());
        assertEquals(20, meta.getLastKeyframeId());
        assertEquals(2, meta.getStatsJson().size());

        assertNull(file.getPayloadError());
        assertEquals(256, file.getSignature().length);
        assertEquals((byte) 0xC3, file.getSignature()[0]);

        List<ROFL2Chunk> chunks = file.getChunks();
        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).getChunkId());
        assertEquals(ROFL2Chunk.STREAM_START_KEYFRAME, chunks.get(0).getStream());
        assertFalse(chunks.get(0).isCompressed());
        assertEquals(ROFL2Chunk.STREAM_GAME_CHUNK, chunks.get(1).getStream());
        assertTrue(chunks.get(1).isCompressed());
        assertEquals(15 + 14 + ROFL2Chunk.HEADER_SIZE + blocks().length, chunks.get(1).getOffset());

        for (ROFL2Chunk chunk : chunks)
        {
            assertArrayEquals(blocks(), chunk.getData());

            List<ROFL2Block> list = chunk.getBlocks();
            assertEquals(2, list.size());
            assertEquals(12.5f, list.get(0).getTimestamp());
            assertEquals(0x1234, list.get(0).getPacketId());
            assertEquals(0xDEADBEEFL, list.get(0).getParam());
            assertEquals("abc", new String(list.get(0).getPayload(), StandardCharsets.UTF_8));

            assertEquals(18, list.get(1).getOffset());
            assertEquals(12.75f, list.get(1).getTimestamp(), 1e-4, "relative timestamp in milliseconds");
            assertEquals(0x1234, list.get(1).getPacketId(), "reused packet id");
            assertEquals(0xDEADBEECL, list.get(1).getParam(), "signed param delta");
            assertEquals("xy", new String(list.get(1).getPayload(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testV2Payloads()
    {
        // no version string: the header falls back to a fixed field, the metadata still parses, the payload is skipped
        ROFLFile noVersion = parse(roflV2(null, new byte[0], "{\"gameVersion\":\"15.1\"}"));
        assertEquals("15.1", noVersion.getHeader().getMetadata().getGameVersion());
        assertNotNull(noVersion.getPayloadError());
        assertTrue(noVersion.getChunks().isEmpty());

        // a chunk running into the signature is reported instead of thrown
        byte[]   payload   = defaultV2Payload();
        ROFLFile truncated = parse(roflV2("15.20", Arrays.copyOf(payload, payload.length - 1), "{}"));
        assertEquals(1, truncated.getChunks().size());
        assertNotNull(truncated.getPayloadError());

        // a zstd body with a broken frame magic fails when the data is read
        byte[] broken = defaultV2Payload();
        broken[2 * ROFL2Chunk.HEADER_SIZE + blocks().length] ^= 0x55;
        ROFL2Chunk chunk = parse(roflV2("15.20", broken, "{}")).getChunks().get(1);
        assertThrows(ParseException.class, chunk::getData);

        // a block that claims more bytes than the chunk has
        byte[] badBlock = new LE().u8(ROFL2Block.MARKER_LEN_U8 | ROFL2Block.MARKER_REUSE_PACKET_ID | ROFL2Block.MARKER_PARAM_RELATIVE)
                                  .f32(1).u8(100).u8(0).ascii("short").toBytes();
        ROFL2Chunk bad = parse(roflV2("15.20", chunk(new LE(), 1, 1, 1, badBlock, false).toBytes(), "{}")).getChunks().getFirst();
        assertThrows(ParseException.class, bad::getBlocks);
    }

    @Test
    public void testV2Invalid()
    {
        byte[] valid = roflV2("15.20.715.1234", defaultV2Payload(), "{\"gameLength\":1}");

        assertThrows(ParseException.class, () -> parse(Arrays.copyOf(valid, 20)), "too small");

        byte[] length = valid.clone();
        le(length).putInt(length.length - 4, length.length);
        assertThrows(ParseException.class, () -> parse(length), "metadata longer than the file");

        byte[] json = valid.clone();
        json[json.length - 5] = ']';
        assertThrows(ParseException.class, () -> parse(json), "broken metadata json");

        byte[] stats = roflV2("15.20", new byte[0], "{\"statsJson\":{\"NAME\":\"x\"}}");
        assertThrows(ParseException.class, () -> parse(stats), "stats json is an object instead of a string");

        for (int len = 0; len < valid.length; len++)
        {
            byte[] truncated = Arrays.copyOf(valid, len);
            assertThrows(ParseException.class, () -> parse(truncated), "truncated to " + len);
        }
    }
}
