package no.stelar7.cdragon.types.scb;

import no.stelar7.cdragon.types.scb.data.*;
import no.stelar7.cdragon.util.reader.RandomAccessReader;

import java.nio.*;
import java.nio.file.Path;

public class SCBParser
{
    public SCBFile parse(Path path)
    {
        RandomAccessReader raf  = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        SCBFile            data = new SCBFile();
        
        readHeader(data, raf);
        readContent(data, raf);
        
        return data;
    }
    
    private void readContent(SCBFile data, RandomAccessReader raf)
    {
        SCBContent content = new SCBContent();
        
        content.setVertexCount(raf.readInt());
        content.setFaceCount(raf.readInt());
        content.setColored(raf.readInt());
        
        if (data.getHeader().getMinor() >= 2)
        {
            content.setCenter(raf.readVec3F());
            content.setPivot(raf.readVec3F());
        }
        
        for (int i = 0; i < content.getVertexCount(); i++)
        {
            content.getVertices().add(raf.readVec3F());
        }
        
        // skip once
        raf.readVec3F();
        
        for (int i = 0; i < content.getFaceCount(); i++)
        {
            content.getFaces().add(readFace(raf));
        }
        
        if (data.getHeader().getMinor() >= 2)
        {
            if (content.getColored() > 0)
            {
                int idxCount = content.getFaceCount() * 3;
                for (int i = 0; i < idxCount; i++)
                {
                    content.getColors().add(raf.readVec3B());
                }
            }
        }
        
        data.setContent(content);
    }
    
    private SCBFace readFace(RandomAccessReader raf)
    {
        SCBFace face = new SCBFace();
        
        face.setIndecies(raf.readVec3I());
        face.setMaterial(raf.readString(64).trim());
        
        face.setU(raf.readVec3F());
        face.setV(raf.readVec3F());
        
        return face;
    }
    
    private void readHeader(SCBFile data, RandomAccessReader raf)
    {
        SCBHeader header = new SCBHeader();
        header.setMagic(raf.readString(8));
        
        header.setMajor(raf.readShort());
        header.setMinor(raf.readShort());
        header.setFilename(raf.readString(128));
        
        
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.putShort(header.getMajor());
        buff.putShort(header.getMinor());
        buff.flip();
        int bver = buff.getInt();
        if (bver != 0x20002 && bver != 0x20001)
        {
            throw new RuntimeException("Unknown version: " + header.getMajor() + "." + header.getMinor());
        }
        
        data.setHeader(header);
    }
}
