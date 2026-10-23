package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.ParseException;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BBQFile
{
    private BBQHeader      header;
    private List<BBQAsset> entries;

    public BBQHeader getHeader()
    {
        return header;
    }

    public void setHeader(BBQHeader header)
    {
        this.header = header;
    }

    public List<BBQAsset> getEntries()
    {
        return entries;
    }

    public void setEntries(List<BBQAsset> entries)
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

    /**
     * Writes the (decompressed) bytes of one file in the bundle
     */
    public void export(BBQAsset asset, Path output)
    {
        try
        {
            Files.write(output, asset.readAllBytes());
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes the (decompressed) bytes of the file with the same name as entry.
     * bbqFile is not used anymore, the data is read from the bundle this was parsed from.
     */
    public void export(Path bbqFile, BBQBundleEntry entry, Path output)
    {
        BBQAsset asset = entries.stream()
                                .filter(e -> e.getName().equals(entry.getName()))
                                .findFirst()
                                .orElseThrow(() -> new ParseException("No file named " + entry.getName() + " in the bundle", -1));
        export(asset, output);
    }
}
