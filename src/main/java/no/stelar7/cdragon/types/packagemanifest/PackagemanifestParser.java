package no.stelar7.cdragon.types.packagemanifest;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.util.*;

/**
 * Parser for packagemanifest files: a PKG1 header line, then one line per file:
 * path,containing file,offset,size,unknown
 */
public class PackagemanifestParser implements Parseable<PackagemanifestFile>
{
    @Override
    public PackagemanifestFile parse(RandomAccessReader raf)
    {
        String[] lines  = raf.readAsString().split("\\r?\\n");
        String   header = parseHeader(lines[0]);
        return new PackagemanifestFile(header, parseFiles(lines));
    }
    
    private List<PackagemanifestLine> parseFiles(String[] data)
    {
        List<PackagemanifestLine> lines = new ArrayList<>();
        
        // the first line is the header
        for (int i = 1; i < data.length; i++)
        {
            String line = data[i].trim();
            if (line.isEmpty())
            {
                continue;
            }
            
            String[] lineContent = line.split(",");
            if (lineContent.length < 5)
            {
                throw new ParseException("Invalid packagemanifest line " + (i + 1) + ": " + line, -1);
            }
            
            try
            {
                int containedOffset = Integer.parseUnsignedInt(lineContent[2].trim());
                int fileSize        = Integer.parseUnsignedInt(lineContent[3].trim());
                int unknown         = Integer.parseInt(lineContent[4].trim());
                
                lines.add(new PackagemanifestLine(lineContent[0], lineContent[1], containedOffset, fileSize, unknown));
                
            } catch (NumberFormatException e)
            {
                throw new ParseException("Invalid number in packagemanifest line " + (i + 1) + ": " + line, -1, e);
            }
        }
        
        return lines;
    }
    
    private String parseHeader(String line)
    {
        String header = line.trim();
        if (!header.startsWith("PKG"))
        {
            throw new ParseException("Invalid packagemanifest header", 0);
        }
        return header;
    }
}
