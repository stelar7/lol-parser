package no.stelar7.cdragon.types.rman;

import java.util.*;

public class RMANFileBody
{
    private int                         headerOffset;
    private RMANFileBodyHeader          header;
    private List<RMANFileBodyBundle>    bundles;
    private List<RMANFileBodyLanguage>  languages;
    private List<RMANFileBodyFile>      files;
    private List<RMANFileBodyDirectory> directories;
    
    public void setHeaderOffset(int headerOffset)
    {
        this.headerOffset = headerOffset;
    }
    
    public void setHeader(RMANFileBodyHeader header)
    {
        this.header = header;
    }
    
    public void setBundles(List<RMANFileBodyBundle> bundles)
    {
        this.bundles = bundles;
    }
    
    public void setLanguages(List<RMANFileBodyLanguage> languages)
    {
        this.languages = languages;
    }
    
    public void setFiles(List<RMANFileBodyFile> files)
    {
        this.files = files;
    }
    
    public void setDirectories(List<RMANFileBodyDirectory> directories)
    {
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
