package no.stelar7.cdragon.viewer;

import no.stelar7.cdragon.types.ogg.OGGParser;
import no.stelar7.cdragon.types.ogg.data.OGGStream;
import no.stelar7.cdragon.types.wem.data.WEMFile;
import no.stelar7.cdragon.types.wpk.WPKParser;
import no.stelar7.cdragon.types.wpk.data.WPKFile;
import no.stelar7.cdragon.util.readers.ParseException;

import java.io.IOException;
import java.nio.file.*;

public class WPKtoOGG
{
    public static void main(String[] args) throws IOException
    {
        WPKParser wpkParser = new WPKParser();
        OGGParser parser    = new OGGParser();

        if (args.length != 2)
        {
            System.out.println("Needs 2 parameters! (WPK file, and output folder)");
            return;
        }

        Path output = Paths.get(args[1]);
        if (!Files.isDirectory(output))
        {
            System.out.println("Output is not a folder");
            return;
        }

        Path    wpkfile = Paths.get(args[0]);
        WPKFile wpk     = wpkParser.parse(wpkfile);

        for (WEMFile wemFile : wpk.getWEMFiles())
        {
            // entries whose WEM header could not be parsed keep their raw bytes, but have no data to convert
            if (wemFile.getData() == null)
            {
                System.out.println("Skipping " + wemFile.getFilename() + ": " + wemFile.getParseError());
                continue;
            }

            try
            {
                OGGStream ogg = parser.parse(wemFile.getData());
                Files.write(output.resolve(wemFile.getFilename() + ".ogg"), ogg.getData().toByteArray());
            } catch (ParseException e)
            {
                System.out.println("Skipping " + wemFile.getFilename() + ": " + e.getMessage());
            }
        }

    }
}
