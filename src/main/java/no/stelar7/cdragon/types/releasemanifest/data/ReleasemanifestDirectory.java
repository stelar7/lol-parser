package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

import java.util.*;

@Data
public class ReleasemanifestDirectory
{
    private String name;
    private List<ReleasemanifestContentFile> files          = new ArrayList<>();
    private List<ReleasemanifestDirectory>   subDirectories = new ArrayList<>();
}
