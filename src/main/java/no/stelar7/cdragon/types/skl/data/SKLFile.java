package no.stelar7.cdragon.types.skl.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.*;
import no.stelar7.cdragon.types.skl.data.versioned.data.*;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.types.Pair;
import no.stelar7.cdragon.util.types.math.*;
import org.joml.Quaternionf;

import java.util.*;

public final class SKLFile
{
    private final SKLHeader header;
    private final SKLDataV0 dataV0;
    private final SKLDataV1 dataV1;
    private final SKLDataV2 dataV2;
    
    public SKLFile(SKLHeader header, SKLDataV0 dataV0, SKLDataV1 dataV1, SKLDataV2 dataV2)
    {
        this.header = header;
        this.dataV0 = dataV0;
        this.dataV1 = dataV1;
        this.dataV2 = dataV2;
    }
    
    public List<ReadableBone> toReadableBones()
    {
        List<ReadableBone> ret = new ArrayList<>();
        
        if (dataV0 != null)
        {
            Map<Short, Pair<SKLBoneV0, ReadableBone>> tempret = new LinkedHashMap<>();
            List<SKLBoneV0>                           bones   = dataV0.getBones();
            
            for (SKLBoneV0 bone : bones)
            {
                ReadableBone newb = new ReadableBone();
                newb.setName(bone.getName());
                newb.setHash(bone.getHash() != 0 ? Integer.toUnsignedLong(bone.getHash()) : HashHandler.computeELFHash(newb.getName()));
                newb.setScale(bone.getScale());
                newb.setPosition(bone.getPosition());
                newb.setRotation(bone.getRotation());
                tempret.put(bone.getId(), new Pair<>(bone, newb));
            }
            
            for (Pair<SKLBoneV0, ReadableBone> entry : tempret.values())
            {
                Pair<SKLBoneV0, ReadableBone> parent = tempret.get(entry.getA().getParent());
                if (parent != null)
                {
                    entry.getB().setParent(parent.getB());
                }
                ret.add(entry.getB());
            }
            return ret;
        }
        
        SKLDataV1 legacy = dataV2 != null ? dataV2 : dataV1;
        if (legacy != null)
        {
            // legacy bones store global transforms, ReadableBone wants them relative to the parent
            List<SKLBoneV1> bones = legacy.getBones();
            for (SKLBoneV1 bone : bones)
            {
                Matrix4x3f local = new Matrix4x3f();
                local.set(bone.getMatrix());
                
                int parentId = bone.getParent();
                if (parentId >= 0 && parentId < bones.size())
                {
                    Matrix4x3f parentInverse = new Matrix4x3f();
                    bones.get(parentId).getMatrix().invert(parentInverse);
                    parentInverse.mul(bone.getMatrix(), local);
                }
                
                Quaternionf rotation = local.getNormalizedRotation(new Quaternionf());
                Vector4f    rot      = new Vector4f();
                rot.set(rotation.x, rotation.y, rotation.z, rotation.w);
                
                ReadableBone newb = new ReadableBone();
                newb.setName(bone.getName());
                newb.setHash(HashHandler.computeELFHash(bone.getName()));
                newb.setPosition(local.decomposePosition());
                newb.setScale(local.decomposeScale());
                newb.setRotation(rot);
                ret.add(newb);
            }
            
            for (int i = 0; i < bones.size(); i++)
            {
                int parentId = bones.get(i).getParent();
                if (parentId >= 0 && parentId < bones.size())
                {
                    ret.get(i).setParent(ret.get(parentId));
                }
            }
            return ret;
        }
        
        throw new IllegalStateException("Skeleton contains no bone data");
    }
    
    public SKLDataV0 getDataV0()
    {
        return dataV0;
    }
    
    public SKLDataV1 getDataV1()
    {
        return dataV1;
    }
    
    public SKLDataV2 getDataV2()
    {
        return dataV2;
    }
    
    public SKLHeader getHeader()
    {
        return header;
    }
}
