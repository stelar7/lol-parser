package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.BinaryReader;

import java.util.*;

public class BBQAssetTypeMetadata
{
    List<Integer>             classIds  = new ArrayList<>();
    Map<Integer, BBQTypeTree> typeTrees = new HashMap<>();
    Map<Integer, byte[]>      hashes    = new HashMap<>();
    BBQAsset                  asset;
    String                    generatorVersion;
    BBQRuntimePlatform        targetPlatform;
    
    public BBQAssetTypeMetadata(BBQAsset asset)
    {
        this.asset = asset;
    }
    
    public static BBQAssetTypeMetadata fromFile(String filename)
    {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    public void load(BinaryReader buf)
    {
        int format = this.asset.format;
        this.generatorVersion = buf.readString();
        this.targetPlatform = BBQRuntimePlatform.from(buf.readInt());
        
        if (format >= 13)
        {
            boolean hasTypeTree = buf.readBoolean();
            int     typeCount   = buf.readInt();
            
            for (int i = 0; i < typeCount; i++)
            {
                int classId = buf.readInt();
                if (format >= 17)
                {
                    byte unknown  = buf.readByte();
                    int  scriptId = buf.readShort();
                    if (classId == 114)
                    {
                        if (scriptId >= 0)
                        {
                            classId = -2 - scriptId;
                        } else
                        {
                            classId = -1;
                        }
                    }
                }
                classIds.add(classId);
                
                byte[] hash;
                if (classId < 0)
                {
                    hash = buf.readBytes(0x20);
                } else
                {
                    hash = buf.readBytes(0x10);
                }
                this.hashes.put(classId, hash);
                
                if (hasTypeTree)
                {
                    BBQTypeTree tree = new BBQTypeTree(format);
                    tree.load(buf);
                    typeTrees.put(classId, tree);
                }
                
                if (format >= 21)
                {
                    buf.readBytes(4);
                }
            }
        } else
        {
            int fieldCount = buf.readInt();
            for (int i = 0; i < fieldCount; i++)
            {
                int         classId = buf.readInt();
                BBQTypeTree tree    = new BBQTypeTree(format);
                tree.load(buf);
                typeTrees.put(classId, tree);
            }
        }
    }
}
