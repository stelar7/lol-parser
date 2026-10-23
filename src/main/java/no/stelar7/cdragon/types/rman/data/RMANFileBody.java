package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public final class RMANFileBody
{
    private final int                         headerOffset;
    private final RMANFileBodyHeader          header;
    private final List<RMANFileBodyBundle>    bundles;
    private final List<RMANFileBodyLanguage>  languages;
    private final List<RMANFileBodyFile>      files;
    private final List<RMANFileBodyDirectory> directories;
    
    public RMANFileBody(int headerOffset, RMANFileBodyHeader header, List<RMANFileBodyBundle> bundles, List<RMANFileBodyLanguage> languages, List<RMANFileBodyFile> files, List<RMANFileBodyDirectory> directories)
    {
        this.headerOffset = headerOffset;
        this.header = header;
        this.bundles = bundles;
        this.languages = languages;
        this.files = files;
        this.directories = directories;
    }
    
    public int getHeaderOffset()
    {
        return headerOffset;
    }
    
    public RMANFileBodyHeader getHeader()
    {
        return header;
    }
    
    public List<RMANFileBodyBundle> getBundles()
    {
        return bundles;
    }
    
    public List<RMANFileBodyLanguage> getLanguages()
    {
        return languages;
    }
    
    public List<RMANFileBodyFile> getFiles()
    {
        return files;
    }
    
    public List<RMANFileBodyDirectory> getDirectories()
    {
        return directories;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RMANFileBody that = (RMANFileBody) o;
        return Objects.equals(header, that.header) &&
               Objects.equals(bundles, that.bundles) &&
               Objects.equals(languages, that.languages) &&
               Objects.equals(files, that.files) &&
               Objects.equals(directories, that.directories);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(header, bundles, languages, files, directories);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBody{" +
               "header=" + header +
               ", bundles=" + bundles +
               ", languages=" + languages +
               ", files=" + files +
               ", directories=" + directories +
               '}';
    }
}
