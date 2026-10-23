package no.stelar7.cdragon.types.crid;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.crid.data.*;
import no.stelar7.cdragon.types.crid.data.utf.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.Pair;
import no.stelar7.cdragon.util.writers.NamedByteWriter;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parser for CRI USM ("CRID") video containers.
 * <p>
 * A USM file is a sequence of chunks, all big-endian:
 * <pre>
 * 0x00 magic (4)            "CRID", "@SFV", "@SFA", "@ALP", "@SBT", "@CUE", ...
 * 0x04 chunk size (4)       size of the chunk after this field
 * 0x08 payload offset (2)   offset of the payload, relative to 0x08
 * 0x0A padding size (2)     bytes of padding at the end of the chunk
 * 0x0C channel number (1)
 * 0x0D reserved (1)
 * 0x0E payload type (2)     low 2 bits: 0 = stream data, 1 = header, 2 = section end, 3 = metadata
 * 0x10 frame time (4), frame rate (4), reserved (8)
 * </pre>
 * The first chunk is a "CRID" chunk with a @UTF table (CRIUSF_DIR_STREAM) describing the streams.
 * <p>
 * Video (@SFV, @ALP) and audio (@SFA) stream payloads are demultiplexed into {@link CRIDFile#getStreamData()}.
 * Subtitle (@SBT) stream payloads are parsed into {@link CRIDFile#getSubtitles()} (their SUBTITLE_HDRINFO tables into {@link CRIDFile#getSubtitleHeaders()}),
 * and the CUEPOINT_HDRINFO / CUEPOINT_INFO tables of @CUE chunks into {@link CRIDFile#getCuePoints()}.
 * See https://github.com/Youjose/PyCriCodecs (CriCodecs/src/usm, CriCodecs/src/utf)
 */
public class CRIDParser implements Parseable<CRIDFile>
{
    private static final String VIDEO_EXT = "m2v";
    private static final String AUDIO_EXT = "adx";

    private static final String USM_SIGNATURE  = "USM\0";
    private static final String UTF_SIGNATURE  = "@UTF";
    private static final String CRID_SIGNATURE = "CRID";

    private static final int MAGIC_SFV = 0x40534656; // "@SFV"
    private static final int MAGIC_SFA = 0x40534641; // "@SFA"
    private static final int MAGIC_ALP = 0x40414C50; // "@ALP"
    private static final int MAGIC_SBT = 0x40534254; // "@SBT"
    private static final int MAGIC_CUE = 0x40435545; // "@CUE"

    private static final int CHUNK_FIXED_HEADER    = 0x08;
    private static final int CHUNK_HEADER_SIZE     = 0x18;
    private static final int PAYLOAD_TYPE_STREAM   = 0;
    private static final int PAYLOAD_TYPE_HEADER   = 1;
    private static final int PAYLOAD_TYPE_METADATA = 3;
    private static final int SBT_RECORD_HEADER     = 0x14;

    @Override
    public CRIDFile parse(RandomAccessReader raf)
    {
        // USM files are big-endian
        raf.setEndian(ByteOrder.BIG_ENDIAN);
        raf.seek(0);

        try
        {
            return parseFile(raf);
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of USM data", raf.pos(), e);
        }
    }

    private CRIDFile parseFile(RandomAccessReader raf)
    {
        CRIDFile file = new CRIDFile();
        file.setHeader(parseHeader(raf));

        int endOfHeader = (int) raf.pos();

        file.setMetadata(parseUTF(raf, endOfHeader));
        if (file.getMetadata().getRows() < 1)
        {
            throw new ParseException("Expected at least one row in the CRID stream table, got " + file.getMetadata().getRows(), endOfHeader);
        }

        if (!"CRIUSF_DIR_STREAM".equals(file.getMetadata().getTableName()))
        {
            throw new ParseException("Expected table to be \"CRIUSF_DIR_STREAM\", got \"" + file.getMetadata().getTableName() + "\"", endOfHeader);
        }

        file.setStreams(parseStreamInfo(file.getMetadata()));
        file.setStreamData(demultiplexStream(raf, file));

        return file;
    }

    private static int intValue(Object value)
    {
        return (value instanceof Number n) ? n.intValue() : 0;
    }

    private static long longValue(Object value)
    {
        return (value instanceof Number n) ? n.longValue() : 0;
    }

    /**
     * Cue point names/parameters are either strings or data columns holding 0x00 terminated UTF-8
     */
    private static String textValue(Object value)
    {
        if (value instanceof String s)
        {
            return s;
        }

        if (value instanceof byte[] b)
        {
            int end = b.length;
            while (end > 0 && b[end - 1] == 0)
            {
                end--;
            }
            return new String(b, 0, end, StandardCharsets.UTF_8);
        }

        return value == null ? null : String.valueOf(value);
    }

    /**
     * Parses the little-endian subtitle records of one @SBT stream payload
     */
    private static void parseSubtitleRecords(byte[] payload, int channel, long offset, List<CRIDSubtitle> output)
    {
        RandomAccessReader records = new RandomAccessReader(payload, ByteOrder.LITTLE_ENDIAN);
        while (records.remaining() > 0)
        {
            if (records.remaining() < SBT_RECORD_HEADER)
            {
                throw new ParseException("Truncated @SBT subtitle record header", offset + records.pos());
            }

            CRIDSubtitle subtitle = new CRIDSubtitle();
            subtitle.setChannel(channel);
            subtitle.setLanguageId(records.readU32());
            subtitle.setTimeUnit(records.readU32());
            subtitle.setStartTime(records.readU32());
            subtitle.setDuration(records.readU32());
            long textSize = records.readU32();
            if (textSize > records.remaining())
            {
                throw new ParseException("Truncated @SBT subtitle text (" + textSize + " bytes)", offset + records.pos());
            }

            byte[] text = records.readBytes(textSize);
            int    end  = text.length;
            while (end > 0 && text[end - 1] == 0)
            {
                end--;
            }
            subtitle.setRawText(Arrays.copyOf(text, end));
            output.add(subtitle);
        }
    }

    private List<CRIDStreamInfo> parseStreamInfo(CRIDUTFTable utfBlock)
    {
        List<CRIDStreamInfo> infos = new ArrayList<>();

        for (int i = 0; i < utfBlock.getRows(); i++)
        {
            CRIDStreamInfo stream = new CRIDStreamInfo();
            stream.setFilename((String) utfBlock.query("filename", i));
            stream.setFilesize(intValue(utfBlock.query("filesize", i)));
            stream.setDatasize(intValue(utfBlock.query("datasize", i)));
            stream.setStreamType(magicToString(intValue(utfBlock.query("stmid", i))));
            stream.setChannel(intValue(utfBlock.query("chno", i)));
            stream.setMinChunk(intValue(utfBlock.query("minchk", i)));
            stream.setMinBuffer(intValue(utfBlock.query("minbuf", i)));
            stream.setAvgBps(intValue(utfBlock.query("avbps", i)));
            infos.add(stream);
        }

        return infos;
    }

    /**
     * Turns a big-endian fourcc into its text, "USM\0" for 0 (the row describing the whole file)
     */
    private static String magicToString(int magic)
    {
        if (magic == 0)
        {
            return USM_SIGNATURE;
        }

        byte[] chars = {(byte) (magic >>> 24), (byte) (magic >>> 16), (byte) (magic >>> 8), (byte) magic};
        return new String(chars, StandardCharsets.ISO_8859_1);
    }

    private CRIDHeader parseHeader(RandomAccessReader raf)
    {
        CRIDHeader header = new CRIDHeader();
        header.setMagic(raf.readString(4));
        if (!header.getMagic().equals(CRID_SIGNATURE))
        {
            throw new ParseException("Invalid header magic, expected \"CRID\", got \"" + header.getMagic() + "\"", 0);
        }

        header.setBlockSize(raf.readInt());
        header.setHeaderSize(raf.readShort());
        header.setFooterSize(raf.readShort());
        header.setPayloadSize(header.getBlockSize() - header.getHeaderSize() - header.getFooterSize());

        header.setBlockType(raf.readInt());
        if (header.getBlockType() != 1)
        {
            throw new ParseException("Invalid header blockType, expected 1, got " + header.getBlockType(), raf.pos() - 4);
        }

        header.setGranule(raf.readInt());
        header.setSamples(raf.readInt());
        header.setUnk(raf.readInt());
        header.setUnk2(raf.readInt());

        if ((header.getUnk() != 0) || (header.getUnk2() != 0))
        {
            throw new ParseException("Invalid header, expected reserved fields to be \"0, 0\", got \"" + header.getUnk() + ", " + header.getUnk2() + "\"", raf.pos() - 8);
        }

        // the payload starts at 0x08 + payload offset
        raf.seek(CHUNK_FIXED_HEADER + Short.toUnsignedInt(header.getHeaderSize()));
        return header;
    }

    /**
     * Reads the 0x00 terminated string at offset in the string table
     */
    private static String stringAt(byte[] strings, int offset)
    {
        if (offset < 0 || offset >= strings.length)
        {
            return "";
        }

        int end = offset;
        while (end < strings.length && strings[end] != 0)
        {
            end++;
        }
        return new String(strings, offset, end - offset, StandardCharsets.UTF_8);
    }

    /**
     * Parses the @UTF table starting at the absolute offset.
     * All offsets in the table are relative to offset + 8.
     */
    private CRIDUTFTable parseUTF(RandomAccessReader raf, int offset)
    {
        raf.seek(offset);
        String magic = raf.readString(4);
        if (!magic.equals(UTF_SIGNATURE))
        {
            throw new ParseException("Invalid @UTF block, expected \"@UTF\", got \"" + magic + "\"", offset);
        }

        int base = offset + 8;

        CRIDUTFTable table = new CRIDUTFTable();
        table.setTableOffset(offset);
        table.setTableSize(raf.readInt());
        table.setVersion(raf.readU16());
        table.setRowOffset(raf.readU16());
        table.setSchemaOffset(0x20);
        table.setStringTableOffset(raf.readInt());
        table.setDataOffset(raf.readInt());
        int tableNameStringIndex = raf.readInt();
        table.setColumns(raf.readShort());
        table.setRowWidth(raf.readShort());
        table.setRows(raf.readInt());

        int stringTableSize = table.getDataOffset() - table.getStringTableOffset();
        if (stringTableSize < 0 || table.getStringTableOffset() < 0 || base + (long) table.getDataOffset() > raf.size())
        {
            throw new ParseException("Invalid @UTF section offsets", offset);
        }

        int schemaPos = (int) raf.pos();
        raf.seek(base + table.getStringTableOffset());
        byte[] strings = raf.readBytes(stringTableSize);
        table.setStringTable(new String(strings, StandardCharsets.UTF_8));
        table.setTableName(stringAt(strings, tableNameStringIndex));
        raf.seek(schemaPos);

        int                      columnCount = Short.toUnsignedInt(table.getColumns());
        List<CRIDUTFTableColumn> schema      = new ArrayList<>(columnCount);
        int[]                    rowOffsets  = new int[columnCount];
        int                      rowCursor   = 0;

        for (int i = 0; i < columnCount; i++)
        {
            CRIDUTFTableColumn column = new CRIDUTFTableColumn();
            int                flags  = raf.readU8();
            column.setTypeFlags(flags);
            column.setColumnName(stringAt(strings, raf.readInt()));

            CRIDUTFTableColumnType type = CRIDUTFTableColumnType.fromFlags(flags);
            if (type == null)
            {
                throw new ParseException("Unknown @UTF column type " + (flags & CRIDUTFTableColumnType.COLUMN_TYPE_MASK) + " for column " + column.getColumnName(), raf.pos() - 5);
            }

            if (CRIDUTFTableColumnStorageType.hasDefault(flags))
            {
                // the constant value is stored inline in the schema
                column.setConstantOffset((int) raf.pos());
                raf.seekFromCurrentPosition(type.getSize());
            } else
            {
                column.setConstantOffset(-1);
            }

            if (CRIDUTFTableColumnStorageType.isPerRow(flags))
            {
                rowOffsets[i] = rowCursor;
                rowCursor += type.getSize();
            } else
            {
                rowOffsets[i] = -1;
            }

            schema.add(column);
        }
        table.setSchema(schema);

        int rowWidth = Short.toUnsignedInt(table.getRowWidth());
        for (int j = 0; j < columnCount; j++)
        {
            CRIDUTFTableColumn     column = schema.get(j);
            CRIDUTFTableColumnType type   = CRIDUTFTableColumnType.fromFlags(column.getTypeFlags());
            List<Object>           values = new ArrayList<>(table.getRows());

            // a constant column has the same value in every row, so only read it once
            Object constant = null;
            if (rowOffsets[j] < 0)
            {
                constant = column.getConstantOffset() >= 0 ? readValue(raf, table, strings, type, column.getConstantOffset()) : zeroValue(type);
            }

            for (int i = 0; i < table.getRows(); i++)
            {
                if (constant != null)
                {
                    values.add(constant);
                } else
                {
                    int dataOffset = base + table.getRowOffset() + (i * rowWidth) + rowOffsets[j];
                    values.add(readValue(raf, table, strings, type, dataOffset));
                }
            }

            column.setValues(values);
        }

        raf.seek(offset + 8L + table.getTableSize());
        return table;
    }

    private static Object zeroValue(CRIDUTFTableColumnType type)
    {
        return switch (type)
        {
            case COLUMN_TYPE_U8, COLUMN_TYPE_U16, COLUMN_TYPE_S32 -> 0;
            case COLUMN_TYPE_S8 -> (byte) 0;
            case COLUMN_TYPE_S16 -> (short) 0;
            case COLUMN_TYPE_U32, COLUMN_TYPE_U64, COLUMN_TYPE_S64 -> 0L;
            case COLUMN_TYPE_FLOAT -> 0f;
            case COLUMN_TYPE_DOUBLE -> 0d;
            case COLUMN_TYPE_STRING -> "";
            case COLUMN_TYPE_DATA -> new byte[0];
            case COLUMN_TYPE_GUID -> new byte[16];
        };
    }

    /**
     * Reads one value at the absolute offset.
     * Unsigned types are widened (u8/u16 to Integer, u32 to Long, u64 to Long), signed ones keep their Java type.
     * Data is returned as a nested CRIDUTFTable if it is one, else as the raw bytes.
     */
    private Object readValue(RandomAccessReader raf, CRIDUTFTable table, byte[] strings, CRIDUTFTableColumnType type, int dataOffset)
    {
        raf.seek(dataOffset);
        return switch (type)
        {
            case COLUMN_TYPE_U8 -> raf.readU8();
            case COLUMN_TYPE_S8 -> raf.readByte();
            case COLUMN_TYPE_U16 -> raf.readU16();
            case COLUMN_TYPE_S16 -> raf.readShort();
            case COLUMN_TYPE_U32 -> raf.readU32();
            case COLUMN_TYPE_S32 -> raf.readInt();
            case COLUMN_TYPE_U64, COLUMN_TYPE_S64 -> raf.readLong();
            case COLUMN_TYPE_FLOAT -> raf.readFloat();
            case COLUMN_TYPE_DOUBLE -> raf.readDouble();
            case COLUMN_TYPE_STRING -> stringAt(strings, raf.readInt());
            case COLUMN_TYPE_GUID -> raf.readBytes(16);
            case COLUMN_TYPE_DATA ->
            {
                int extDataOffset = raf.readInt();
                int extDataSize   = raf.readInt();
                if (extDataSize <= 0)
                {
                    yield new byte[0];
                }

                int absolute = table.getTableOffset() + 8 + table.getDataOffset() + extDataOffset;
                raf.seek(absolute);
                if (extDataSize >= 4 && UTF_SIGNATURE.equals(raf.readString(4)))
                {
                    yield parseUTF(raf, absolute);
                }

                raf.seek(absolute);
                yield raf.readBytes(extDataSize);
            }
        };
    }

    private record StreamKey(int magic, int channel) {}

    private List<Pair<String, byte[]>> demultiplexStream(RandomAccessReader reader, CRIDFile file)
    {
        List<CRIDStreamInfo>            streamData = file.getStreams();
        Map<StreamKey, NamedByteWriter> writers    = new LinkedHashMap<>();
        List<CRIDSubtitle>              subtitles  = new ArrayList<>();
        Map<Integer, CRIDUTFTable>      subHeaders = new TreeMap<>();
        List<Map<String, Object>>       cueRows    = new ArrayList<>();
        long                            cueUnit    = 0;

        long pos = 0;
        while (reader.size() - pos >= CHUNK_FIXED_HEADER)
        {
            reader.seek(pos);
            int  magic     = reader.readInt();
            long chunkSize = reader.readU32();
            long chunkEnd  = pos + CHUNK_FIXED_HEADER + chunkSize;

            if (chunkSize < CHUNK_HEADER_SIZE - CHUNK_FIXED_HEADER || chunkEnd > reader.size())
            {
                throw new ParseException("Invalid USM chunk size " + chunkSize, pos);
            }

            int payloadOffset = reader.readU16();
            int padding       = reader.readU16();
            int channel       = reader.readU8();
            reader.readU8();
            int payloadType = reader.readU16() & 0x3;

            boolean video = magic == MAGIC_SFV || magic == MAGIC_ALP;
            boolean audio = magic == MAGIC_SFA;

            // header, section end and metadata payloads (@UTF tables and "#... END" markers) are not part of the stream
            if ((video || audio) && payloadType == PAYLOAD_TYPE_STREAM)
            {
                long payloadStart = pos + CHUNK_FIXED_HEADER + payloadOffset;
                long payloadSize  = chunkSize - payloadOffset - padding;

                if (payloadSize > 0 && payloadStart + payloadSize <= chunkEnd)
                {
                    StreamKey key = new StreamKey(magic, channel);
                    NamedByteWriter writer = writers.computeIfAbsent(key, k -> new NamedByteWriter(createOutputName(streamData, k, audio)));

                    reader.seek(payloadStart);
                    writer.writeByteArray(reader.readBytes(payloadSize));
                }
            }

            if (magic == MAGIC_SBT || magic == MAGIC_CUE)
            {
                long payloadStart = pos + CHUNK_FIXED_HEADER + payloadOffset;
                long payloadSize  = chunkSize - payloadOffset - padding;

                if (payloadSize > 0 && payloadStart + payloadSize <= chunkEnd)
                {
                    if (payloadType == PAYLOAD_TYPE_STREAM && magic == MAGIC_SBT)
                    {
                        reader.seek(payloadStart);
                        parseSubtitleRecords(reader.readBytes(payloadSize), channel, payloadStart, subtitles);
                    } else if (payloadType == PAYLOAD_TYPE_HEADER || payloadType == PAYLOAD_TYPE_METADATA)
                    {
                        // header (SUBTITLE_HDRINFO / CUEPOINT_HDRINFO) and metadata (CUEPOINT_INFO) payloads are @UTF tables;
                        // the "#HEADER END" / "#METADATA END" markers use the same payload types, so check the magic
                        reader.seek(payloadStart);
                        if (payloadSize >= 4 && UTF_SIGNATURE.equals(reader.readString(4)))
                        {
                            CRIDUTFTable table = parseUTF(reader, (int) payloadStart);
                            if (magic == MAGIC_SBT)
                            {
                                subHeaders.put(channel, table);
                            } else if ("CUEPOINT_HDRINFO".equals(table.getTableName()) && table.getRows() > 0)
                            {
                                cueUnit = longValue(table.query("time_unit", 0));
                            } else if ("CUEPOINT_INFO".equals(table.getTableName()))
                            {
                                for (int i = 0; i < table.getRows(); i++)
                                {
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    for (CRIDUTFTableColumn column : table.getSchema())
                                    {
                                        row.put(column.getColumnName(), column.getValues().get(i));
                                    }
                                    cueRows.add(row);
                                }
                            }
                        }
                    }
                }
            }

            // unknown chunks are skipped by their size
            pos = chunkEnd;
        }

        List<CRIDCuePoint> cuePoints = new ArrayList<>(cueRows.size());
        for (Map<String, Object> row : cueRows)
        {
            CRIDCuePoint cue = new CRIDCuePoint();
            cue.setTime(longValue(row.get("time")));
            cue.setTimeUnit(cueUnit);
            cue.setName(textValue(row.get("name")));
            cue.setParameter(textValue(row.get("parameter")));
            cuePoints.add(cue);
        }

        file.setSubtitles(subtitles);
        file.setSubtitleHeaders(subHeaders);
        file.setCueTimeUnit(cueUnit);
        file.setCuePoints(cuePoints);

        List<Pair<String, byte[]>> returns = new ArrayList<>();
        for (Map.Entry<StreamKey, NamedByteWriter> entry : writers.entrySet())
        {
            byte[] data = entry.getValue().toByteArray();
            String name = entry.getValue().getName();
            if (entry.getKey().magic() == MAGIC_SFA)
            {
                name = UtilHandler.replaceEnding(name, AUDIO_EXT, detectAudioExtension(data));
            }

            returns.add(new Pair<>(name, data));
        }
        return returns;
    }

    private String createOutputName(List<CRIDStreamInfo> streamData, StreamKey key, boolean audio)
    {
        String type = magicToString(key.magic());

        CRIDStreamInfo stream = streamData.stream()
                                          .filter(a -> type.equals(a.getStreamType()) && a.getChannels() == key.channel())
                                          .findFirst()
                                          .orElseGet(() -> streamData.stream().filter(a -> type.equals(a.getStreamType())).findFirst().orElse(null));

        String base = safeBaseName(stream != null ? stream.getFilename() : null);
        String name = base + type;
        if (key.channel() != 0)
        {
            name += "_ch" + key.channel();
        }

        return name + "." + (audio ? AUDIO_EXT : VIDEO_EXT);
    }

    /**
     * The "filename" column is often the encoder's source path (e.g. "D:\movies\intro.avi"); keep only its last part,
     * without characters Windows does not allow, so the stream is always written inside the extraction folder
     */
    static String safeBaseName(String filename)
    {
        if (filename == null)
        {
            return "stream";
        }

        String name = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\x00-\\x1F<>:\"|?*]", "_").strip();
        if (name.isEmpty() || name.chars().allMatch(c -> c == '.'))
        {
            return "stream";
        }
        return name;
    }

    private static String detectAudioExtension(byte[] data)
    {
        if (data.length < 4)
        {
            return "bin";
        }

        if (data[0] == 'A' && data[1] == 'I' && data[2] == 'X' && data[3] == 'F')
        {
            return "aix";
        }

        if ((data[0] & 0xFF) == 0x80)
        {
            return "adx";
        }

        // the high bit of each signature byte is set if the HCA file is encrypted
        if ((data[0] & 0x7F) == 'H' && (data[1] & 0x7F) == 'C' && (data[2] & 0x7F) == 'A')
        {
            return "hca";
        }

        return "bin";
    }
}
