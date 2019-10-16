package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class BBQParser implements Parseable<BBQFile>
{
    
    @Override
    public BBQFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.BIG_ENDIAN));
    }
    
    @Override
    public BBQFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.BIG_ENDIAN));
    }
    
    public BBQFile parse(RandomAccessReader raf)
    {
        BBQFile file = new BBQFile();
        
        BBQHeader            header = parseHeader(raf);
        List<BBQBundleEntry> meta   = parseEntryList(raf, header);
        
        file.setHeader(header);
        file.setEntries(meta);
        
        return file;
    }
    
    private List<BBQBundleEntry> parseEntryList(RandomAccessReader raf, BBQHeader header)
    {
        List<BBQBundleEntry> entries = new ArrayList<>();
        
        int     headerSize = raf.pos();
        boolean metaAtEnd  = header.isMetadataAtEnd();
        
        if (metaAtEnd)
        {
            raf.seek((int) (header.getTotalFileSize() - header.getMetadataCompressedSize()));
        }
        
        byte[] data = null;
        switch (header.getCompressionMode())
        {
            case 0:
            {
                data = raf.readBytes(raf.remaining());
                break;
            }
            case 1:
            {
                data = CompressionHandler.uncompressLZMA(raf.readBytes(raf.remaining()));
                break;
            }
            case 2:
            case 3:
            {
                data = CompressionHandler.uncompressLZ4(raf.readBytes(header.getMetadataCompressedSize()), header.getMetadataUncompressedSize());
                break;
            }
        }
        
        RandomAccessReader metaReader = new RandomAccessReader(data, ByteOrder.BIG_ENDIAN);
        
        // 16 bytes unknown data
        metaReader.readBytes(16);
        
        // block info, we dont care about this yet
        int blocks = metaReader.readInt();
        for (int i = 0; i < blocks; i++)
        {
            metaReader.readInt();
            metaReader.readInt();
            metaReader.readShort();
        }
        
        
        int files = metaReader.readInt();
        for (int i = 0; i < files; i++)
        {
            BBQBundleEntry entry = parseMetaEntry(metaReader, raf);
            entries.add(entry);
        }
        
        entries.sort(Comparator.comparing(BBQBundleEntry::getOffset));
        return entries;
    }
    
    private BBQBundleEntry parseMetaEntry(RandomAccessReader meta, RandomAccessReader raf)
    {
        BBQBundleEntry entry = new BBQBundleEntry();
        entry.setOffset(meta.readLong());
        entry.setSize(meta.readLong());
        entry.setFlags(meta.readInt());
        entry.setName(meta.readString());
        return entry;
    }
    
    private BBQHeader parseHeader(RandomAccessReader raf)
    {
        BBQHeader header = new BBQHeader();
        
        header.setSignature(raf.readString());
        if (!header.getSignature().equalsIgnoreCase("UnityFS"))
        {
            throw new RuntimeException("Invalid file signature");
        }
        
        header.setVersion(raf.readInt());
        header.setPlayerVersion(raf.readString());
        header.setFsVersion(raf.readString());
        header.setTotalFileSize(raf.readLong());
        header.setMetadataCompressedSize(raf.readInt());
        header.setMetadataUncompressedSize(raf.readInt());
        header.setFlags(raf.readInt());
        
        header.setCompressionMode(header.getFlags() & 0x3f);
        header.setHasEntryInfo((header.getFlags() & 0x40) == 0x40);
        header.setMetadataAtEnd((header.getFlags() & 0x80) == 0x80);
        
        header.setHeaderSize(raf.pos() + (header.isMetadataAtEnd() ? 0 : header.getMetadataCompressedSize()));
        
        return header;
    }
}
