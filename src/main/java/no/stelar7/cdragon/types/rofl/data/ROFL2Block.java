package no.stelar7.cdragon.types.rofl.data;

/**
 * One packet ("block") inside a decompressed ROFL2 chunk.
 * <p>
 * Each block starts with a marker byte whose bits select compact encodings, followed by (little-endian):
 * <pre>
 * timestamp  0x80 set: u8 millisecond delta added to the previous timestamp, else f32 seconds
 * length     0x10 set: u8, else u32
 * packet id  0x40 set: same as the previous block, else u16
 * param      0x20 set: i8 delta added to the previous param, else u32
 * payload    length bytes
 * </pre>
 * The previous timestamp / packet id / param start at 0 for every chunk.
 * See src/rofl/block.rs in https://github.com/Toastaspiring/ROFL-X (derived from https://github.com/Mowokuma/ROFL).
 * The payloads are game packets, which are not decoded here.
 */
public class ROFL2Block
{
    public static final int MARKER_TS_RELATIVE     = 0x80;
    public static final int MARKER_REUSE_PACKET_ID = 0x40;
    public static final int MARKER_PARAM_RELATIVE  = 0x20;
    public static final int MARKER_LEN_U8          = 0x10;

    private int    offset;
    private int    marker;
    private float  timestamp;
    private int    packetId;
    private long   param;
    private byte[] payload;

    /**
     * Offset of the block in the decompressed chunk
     */
    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public int getMarker()
    {
        return marker;
    }

    public void setMarker(int marker)
    {
        this.marker = marker;
    }

    /**
     * Game time in seconds
     */
    public float getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(float timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getPacketId()
    {
        return packetId;
    }

    public void setPacketId(int packetId)
    {
        this.packetId = packetId;
    }

    /**
     * Unsigned 32 bit parameter (usually the network id of the entity the packet is about)
     */
    public long getParam()
    {
        return param;
    }

    public void setParam(long param)
    {
        this.param = param;
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    @Override
    public String toString()
    {
        return "ROFL2Block{" +
               "offset=" + offset +
               ", timestamp=" + timestamp +
               ", packetId=" + packetId +
               ", param=" + param +
               ", length=" + (payload == null ? 0 : payload.length) +
               '}';
    }
}
