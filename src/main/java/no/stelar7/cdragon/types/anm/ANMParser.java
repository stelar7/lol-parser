package no.stelar7.cdragon.types.anm;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.anm.data.versioned.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class ANMParser implements Parseable<ANMFile>
{
    @Override
    public ANMFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public ANMFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public ANMFile parse(RandomAccessReader raf)
    {
        ANMFile anmFile = new ANMFile();
        anmFile.setHeader(parseHeader(raf));
        
        switch (anmFile.getHeader().getVersion())
        {
            case 1:
                anmFile.setVersion1(parseVersion1(raf));
                break;
            case 3:
                anmFile.setVersion3(parseVersion3(raf));
                break;
            case 4:
                anmFile.setVersion4(parseVersion4(raf));
                break;
            case 5:
                anmFile.setVersion5(parseVersion5(raf));
                break;
            default:
                throw new RuntimeException("Invalid ANM version");
        }
        
        return anmFile;
    }
    
    private ANMDataVersion5 parseVersion5(RandomAccessReader raf)
    {
        ANMDataVersion5 ver = new ANMDataVersion5();
        
        ver.setFileSize(raf.readInt());
        
        ver.setUnknown1(raf.readInt());
        ver.setUnknown2(raf.readInt());
        ver.setUnknown3(raf.readInt());
        
        ver.setBoneCount(raf.readInt());
        ver.setFrameCount(raf.readInt());
        ver.setFrameDelay(raf.readFloat());
        ver.setHashOffset(raf.readInt());
        
        ver.setUnknown4(raf.readInt());
        ver.setUnknown5(raf.readInt());
        
        ver.setPositionOffset(raf.readInt());
        ver.setRotationOffset(raf.readInt());
        ver.setFrameOffset(raf.readInt());
        
        ver.setUnknown6(raf.readInt());
        ver.setUnknown7(raf.readInt());
        ver.setUnknown8(raf.readInt());
        
        for (int i = 0; i < (ver.getRotationOffset() - ver.getPositionOffset()) / 12; i++)
        {
            ver.getPositions().add(raf.readVec3F());
        }
        
        for (int i = 0; i < (ver.getHashOffset() - ver.getRotationOffset()) / 6; i++)
        {
            ver.getRotations().add(raf.readVec3S());
        }
        
        for (int i = 0; i < (ver.getFrameOffset() - ver.getHashOffset()) / 4; i++)
        {
            ver.getHashes().add(raf.readInt());
        }
        
        for (int i = 0; i < ver.getFrameCount(); i++)
        {
            List<ANMFrame> frames = new ArrayList<>();
            for (int j = 0; j < ver.getBoneCount(); j++)
            {
                ANMFrame frame = new ANMFrame();
                
                frame.setBoneHash(j);
                frame.setPositionId(raf.readShort());
                frame.setScaleId(raf.readShort());
                frame.setRotationId(raf.readShort());
                
                frames.add(frame);
            }
            ver.getFrames().put(i, frames);
        }
        
        
        return ver;
    }
    
    private ANMDataVersion4 parseVersion4(RandomAccessReader raf)
    {
        ANMDataVersion4 ver = new ANMDataVersion4();
        
        ver.setDataSize(raf.readInt());
        ver.setDesignerId(raf.readInt());
        
        ver.setUnknown1(raf.readInt());
        ver.setUnknown2(raf.readInt());
        
        ver.setBoneCount(raf.readInt());
        ver.setFrameCount(raf.readInt());
        ver.setFPS(raf.readInt());
        
        ver.setUnknown3(raf.readInt());
        ver.setUnknown4(raf.readInt());
        ver.setUnknown5(raf.readInt());
        
        ver.setPositionOffset(raf.readInt());
        ver.setRotationOffset(raf.readInt());
        ver.setFrameOffset(raf.readInt());
        
        ver.setUnknown6(raf.readInt());
        ver.setUnknown7(raf.readInt());
        ver.setUnknown8(raf.readInt());
        
        for (int i = 0; i < (ver.getRotationOffset() - ver.getPositionOffset()) / 12; i++)
        {
            ver.getPositions().add(raf.readVec3F());
        }
        
        for (int i = 0; i < (ver.getFrameOffset() - ver.getRotationOffset()) / 16; i++)
        {
            ver.getRotations().add(raf.readQuaternion());
        }
        
        for (int i = 0; i < ver.getFrameCount(); i++)
        {
            List<ANMFrame> frames = new ArrayList<>();
            for (int j = 0; j < ver.getBoneCount(); j++)
            {
                ANMFrame frame = new ANMFrame();
                
                frame.setBoneHash(raf.readInt());
                frame.setPositionId(raf.readShort());
                frame.setScaleId(raf.readShort());
                frame.setRotationId(raf.readShort());
                frame.setUnknown(raf.readShort());
                
                frames.add(frame);
            }
            ver.getFrames().put(i, frames);
        }
        
        return ver;
    }
    
    private ANMDataVersion3 parseVersion3(RandomAccessReader raf)
    {
        ANMDataVersion3 ver = new ANMDataVersion3();
        
        ver.setDesignerId(raf.readInt());
        ver.setBoneCount(raf.readInt());
        ver.setFrameCount(raf.readInt());
        ver.setFps(raf.readInt());
        
        for (int i = 0; i < ver.getBoneCount(); i++)
        {
            ANMBone bone = new ANMBone();
            
            bone.setName(raf.readString(32));
            bone.setFlag(raf.readInt());
            
            for (int j = 0; j < ver.getFrameCount(); j++)
            {
                ANMBoneFrame frame = new ANMBoneFrame();
                
                frame.setRotation(raf.readQuaternion());
                frame.setTranslation(raf.readVec3F());
                
                bone.getFrames().add(frame);
            }
            
            ver.getBones().add(bone);
        }
        
        return ver;
    }
    
    private ANMDataVersion1 parseVersion1(RandomAccessReader raf)
    {
        ANMDataVersion1 ver = new ANMDataVersion1();
        ver.setDataSize(raf.readInt());
        ver.setSubMagic(raf.readString(4));
        ver.setSubVersion(raf.readInt());
        ver.setBoneCount(raf.readInt());
        ver.setEntryCount(raf.readInt());
        ver.setUnknown1(raf.readInt());
        ver.setAnimationLength(raf.readFloat());
        ver.setFPS(raf.readFloat());
        
        ver.setUnknown2(raf.readInt());
        ver.setUnknown3(raf.readInt());
        ver.setUnknown4(raf.readInt());
        ver.setUnknown5(raf.readInt());
        ver.setUnknown6(raf.readInt());
        ver.setUnknown7(raf.readInt());
        
        ver.setMinTranslation(raf.readVec3F());
        ver.setMaxTranslation(raf.readVec3F());
        ver.setMinScale(raf.readVec3F());
        ver.setMaxScale(raf.readVec3F());
        
        ver.setEntryOffset(raf.readInt());
        ver.setIndexOffset(raf.readInt());
        ver.setHashOffset(raf.readInt());
        
        for (int i = 0; i < ver.getEntryCount(); i++)
        {
            ANMEntry entry = new ANMEntry();
            
            entry.setCompressedTime(raf.readShort());
            entry.setHashId(raf.readByte());
            entry.setDataType(raf.readByte());
            entry.setCompressedData(raf.readVec3S());
            
            ver.getEntries().add(entry);
        }
        
        int indexCounter = (ver.getHashOffset() - ver.getIndexOffset()) / 2;
        for (int i = 0; i < indexCounter; i++)
        {
            ver.getIndecies().add(raf.readShort());
        }
        
        for (int i = 0; i < ver.getBoneCount(); i++)
        {
            ver.getBoneHashes().add(raf.readInt());
        }
        
        return ver;
    }
    
    private ANMHeader parseHeader(RandomAccessReader raf)
    {
        ANMHeader header = new ANMHeader();
        header.setMagic(raf.readString(8));
        header.setVersion(raf.readInt());
        return header;
    }
}
