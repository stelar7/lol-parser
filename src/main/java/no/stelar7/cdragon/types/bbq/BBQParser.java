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
        BBQGlobalLoader loader = new BBQGlobalLoader(null);
        
        BBQFile file = new BBQFile();
        BBQHeader      header = parseHeader(raf);
        List<BBQAsset> assets = parseEntryList(raf, header);
        
        file.setHeader(header);
        file.setEntries(assets);
        return file;
    }
    
    private List<BBQAsset> parseEntryList(RandomAccessReader raf, BBQHeader header)
    {
        int     currentPos = raf.pos();
        boolean metaAtEnd  = header.isMetadataAtEnd();
        
        if (metaAtEnd)
        {
            raf.seekFromEnd(-header.getMetadataCompressedSize());
        }
        
        byte[] data = raf.readBytes(header.getMetadataCompressedSize());
        switch (header.getCompressionMode())
        {
            case NONE:
            {
                break;
            }
            case LZMA:
            {
                data = CompressionHandler.uncompressLZMA(data);
                break;
            }
            case LZ4:
            case LZ4HC:
            case LZHAM:
            {
                data = CompressionHandler.uncompressLZ4(data, header.getMetadataUncompressedSize());
                break;
            }
        }
        
        raf.seek(currentPos);
        
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
            blockList.add(block);
        }
        
        BBQBlockStore storage = new BBQBlockStore(blockList, raf);
        
        int                  nodeCount   = metaReader.readInt();
        List<BBQBundleEntry> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++)
        {
            BBQBundleEntry entry = parseMetaEntry(metaReader, raf);
            nodes.add(entry);
        }
        
        nodes.sort(Comparator.comparing(BBQBundleEntry::getOffset));

        List<BBQAsset> assets = new ArrayList<>();
        for (BBQBundleEntry block : nodes) {
            BBQAsset asset = BBQAsset.fromBundle(storage, header);
            asset.name = block.getName();
            asset.load();
            assets.add(asset);
        }
        
        return assets;
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
        if (!header.getSignature().startsWith("Unity"))
        {
            throw new RuntimeException("Invalid file signature");
        }
        
        header.setFormatVersion(raf.readInt());
        header.setUnityVersion(raf.readString());
        header.setGeneratorVersion(raf.readString());
        
        if (header.isUnityFS())
        {
            loadUnityFS(header, raf);
        } else if (header.isRAW() || header.isWEB())
        {
            throw new UnsupportedOperationException("Unable to parse RAW and WEB files");
        } else
        {
            throw new UnsupportedOperationException("Unable to parse this type of file");
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
        header.setBlockAndDirCombined((header.getFlags() & 0x40) == 0x40);
        header.setMetadataAtEnd((header.getFlags() & 0x80) == 0x80);
        header.setHeaderSize(raf.pos() + (header.isMetadataAtEnd() ? 0 : header.getMetadataCompressedSize()));
    }
}
