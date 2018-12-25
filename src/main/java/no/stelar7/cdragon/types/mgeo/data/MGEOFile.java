package no.stelar7.cdragon.types.mgeo.data;

import java.util.*;

public class MGEOFile
{
    private MGEOFileHeader     header;
    private List<MGEOFileMesh> meshes;
    private MGEOBucketGeometry bucketGeometry;
    
    public MGEOFileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(MGEOFileHeader header)
    {
        this.header = header;
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
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        MGEOFile mgeoFile = (MGEOFile) o;
        return Objects.equals(header, mgeoFile.header) &&
               Objects.equals(meshes, mgeoFile.meshes) &&
               Objects.equals(bucketGeometry, mgeoFile.bucketGeometry);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(header, meshes, bucketGeometry);
    }
    
    @Override
    public String toString()
    {
        return "MGEOFile{" +
               "header=" + header +
               ", meshes=" + meshes +
               ", bucketGeometry=" + bucketGeometry +
               '}';
    }
}
