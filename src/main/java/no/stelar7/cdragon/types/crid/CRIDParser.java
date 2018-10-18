package no.stelar7.cdragon.types.crid;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CRIDParser implements Parseable
{
    private final String VIDEO_EXT = "m2v";
    private final String AUDIO_EXT = "adx";
    private final String HCA_EXT   = "hca";
    
    private final String ADX_EXT = ".adx";
    private final String AIX_EXT = ".aix";
    private final String AC3_EXT = ".ac3";
    
    private final ByteArray AIX_SIGNATURE  = new ByteArray(new byte[]{0x41, 0x49, 0x58, 0x46});
    private final ByteArray HCA_SIGNATURE  = new ByteArray(new byte[]{0x48, 0x43, 0x41, 0x00});
    private final ByteArray ALP_SIGNATURE  = new ByteArray("@ALP".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SFV_SIGNATURE  = new ByteArray("@SFV".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SFA_SIGNATURE  = new ByteArray("@SFA".getBytes(StandardCharsets.UTF_8));
    private final ByteArray SBT_SIGNATURE  = new ByteArray("@SBT".getBytes(StandardCharsets.UTF_8));
    private final ByteArray CUE_SIGNATURE  = new ByteArray("@CUE".getBytes(StandardCharsets.UTF_8));
    private final ByteArray UTF_SIGNATURE  = new ByteArray("@UTF".getBytes(StandardCharsets.UTF_8));
    private final ByteArray CRID_SIGNATURE = new ByteArray("CRID".getBytes(StandardCharsets.UTF_8));
    
    private final ByteArray HEADER_END   = new ByteArray("#HEADER END     ===============".getBytes(StandardCharsets.UTF_8));
    private final ByteArray METADATA_END = new ByteArray("#METADATA END     ===============".getBytes(StandardCharsets.UTF_8));
    private final ByteArray CONTENTS_END = new ByteArray("#CONTENTS END     ===============".getBytes(StandardCharsets.UTF_8));
    
    private final Map<Integer, BlockType> BLOCK_DICT = new HashMap<>()
    {
        {
            // System Packets
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xB9}).getInt(), new BlockType(PacketType.EOF, -1));
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xBA}).getInt(), new BlockType(PacketType.STATIC, 0xE)); // Pack Header
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xBB}).getInt(), new BlockType(PacketType.SIZE, 2)); // System Header, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xBD}).getInt(), new BlockType(PacketType.SIZE, 2)); // Private Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xBE}).getInt(), new BlockType(PacketType.SIZE, 2)); // Padding Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xBF}).getInt(), new BlockType(PacketType.SIZE, 2)); // Private Stream, two bytes following equal length (Big Endian)
            
            // Audio Streams
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC1}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC2}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC3}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC4}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC5}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC6}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC7}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC8}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xC9}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCA}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCB}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCC}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCD}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCE}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xCF}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD0}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD1}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD2}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD3}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD4}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD5}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD6}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD7}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD8}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xD9}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDA}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDB}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDC}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDD}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDE}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xDF}).getInt(), new BlockType(PacketType.SIZE, 2)); // Audio Stream, two bytes following equal length (Big Endian)
            
            // Video Streams
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE0}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE1}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE2}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE3}).getInt(), new BlockType(PacketType.SIZE, 2));
            
            // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE4}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE5}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE6}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE7}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE8}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xE9}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xEA}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xEB}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xEC}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xED}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xEE}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            put(ByteBuffer.wrap(new byte[]{0x00, 0x00, 0x01, (byte) 0xEF}).getInt(), new BlockType(PacketType.SIZE, 2)); // Video Stream, two bytes following equal length (Big Endian)
            
            put(ByteBuffer.wrap(ALP_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CRID_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFV_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SFA_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(SBT_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
            put(ByteBuffer.wrap(CUE_SIGNATURE.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(), new BlockType(PacketType.SIZE, 4));
        }
    };
    
    
    @Override
    public Object parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public Object parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public Object parse(RandomAccessReader raf)
    {
        demultiplexStream(raf);
        return null;
    }
    
    
    private void demultiplexStream(RandomAccessReader reader)
    {
        if (reader.seekUntil(CRID_SIGNATURE))
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
                        reader.seek(currentPos);
                        
                        int blockSize = 0;
                        switch (block.getSize())
                        {
                            case 4:
                                blockSize = ByteBuffer.wrap(blockSizeArray).getInt();
                                break;
                            case 2:
                                blockSize = ByteBuffer.wrap(blockSizeArray).getShort();
                                break;
                            case 1:
                                blockSize = ByteBuffer.wrap(blockSizeArray).get();
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
                                byte[] currentBlockName = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(currentStreamKey).array();
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
                                    streamOutputWriters.get(currentStreamKey).save();
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
                                    streamOutputWriters.get(currentStreamKey).save();
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
            finalize(reader, streamOutputWriters);
        }
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
    
    private void finalize(RandomAccessReader raf, Map<Integer, NamedByteWriter> outputFiles)
    {
        outputFiles.forEach((key, value) -> {
            String filename   = value.name;
            int    headerSize = 0;
            
            
            RandomAccessReader headEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            headEnd.seekUntil(HEADER_END);
            int headerEndOffset = headEnd.pos();
            
            RandomAccessReader metaEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            metaEnd.seekUntil(METADATA_END);
            int metadataEndOffset = metaEnd.pos();
            
            if (metadataEndOffset > headerEndOffset)
            {
                headerSize = metadataEndOffset + METADATA_END.getData().length;
            } else
            {
                headerSize = headerEndOffset + METADATA_END.getData().length;
            }
            
            RandomAccessReader footEnd = new RandomAccessReader(value.toByteArray(), ByteOrder.LITTLE_ENDIAN);
            footEnd.seekUntil(CONTENTS_END);
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
                fileExt = "unkn";
            }
          /*
            try (FileOutputStream fos = new FileOutputStream(filename + "." + fileExt))
            {
                fos.write(value.toByteArray());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            */
        });
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
