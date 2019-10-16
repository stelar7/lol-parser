package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BBQFile
{
    private BBQHeader            header;
    private List<BBQBundleEntry> entries;
    
    public BBQHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(BBQHeader header)
    {
        this.header = header;
    }
    
    public List<BBQBundleEntry> getEntries()
    {
        return entries;
    }
    
    public void setEntries(List<BBQBundleEntry> entries)
    {
        this.entries = entries;
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
        BBQFile bbqFile = (BBQFile) o;
        return Objects.equals(header, bbqFile.header) &&
               Objects.equals(entries, bbqFile.entries);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(header, entries);
    }
    
    @Override
    public String toString()
    {
        return "BBQFile{" +
               "header=" + header +
               ", entries=" + entries +
               '}';
    }
    
    public void export(Path bbqFile, BBQBundleEntry entry, Path output)
    {
        try
        {
            byte[]             data = null;
            RandomAccessReader raf  = new RandomAccessReader(bbqFile);
            raf.seek(this.header.getHeaderSize());
            
            // todo this fails with invalid data...
            if (this.header.getCompressionMode() > 0)
            {
                try (LZMACompressorInputStream is = new LZMACompressorInputStream(new ByteArrayInputStream(raf.readRemaining()));
                     ByteArrayOutputStream bos = new ByteArrayOutputStream())
                {
                    data = is.readNBytes((int) entry.getSize());
                }
            } else
            {
                raf.seek((int) entry.getOffset() + header.getHeaderSize());
                data = raf.readBytes((int) entry.getSize());
            }
            
            Files.write(output, data);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

