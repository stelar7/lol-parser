package no.stelar7.cdragon.types.bnk;

import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

public class BNKHIRCObject
{
    private BNKHIRCType type = BNKHIRCType.UNKNOWN;
    private int         rawType;
    private int         bankVersion = Integer.MAX_VALUE;
    private int         length;
    private int         id;
    private byte[]      data;
    private String      parseError;

    private Map<BNKHIRCSettingType, Float> settings;
    private BNKHIRCSound                   sound;

    public BNKHIRCType getType()
    {
        return type;
    }

    /**
     * Sets the type, as it is stored in a bank of the given version (the ids of the later types depend on the version)
     */
    public void setType(int val, int bankVersion)
    {
        this.rawType = val;
        this.bankVersion = bankVersion;
        this.type = BNKHIRCType.fromValue(val, bankVersion);
    }

    /**
     * The type as stored in the file, also for UNKNOWN types
     */
    public int getRawType()
    {
        return rawType;
    }

    public int getBankVersion()
    {
        return bankVersion;
    }

    /**
     * The values of a SETTING object, null for other types
     */
    public Map<BNKHIRCSettingType, Float> getSettings()
    {
        return settings;
    }

    /**
     * The media source and parent ids of a SFX_VOICE object, null for other types or if it couldn't be parsed
     */
    public BNKHIRCSound getSound()
    {
        return sound;
    }

    /**
     * Why the object internals couldn't be parsed, or null. The raw data is kept either way.
     */
    public String getParseError()
    {
        return parseError;
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

    /**
     * The object data after the id
     */
    public byte[] getData()
    {
        return data;
    }

    public void setData(byte[] data)
    {
        this.data = data;
    }

    /**
     * Parses the internals of known object types from the data, using the bank version given with the type.
     * Never reads outside the data; on failure getParseError() is set.
     */
    public void parse()
    {
        RandomAccessReader raf = new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN);
        try
        {
            switch (type)
            {
                case SETTING -> parseSetting(raf);
                case SFX_VOICE -> sound = parseSound(raf, bankVersion);
                default ->
                {
                    // kept as raw data
                }
            }
        } catch (ParseException | IndexOutOfBoundsException e)
        {
            parseError = e.getMessage();
        }
    }

    private void parseSetting(RandomAccessReader raf)
    {
        Map<BNKHIRCSettingType, Float> settingsMap = new LinkedHashMap<>();

        int   settingCount = raf.readU8();
        int[] types        = new int[settingCount];
        need(raf, settingCount * 5L);
        for (int i = 0; i < settingCount; i++)
        {
            types[i] = raf.readU8();
        }
        for (int i = 0; i < settingCount; i++)
        {
            settingsMap.put(BNKHIRCSettingType.fromValue(types[i]), raf.readFloat());
        }

        this.settings = settingsMap;
    }

    private static void need(RandomAccessReader raf, long bytes)
    {
        if (bytes < 0 || bytes > raf.remaining())
        {
            throw new ParseException("Object truncated, need " + bytes + " bytes, " + raf.remaining() + " left", raf);
        }
    }

    private static int u8(RandomAccessReader raf)
    {
        need(raf, 1);
        return raf.readU8();
    }

    private static int u32(RandomAccessReader raf)
    {
        need(raf, 4);
        return raf.readInt();
    }

    private static byte[] bytes(RandomAccessReader raf, long count)
    {
        need(raf, count);
        return raf.readBytes(count);
    }

    /**
     * CAkSound::SetInitialValues -> CAkBankMgr::LoadSource + the start of CAkParameterNodeBase::SetNodeBaseParams (wwiser)
     */
    static BNKHIRCSound parseSound(RandomAccessReader raf, int version)
    {
        if (version <= 26)
        {
            throw new ParseException("Sound objects of bank version " + version + " are not supported", raf);
        }

        BNKHIRCSound sound = new BNKHIRCSound();
        sound.setBankVersion(version);

        // AkBankSourceData
        int pluginId = u32(raf);
        sound.setPluginId(pluginId);

        int streamType = version <= 89 ? u32(raf) : u8(raf);
        sound.setStreamType(streamType);

        if (version <= 46)
        {
            // AkAudioFormat: sample rate, format bits
            bytes(raf, 8);
        }

        // AkMediaInformation
        int sourceId = u32(raf);
        sound.setSourceId(sourceId);

        if (version <= 88)
        {
            sound.setFileId(u32(raf));
            if (streamType != 1)
            {
                sound.setFileOffset(Integer.toUnsignedLong(u32(raf)));
                sound.setInMemoryMediaSize(Integer.toUnsignedLong(u32(raf)));
            }
        } else if (version <= 89)
        {
            sound.setFileId(u32(raf));
            if (streamType != 1)
            {
                sound.setFileOffset(Integer.toUnsignedLong(u32(raf)));
            }
            sound.setInMemoryMediaSize(Integer.toUnsignedLong(u32(raf)));
        } else if (version <= 112)
        {
            sound.setFileId(u32(raf));
            if (streamType != 2)
            {
                sound.setFileOffset(Integer.toUnsignedLong(u32(raf)));
            }
            sound.setInMemoryMediaSize(Integer.toUnsignedLong(u32(raf)));
        } else if (version <= 150)
        {
            // the file id is the source id
            sound.setFileId(sourceId);
            sound.setInMemoryMediaSize(Integer.toUnsignedLong(u32(raf)));
        } else
        {
            sound.setFileId(sourceId);
            sound.setCacheId(u32(raf));
            sound.setInMemoryMediaSize(Integer.toUnsignedLong(u32(raf)));
        }

        sound.setSourceBits(u8(raf));

        // plugin params (wwiser parse_plugin_params): only for source plugins (and, before v127, type 5)
        int     pluginType = pluginId & 0x0F;
        boolean hasParam   = pluginType == 2 || (version <= 126 && pluginType == 5);
        if (hasParam && pluginId != 0)
        {
            long size = Integer.toUnsignedLong(u32(raf));
            if (size > 0)
            {
                sound.setPluginParams(bytes(raf, size));
            }
        }

        // NodeBaseParams, up to the bus and parent ids
        readInitialFxParams(raf, version);

        if (version > 136)
        {
            readInitialMetadataParams(raf);
        }

        if (version > 89 && version <= 145)
        {
            // bOverrideAttachmentParams
            u8(raf);
        }

        int overrideBusId  = u32(raf);
        int directParentId = u32(raf);
        sound.setParentInfo(overrideBusId, directParentId);

        return sound;
    }

    // CAkParameterNode::SetInitialFxParams
    private static void readInitialFxParams(RandomAccessReader raf, int version)
    {
        // bIsOverrideParentFX
        u8(raf);
        int count = u8(raf);
        if (count == 0)
        {
            return;
        }

        // bitsFXBypass / bBypassAll
        u8(raf);
        for (int i = 0; i < count; i++)
        {
            // uFXIndex, fxID
            u8(raf);
            u32(raf);

            if (version <= 48)
            {
                // bIsRendered, then the plugin preset
                u8(raf);
                long presetSize = Integer.toUnsignedLong(u32(raf));
                bytes(raf, presetSize);

                if (version > 46)
                {
                    long bankDataCount = Integer.toUnsignedLong(u32(raf));
                    bytes(raf, bankDataCount * 8);
                }
            } else if (version <= 145)
            {
                // bIsShareSet, bIsRendered
                u8(raf);
                u8(raf);
            } else
            {
                // bitVector: bypass, shareset, rendered
                u8(raf);
            }
        }
    }

    // CAkParameterNode::SetInitialMetadataParams
    private static void readInitialMetadataParams(RandomAccessReader raf)
    {
        // bIsOverrideParentMetadata
        u8(raf);
        int count = u8(raf);
        // uFXIndex, fxID, bIsShareSet
        bytes(raf, count * 6L);
    }
}
