package no.stelar7.cdragon.types.mgeo.data;

import java.util.*;

/**
 * Map geometry (.mapgeo) file
 */
public class MGEOFile
{
    private MGEOFileHeader              header;
    // only versions below 7
    private boolean                     useSeparatePointLights;
    private List<MGEOTextureOverride>   shaderTextureOverrides = new ArrayList<>();
    private List<MGEOVertexDeclaration> vertexDeclarations = new ArrayList<>();
    private List<MGEOFileMesh>          meshes = new ArrayList<>();
    // the first entry of bucketGeometries
    private MGEOBucketGeometry          bucketGeometry;
    private List<MGEOBucketGeometry>    bucketGeometries = new ArrayList<>();
    private List<MGEOPlanarReflector>   planarReflectors = new ArrayList<>();
    
    public MGEOFileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(MGEOFileHeader header)
    {
        this.header = header;
    }
    
    public boolean isUseSeparatePointLights()
    {
        return useSeparatePointLights;
    }
    
    public void setUseSeparatePointLights(boolean useSeparatePointLights)
    {
        this.useSeparatePointLights = useSeparatePointLights;
    }
    
    public List<MGEOTextureOverride> getShaderTextureOverrides()
    {
        return shaderTextureOverrides;
    }
    
    public void setShaderTextureOverrides(List<MGEOTextureOverride> shaderTextureOverrides)
    {
        this.shaderTextureOverrides = shaderTextureOverrides;
    }
    
    public List<MGEOVertexDeclaration> getVertexDeclarations()
    {
        return vertexDeclarations;
    }
    
    public void setVertexDeclarations(List<MGEOVertexDeclaration> vertexDeclarations)
    {
        this.vertexDeclarations = vertexDeclarations;
    }
    
    public List<MGEOFileMesh> getMeshes()
    {
        return meshes;
    }
    
    public void setMeshes(List<MGEOFileMesh> meshes)
    {
        this.meshes = meshes;
    }
    
    public MGEOBucketGeometry getBucketGeometry()
    {
        return bucketGeometry;
    }
    
    public void setBucketGeometry(MGEOBucketGeometry bucketGeometry)
    {
        this.bucketGeometry = bucketGeometry;
    }
    
    public List<MGEOBucketGeometry> getBucketGeometries()
    {
        return bucketGeometries;
    }
    
    public void setBucketGeometries(List<MGEOBucketGeometry> bucketGeometries)
    {
        this.bucketGeometries = bucketGeometries;
    }
    
    public List<MGEOPlanarReflector> getPlanarReflectors()
    {
        return planarReflectors;
    }
    
    public void setPlanarReflectors(List<MGEOPlanarReflector> planarReflectors)
    {
        this.planarReflectors = planarReflectors;
    }
}
