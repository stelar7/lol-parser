package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.BinaryReader;

public class BBQAssetReference
{
    String   assetPath;
    String   guid;
    int      type;
    String   filePath;
    BBQAsset source;
    
    public BBQAssetReference(BBQAsset asset)
    {
        this.source = asset;
    }
    
    public void load(BinaryReader buf)
    {
        this.assetPath = buf.readString();
        this.guid = HashHandler.toHex(buf.readBytes(16));
        this.type = buf.readInt();
        this.filePath = buf.readString();
    }
}
