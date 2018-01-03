package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

@Data
public class ReleasemanifestDataDirectory
{
    private int nameIndex;
    private int subdirectoryStartIndex;
    private int subdirectoryCount;
    private int fileStartIndex;
    private int fileCount;
    
}
