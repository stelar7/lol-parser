package no.stelar7.cdragon.types.packagemanifest.data;

import lombok.Data;

import java.util.List;

@Data
public class PackagemanifestFile
{
    private String                    header;
    private List<PackagemanifestLine> files;
}
