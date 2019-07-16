package no.stelar7.cdragon.types.ktx.ktx.data;

import java.util.*;

public class KTX11FileMipArray
{
    List<KTX11FileMipFace> faces;
    
    public List<KTX11FileMipFace> getFaces()
    {
        return faces;
    }
    
    public void setFaces(List<KTX11FileMipFace> faces)
    {
        this.faces = faces;
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
        KTX11FileMipArray that = (KTX11FileMipArray) o;
        return Objects.equals(faces, that.faces);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(faces);
    }
    
    @Override
    public String toString()
    {
        return "KTX11FileMipArray{" +
               "faces=" + faces +
               '}';
    }
}
