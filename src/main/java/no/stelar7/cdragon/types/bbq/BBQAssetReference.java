package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.BinaryReader;

/**
 * An external file referenced by a serialized file
 */
public class BBQAssetReference
{
    String   assetPath;
    String   guid;
    int      type;
    String   filePath;
    BBQAsset source;

    public BBQAssetReference(BBQAsset asset, BinaryReader buf)
    {
        this.source = asset;
        load(buf);
    }

    public void load(BinaryReader buf)
    {
        int format = this.source.format;
        if (format >= 6)
        {
            this.assetPath = buf.readString();
        }

        if (format >= 5)
        {
            this.guid = HashHandler.toHex(buf.readBytes(16));
            this.type = buf.readInt();
        }

        this.filePath = buf.readString();
    }

    public String getAssetPath()
    {
        return assetPath;
    }

    public String getGuid()
    {
        return guid;
    }

    public int getType()
    {
        return type;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public BBQAsset resolve()
    {
        return this.source.getAsset(this.filePath);
    }
}
