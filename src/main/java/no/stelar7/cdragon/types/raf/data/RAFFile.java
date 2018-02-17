package no.stelar7.cdragon.types.raf.data;

import lombok.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.List;

@Data
public class RAFFile
{
    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.NONE)
    private final RandomAccessReader datReader;
    
    private RAFHeader            header;
    private List<RAFContentFile> files;
    private List<RAFContentPath> paths;
    private List<String>         strings;
    private int                  pathsSize;
    private int                  pathsCount;
    private int                  fileCount;
    
    
    public RAFFile(Path pathToRaf)
    {
        this.datReader = new RandomAccessReader(pathToRaf.resolveSibling(pathToRaf.getFileName() + ".dat"), ByteOrder.BIG_ENDIAN);
    }
    
    
    public void extractFiles(Path path)
    {
        try
        {
            for (int index = 0; index < fileCount; index++)
            {
                RAFContentFile file = files.get(index);
                String         name = strings.get(file.getPathIndex());
                byte[]         data = readContentFromData(file);
                
                Path outputPath = path.resolve(name);
                Files.createDirectories(outputPath.getParent());
                
                Files.write(outputPath, data);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            datReader.close();
        }
    }
    
    public byte[] readContentFromData(RAFContentFile file)
    {
        boolean deflated = false;
        
        datReader.seek(file.getOffset());
        byte[] fileHeader = datReader.readBytes(2);
        
        if (FileTypeHandler.isProbableDEFLATE(fileHeader))
        {
            deflated = true;
        }
        
        datReader.seek(file.getOffset());
        byte[] data = datReader.readBytes(file.getSize());
        
        if (deflated)
        {
            return CompressionHandler.uncompressDEFLATE(data);
        }
        
        return data;
    }
}
