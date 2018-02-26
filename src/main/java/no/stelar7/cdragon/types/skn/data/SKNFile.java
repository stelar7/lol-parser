package no.stelar7.cdragon.types.skn.data;

import lombok.Data;

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
    
    private int         unknown;
    private List<Short> unknown2;
    
    private List<Short>       indecies;
    private List<SKNData>     vertices;
    private List<SKNMaterial> materials;
}
