package no.stelar7.cdragon.types.packagemanifest;

import lombok.Data;
import no.stelar7.cdragon.types.packagemanifest.data.PackagemanifestLine;

import java.util.List;

@Data
public class PackagemanifestFile
{
    private String                    header;
    private List<PackagemanifestLine> files;
}
