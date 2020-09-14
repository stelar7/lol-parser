package no.stelar7.cdragon.types.bbq;

import java.util.*;

public class BBQGlobalLoader
{
    Map<String, BBQFile>  bundles = new HashMap<>();
    Map<String, BBQAsset> assets  = new HashMap<>();
    List<String>          files   = new ArrayList<>();
    String                basePath;
    
    public BBQGlobalLoader(String basePath)
    {
        this.basePath = basePath;
    }
    
    public static BBQAsset getAsset(String filePath)
    {
        //TODO
        return null;
    }
    
    public static BBQAsset getAssetByFilename(String filePath)
    {
        //TODO
        return null;
    }
}
