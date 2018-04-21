package no.stelar7.cdragon.types.packagemanifest.data;

import java.util.List;

public class PackagemanifestFile
{
    private String                    header;
    private List<PackagemanifestLine> files;
    
    public String getHeader()
    {
        return header;
    }
    
    public void setHeader(String header)
    {
        this.header = header;
    }
    
    public List<PackagemanifestLine> getFiles()
    {
        return files;
    }
    
    public void setFiles(List<PackagemanifestLine> files)
    {
        this.files = files;
    }
}
