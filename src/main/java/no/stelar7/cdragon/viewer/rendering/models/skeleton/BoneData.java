package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.anm.data.versioned.*;
import no.stelar7.cdragon.util.types.math.Vector3s;
import org.joml.*;

import java.lang.Math;

public class BoneData
{
    int         hash;
    String      name;
    Vector3f    position;
    Quaternionf rotation;
    Vector3f    scale;
    
    public static BoneData fromFrame(ANMFile file, ANMFrame frame, Skeleton skl)
    {
        BoneData data = new BoneData();
        
        if (file.getVersion5() != null)
        {
            return data;
        }
        
        ANMDataVersion5 fileData = file.getVersion5();
        if (skl.bones.stream().noneMatch(bone -> bone.getHash() == fileData.getHashes().get(frame.getBoneHash())))
        {
            return data;
        }
        
        data.hash = fileData.getHashes().get(frame.getBoneHash());
        data.name = skl.bones.stream().filter(bone -> bone.getHash() == data.hash).findFirst().get().getName();
        data.position = fileData.getPositions().get(frame.getPositionId());
        data.scale = fileData.getPositions().get(frame.getScaleId());
        
        Vector3s rotationData = fileData.getRotations().get(frame.getRotationId());
        long     dataA        = ((long) rotationData.getZ() << 48) >>> 16;
        long     dataB        = ((long) rotationData.getY() << 48) >>> 32;
        long     dataC        = ((long) rotationData.getX() << 48) >>> 48;
        long     value        = dataA | dataB | dataC;
        
        short flag = (short) ((value >> 45) & 3);
        short sx   = (short) ((value >> 30) & 0x7FFF);
        short sy   = (short) ((value >> 15) & 0x7FFF);
        short sz   = (short) ((value) & 0x7FFF);
        data.rotation = uncompressRotation(flag, sx, sy, sz);
        
        return data;
    }
    
    private static Quaternionf uncompressRotation(short flag, short sx, short sy, short sz)
    {
        float sqrt2    = (float) Math.sqrt(2.0f);
        float invSqrt2 = (float) (1f / Math.sqrt(2.0f));
        
        float fx     = (sx / 32767f) * sqrt2 - invSqrt2;
        float fy     = (sy / 32767f) * sqrt2 - invSqrt2;
        float fz     = (sz / 32767f) * sqrt2 - invSqrt2;
        float sub    = (float) Math.max(0f, 1f - Math.pow(fx, 2) + Math.pow(fy, 2) + Math.pow(fz, 2));
        float invSub = (float) (1f / Math.sqrt(sub));
        float fw     = sub == 0 ? 0 : (((3f - sub * invSub * invSub) * invSub * .5f) * sub);
        
        switch (flag)
        {
            case 0:
                return new Quaternionf(fw, fx, fy, fz);
            case 1:
                return new Quaternionf(fx, fw, fy, fz);
            case 2:
                return new Quaternionf(fx, fy, fw, fz);
            case 3:
                return new Quaternionf(fx, fy, fz, fw);
            default:
                throw new RuntimeException("Error in unpacking quaternion");
        }
    }
}
