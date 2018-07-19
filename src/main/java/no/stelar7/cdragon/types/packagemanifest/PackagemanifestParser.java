package no.stelar7.cdragon.types.packagemanifest;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class PackagemanifestParser implements Parseable<PackagemanifestFile>
{
    @Override
    public PackagemanifestFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public PackagemanifestFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public PackagemanifestFile parse(RandomAccessReader raf)
    {
        PackagemanifestFile file = new PackagemanifestFile();
        
        file.setHeader(parseHeader(raf));
        file.setFiles(parseFiles(raf));
        
        return file;
    }
    
    private List<PackagemanifestLine> parseFiles(RandomAccessReader raf)
    {
        List<PackagemanifestLine> lines = new ArrayList<>();
        
        String data = raf.readAsString();
        for (String line : data.split("\n"))
        {
            if(line.trim().isEmpty()) {
                continue;
            }
            
            String[] lineContent = line.split(",");
            
            PackagemanifestLine dataLine = new PackagemanifestLine();
            dataLine.setFilePath(lineContent[0]);
            dataLine.setContainedInFile(lineContent[1]);
            dataLine.setContainedOffset(Integer.parseInt(lineContent[2]));
            dataLine.setFileSize(Integer.parseInt(lineContent[2]));
            dataLine.setUnknown(Integer.parseInt(lineContent[3]));
            
            lines.add(dataLine);
        }
        
        return lines;
    }
    
    private String parseHeader(RandomAccessReader raf)
    {
        String header = new String(raf.readBytes(4), StandardCharsets.UTF_8);
        if (!header.startsWith("PKG"))
        {
            throw new IllegalArgumentException("Invalid header");
        }
        return header;
    }
}
