package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class SKNtoObj
{
    public static void main(String[] args) throws IOException
    {
        SKNParser parser = new SKNParser();
        
        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (skn file, and output folder)");
        }
        
        Path    path = Paths.get(args[0]);
        SKNFile skn  = parser.parse(path);
        
        if (skn == null)
        {
            System.out.println("Failed to parse skn file!");
        }
        
        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(output))
        {
            System.out.println("Output is not a folder");
        }
        
        for (SKNMaterial material : skn.getMaterials())
        {
            Path outputPath = output.resolve(material.getName() + ".obj");
            Files.write(outputPath, skn.toOBJ(material).getBytes(StandardCharsets.UTF_8));
            System.out.println("Saved file: " + outputPath.toAbsolutePath().toString());
        }
    }
}
