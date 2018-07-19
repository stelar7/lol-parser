package no.stelar7.cdragon.types.skl;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skl.data.*;
import no.stelar7.cdragon.types.skl.data.versioned.bone.*;
import no.stelar7.cdragon.types.skl.data.versioned.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

// TODO
public class SKLParser implements Parseable<SKLFile>
{
    
    @Override
    public SKLFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKLFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKLFile parse(RandomAccessReader raf)
    {
        SKLFile file = new SKLFile();
        file.setHeader(parseHeader(raf));
        
        if (file.getHeader().getVersion() == 0)
        {
            SKLDataV0 data = new SKLDataV0();
            data.setUnknown1(raf.readShort());
            data.setBoneCount(raf.readShort());
            data.setBoneIndexCount(raf.readInt());
            
            SKLDataV0Offsets offsets = new SKLDataV0Offsets();
            offsets.setBoneStart(raf.readInt());
            offsets.setAnimationStart(raf.readInt());
            offsets.setBoneIndexStart(raf.readInt());
            offsets.setBoneIndexEnd(raf.readInt());
            offsets.setHalfwayBoneString(raf.readInt());
            offsets.setBoneNameStart(raf.readInt());
            offsets.setPadding(raf.readString(20));
            data.setOffsets(offsets);
            
            for (int i = 0; i < data.getBoneCount(); i++)
            {
                SKLBoneV0 bone = new SKLBoneV0();
                bone.setUnknown1(raf.readShort());
                bone.setId(raf.readShort());
                bone.setParent(raf.readShort());
                bone.setUnknown2(raf.readShort());
                bone.setHash(raf.readInt());
                bone.setTPO(raf.readFloat());
                bone.setPosition(raf.readVec3F());
                bone.setScale(raf.readVec3F());
                bone.setRotation(raf.readVec4F());
                bone.setCt(raf.readVec3F());
                bone.setPadding(raf.readString(32));
                data.getBones().add(bone);
            }
            
            for (int i = 0; i < data.getBoneCount(); i++)
            {
                SKLBoneV0Extra extra = new SKLBoneV0Extra();
                extra.setBoneId(raf.readInt());
                extra.setBoneHash(raf.readInt());
                data.getBoneExtra().add(extra);
            }
            
            data.setBoneIndecies(raf.readShorts(data.getBoneIndexCount()));
            
            raf.seek(data.getOffsets().getBoneNameStart());
            
            for (int i = 0; i < data.getBoneCount(); i++)
            {
                StringBuilder name = new StringBuilder();
                do
                {
                    name.append(raf.readString(4));
                } while (!name.toString().contains("\u0000"));
                
                data.getBoneNames().add(name.toString());
            }
            file.setDataV0(data);
        } else if (file.getHeader().getVersion() == 1)
        {
            SKLDataV1 data = new SKLDataV1();
            data.setDesignerId(raf.readInt());
            data.setBoneCount(raf.readInt());
            
            for (int i = 0; i < data.getBoneCount(); i++)
            {
                SKLBoneV1 bone = new SKLBoneV1();
                bone.setName(raf.readString(32));
                bone.setParent(raf.readInt());
                bone.setScale(raf.readFloat());
                bone.setMatrix(raf.readMatrix4x3());
                data.getBones().add(bone);
            }
        } else if (file.getHeader().getVersion() == 2)
        {
            SKLDataV2 data = new SKLDataV2();
            data.setDesignerId(raf.readInt());
            data.setBoneCount(raf.readInt());
            
            for (int i = 0; i < data.getBoneCount(); i++)
            {
                SKLBoneV1 bone = new SKLBoneV1();
                bone.setName(raf.readString(32));
                bone.setParent(raf.readInt());
                bone.setScale(raf.readFloat());
                bone.setMatrix(raf.readMatrix4x3());
                data.getBones().add(bone);
            }
            
            data.setBoneIndexCounter(raf.readInt());
            data.setBoneIndecies(raf.readInts(data.getBoneIndexCounter()));
        }
        
        return file;
    }
    
    private SKLHeader parseHeader(RandomAccessReader raf)
    {
        SKLHeader header = new SKLHeader();
        header.setMagic(raf.readString(8));
        header.setVersion(raf.readInt());
        return header;
    }
}
