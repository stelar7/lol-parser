package no.stelar7.cdragon.types.bnk.data;

/**
 * A section that is not parsed (ENVS, FXPR, STMG, INIT, PLAT, ...), kept as raw bytes
 */
public final class BNKRawSection extends BNKHeader
{
    public BNKRawSection(BNKHeader header, byte[] data)
    {
        super(header);
        this.data = data;
    }

    private final byte[] data;

    public byte[] getData()
    {
        return data;
    }
}
