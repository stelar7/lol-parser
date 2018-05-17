package no.stelar7.cdragon.types.bnk;

import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.util.*;

public class BNKHIRCObject
{
    private BNKHIRCType type;
    private int         length;
    private int         id;
    private byte[]      data;
    
    public BNKHIRCType getType()
    {
        return type;
    }
    
    public void setType(byte val)
    {
        this.type = BNKHIRCType.fromByte(val);
    }
    
    public int getLength()
    {
        return length;
    }
    
    public void setLength(int length)
    {
        this.length = length;
    }
    
    public int getId()
    {
        return id;
    }
    
    public void setId(int id)
    {
        this.id = id;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
    }
    
    public void parse()
    {
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        switch (type)
        {
            case SETTING:
            {
                Map<BNKHIRCSettingType, Float> settingsMap = new HashMap<>();
                List<Byte>                     settings    = new ArrayList<>();
                
                byte settingCount = raf.readByte();
                for (int i = 0; i < settingCount; i++)
                {
                    settings.add(raf.readByte());
                }
                for (int i = 0; i < settingCount; i++)
                {
                    settingsMap.put(BNKHIRCSettingType.fromByte(settings.get(i)), raf.readFloat());
                }
                break;
            }
            case SFX_VOICE:
            {
                int dataSize = 17;
                
                byte[] unknown   = raf.readBytes(4);
                int    soundType = raf.readInt(); // embedd, stream or prefetch
                int    audioId   = raf.readInt();
                int    sourceId  = raf.readInt();
                
                if (soundType == 0)
                {
                    int offset = raf.readInt();
                    int length = raf.readInt();
                    
                    dataSize += 8;
                }
                
                byte   type      = raf.readByte();
                byte[] soundData = raf.readBytes(getLength() - dataSize - 4);
                parseSoundStruct(soundData);
                break;
            }
            default:
            {
                //System.out.println("unhandled event: " + type);
            }
        }
    }
    
    private void parseSoundStruct(byte[] soundData)
    {
        /* todo
        RandomAccessReader raf = new RandomAccessReader(soundData, ByteOrder.LITTLE_ENDIAN);
        
        boolean overrideParent = raf.readBoolean();
        
        byte effects = raf.readByte();
        if (effects > 0)
        {
            byte mask = raf.readByte();
            for (int i = 0; i < effects; i++)
            {
                // do i need to do some fancy stuff here?
                byte   index = raf.readByte();
                int    id    = raf.readInt();
                byte[] zero  = raf.readBytes(2);
            }
        }
        
        int     outputId         = raf.readInt();
        int     parent           = raf.readInt();
        boolean overridePlayback = raf.readBoolean();
        boolean offsetByDistance = raf.readBoolean();
        
        byte params = raf.readByte();
        for (int i = 0; i < params; i++)
        {
            byte type = raf.readByte();
        }
        
        for (int i = 0; i < params; i++)
        {
            // int or float depending on parameter type (only int if loop, so we can assume float)
            float value = raf.readFloat();
        }
        
        byte    zero        = raf.readByte();
        boolean positioning = raf.readBoolean();
        if (positioning)
        {
            byte type = raf.readByte();
            if (type == 0)
            {
                boolean panner = raf.readBoolean();
            } else
            {
                int     sourcePos   = raf.readInt();
                int     attenuation = raf.readInt();
                boolean spatial     = raf.readBoolean();
                
                if (sourcePos == 2)
                {
                    int     playType          = raf.readInt();
                    boolean loop              = raf.readBoolean();
                    int     transitionTime    = raf.readInt();
                    boolean followOrientation = raf.readBoolean();
                } else if (sourcePos == 3)
                {
                    boolean updatePerFrame = raf.readBoolean();
                }
            }
        }
        
        boolean overrideAux       = raf.readBoolean();
        boolean gameAux           = raf.readBoolean();
        boolean parentOverrideAux = raf.readBoolean();
        boolean userAuxExists     = raf.readBoolean();
        
        if (userAuxExists)
        {
            int id0 = raf.readInt();
            int id1 = raf.readInt();
            int id2 = raf.readInt();
            int id3 = raf.readInt();
        }
        
        boolean playbackLimit = raf.readBoolean();
        if (playbackLimit)
        {
            byte prio  = raf.readByte();
            byte limit = raf.readByte();
        }
        
        byte    limitPerInstance      = raf.readByte();
        byte    virtualBehavior       = raf.readByte();
        boolean overridePlaybackLimit = raf.readBoolean();
        boolean overrideVirtualVoice  = raf.readBoolean();
        
        int stateGroupCount = raf.readInt();
        for (int i = 0; i < stateGroupCount; i++)
        {
            int   id         = raf.readInt();
            byte  changeLoc  = raf.readByte();
            short stateDiffs = raf.readShort();
            for (int j = 0; j < stateDiffs; j++)
            {
                int stateId   = raf.readInt();
                int settingId = raf.readInt();
            }
        }
        
        short rtpc = raf.readShort();
        for (int i = 0; i < rtpc; i++)
        {
            int  id        = raf.readInt();
            int  yaxis     = raf.readInt();
            int  unk       = raf.readInt();
            byte unk2      = raf.readByte();
            byte unkPoints = raf.readByte();
            byte unk4      = raf.readByte();
            for (int j = 0; j < unkPoints; j++)
            {
                float xCord = raf.readFloat();
                float yCord = raf.readFloat();
                int   shape = raf.readInt();
            }
        }
        
        System.out.println();
        */
    }
}
