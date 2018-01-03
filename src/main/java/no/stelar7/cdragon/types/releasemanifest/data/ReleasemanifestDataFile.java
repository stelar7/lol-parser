package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

import java.util.List;

@Data
public class ReleasemanifestDataFile
{
    private ReleasemanifestHeader                header;
    private List<ReleasemanifestDataDirectory>   directories;
    private List<ReleasemanifestDataContentFile> files;
    private List<String>                         strings;
}
