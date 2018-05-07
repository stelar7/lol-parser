package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.*;

import java.text.*;
import java.util.*;

public class SKNFile
{
    private int   magic;
    private short version;
    private short objectCount;
    private int   indexCount;
    private int   vertexCount;
    private int   materialCount;
    
    private int    unknown;
    private byte[] unknown2;
    
    private int      containsTangent;
    private Vector3f boundingBoxMin;
    private Vector3f boundingBoxMax;
    private Vector3f boundingSphereLocation;
    private float    boundingSphereRadius;
    
    private List<Short>       indecies;
    private List<SKNData>     vertices;
    private List<SKNMaterial> materials;
    
    public int getMagic()
    {
        return magic;
    }
    
    public void setMagic(int magic)
    {
        this.magic = magic;
    }
    
    public short getVersion()
    {
        return version;
    }
    
    public void setVersion(short version)
    {
        this.version = version;
    }
    
    public short getObjectCount()
    {
        return objectCount;
    }
    
    public void setObjectCount(short objectCount)
    {
        this.objectCount = objectCount;
    }
    
    public int getIndexCount()
    {
        return indexCount;
    }
    
    public void setIndexCount(int indexCount)
    {
        this.indexCount = indexCount;
    }
    
    public int getVertexCount()
    {
        return vertexCount;
    }
    
    public void setVertexCount(int vertexCount)
    {
        this.vertexCount = vertexCount;
    }
    
    public int getMaterialCount()
    {
        return materialCount;
    }
    
    public void setMaterialCount(int materialCount)
    {
        this.materialCount = materialCount;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(int unknown)
    {
        this.unknown = unknown;
    }
    
    public byte[] getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(byte[] unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public int getContainsTangent()
    {
        return containsTangent;
    }
    
    public void setContainsTangent(int containsTangent)
    {
        this.containsTangent = containsTangent;
    }
    
    public Vector3f getBoundingBoxMin()
    {
        return boundingBoxMin;
    }
    
    public void setBoundingBoxMin(Vector3f boundingBoxMin)
    {
        this.boundingBoxMin = boundingBoxMin;
    }
    
    public Vector3f getBoundingBoxMax()
    {
        return boundingBoxMax;
    }
    
    public void setBoundingBoxMax(Vector3f boundingBoxMax)
    {
        this.boundingBoxMax = boundingBoxMax;
    }
    
    public Vector3f getBoundingSphereLocation()
    {
        return boundingSphereLocation;
    }
    
    public void setBoundingSphereLocation(Vector3f boundingSphereLocation)
    {
        this.boundingSphereLocation = boundingSphereLocation;
    }
    
    public float getBoundingSphereRadius()
    {
        return boundingSphereRadius;
    }
    
    public void setBoundingSphereRadius(float boundingSphereRadius)
    {
        this.boundingSphereRadius = boundingSphereRadius;
    }
    
    public List<Short> getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(List<Short> indecies)
    {
        this.indecies = indecies;
    }
    
    public List<SKNData> getVertices()
    {
        return vertices;
    }
    
    public void setVertices(List<SKNData> vertices)
    {
        this.vertices = vertices;
    }
    
    public List<SKNMaterial> getMaterials()
    {
        return materials;
    }
    
    public void setMaterials(List<SKNMaterial> materials)
    {
        this.materials = materials;
    }
    
    public List<Vector3f> getScaledVertices()
    {
        // scale to 0-1 then load
        float minx = Integer.MAX_VALUE;
        float maxx = Integer.MIN_VALUE;
        float miny = Integer.MAX_VALUE;
        float maxy = Integer.MIN_VALUE;
        float minz = Integer.MAX_VALUE;
        float maxz = Integer.MIN_VALUE;
        
        // scale factor
        for (int i = 0; i < getVertexCount(); i++)
        {
            SKNData  v   = getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            maxx = Math.max(maxx, pos.x);
            minx = Math.min(minx, pos.x);
            maxy = Math.max(maxy, pos.y);
            miny = Math.min(miny, pos.y);
            maxz = Math.max(maxz, pos.z);
            minz = Math.min(minz, pos.z);
        }
        
        // load with scaling
        List<Vector3f> verts = new ArrayList<>();
        for (int i = 0; i < getVertexCount(); i++)
        {
            SKNData  v   = getVertices().get(i);
            Vector3f pos = v.getPosition();
            
            Vector3f vert = new Vector3f();
            vert.x = UtilHandler.scale(pos.x, minx, maxx, -1, 1);
            vert.y = UtilHandler.scale(pos.y, miny, maxy, -1, 1);
            vert.z = UtilHandler.scale(pos.z, minz, maxz, -1, 1);
            verts.add(vert);
        }
        
        return verts;
    }
    
    public List<Integer> getIndeciesAsIntegerList()
    {
        List<Integer> inds = new ArrayList<>();
        for (int i = 0; i < getIndecies().size(); i++)
        {
            Short in = getIndecies().get(i);
            inds.add(Integer.valueOf(in));
        }
        return inds;
    }
    
    public String toOBJ()
    {
        StringBuilder        sb           = new StringBuilder();
        List<Vector3f>       scaledVerts  = getScaledVertices();
        List<Integer>        indeciesList = getIndeciesAsIntegerList();
        DecimalFormatSymbols dfs          = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat        df           = new DecimalFormat("###.#######", dfs);
        
        for (Vector3f pos : scaledVerts)
        {
            sb.append(String.format("v %s %s %s%n", df.format(pos.x), df.format(pos.y), df.format(pos.z)));
        }
        
        for (SKNData vertex : vertices)
        {
            Vector2f pos = vertex.getUv();
            sb.append(String.format("vt %s %s%n", df.format(pos.x), df.format(pos.y)));
        }
        
        for (SKNData vertex : vertices)
        {
            Vector3f pos = vertex.getNormals();
            sb.append(String.format("vn %s %s %s%n", df.format(pos.x), df.format(pos.y), df.format(pos.z)));
        }
        
        
        for (int i = 0; i < indeciesList.size(); i = i + 3)
        {
            int a = indeciesList.get(i);
            int b = indeciesList.get(i + 1);
            int c = indeciesList.get(i + 2);
            
            sb.append(String.format("f %1$s/%1$s/%1$s %2$s/%2$s/%2$s %3$s/%3$s/%3$s%n", a, b, c));
        }
        
        return sb.toString();
    }
}
