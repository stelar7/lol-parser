package no.stelar7.cdragon.types.packagemanifest.data;

import java.util.List;

public final class PackagemanifestFile
{
    private final String                    header;
    private final List<PackagemanifestLine> files;
    
    public PackagemanifestFile(String header, List<PackagemanifestLine> files)
    {
        this.header = header;
        this.files = files;
    }
    
    public String getHeader()
    {
        return header;
    }
    
    public List<PackagemanifestLine> getFiles()
    {
        return files;
    }
}
