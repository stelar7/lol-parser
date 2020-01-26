package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class BINtoJson
{
    public static void main(String[] args) throws IOException
    {
        BINParser parser = new BINParser();
        
        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (bin file, and output file)");
            System.exit(0);
        }
        
        Path    path   = Paths.get(args[0]);
        BINFile parsed = parser.parse(path);
        
        Path output = Paths.get(args[1]);
        Files.write(output, parsed.toJson().getBytes(StandardCharsets.UTF_8));
        
        System.out.println("Saved file: " + output.toAbsolutePath().toString());
    }
}
