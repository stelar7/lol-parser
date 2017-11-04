package no.stelar7.cdragon.wad.data;

import lombok.Data;
import no.stelar7.cdragon.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.wad.data.header.WADHeaderBase;

import java.io.File;
import java.util.*;

@Data
public class WADFile
{
    private WADHeaderBase header;
    
    private List<WADContentHeaderV1> fileHeaders = new ArrayList<>();
    private List<File>               files       = new ArrayList<>();
}
