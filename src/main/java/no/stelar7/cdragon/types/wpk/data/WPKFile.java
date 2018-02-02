package no.stelar7.cdragon.types.wpk.data;

import lombok.Data;
import no.stelar7.cdragon.types.wem.data.WEMFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Data
public class WPKFile
{
    private WPKHeader header;
    private List<Integer> offsets  = new ArrayList<>();
    private List<WEMFile> WEMFiles = new ArrayList<>();
    
    public void extractFiles(Path output)
    {
        for (WEMFile file : WEMFiles)
        {
            try
            {
                Files.write(output.resolve(file.getFilename()), file.getData().getDataBytes());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
}
