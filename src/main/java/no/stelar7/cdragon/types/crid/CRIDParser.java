package no.stelar7.cdragon.types.crid;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.crid.data.*;
import no.stelar7.cdragon.types.crid.data.utf.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.writers.NamedByteWriter;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("FieldCanBeLocal")
public class CRIDParser implements Parseable<List<Pair<String, byte[]>>>
{
    private final String VIDEO_EXT = "m2v";
    private final String AUDIO_EXT = "adx";
    private final String HCA_EXT   = "hca";
    
    private final String ADX_EXT = ".adx";
    private final String AIX_EXT = ".aix";
    private final String AC3_EXT = ".ac3";
    
    private final String HCA_SIGNATURE  = "HCA\0";
    private final String AIX_SIGNATURE  = "AIXF";
    private final String ALP_SIGNATURE  = "@ALP";
    private final String SFV_SIGNATURE  = "@SFV";
    private final String SFA_SIGNATURE  = "@SFA";
    private final String SBT_SIGNATURE  = "@SBT";
    private final String CUE_SIGNATURE  = "@CUE";
    private final String UTF_SIGNATURE  = "@UTF";
    private final String CRID_SIGNATURE = "CRID";
    
    private final String HEADER_END   = "#HEADER END     ===============\0";
    private final String METADATA_END = "#METADATA END   ===============\0";
    private final String CONTENTS_END = "#CONTENTS END   ===============\0";
    
    private final Map<Integer, BlockType> BLOCK_DICT = new HashMap<>()
    {
        {
            put(ByteBuffer.wrap(ALP_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CRID_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFV_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFA_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SBT_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CUE_SIGNATURE.getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
        }
    };
    
    
    @Override
    public List<Pair<String, byte[]>> parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.BIG_ENDIAN));
    }
    
    @Override
    public List<Pair<String, byte[]>> parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.BIG_ENDIAN));
    }
    
    @Override
    public List<Pair<String, byte[]>> parse(RandomAccessReader raf)
    {
        CRIDHeader header = parseHeader(raf);
        
        int endOfHeader = raf.pos();
        
        CRIDUTFTable utfBlock = parseUTF(raf, endOfHeader);
        if (utfBlock.getRows() < 1)
        {
            System.out.format("Expected atleast one row, (got %s)%n", utfBlock.getRows());
            System.exit(0);
        }
        
        if (!utfBlock.getTableName().equals("CRIUSF_DIR_STREAM"))
        {
            System.out.format("Expected table to be \"CRIUSF_DIR_STREAM\", (got %s)%n", utfBlock.getTableName());
            System.exit(0);
        }
        
        
        raf.seek(0);
        return demultiplexStream(raf);
    }
    
    private CRIDHeader parseHeader(RandomAccessReader raf)
    {
        CRIDHeader header = new CRIDHeader();
        header.setMagic(raf.readString(4));
        if (!header.getMagic().equals(CRID_SIGNATURE))
        {
            System.out.format("Invalid header magic, expected \"CRID\" (got \"%s\")%n", header.getMagic());
            System.exit(0);
        }
        
        header.setBlockSize(raf.readInt());
        header.setHeaderSize(raf.readShort());
        header.setFooterSize(raf.readShort());
        header.setPayloadSize(header.getBlockSize() - header.getHeaderSize() - header.getFooterSize());
        
        header.setBlockType(raf.readInt());
        if (header.getBlockType() != 1)
        {
            System.out.format("Invalid header blockType, expected \"1\" (got \"%s\")%n", header.getBlockType());
            System.exit(0);
        }
        
        header.setGranule(raf.readInt());
        header.setSamples(raf.readInt());
        header.setUnk(raf.readInt());
        header.setUnk2(raf.readInt());
        
        if ((header.getUnk() != 0) || (header.getUnk2() != 0))
        {
            System.out.format("Invalid header, expected \"0, 0\" (got \"%s, %s\")%n", header.getUnk(), header.getUnk2());
            System.exit(0);
        }
        
        return header;
    }
    
    private CRIDUTFTable parseUTF(RandomAccessReader raf, int offset)
    {
        raf.seek(offset);
        String magic = raf.readString(4);
        if (!magic.equals(UTF_SIGNATURE))
        {
            System.out.format("Invalid @UTF block, expected \"@UTF\" (got \"%s\")%n", magic);
            System.exit(0);
        }
        
        CRIDUTFTable table = new CRIDUTFTable();
        table.setTableOffset(offset);
        table.setTableSize(raf.readInt());
        table.setSchemaOffset(0x20);
        table.setRowOffset(raf.readInt());
        table.setStringTableOffset(raf.readInt());
        table.setDataOffset(raf.readInt());
        int tableNameStringIndex = raf.readInt();
        table.setColumns(raf.readShort());
        table.setRowWidth(raf.readShort());
        table.setRows(raf.readInt());
        
        int                      stringTableSize = table.getDataOffset() - table.getStringTableOffset();
        List<CRIDUTFTableColumn> schema          = new ArrayList<>(table.getColumns());
        
        int pos = raf.pos();
        raf.seek(offset + table.getStringTableOffset() + 8);
        table.setStringTable(raf.readString(stringTableSize));
        table.setTableName(table.getStringTable().substring(tableNameStringIndex, table.getStringTable().indexOf('\0', tableNameStringIndex)));
        raf.seek(pos);
        
        for (int i = 0; i < table.getColumns(); i++)
        {
            CRIDUTFTableColumn column = new CRIDUTFTableColumn();
            column.setType(raf.readByte());
            int columnNameOffset = raf.readInt();
            column.setColumnName(table.getStringTable().substring(columnNameOffset, table.getStringTable().indexOf('\0', columnNameOffset)));
            int maskedType = column.getType() & CRIDUTFTableColumnType.COLUMN_TYPE_MASK.getValue();
            if (maskedType == 0x30)
            {
                column.setConstantOffset(raf.pos());
                switch (CRIDUTFTableColumnType.valueOf(maskedType))
                {
                    case COLUMN_TYPE_STRING:
                    {
                        raf.readInt();
                        break;
                    }
                    case COLUMN_TYPE_8BYTE:
                    case COLUMN_TYPE_DATA:
                    {
                        raf.readLong();
                        break;
                    }
                    case COLUMN_TYPE_FLOAT:
                    case COLUMN_TYPE_4BYTE2:
                    case COLUMN_TYPE_4BYTE:
                    {
                        raf.readInt();
                        break;
                    }
                    case COLUMN_TYPE_2BYTE2:
                    case COLUMN_TYPE_2BYTE:
                    {
                        raf.readShort();
                        break;
                    }
                    case COLUMN_TYPE_1BYTE2:
                    case COLUMN_TYPE_1BYTE:
                    {
                        raf.readByte();
                        break;
                    }
                }
            }
            schema.add(column);
        }
        table.setSchema(schema);
        
        raf.seek(offset + table.getStringTableOffset() + 8);
        table.setStringTable(raf.readString(stringTableSize));
        
        for (int i = 0; i < table.getRows(); i++)
        {
            int rowOffset      = table.getTableOffset() + 8 + table.getRowOffset() + (i * table.getRowWidth());
            int rowStartOffset = rowOffset;
            
            for (int j = 0; j < table.getColumns(); j++)
            {
                int     type           = table.getSchema().get(j).getType();
                String  columnName     = table.getSchema().get(j).getColumnName();
                int     constantOffset = table.getSchema().get(j).getConstantOffset();
                boolean constant       = false;
                
                int realOffset = rowOffset - rowStartOffset;
                
                switch (CRIDUTFTableColumnStorageType.valueOf(type & CRIDUTFTableColumnStorageType.COLUMN_STORAGE_MASK.getValue()))
                {
                    case COLUMN_STORAGE_PERROW:
                    {
                        break;
                    }
                    case COLUMN_STORAGE_CONSTANT:
                    {
                        constant = true;
                        break;
                    }
                    case COLUMN_STORAGE_ZERO:
                    {
                        table.getSchema().get(j).getValues().add(i, 0);
                    }
                }
                
                
                int dataOffset;
                int bytesRead = 0;
                
                if (constant)
                {
                    dataOffset = constantOffset;
                    table.getSchema().get(j).getValues().add(i, "constant");
                } else
                {
                    dataOffset = rowOffset;
                }
                
                switch (CRIDUTFTableColumnType.valueOf(type & CRIDUTFTableColumnType.COLUMN_TYPE_MASK.getValue()))
                {
                    case COLUMN_TYPE_STRING:
                    {
                        raf.seek(dataOffset);
                        int    stringOffset = raf.readInt();
                        String value        = table.getStringTable().substring(stringOffset, table.getStringTable().indexOf('\0', stringOffset));
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 4;
                        break;
                    }
                    
                    case COLUMN_TYPE_DATA:
                    {
                        raf.seek(dataOffset);
                        int    extDataOffset = raf.readInt();
                        int    extDataSize   = raf.readInt();
                        Object value         = new Pair<>(extDataOffset, extDataSize);
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 8;
    
                        if (extDataSize != 0)
                        {
                            value = parseUTF(raf, table.getTableOffset() + 8 + table.getDataOffset() + extDataOffset);
                            table.getSchema().get(j).getValues().add(i, value);
                        }
                        break;
                    }
                    
                    case COLUMN_TYPE_8BYTE:
                    {
                        raf.seek(dataOffset);
                        long value = raf.readLong();
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 8;
                        break;
                    }
                    case COLUMN_TYPE_4BYTE2:
                    {
                        table.getSchema().get(j).getValues().add(i, "type 2");
                        break;
                    }
                    
                    case COLUMN_TYPE_4BYTE:
                    {
                        raf.seek(dataOffset);
                        int value = raf.readInt();
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 4;
                        break;
                    }
                    
                    case COLUMN_TYPE_2BYTE2:
                    {
                        table.getSchema().get(j).getValues().add(i, "type2");
                        break;
                    }
                    
                    case COLUMN_TYPE_2BYTE:
                    {
                        raf.seek(dataOffset);
                        int value = raf.readShort();
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 2;
                        break;
                    }
                    
                    case COLUMN_TYPE_FLOAT:
                    {
                        raf.seek(dataOffset);
                        float value = raf.readFloat();
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 4;
                        break;
                    }
                    
                    case COLUMN_TYPE_1BYTE2:
                    {
                        table.getSchema().get(j).getValues().add(i, "type 2");
                        break;
                    }
                    
                    case COLUMN_TYPE_1BYTE:
                    {
                        raf.seek(dataOffset);
                        float value = raf.readByte();
                        table.getSchema().get(j).getValues().add(i, value);
                        bytesRead = 1;
                        break;
                    }
                    default:
                    {
                        System.out.println("Unknown type!!");
                        break;
                    }
                }
                
                if (!constant)
                {
                    rowOffset += bytesRead;
                }
                
            }
        }
        
        return table;
    }
    
    private List<Pair<String, byte[]>> demultiplexStream(RandomAccessReader reader)
    {
        if (reader.readUntillString(CRID_SIGNATURE))
        {
            Map<Integer, NamedByteWriter> streamOutputWriters = new HashMap<>();
            Map<Integer, String>          streamIdFileType    = new HashMap<>();
            
            int currentPos = reader.pos();
            outer:
            while (reader.remaining() > 0)
            {
                byte[] currentBlockId    = reader.readBytes(4);
                int    currentBlockIdVal = ByteBuffer.wrap(currentBlockId).order(ByteOrder.LITTLE_ENDIAN).getInt();
                if (!BLOCK_DICT.containsKey(currentBlockIdVal))
                {
                    System.out.println("unknown block found..?");
                }
                
                BlockType block = BLOCK_DICT.get(currentBlockIdVal);
                
                switch (block.getType())
                {
                    case STATIC:
                        currentPos += block.getSize();
                        break;
                    case EOF:
                        break outer;
                    case SIZE:
                    {
                        reader.seek(currentPos + currentBlockId.length);
                        byte[] blockSizeArray = reader.readBytes(block.getSize());
                        UtilHandler.reverse(blockSizeArray);
                        reader.seek(currentPos);
                        
                        int blockSize = 0;
                        switch (block.getSize())
                        {
                            case 4:
                                blockSize = ByteBuffer.wrap(blockSizeArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                break;
                            case 2:
                                blockSize = ByteBuffer.wrap(blockSizeArray).order(ByteOrder.LITTLE_ENDIAN).getShort();
                                break;
                            case 1:
                                blockSize = ByteBuffer.wrap(blockSizeArray).order(ByteOrder.LITTLE_ENDIAN).get();
                                break;
                        }
                        
                        boolean audioBlock = isAudioBlock(currentBlockId);
                        boolean videoBlock = isVideoBlock(currentBlockId);
                        
                        if (videoBlock || audioBlock)
                        {
                            int streamId         = 0;
                            int currentStreamKey = currentBlockIdVal;
                            
                            if (!streamOutputWriters.containsKey(currentStreamKey))
                            {
                                byte[] currentBlockName = ByteBuffer.allocate(4).putInt(currentStreamKey).order(ByteOrder.LITTLE_ENDIAN).array();
                                String filePath         = UtilHandler.pathToFilename(reader.getPath());
                                String blockHash        = HashHandler.toHex(currentBlockName);
                                String outputFilename   = filePath + "_" + blockHash;
                                
                                if (audioBlock)
                                {
                                    outputFilename += "." + AUDIO_EXT;
                                    streamIdFileType.putIfAbsent(streamId, AUDIO_EXT);
                                } else
                                {
                                    outputFilename += "." + VIDEO_EXT;
                                }
                                
                                streamOutputWriters.put(currentStreamKey, new NamedByteWriter(outputFilename));
                            }
                            
                            if (audioBlock)
                            {
                                int audioBlockSkipSize   = getAudioPacketHeaderSize(reader, currentPos);
                                int audioBlockFooterSize = getAudioPacketFooterSize(reader, currentPos);
                                
                                int cutSize = blockSize - audioBlockSkipSize - audioBlockFooterSize;
                                if (cutSize > 0)
                                {
                                    int pos = reader.pos();
                                    reader.seek(currentPos + currentBlockId.length + blockSizeArray.length + audioBlockSkipSize);
                                    byte[] data = reader.readBytes(blockSize - audioBlockSkipSize);
                                    streamOutputWriters.get(currentStreamKey).writeByteArray(data, cutSize);
                                    reader.seek(pos);
                                    
                                }
                            } else
                            {
                                int videoBlockSkipSize   = getVideoPacketHeaderSize(reader, currentPos);
                                int videoBlockFooterSize = getVideoPacketFooterSize(reader, currentPos);
                                
                                int cutSize = blockSize - videoBlockSkipSize - videoBlockFooterSize;
                                if (cutSize > 0)
                                {
                                    int pos = reader.pos();
                                    reader.seek(currentPos + currentBlockId.length + blockSizeArray.length + videoBlockSkipSize);
                                    byte[] data = reader.readBytes(blockSize - videoBlockSkipSize);
                                    streamOutputWriters.get(currentStreamKey).writeByteArray(data, cutSize);
                                    reader.seek(pos);
                                }
                            }
                        }
                        
                        currentPos += currentBlockId.length + blockSizeArray.length + blockSize;
                        reader.seek(currentPos);
                        blockSizeArray = new byte[0];
                    }
                }
            }
            return finalize(reader, streamOutputWriters);
        }
        return null;
    }
    
    private int getVideoPacketFooterSize(RandomAccessReader reader, int currentPos)
    {
        return getVaryingByteValueAtRelativeOffset(reader, currentPos, ByteOrder.BIG_ENDIAN, 2, 10);
    }
    
    private int getVideoPacketHeaderSize(RandomAccessReader reader, int currentPos)
    {
        return getVaryingByteValueAtRelativeOffset(reader, currentPos, ByteOrder.BIG_ENDIAN, 2, 8);
    }
    
    private int getAudioPacketFooterSize(RandomAccessReader reader, int currentPos)
    {
        return getVaryingByteValueAtRelativeOffset(reader, currentPos, ByteOrder.BIG_ENDIAN, 2, 10);
    }
    
    private int getAudioPacketHeaderSize(RandomAccessReader reader, int currentPos)
    {
        return getVaryingByteValueAtRelativeOffset(reader, currentPos, ByteOrder.BIG_ENDIAN, 2, 8);
    }
    
    private int getVaryingByteValueAtRelativeOffset(RandomAccessReader reader, int currentOffset, ByteOrder order, int size, int value)
    {
        int newValueOffset = currentOffset + value;
        int newValueLength = size;
        
        return getVaryingByteValueAtOffset(reader, newValueOffset, newValueLength, order == ByteOrder.LITTLE_ENDIAN, false);
    }
    
    private int getVaryingByteValueAtOffset(RandomAccessReader reader, int valueOffset, int valueLength, boolean order, boolean negative)
    {
        
        if (negative && (valueOffset < 0))
        {
            valueOffset = reader.pos() + valueOffset;
        }
        
        int pos = reader.pos();
        reader.seek(valueOffset);
        byte[] newValueBytes = ByteBuffer.wrap(reader.readBytes(valueLength)).order(ByteOrder.LITTLE_ENDIAN).array();
        reader.seek(pos);
        
        switch (newValueBytes.length)
        {
            case 1:
                return ByteBuffer.wrap(newValueBytes).order(ByteOrder.BIG_ENDIAN).get();
            case 2:
                return ByteBuffer.wrap(newValueBytes).order(ByteOrder.BIG_ENDIAN).getShort();
            case 4:
                return ByteBuffer.wrap(newValueBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            default:
                return -1;
        }
    }
    
    private boolean isVideoBlock(byte[] blockToCheck)
    {
        return compareSegmentUsingSourceOffset(blockToCheck, 0, SFV_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    }
    
    private boolean isAudioBlock(byte[] blockToCheck)
    {
        return compareSegmentUsingSourceOffset(blockToCheck, 0, SFA_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    }
    
    private boolean isUTFBlock(byte[] blockToCheck)
    {
        return compareSegmentUsingSourceOffset(blockToCheck, 0, UTF_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    }
    
    private List<Pair<String, byte[]>> finalize(RandomAccessReader raf, Map<Integer, NamedByteWriter> outputFiles)
    {
        List<Pair<String, byte[]>> returns = new ArrayList<>();
        for (Entry<Integer, NamedByteWriter> entry : outputFiles.entrySet())
        {
            Integer         key      = entry.getKey();
            NamedByteWriter value    = entry.getValue();
            String          filename = value.getName();
            
            RandomAccessReader headEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            RandomAccessReader metaEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            
            headEnd.readUntillString(HEADER_END);
            metaEnd.readUntillString(METADATA_END);
            
            int headerEndOffset   = headEnd.pos();
            int metadataEndOffset = metaEnd.pos();
            
            int headerSize = METADATA_END.length() + ((metadataEndOffset > headerEndOffset) ? metadataEndOffset : headerEndOffset);
            
            RandomAccessReader footEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            footEnd.readUntillString(CONTENTS_END);
            int footerOffset = footEnd.pos() - headerSize;
            int footerSize   = value.toByteArray().length - footerOffset;
            
            String fileExt = "";
            
            if (isAudioBlock(ByteBuffer.allocate(4).putInt(key).array()))
            {
                RandomAccessReader checkRead = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
                checkRead.seek(headerSize);
                byte[] check = checkRead.readBytes(4);
                
                
                if (compareSegmentUsingSourceOffset(check, 0, AIX_SIGNATURE.getBytes(StandardCharsets.UTF_8)))
                {
                    fileExt = AIX_EXT;
                } else if (check[0] == 0x80)
                {
                    fileExt = ADX_EXT;
                } else if (compareSegmentUsingSourceOffset(check, 0, HCA_SIGNATURE.getBytes(StandardCharsets.UTF_8)))
                {
                    fileExt = HCA_EXT;
                } else
                {
                    fileExt = "bin";
                }
            } else
            {
                fileExt = UtilHandler.getEnding(filename);
            }
            
            String outputName = UtilHandler.replaceEnding(filename, AUDIO_EXT, fileExt);
            outputName = UtilHandler.replaceEnding(outputName, VIDEO_EXT, fileExt);
            
            value.remove(0, headerSize);
            byte[] remainingContent = Arrays.copyOfRange(value.toByteArray(), footerOffset, footerOffset + footerSize);
            byte[] footerContent    = new String(remainingContent).replaceAll("\0", "").getBytes(StandardCharsets.UTF_8);
            value.remove(footerOffset, Math.min(footerContent.length + 1, footerSize));
            
            returns.add(new Pair<>(outputName, value.toByteArray()));
        }
        return returns;
    }
    
    private boolean compareSegmentUsingSourceOffset(byte[] sourceArray, int offset, byte[] target)
    {
        for (int j = offset, i = offset; i < target.length; i++, j++)
        {
            if (sourceArray[i] != target[j])
            {
                return false;
            }
        }
        
        return true;
    }
}
