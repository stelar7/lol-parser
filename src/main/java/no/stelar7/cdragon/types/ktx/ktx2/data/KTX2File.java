package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.List;

public class KTX2File
{
    KTX2FileHeader                          header;
    KTX2FileIndecies                        indecies;
    List<KTX2FileLevel>                     levels;
    List<KTX2FileDataFormatDescriptorBlock> dataFormatBlocks;
    
    public KTX2FileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(KTX2FileHeader header)
    {
        this.header = header;
    }
    
    public KTX2FileIndecies getIndecies()
    {
        return indecies;
    }
    
    public void setIndecies(KTX2FileIndecies indecies)
    {
        this.indecies = indecies;
    }
    
    public List<KTX2FileLevel> getLevels()
    {
        return levels;
    }
    
    public void setLevels(List<KTX2FileLevel> levels)
    {
        this.levels = levels;
    }
    
    public List<KTX2FileDataFormatDescriptorBlock> getDataFormatBlocks()
    {
        return dataFormatBlocks;
    }
    
    public void setDataFormatBlocks(List<KTX2FileDataFormatDescriptorBlock> dataFormatBlocks)
    {
        this.dataFormatBlocks = dataFormatBlocks;
    }
}
