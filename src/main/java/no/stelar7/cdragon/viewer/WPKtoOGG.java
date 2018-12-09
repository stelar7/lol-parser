package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.WEMParser;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.IOException;
import java.nio.file.*;

public class WPKtoOGG
{
    public static void main(String[] args) throws IOException
    {
        WPKParser wpkParser = new WPKParser();
        OGGParser parser    = new OGGParser();
        WEMParser wemparser = new WEMParser();
        
        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (WPK file, and output folder)");
        }
        
        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(output))
        {
            System.out.println("Output is not a folder");
        }
        
        Path    wpkfile = Paths.get(args[0]);
        WPKFile wpk     = wpkParser.parse(wpkfile);
        
        for (WEMFile wemFile : wpk.getWEMFiles())
        {
            WEMFile   wem = wemparser.parse(new ByteArray(wemFile.getData().getDataBytes()));
            OGGStream ogg = parser.parse(wem.getData());
            Files.write(wpkfile.resolveSibling(wemFile.getFilename() + ".ogg"), ogg.getData().toByteArray());
        }
        
    }
}
