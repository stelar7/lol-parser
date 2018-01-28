package no.stelar7.cdragon.types.bnk;

import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;

public class BNKParser
{
    public BNKFile parse(Path path)
    {
        RandomAccessReader raf  = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        BNKFile            file = new BNKFile();
        
        parseBKDH(raf, file);
        parseDIDX(raf, file);
        parseDATA(raf, file);
        if (raf.isEOF())
        {
            return file;
        } else
        {
            System.err.println("File has more content????");
        }
//        parseENVS(raf, file);
//        parseFXPR(raf, file);
//        parseHIRC(raf, file);
//        parseSTID(raf, file);
//        parseSTMG(raf, file);
        return file;
    }
    
    private void parseDATA(RandomAccessReader raf, BNKFile file)
    {
        BNKDATA data = new BNKDATA(parseHeader(raf));
        int     pos  = raf.pos();
        
        for (BNKDIDXEntry entry : file.getDataIndex().getEntries())
        {
            raf.seek(pos + entry.getFileOffset());
            byte[] content = raf.readBytes(entry.getFileSize());
            
            BNKDATAWEMFile wem = new BNKDATAWEMFile();
            wem.setFileId(entry.getFileId());
            wem.setData(content);
            
            data.getWemFiles().add(wem);
        }
    
        file.setData(data);
        raf.seek(pos + data.getLength());
    }
    
    private void parseDIDX(RandomAccessReader raf, BNKFile file)
    {
        BNKDIDX didx      = new BNKDIDX(parseHeader(raf));
        int     didxCount = didx.getLength() / 12;
        
        for (int i = 0; i < didxCount; i++)
        {
            BNKDIDXEntry entry = new BNKDIDXEntry();
            
            entry.setFileId(raf.readInt());
            entry.setFileOffset(raf.readInt());
            entry.setFileSize(raf.readInt());
            
            didx.getEntries().add(entry);
        }
        
        file.setDataIndex(didx);
    }
    
    private void parseBKDH(RandomAccessReader raf, BNKFile file)
    {
        BNKBKHD bankHeader = new BNKBKHD(parseHeader(raf));
        bankHeader.setBankVersion(raf.readInt());
        bankHeader.setBankId(raf.readInt());
        bankHeader.setLanguageId(raf.readInt());
        bankHeader.setFeedback(raf.readInt());
        // padding?
        raf.seek((raf.pos() + (bankHeader.getLength() + 8)) - 24);
        file.setBankHeader(bankHeader);
    }
    
    private BNKHeader parseHeader(RandomAccessReader raf)
    {
        BNKHeader header = new BNKHeader();
        header.setSection(raf.readString(4));
        header.setLength(raf.readInt());
        return header;
    }
}
