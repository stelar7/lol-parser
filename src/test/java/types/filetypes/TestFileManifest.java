package types.filetypes;

import no.stelar7.cdragon.types.filemanifest.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class TestFileManifest
{
    ManifestContentParser parser = new ManifestContentParser();
    
    @Test
    public void testV0()
    {
        ManifestContentFileV0 manifestContentFileV0 = parser.parseV0(Paths.get("C:\\Users\\Steffen\\Downloads\\e0bded2423b63302"));
        System.out.println();
    }
    
    @Test
    public void testV1()
    {
        ManifestContentFileV1 manifestContentFileV1 = parser.parseV1(Paths.get("C:\\Users\\Steffen\\Downloads\\e0bded2423b63302"));
        System.out.println();
    }
    
    @Test
    public void testV2()
    {
        ManifestContentFileV2 manifestContentFileV2 = parser.parseV2(Paths.get("C:\\Users\\Steffen\\Downloads\\championskins.info"));
        System.out.println();
    }
    
    @Test
    public void testAtlas()
    {
        ManifestContentFileAtlas manifestContentFileAtlas = parser.parseAtlas(Paths.get("C:\\Users\\Steffen\\Downloads\\e45fdbbf6786542c"));
        System.out.println();
    }
}
