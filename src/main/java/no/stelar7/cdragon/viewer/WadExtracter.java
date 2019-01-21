package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;

import java.nio.file.*;

public class WadExtracter
{
    public static void main(String[] args)
    {
        WADParser parser = new WADParser();
        
        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (wad file, and output folder)");
        }
        
        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(output))
        {
            System.out.println("Output is not a folder");
        }
        
        Path    path = Paths.get(args[0]);
        WADFile wad  = parser.parse(path);
        
        wad.extractFiles(path);
    }
}
