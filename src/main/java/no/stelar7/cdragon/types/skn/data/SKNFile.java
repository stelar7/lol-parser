package no.stelar7.cdragon.types.skn.data;

import lombok.Data;
import no.stelar7.cdragon.util.types.Vector3f;

import java.util.List;

@Data
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
}
