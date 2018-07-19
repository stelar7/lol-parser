package no.stelar7.cdragon.types.skn;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.skn.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

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
    public SKNFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public SKNFile parse(RandomAccessReader raf)
    {
        SKNFile file = new SKNFile();
        file.setMagic(raf.readInt());
        file.setMajor(raf.readShort());
        file.setMinor(raf.readShort());
        file.setObjectCount(raf.readInt());
        file.setMaterials((parseMaterials(raf, file.getObjectCount())));
        
        if (file.getMajor() == 4)
        {
            file.setUnknown(raf.readInt());
        }
        
        file.setIndexCount(raf.readInt());
        file.setVertexCount(raf.readInt());
        
        if (file.getMajor() == 4)
        {
            file.setVertexSize(raf.readInt());
            file.setContainsTangent(raf.readInt());
            file.setBoundingBoxMin(raf.readVec3F());
            file.setBoundingBoxMax(raf.readVec3F());
            file.setBoundingSphereLocation(raf.readVec3F());
            file.setBoundingSphereRadius(raf.readFloat());
        }
        
        file.setIndecies(raf.readShorts(file.getIndexCount()));
        file.setVertices(parseVertices(raf, file));
        
        for (int i = 0; i < file.getObjectCount(); i++)
        {
            SKNMaterial mat = file.getMaterials().get(i);
            mat.setVertices(file.getVertices().subList(mat.getStartVertex(), mat.getStartVertex() + mat.getNumVertex()));
            
            List<Integer> inds           = new ArrayList<>();
            List<Short>   sublist        = file.getIndecies().subList(mat.getStartIndex(), mat.getStartIndex() + mat.getNumIndex());
            boolean       shouldSubtract = sublist.stream().noneMatch(in -> in > mat.getStartVertex());
            for (Short sh : sublist)
            {
                inds.add(sh - (shouldSubtract ? 0 : mat.getStartVertex()));
            }
            mat.setIndecies(inds);
        }
        
        
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
    
    private List<SKNData> parseVertices(RandomAccessReader raf, SKNFile file)
    {
        List<SKNData> datas = new ArrayList<>();
        
        for (int i = 0; i < file.getVertexCount(); i++)
        {
            SKNData data = new SKNData();
            
            data.setPosition(raf.readVec3F());
            data.setBoneIndecies(raf.readVec4B());
            data.setWeight(raf.readVec4F());
            data.setNormals(raf.readVec3F());
            data.setUv(raf.readVec2F());
            if (file.containsTangent())
            {
                data.setTangent(raf.readVec4B());
            }
            
            datas.add(data);
        }
        return datas;
    }
}
