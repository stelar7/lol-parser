package no.stelar7.cdragon.types.skl.data;

import no.stelar7.api.l4j8.basic.utils.Pair;
import no.stelar7.cdragon.types.skl.data.versioned.bone.SKLBoneV0;
import no.stelar7.cdragon.types.skl.data.versioned.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.*;
import java.util.Map.Entry;

public class SKLFile
{
    private SKLHeader header;
    private SKLDataV0 dataV0;
    private SKLDataV1 dataV1;
    private SKLDataV2 dataV2;
    
    public List<ReadableBone> toReadableBones()
    {
        List<ReadableBone> ret = new ArrayList<>();
        
        if (dataV0 != null)
        {
            Map<Short, Pair<SKLBoneV0, ReadableBone>> tempret = new HashMap<>();
            List<SKLBoneV0>                           bones   = dataV0.getBones();
            
            for (int i = 0; i < bones.size(); i++)
            {
                SKLBoneV0 bone = bones.get(i);
                
                ReadableBone newb     = new ReadableBone();
                String       boneName = dataV0.getBoneNames().get(i);
                boneName = boneName.substring(0, boneName.indexOf("\u0000"));
                newb.setName(boneName);
                newb.setHash(bone.getHash() != 0 ? bone.getHash() : HashHandler.computeELFHash(newb.getName()));
                newb.setScale(bone.getScale());
                newb.setPosition(bone.getPosition());
                newb.setRotation(bone.getRotation());
                tempret.put(bone.getId(), new Pair<>(bone, newb));
            }
            
            for (Entry<Short, Pair<SKLBoneV0, ReadableBone>> entry : tempret.entrySet())
            {
                ReadableBone rself = entry.getValue().getValue();
                SKLBoneV0    bself = entry.getValue().getKey();
                
                Optional<Entry<Short, Pair<SKLBoneV0, ReadableBone>>> dat = tempret.entrySet().stream().filter(t -> t.getKey() == bself.getParent()).findFirst();
                if (dat.isPresent())
                {
                    ReadableBone rother = dat.get().getValue().getValue();
                    rself.setParent(rother);
                }
                
                ret.add(rself);
            }
            return ret;
        }
        
        throw new RuntimeException("Only able to transform v0 bones atm..");
        
        /*
        
        if (dataV1 != null)
        {
            List<SKLBoneV1> bones = dataV1.getBones();
            
            for (int i = 0; i < bones.size(); i++)
            {
                SKLBoneV1 bone = bones.get(i);
                
                ReadableBone newb     = new ReadableBone();
                String       boneName = dataV0.getBoneNames().get(i);
                boneName = boneName.substring(0, boneName.indexOf("\u0000"));
                newb.setName(boneName);
                newb.setHash(HashHandler.computeELFHash(newb.getName()));
                
                // TODO what?
                newb.setScale(new Vector3f(bone.getScale()));
                newb.setScale(bone.getMatrix().decomposeScale());
                
                newb.setPosition(bone.getMatrix().decomposePosition());
                newb.setRotation(bone.getMatrix().decomposeRotation());
            }
            
            return ret;
        }
        */
        
    }
    
    public SKLDataV0 getDataV0()
    {
        return dataV0;
    }
    
    public void setDataV0(SKLDataV0 dataV0)
    {
        this.dataV0 = dataV0;
    }
    
    public SKLDataV1 getDataV1()
    {
        return dataV1;
    }
    
    public void setDataV1(SKLDataV1 dataV1)
    {
        this.dataV1 = dataV1;
    }
    
    public SKLDataV2 getDataV2()
    {
        return dataV2;
    }
    
    public void setDataV2(SKLDataV2 dataV2)
    {
        this.dataV2 = dataV2;
    }
    
    public SKLHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(SKLHeader header)
    {
        this.header = header;
    }
}
