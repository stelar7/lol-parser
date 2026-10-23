package offline;

import no.stelar7.cdragon.types.crid.CRIDParser;
import no.stelar7.cdragon.types.crid.data.*;
import no.stelar7.cdragon.types.crid.data.utf.CRIDUTFTable;
import no.stelar7.cdragon.util.readers.ParseException;
import no.stelar7.cdragon.util.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRI USM ("CRID") movies built in code: big-endian @UTF tables (all storage and value types),
 * a CRID header chunk with its CRIUSF_DIR_STREAM table, @SFV / @SFA stream chunks with header and section end chunks,
 * little-endian @SBT subtitle records and @CUE cue point tables
 */
public class CridTest
{
    private static final int ZERO            = 0x10;
    private static final int CONSTANT        = 0x30;
    private static final int PER_ROW         = 0x50;
    private static final int PER_ROW_DEFAULT = 0x70;

    private static final int U8     = 0x0;
    private static final int S8     = 0x1;
    private static final int U16    = 0x2;
    private static final int S16    = 0x3;
    private static final int U32    = 0x4;
    private static final int S32    = 0x5;
    private static final int U64    = 0x6;
    private static final int S64    = 0x7;
    private static final int FLOAT  = 0x8;
    private static final int DOUBLE = 0x9;
    private static final int STRING = 0xA;
    private static final int DATA   = 0xB;
    private static final int GUID   = 0xC;

    private static final int[] SIZES = {1, 1, 2, 2, 4, 4, 8, 8, 4, 8, 4, 8, 16};

    private static final int TYPE_STREAM   = 0;
    private static final int TYPE_HEADER   = 1;
    private static final int TYPE_END      = 2;
    private static final int TYPE_METADATA = 3;

    private static final int SFV = 0x40534656;
    private static final int SFA = 0x40534641;

    private static LE be()
    {
        return new LE(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Builds a @UTF table: "@UTF", size, then (relative to offset 8) header, schema, rows, string pool and data pool
     */
    private static final class Utf
    {
        private record Column(int flags, String name, Object constant, Object[] values) {}

        private final String       name;
        private final int          rows;
        private final List<Column> columns = new ArrayList<>();

        private final Map<String, Integer> stringIndex = new HashMap<>();
        private final ByteArrayOutputStream strings     = new ByteArrayOutputStream();
        private final ByteArrayOutputStream data        = new ByteArrayOutputStream();

        Utf(String name, int rows)
        {
            this.name = name;
            this.rows = rows;
        }

        Utf column(int storage, int type, String columnName, Object constant, Object... values)
        {
            columns.add(new Column(storage | type, columnName, constant, values));
            return this;
        }

        private int intern(String s)
        {
            return stringIndex.computeIfAbsent(s, k ->
            {
                int index = strings.size();
                strings.writeBytes((k + "\0").getBytes(StandardCharsets.UTF_8));
                return index;
            });
        }

        private void write(LE out, int type, Object v)
        {
            switch (type)
            {
                case U8, S8 -> out.u8(((Number) v).intValue());
                case U16, S16 -> out.i16(((Number) v).intValue());
                case U32, S32 -> out.i32(((Number) v).intValue());
                case U64, S64 -> out.i64(((Number) v).longValue());
                case FLOAT -> out.f32(((Number) v).floatValue());
                case DOUBLE -> out.i64(Double.doubleToLongBits(((Number) v).doubleValue()));
                case STRING -> out.i32(intern((String) v));
                case DATA ->
                {
                    byte[] b = (byte[]) v;
                    out.i32(data.size()).i32(b.length);
                    data.writeBytes(b);
                }
                case GUID -> out.bytes((byte[]) v);
                default -> throw new IllegalArgumentException("type " + type);
            }
        }

        byte[] build()
        {
            intern("<NULL>");
            int nameIndex = intern(name);

            LE  schema   = be();
            int rowWidth = 0;
            for (Column c : columns)
            {
                int type = c.flags() & 0xF;
                schema.u8(c.flags()).i32(intern(c.name()));
                if ((c.flags() & 0x20) != 0)
                {
                    write(schema, type, c.constant());
                }
                if ((c.flags() & 0x40) != 0)
                {
                    rowWidth += SIZES[type];
                }
            }

            LE rowData = be();
            for (int r = 0; r < rows; r++)
            {
                for (Column c : columns)
                {
                    if ((c.flags() & 0x40) != 0)
                    {
                        write(rowData, c.flags() & 0xF, c.values()[r]);
                    }
                }
            }

            int rowOffset    = 24 + schema.size();
            int stringOffset = rowOffset + rowData.size();
            int dataOffset   = stringOffset + strings.size();
            return be().ascii("@UTF").i32(dataOffset + data.size())
                       .i16(1).i16(rowOffset).i32(stringOffset).i32(dataOffset).i32(nameIndex)
                       .i16(columns.size()).i16(rowWidth).i32(rows)
                       .bytes(schema.toBytes()).bytes(rowData.toBytes()).bytes(strings.toByteArray()).bytes(data.toByteArray())
                       .toBytes();
        }
    }

    private static byte[] chunk(String magic, int channel, int type, byte[] payload, int padding)
    {
        return be().ascii(magic).i32(0x18 + payload.length + padding).i16(0x18).i16(padding)
                   .u8(channel).u8(0).i16(type).i32(0).i32(2997).zeros(8)
                   .bytes(payload).zeros(padding)
                   .toBytes();
    }

    private static byte[] end(String text)
    {
        return (text + "\0").getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] streamTable()
    {
        return streamTable("intro");
    }

    private static byte[] streamTable(String streamName)
    {
        return new Utf("CRIUSF_DIR_STREAM", 3)
                .column(CONSTANT, U32, "fmtver", 0x01000000)
                .column(PER_ROW, STRING, "filename", null, "intro.usm", streamName, streamName)
                .column(PER_ROW, U32, "filesize", null, 4096, 1024, 512)
                .column(ZERO, U32, "datasize", null)
                .column(PER_ROW, U32, "stmid", null, 0, SFV, SFA)
                .column(PER_ROW, U16, "chno", null, 0xFFFF, 0, 0)
                .column(PER_ROW, U16, "minchk", null, 1, 3, 3)
                .column(PER_ROW, U32, "minbuf", null, 0, 100, 50)
                .column(PER_ROW, U32, "avbps", null, 0, 800000, 64000)
                .build();
    }

    private static byte[] usm(byte[]... chunks)
    {
        LE out = be().bytes(chunk("CRID", 0, TYPE_HEADER, streamTable(), 8));
        for (byte[] c : chunks)
        {
            out.bytes(c);
        }
        return out.toBytes();
    }

    private static byte[] sbtRecord(int language, int timeUnit, int start, int duration, String text)
    {
        byte[] t = (text + "\0").getBytes(StandardCharsets.UTF_8);
        return new LE().i32(language).i32(timeUnit).i32(start).i32(duration).i32(t.length).bytes(t).toBytes();
    }

    private static final byte[] VIDEO_1 = {0x00, 0x00, 0x01, (byte) 0xB3, 1, 2, 3};
    private static final byte[] VIDEO_2 = {9, 8, 7, 6, 5};
    private static final byte[] AUDIO_1 = {(byte) 0x80, 0x00, 0x00, 0x20, 11, 12};
    private static final byte[] AUDIO_2 = {13, 14, 15};
    private static final byte[] HCA     = {(byte) ('H' | 0x80), (byte) ('C' | 0x80), (byte) ('A' | 0x80), 0, 1};

    private static byte[] fullUsm()
    {
        byte[] videoHeader = new Utf("VIDEO_HDRINFO", 1).column(PER_ROW, U32, "width", null, 640).build();
        byte[] subHeader   = new Utf("SUBTITLE_HDRINFO", 1).column(CONSTANT, U32, "num_languages", 2).build();
        byte[] cueHeader   = new Utf("CUEPOINT_HDRINFO", 1).column(CONSTANT, U32, "time_unit", 100).build();
        byte[] cueInfo = new Utf("CUEPOINT_INFO", 2)
                .column(PER_ROW, U64, "time", null, 150L, 400L)
                .column(PER_ROW, STRING, "name", null, "start", "boom")
                .column(PER_ROW, DATA, "parameter", null, end("a=1"), new byte[0])
                .build();

        LE sbt = new LE().bytes(sbtRecord(0, 1000, 1500, 2000, "Hello\r\nWorld")).bytes(sbtRecord(0, 30, 150, 15, "Bye"));

        return usm(
                chunk("@SFV", 0, TYPE_HEADER, videoHeader, 4),
                chunk("@SFV", 0, TYPE_END, end("#HEADER END     ==============="), 0),
                chunk("@SBT", 0, TYPE_HEADER, subHeader, 0),
                chunk("@CUE", 0, TYPE_HEADER, cueHeader, 0),
                chunk("@CUE", 0, TYPE_METADATA, cueInfo, 2),
                chunk("@CUE", 0, TYPE_END, end("#METADATA END   ==============="), 0),
                chunk("@SFV", 0, TYPE_STREAM, VIDEO_1, 5),
                chunk("@SFA", 0, TYPE_STREAM, AUDIO_1, 0),
                chunk("@XYZ", 0, TYPE_STREAM, new byte[]{1, 2, 3, 4}, 0),
                chunk("@SBT", 0, TYPE_STREAM, sbt.toBytes(), 3),
                chunk("@SFA", 1, TYPE_STREAM, HCA, 0),
                chunk("@SFV", 0, TYPE_STREAM, VIDEO_2, 0),
                chunk("@SFA", 0, TYPE_STREAM, AUDIO_2, 1),
                chunk("@SBT", 0, TYPE_STREAM, sbtRecord(1, 1000, 3_723_004, 1000, "Hei"), 0),
                chunk("@SFV", 0, TYPE_END, end("#CONTENTS END   ==============="), 0)
                  );
    }

    private static CRIDFile parse(byte[] data)
    {
        return new CRIDParser().parse(new ByteArray(data));
    }

    private static byte[] concat(byte[] a, byte[] b)
    {
        return new LE().bytes(a).bytes(b).toBytes();
    }

    @Test
    public void testUtfTable()
    {
        byte[] guid   = new byte[16];
        byte[] nested = new Utf("NESTED", 1).column(PER_ROW, S16, "value", null, -2).build();
        for (int i = 0; i < guid.length; i++)
        {
            guid[i] = (byte) i;
        }

        byte[] table = new Utf("CRIUSF_DIR_STREAM", 2)
                .column(ZERO, U8, "zero_u8", null)
                .column(ZERO, S64, "zero_s64", null)
                .column(ZERO, STRING, "zero_string", null)
                .column(ZERO, DATA, "zero_data", null)
                .column(CONSTANT, STRING, "constant_string", "shared")
                .column(CONSTANT, FLOAT, "constant_float", 29.97f)
                .column(PER_ROW_DEFAULT, U16, "default_u16", 7, 0xFFFE, 1)
                .column(PER_ROW, U8, "u8", null, 0xFF, 1)
                .column(PER_ROW, S8, "s8", null, -1, 2)
                .column(PER_ROW, S16, "s16", null, -300, 3)
                .column(PER_ROW, U32, "u32", null, 0xFFFFFFFF, 4)
                .column(PER_ROW, S32, "s32", null, -5, 5)
                .column(PER_ROW, U64, "u64", null, 1L << 40, 6L)
                .column(PER_ROW, DOUBLE, "double", null, 0.5, -1.25)
                .column(PER_ROW, STRING, "filename", null, "intro.usm", "æøå")
                .column(PER_ROW, DATA, "data", null, new byte[]{1, 2, 3}, nested)
                .column(PER_ROW, GUID, "guid", null, guid, new byte[16])
                .build();

        CRIDFile     file = parse(chunk("CRID", 0, TYPE_HEADER, table, 0));
        CRIDUTFTable t    = file.getMetadata();
        assertEquals("CRID", file.getHeader().getMagic());
        assertEquals(2997, file.getHeader().getSamples());
        assertEquals("CRIUSF_DIR_STREAM", t.getTableName());
        assertEquals(1, t.getVersion());
        assertEquals(2, t.getRows());
        assertEquals(17, t.getSchema().size());

        for (int r = 0; r < 2; r++)
        {
            assertEquals(0, t.query("zero_u8", r));
            assertEquals(0L, t.query("zero_s64", r));
            assertEquals("", t.query("zero_string", r));
            assertArrayEquals(new byte[0], (byte[]) t.query("zero_data", r));
            assertEquals("shared", t.query("constant_string", r));
            assertEquals(29.97f, t.query("constant_float", r));
        }

        assertEquals(0xFFFE, t.query("default_u16", 0), "a per-row value wins over the default");
        assertEquals(1, t.query("default_u16", 1));
        assertEquals(0xFF, t.query("u8", 0), "u8 is unsigned");
        assertEquals((byte) -1, t.query("s8", 0));
        assertEquals((short) -300, t.query("s16", 0));
        assertEquals(0xFFFFFFFFL, t.query("u32", 0), "u32 is widened to long");
        assertEquals(-5, t.query("s32", 0));
        assertEquals(1L << 40, t.query("u64", 0));
        assertEquals(6L, t.query("u64", 1));
        assertEquals(0.5, t.query("double", 0));
        assertEquals(-1.25, t.query("double", 1));
        assertEquals("intro.usm", t.query("filename", 0));
        assertEquals("æøå", t.query("filename", 1), "strings are UTF-8");
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) t.query("data", 0));
        assertArrayEquals(guid, (byte[]) t.query("guid", 0));
        assertNull(t.query("missing", 0));

        CRIDUTFTable inner = assertInstanceOf(CRIDUTFTable.class, t.query("data", 1), "data holding a @UTF table is parsed");
        assertEquals("NESTED", inner.getTableName());
        assertEquals((short) -2, inner.query("value", 0));

        assertEquals(2, file.getStreams().size());
        assertEquals("USM\0", file.getStreams().get(0).getStreamType(), "a missing stmid is the whole-file row");
        assertTrue(file.getStreamData().isEmpty());
    }

    @Test
    public void testDemux(@TempDir Path dir) throws IOException
    {
        CRIDFile file = parse(fullUsm());

        List<CRIDStreamInfo> streams = file.getStreams();
        assertEquals(3, streams.size());
        assertEquals("USM\0", streams.get(0).getStreamType());
        assertEquals("intro.usm", streams.get(0).getFilename());
        assertEquals(0xFFFF, streams.get(0).getChannels());
        assertEquals("@SFV", streams.get(1).getStreamType());
        assertEquals(1024, streams.get(1).getFilesize());
        assertEquals(800000, streams.get(1).getAvgBps());
        assertEquals(3, streams.get(1).getMinChunk());
        assertEquals(100, streams.get(1).getMinBuffer());
        assertEquals("@SFA", streams.get(2).getStreamType());
        assertEquals(0, streams.get(2).getDatasize());

        Map<String, byte[]> data = new LinkedHashMap<>();
        for (Pair<String, byte[]> pair : file.getStreamData())
        {
            data.put(pair.getA(), pair.getB());
        }
        assertEquals(List.of("intro@SFV.m2v", "intro@SFA.adx", "intro@SFA_ch1.hca"), List.copyOf(data.keySet()));
        assertArrayEquals(concat(VIDEO_1, VIDEO_2), data.get("intro@SFV.m2v"), "only stream payloads, without header/end chunks or padding");
        assertArrayEquals(concat(AUDIO_1, AUDIO_2), data.get("intro@SFA.adx"));
        assertArrayEquals(HCA, data.get("intro@SFA_ch1.hca"), "channels are demuxed separately, encrypted HCA is detected");

        file.extractStreams(dir);
        for (Map.Entry<String, byte[]> entry : data.entrySet())
        {
            assertArrayEquals(entry.getValue(), Files.readAllBytes(dir.resolve(entry.getKey())));
        }
        assertEquals(file.toSRT(0), Files.readString(dir.resolve("intro@SBT_0.srt")));
        assertEquals(file.toSRT(1), Files.readString(dir.resolve("intro@SBT_1.srt")));
        try (var listing = Files.list(dir))
        {
            assertEquals(5, listing.count());
        }
    }

    @Test
    public void testStreamNamesStayInFolder(@TempDir Path dir) throws IOException
    {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("D:\\movies\\intro.avi", "intro.avi@SFV.m2v");
        expected.put("/home/user/intro", "intro@SFV.m2v");
        expected.put("../../escape", "escape@SFV.m2v");
        expected.put("..\\..", "stream@SFV.m2v");
        expected.put("..", "stream@SFV.m2v");
        expected.put("", "stream@SFV.m2v");
        expected.put("C:evil", "C_evil@SFV.m2v");
        expected.put("a?b*c", "a_b_c@SFV.m2v");

        for (Map.Entry<String, String> entry : expected.entrySet())
        {
            byte[] usm = be().bytes(chunk("CRID", 0, TYPE_HEADER, streamTable(entry.getKey()), 8))
                             .bytes(chunk("@SFV", 0, TYPE_STREAM, VIDEO_1, 0))
                             .bytes(chunk("@SFV", 0, TYPE_END, end("#CONTENTS END   ==============="), 0))
                             .toBytes();
            CRIDFile file = parse(usm);
            assertEquals(entry.getValue(), file.getStreamData().get(0).getA(), entry.getKey());

            Path out = dir.resolve("case" + expected.keySet().stream().toList().indexOf(entry.getKey())).resolve("inner");
            file.extractStreams(out);
            assertArrayEquals(VIDEO_1, Files.readAllBytes(out.resolve(entry.getValue())), entry.getKey());
        }

        // a hand-built file with a path as the stream name is refused instead of written outside the folder
        CRIDFile manual = new CRIDFile();
        manual.setStreamData(List.of(new Pair<>("../outside.m2v", VIDEO_1)));
        Path inner = dir.resolve("manual").resolve("inner");
        assertThrows(IllegalArgumentException.class, () -> manual.extractStreams(inner));
        assertFalse(Files.exists(dir.resolve("manual").resolve("outside.m2v")));
    }

    @Test
    public void testSubtitles()
    {
        CRIDFile           file = parse(fullUsm());
        List<CRIDSubtitle> subs = file.getSubtitles();
        assertEquals(3, subs.size());

        CRIDSubtitle first = subs.get(0);
        assertEquals(0, first.getLanguageId());
        assertEquals(1000, first.getTimeUnit());
        assertEquals(1500, first.getStartTime());
        assertEquals(2000, first.getDuration());
        assertEquals("Hello\r\nWorld", first.getText(), "the terminator is stripped");
        assertEquals(1500, first.getStartMillis());
        assertEquals(3500, first.getEndMillis());
        assertEquals(5000, subs.get(1).getStartMillis(), "time unit 30 ticks per second");
        assertEquals(5500, subs.get(1).getEndMillis());
        assertEquals("Hei", subs.get(2).getText());

        assertEquals(Set.of(0), file.getSubtitleHeaders().keySet());
        assertEquals("SUBTITLE_HDRINFO", file.getSubtitleHeaders().get(0).getTableName());

        assertEquals("1\n00:00:01,500 --> 00:00:03,500\nHello\nWorld\n\n" +
                     "2\n00:00:05,000 --> 00:00:05,500\nBye\n\n", file.toSRT(0));
        assertEquals("1\n01:02:03,004 --> 01:02:04,004\nHei\n\n", file.toSRT(1));
        assertEquals("", file.toSRT(7));
    }

    @Test
    public void testCuePoints()
    {
        CRIDFile           file = parse(fullUsm());
        List<CRIDCuePoint> cues = file.getCuePoints();
        assertEquals(100, file.getCueTimeUnit());
        assertEquals(2, cues.size());

        assertEquals(150, cues.get(0).getTime());
        assertEquals(1500, cues.get(0).getTimeMillis());
        assertEquals("start", cues.get(0).getName());
        assertEquals("a=1", cues.get(0).getParameter(), "data parameters are 0x00 terminated text");
        assertEquals(4000, cues.get(1).getTimeMillis());
        assertEquals("boom", cues.get(1).getName());
        assertEquals("", cues.get(1).getParameter());

        CRIDFile plain = parse(usm(chunk("@SFV", 0, TYPE_STREAM, VIDEO_1, 0)));
        assertEquals(0, plain.getCueTimeUnit());
        assertTrue(plain.getCuePoints().isEmpty());
        assertTrue(plain.getSubtitles().isEmpty());
    }

    @Test
    public void testInvalid()
    {
        byte[] garbage = new byte[64];
        new Random(7).nextBytes(garbage);
        assertThrows(ParseException.class, () -> parse(garbage), "garbage");
        assertThrows(ParseException.class, () -> parse(new byte[0]), "empty");

        byte[] notUtf = chunk("CRID", 0, TYPE_HEADER, new byte[32], 0);
        assertThrows(ParseException.class, () -> parse(notUtf), "payload is not a @UTF table");

        byte[] blockType = chunk("CRID", 0, TYPE_STREAM, streamTable(), 0);
        assertThrows(ParseException.class, () -> parse(blockType), "CRID chunk is not a header chunk");

        byte[] tableName = chunk("CRID", 0, TYPE_HEADER, new Utf("OTHER", 1).column(ZERO, U8, "x", null).build(), 0);
        assertThrows(ParseException.class, () -> parse(tableName), "wrong table name");

        byte[] noRows = chunk("CRID", 0, TYPE_HEADER, new Utf("CRIUSF_DIR_STREAM", 0).column(ZERO, U8, "x", null).build(), 0);
        assertThrows(ParseException.class, () -> parse(noRows), "no rows");

        byte[] columnType = chunk("CRID", 0, TYPE_HEADER, new Utf("CRIUSF_DIR_STREAM", 1).column(ZERO, 0xF, "x", null).build(), 0);
        assertThrows(ParseException.class, () -> parse(columnType), "unknown column type");

        byte[] sbt = chunk("@SBT", 0, TYPE_STREAM, Arrays.copyOf(sbtRecord(0, 1000, 0, 10, "text"), 22), 0);
        assertThrows(ParseException.class, () -> parse(usm(sbt)), "truncated subtitle text");

        byte[] chunkSize = usm(be().ascii("@SFV").i32(0x7FFFFFF0).zeros(0x18).toBytes());
        assertThrows(ParseException.class, () -> parse(chunkSize), "chunk larger than the file");

        // every truncation either parses (cut at a chunk boundary) or throws a ParseException, never anything else
        byte[] full     = fullUsm();
        int    cridSize = chunk("CRID", 0, TYPE_HEADER, streamTable(), 8).length;
        for (int length = 0; length < full.length; length++)
        {
            byte[] cut = Arrays.copyOf(full, length);
            try
            {
                parse(cut);
                assertTrue(length >= cridSize, "a file cut inside the CRID chunk must not parse (length " + length + ")");
            } catch (ParseException e)
            {
                // expected
            } catch (RuntimeException e)
            {
                fail("length " + length + " threw " + e, e);
            }
        }
    }
}
