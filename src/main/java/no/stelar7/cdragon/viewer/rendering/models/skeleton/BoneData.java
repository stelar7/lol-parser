package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.anm.data.versioned.ANMDataVersion5;
import no.stelar7.cdragon.util.handlers.UtilHandler;
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
        BoneData        data     = new BoneData();
        ANMDataVersion5 fileData = file.getVersion5();
        
        data.hash = fileData.getHashes().get(frame.getBoneHash());
        data.name = skl.bones.stream().filter(bone -> bone.getHash() == data.hash).findFirst().get().getName();
        data.position = fileData.getPositions().get(frame.getPositionId());
        data.scale = fileData.getPositions().get(frame.getScaleId());
        
        Vector3s rotationData = fileData.getRotations().get(frame.getRotationId());
        String   bits         = extractBits(rotationData);
        short    flag         = Short.parseShort(bits.substring(0, 3), 2);
        short    sx           = Short.parseShort(bits.substring(3, 18), 2);
        short    sy           = Short.parseShort(bits.substring(18, 33), 2);
        short    sz           = Short.parseShort(bits.substring(33, 48), 2);
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
    
    @SuppressWarnings("SuspiciousNameCombination")
    private static String extractBits(Vector3s array)
    {
        /*
        for some reason this is incorrect...
        long xVal2 = array.getX();
        long yVal2 = array.getY();
        long zVal2 = array.getZ();
        
        long  data = zVal2 | yVal2 << 16 | xVal2 << 32;
        short flag = (short) ((data >> 45) & 3);
        short a    = (short) ((data >> 30) & 0x7FFF);
        short b    = (short) ((data >> 15) & 0x7FFF);
        short c    = (short) ((data) & 0x7FFF);
        */
        
        short xVal = array.getX();
        short yVal = array.getY();
        short zVal = array.getZ();
        
        StringBuilder xString = new StringBuilder(Long.toBinaryString(Short.toUnsignedLong(xVal)));
        UtilHandler.leftPad(xString, "0", 16);
        
        StringBuilder yString = new StringBuilder(Long.toBinaryString(Short.toUnsignedLong(yVal)));
        UtilHandler.leftPad(yString, "0", 16);
        
        StringBuilder zString = new StringBuilder(Long.toBinaryString(Short.toUnsignedLong(zVal)));
        UtilHandler.leftPad(zString, "0", 16);
        
        return zString.toString() + yString.toString() + xString.toString();
    }
}
