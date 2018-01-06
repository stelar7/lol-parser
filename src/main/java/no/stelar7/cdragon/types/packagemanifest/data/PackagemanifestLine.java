package no.stelar7.cdragon.types.packagemanifest.data;

import lombok.Data;

@Data
public class PackagemanifestLine
{
    private String filePath;
    private String containedInFile;
    private int    containedOffset;
    private int    fileSize;
    private int    unknown;
}
