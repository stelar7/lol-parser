package no.stelar7.cdragon.types.rman;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class RMANParser implements Parseable<RMANFile>
{
    
    @Override
    public RMANFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(RandomAccessReader raf)
    {
        RMANFile file = new RMANFile();
        file.setHeader(parseHeader(raf));
        
        raf.seek(file.getHeader().getOffset());
        file.setContent(CompressionHandler.uncompressZSTD(raf.readBytes(file.getHeader().getLength())));
        
        file.setRemainder(raf.readRemaining());
        
        return file;
    }
    
    private RMANFileHeader parseHeader(RandomAccessReader raf)
    {
        RMANFileHeader header = new RMANFileHeader();
        header.setMagic(raf.readString(4));
        header.setMajor(raf.readByte());
        header.setMinor(raf.readByte());
        header.setUnknown(raf.readByte());
        header.setSigned(raf.readByte());
        header.setOffset(raf.readInt());
        header.setLength(raf.readInt());
        return header;
    }
}