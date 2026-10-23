package no.stelar7.cdragon.types.bnk.data;

/**
 * The parsed start of a HIRC sound (SFX/voice, CAkSound) object: the media source (AkBankSourceData) and the parent ids
 * from the node base params. The rest of the object stays in the raw data.
 * <p>
 * Layout follows wwiser (CAkBankMgr::LoadSource, CAkParameterNodeBase::SetNodeBaseParams) by bank version.
 */
public class BNKHIRCSound
{
    private int    pluginId;
    private int    streamType;
    private int    sourceId;
    private int    fileId;
    private long   fileOffset        = -1;
    private long   inMemoryMediaSize = -1;
    private int    cacheId;
    private int    sourceBits;
    private byte[] pluginParams;

    private boolean hasParentInfo;
    private int     overrideBusId;
    private int     directParentId;

    private int bankVersion;

    /**
     * The full plugin/codec id: bits 0-3 plugin type (1 = codec, 2 = source, ...), bits 4-15 company, bits 16-31 plugin id.
     * For codecs the plugin id is the codec (e.g. 0x00040001 = Vorbis, 0x00010001 = PCM, 0x00020001 = ADPCM, 0x00130001 = Opus WEM)
     */
    public int getPluginId()
    {
        return pluginId;
    }

    public int getPluginType()
    {
        return pluginId & 0x0F;
    }

    public int getPluginCompany()
    {
        return (pluginId >>> 4) & 0x3FF;
    }

    /**
     * The raw stream type (AKBKSourceType), see {@link #isStreamed()} / {@link #isPrefetched()} for its meaning
     */
    public int getStreamType()
    {
        return streamType;
    }

    /**
     * @return true if the media is streamed from a separate .wem (v89 and older: type 1, newer: type 2)
     */
    public boolean isStreamed()
    {
        return streamType == (bankVersion <= 89 ? 1 : 2);
    }

    /**
     * @return true if the start of the media is in a bank and the rest is streamed (v89 and older: type 2, newer: type 1)
     */
    public boolean isPrefetched()
    {
        return streamType == (bankVersion <= 89 ? 2 : 1);
    }

    /**
     * @return true if the whole media is in a bank (type 0)
     */
    public boolean isInMemory()
    {
        return streamType == 0;
    }

    public int getSourceId()
    {
        return sourceId;
    }

    /**
     * The id of the .wem (or of the bank holding it); since v113 this is the source id
     */
    public int getFileId()
    {
        return fileId;
    }

    /**
     * @return the offset of the media in the bank, or -1 if not stored (v112 and older, not streamed)
     */
    public long getFileOffset()
    {
        return fileOffset;
    }

    /**
     * @return the size of the in-memory (or prefetched) part of the media, or -1 if not stored
     */
    public long getInMemoryMediaSize()
    {
        return inMemoryMediaSize;
    }

    /**
     * Only stored in v151 and newer
     */
    public int getCacheId()
    {
        return cacheId;
    }

    /**
     * The raw source flags; bit 0 is language specific in every version
     */
    public int getSourceBits()
    {
        return sourceBits;
    }

    public boolean isLanguageSpecific()
    {
        return (sourceBits & 1) != 0;
    }

    /**
     * The parameters of a source plugin (e.g. a tone generator), null if there are none
     */
    public byte[] getPluginParams()
    {
        return pluginParams;
    }

    /**
     * @return true if the node base params were parsed far enough to read the bus and parent ids
     */
    public boolean hasParentInfo()
    {
        return hasParentInfo;
    }

    public int getOverrideBusId()
    {
        return overrideBusId;
    }

    public int getDirectParentId()
    {
        return directParentId;
    }

    public int getBankVersion()
    {
        return bankVersion;
    }

    public void setPluginId(int pluginId)
    {
        this.pluginId = pluginId;
    }

    public void setStreamType(int streamType)
    {
        this.streamType = streamType;
    }

    public void setSourceId(int sourceId)
    {
        this.sourceId = sourceId;
    }

    public void setFileId(int fileId)
    {
        this.fileId = fileId;
    }

    public void setFileOffset(long fileOffset)
    {
        this.fileOffset = fileOffset;
    }

    public void setInMemoryMediaSize(long inMemoryMediaSize)
    {
        this.inMemoryMediaSize = inMemoryMediaSize;
    }

    public void setCacheId(int cacheId)
    {
        this.cacheId = cacheId;
    }

    public void setSourceBits(int sourceBits)
    {
        this.sourceBits = sourceBits;
    }

    public void setPluginParams(byte[] pluginParams)
    {
        this.pluginParams = pluginParams;
    }

    public void setParentInfo(int overrideBusId, int directParentId)
    {
        this.hasParentInfo = true;
        this.overrideBusId = overrideBusId;
        this.directParentId = directParentId;
    }

    public void setBankVersion(int bankVersion)
    {
        this.bankVersion = bankVersion;
    }

    @Override
    public String toString()
    {
        return "BNKHIRCSound{" +
               "pluginId=0x" + Integer.toHexString(pluginId) +
               ", streamType=" + streamType +
               ", sourceId=" + Integer.toUnsignedString(sourceId) +
               ", fileId=" + Integer.toUnsignedString(fileId) +
               ", fileOffset=" + fileOffset +
               ", inMemoryMediaSize=" + inMemoryMediaSize +
               ", sourceBits=0x" + Integer.toHexString(sourceBits) +
               (hasParentInfo ? ", overrideBusId=" + Integer.toUnsignedString(overrideBusId) + ", directParentId=" + Integer.toUnsignedString(directParentId) : "") +
               '}';
    }
}
