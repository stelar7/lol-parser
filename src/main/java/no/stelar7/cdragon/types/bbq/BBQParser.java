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
        return parse(new RandomAccessReader(data.getDataRaw(), ByteOrder.BIG_ENDIAN));
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
        int     headerSize = raf.pos();
        boolean metaAtEnd  = header.isMetadataAtEnd();
        
        if (metaAtEnd)
        {
            raf.seek((int) (header.getTotalFileSize() - header.getMetadataCompressedSize()));
        }
        
        byte[] data = null;
        switch (header.getCompressionMode())
        {
            case NONE:
            {
                data = raf.readBytes(raf.remaining());
                break;
            }
            case LZMA:
            {
                data = CompressionHandler.uncompressLZMA(raf.readBytes(raf.remaining()));
                break;
            }
            case LZ4:
            case LZ4HC:
            case LZHAM:
            {
                data = CompressionHandler.uncompressLZ4(raf.readBytes(header.getMetadataCompressedSize()), header.getMetadataUncompressedSize());
                break;
            }
        }
        
        RandomAccessReader metaReader = new RandomAccessReader(data, ByteOrder.BIG_ENDIAN);
        byte[]             guid       = metaReader.readBytes(16);
        
        int                blocks    = metaReader.readInt();
        List<BBQBlockInfo> blockList = new ArrayList<>();
        for (int i = 0; i < blocks; i++)
        {
            BBQBlockInfo block = new BBQBlockInfo();
            block.setUncompressedSize(metaReader.readInt());
            block.setCompressedSize(metaReader.readInt());
            block.setFlags(metaReader.readShort());
        }
        
        int                  nodes   = metaReader.readInt();
        List<BBQBundleEntry> entries = new ArrayList<>();
        for (int i = 0; i < nodes; i++)
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
        
        header.setFormatVersion(raf.readInt());
        header.setUnityVersion(raf.readString());
        header.setGeneratorVersion(raf.readString());
        
        if (header.isUnityFS()) {
            loadUnityFS(header, raf);
        } else if (header.isRAW() || header.isWEB()) {
            throw new UnsupportedOperationException("Unable to parse RAW and WEB files");
        } else {
            throw new UnsupportedOperationException("Unable to parse RAW and WEB files");
        }
        
        return header;
    }
    
    private void loadRAW(BBQHeader header, RandomAccessReader raf)
    {
    
    }
    
    private void loadUnityFS(BBQHeader header, RandomAccessReader raf)
    {
        header.setTotalFileSize(raf.readLong());
        header.setMetadataCompressedSize(raf.readInt());
        header.setMetadataUncompressedSize(raf.readInt());
        header.setFlags(raf.readInt());
    
        header.setCompressionMode(BBQCompressionType.from(header.getFlags() & 0x3f));
        header.setHasEntryInfo((header.getFlags() & 0x40) == 0x40);
        header.setMetadataAtEnd((header.getFlags() & 0x80) == 0x80);
        
        header.setHeaderSize(raf.pos() + (header.isMetadataAtEnd() ? 0 : header.getMetadataCompressedSize()));
    }
    
    
}
