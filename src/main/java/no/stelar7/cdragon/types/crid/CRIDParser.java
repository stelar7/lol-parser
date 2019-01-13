package no.stelar7.cdragon.types.crid;

import no.stelar7.cdragon.interfaces.Parseable;
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
    
    private final ByteArray HCA_SIGNATURE  = new ByteArray("HCA".getBytes(StandardCharsets.UTF_8), 4);
    private final ByteArray AIX_SIGNATURE  = new ByteArray("AIXF".getBytes(StandardCharsets.UTF_8));
    private final ByteArray ALP_SIGNATURE  = new ByteArray("@ALP".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SFV_SIGNATURE  = new ByteArray("@SFV".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SFA_SIGNATURE  = new ByteArray("@SFA".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SBT_SIGNATURE  = new ByteArray("@SBT".getBytes(StandardCharsets.UTF_8));
    private final ByteArray CUE_SIGNATURE  = new ByteArray("@CUE".getBytes(StandardCharsets.UTF_8));
    private final ByteArray UTF_SIGNATURE  = new ByteArray("@UTF".getBytes(StandardCharsets.UTF_8));
    private final ByteArray CRID_SIGNATURE = new ByteArray("CRID".getBytes(StandardCharsets.UTF_8));
    
    private final ByteArray HEADER_END   = new ByteArray("#HEADER END     ===============".getBytes(StandardCharsets.UTF_8), 32);
    private final ByteArray METADATA_END = new ByteArray("#METADATA END   ===============".getBytes(StandardCharsets.UTF_8), 32);
    private final ByteArray CONTENTS_END = new ByteArray("#CONTENTS END   ===============".getBytes(StandardCharsets.UTF_8), 32);
    
    private final Map<Integer, BlockType> BLOCK_DICT = new HashMap<>()
    {
        {
            put(ByteBuffer.wrap(ALP_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CRID_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFV_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFA_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SBT_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CUE_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
        }
    };
    
    
    @Override
    public List<Pair<String, byte[]>> parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public List<Pair<String, byte[]>> parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public List<Pair<String, byte[]>> parse(RandomAccessReader raf)
    {
        return demultiplexStream(raf);
    }
    
    private List<Pair<String, byte[]>> demultiplexStream(RandomAccessReader reader)
    {
        if (reader.readUntillString(new String(CRID_SIGNATURE.getData(), StandardCharsets.UTF_8)))
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
        return compareSegmentUsingSourceOffset(blockToCheck, 0, SFV_SIGNATURE.getData());
    }
    
    private boolean isAudioBlock(byte[] blockToCheck)
    {
        return compareSegmentUsingSourceOffset(blockToCheck, 0, SFA_SIGNATURE.getData());
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
            
            headEnd.readUntillString(new String(HEADER_END.getData(), StandardCharsets.UTF_8));
            metaEnd.readUntillString(new String(METADATA_END.getData(), StandardCharsets.UTF_8));
            
            int headerEndOffset   = headEnd.pos();
            int metadataEndOffset = metaEnd.pos();
            
            int headerSize = METADATA_END.getData().length + ((metadataEndOffset > headerEndOffset) ? metadataEndOffset : headerEndOffset);
            
            RandomAccessReader footEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            footEnd.readUntillString(new String(CONTENTS_END.getData(), StandardCharsets.UTF_8));
            int footerOffset = footEnd.pos() - headerSize;
            int footerSize   = value.toByteArray().length - footerOffset;
            
            String fileExt = "";
            
            if (isAudioBlock(ByteBuffer.allocate(4).putInt(key).array()))
            {
                RandomAccessReader checkRead = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
                checkRead.seek(headerSize);
                byte[] check = checkRead.readBytes(4);
                
                
                if (compareSegmentUsingSourceOffset(check, 0, AIX_SIGNATURE.getData()))
                {
                    fileExt = AIX_EXT;
                } else if (check[0] == 0x80)
                {
                    fileExt = ADX_EXT;
                } else if (compareSegmentUsingSourceOffset(check, 0, HCA_SIGNATURE.getData()))
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
