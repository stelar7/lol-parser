package no.stelar7.cdragon.types.rbun;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class RBUNParser implements Parseable<RBUNFile>
{
    
    @Override
    public RBUNFile parse(Path path)
    {
        try (RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN))
        {
            return parse(raf);
        }
    }
    
    public RBUNFile parseReadOnly(Path path)
    {
        try (RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN, false))
        {
            return parse(raf);
        }
    }
    
    @Override
    public RBUNFile parse(ByteArray data)
    {
        try (RandomAccessReader raf = new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN))
        {
            return parse(raf);
        }
    }
    
    @Override
    public RBUNFile parse(RandomAccessReader raf)
    {
        raf.seek(raf.remaining());
        RBUNFile file = new RBUNFile();
        file.setMagic(raf.readStringReverse(4));
        file.setVersion(raf.readIntReverse());
        file.setChunkCount(raf.readIntReverse());
        file.setBundleId(HashHandler.toHex(raf.readLongReverse(), 16));
        
        List<RBUNChunkInfo> chunkInfos = new ArrayList<>();
        for (int i = 0; i < file.getChunkCount(); i++)
        {
            RBUNChunkInfo ci = new RBUNChunkInfo();
            ci.setCompressedSize(raf.readIntReverse());
            ci.setUncompressedSize(raf.readIntReverse());
            ci.setChunkId(HashHandler.toHex(raf.readLongReverse(), 16));
            chunkInfos.add(ci);
        }
        
        file.setChunks(chunkInfos);
        raf.close();
        return file;
    }
}
