package no.stelar7.cdragon.types.skn;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class SKNParser implements Parseable<SKNFile>
{
    @Override
    public SKNFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKNFile parse(byte[] data)
    {
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKNFile parse(RandomAccessReader raf)
    {
        SKNFile file = new SKNFile();
        file.setMagic(raf.readInt());
        file.setVersion(raf.readShort());
        file.setObjectCount(raf.readShort());
        
        if (file.getVersion() >= 1)
        {
            file.setMaterialCount(raf.readInt());
            file.setMaterials(parseMaterials(raf, file.getMaterialCount()));
        }
        
        if (file.getVersion() >= 4)
        {
            file.setUnknown(raf.readInt());
        }
        
        file.setIndexCount(raf.readInt());
        file.setVertexCount(raf.readInt());
        
        if (file.getVersion() >= 4)
        {
            file.setUnknown2(raf.readShorts(24));
        }
        
        if (file.getVersion() >= 1)
        {
            file.setIndecies(raf.readShorts(file.getIndexCount()));
        }
        
        file.setVertices(parseVertices(raf, file.getVertexCount()));
        
        return file;
    }
    
    private List<SKNMaterial> parseMaterials(RandomAccessReader raf, int count)
    {
        List<SKNMaterial> materials = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            SKNMaterial material = new SKNMaterial();
            material.setName(raf.readString(64).trim());
            material.setStartVertex(raf.readInt());
            material.setNumVertex(raf.readInt());
            material.setStartIndex(raf.readInt());
            material.setNumIndex(raf.readInt());
            materials.add(material);
        }
        return materials;
    }
    
    private List<SKNData> parseVertices(RandomAccessReader raf, int count)
    {
        List<SKNData> datas = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            SKNData data = new SKNData();
            
            data.setPosition(raf.readVec3F());
            data.setBoneIndecies(raf.readVec4B());
            data.setWeight(raf.readVec4F());
            data.setNormals(raf.readVec3F());
            data.setUv(raf.readVec2F());
            
            datas.add(data);
        }
        return datas;
    }
}
