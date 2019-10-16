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
        
        BBQHeader   header = parseHeader(raf);
        BBQMetadata meta   = parseMetadata(raf, header);
        
        return file;
    }
    
    private BBQMetadata parseMetadata(RandomAccessReader raf, BBQHeader header)
    {
        BBQMetadata meta = new BBQMetadata();
        
        int     headerSize = raf.pos();
        boolean metaAtEnd  = header.isMetadataAtEnd;
        
        if (metaAtEnd)
        {
            raf.seek((int) (header.totalFileSize - header.metadataCompressedSize));
        }
        
        byte[] data = null;
        switch (header.compressionMode)
        {
            case 2:
            case 3:
                data = CompressionHandler.uncompressLZ4(raf.readBytes(header.metadataCompressedSize), header.metadataUncompressedSize);
        }
        
        RandomAccessReader metaReader = new RandomAccessReader(data, ByteOrder.BIG_ENDIAN);
        metaReader.seek(0x10);
        int count = metaReader.readInt();
        
        Map<String, BBQBundleEntry> entries = new HashMap<>();
        for (int i = 0; i < count; i++)
        {
            // TODO fix this..?
            BBQBundleEntry entry = parseMetaEntry(metaReader);
            entries.put(entry.name, entry);
        }
        
        System.out.println();
        
        return meta;
    }
    
    private BBQBundleEntry parseMetaEntry(RandomAccessReader raf)
    {
        BBQBundleEntry entry = new BBQBundleEntry();
        entry.offset = raf.readLong();
        entry.size = raf.readLong();
        entry.index = raf.readInt();
        entry.nameOrigin = raf.readString();
        return entry;
    }
    
    private BBQHeader parseHeader(RandomAccessReader raf)
    {
        BBQHeader header = new BBQHeader();
        
        header.signature = raf.readString();
        if (!header.signature.equalsIgnoreCase("UnityFS"))
        {
            throw new RuntimeException("Invalid file signature");
        }
        
        header.version = raf.readInt();
        header.playerVersion = raf.readString();
        header.fsVersion = raf.readString();
        header.totalFileSize = raf.readLong();
        header.metadataCompressedSize = raf.readInt();
        header.metadataUncompressedSize = raf.readInt();
        header.flags = raf.readInt();
        
        header.compressionMode = header.flags & 0x3f;
        header.hasEntryInfo = (header.flags & 0x40) == 0x40;
        header.isMetadataAtEnd = (header.flags & 0x80) == 0x80;
        
        return header;
    }
}
