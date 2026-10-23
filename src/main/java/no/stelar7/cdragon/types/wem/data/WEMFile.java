package no.stelar7.cdragon.types.wem.data;

public final class WEMFile
{
    private final int     dataOffset;
    private final int     dataLength;
    private final String  filename;
    private final WEMData data;

    // the raw bytes are also available through data.getDataBytes() when the header parsed, so don't export them twice
    private final transient byte[] rawData;
    private final           String parseError;

    /**
     * @param filename   the name from the container, or null
     * @param data       the parsed wem, or null if it could not be parsed
     * @param parseError why the wem could not be parsed, or null
     */
    public WEMFile(int dataOffset, int dataLength, String filename, WEMData data, byte[] rawData, String parseError)
    {
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
        this.filename = filename;
        this.data = data;
        this.rawData = rawData;
        this.parseError = parseError;
    }

    public int getDataOffset()
    {
        return dataOffset;
    }

    public int getDataLength()
    {
        return dataLength;
    }

    public String getFilename()
    {
        return filename;
    }

    /**
     * The parsed header, or null if it could not be parsed (see getParseError, the bytes are still available from getRawData)
     */
    public WEMData getData()
    {
        return data;
    }

    /**
     * The raw bytes of this wem, available even when the header could not be parsed
     */
    public byte[] getRawData()
    {
        if (rawData == null && data != null)
        {
            return data.getDataBytes();
        }
        return rawData;
    }

    /**
     * Why the header could not be parsed, or null if it was parsed
     */
    public String getParseError()
    {
        return parseError;
    }

    @Override
    public String toString()
    {
        return "WEMFile{" +
               "filename='" + filename + '\'' +
               ", data=" + data +
               (parseError != null ? ", parseError='" + parseError + '\'' : "") +
               '}';
    }
}
