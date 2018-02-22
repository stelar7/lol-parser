package no.stelar7.cdragon.types.bnk;

import no.stelar7.cdragon.types.bnk.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class BNKParser
{
    private final List<String> order = Arrays.asList("BKHD", "DIDX", "DATA", "ENVS", "FXPR", "HIRC", "STID", "STMG");
    
    public BNKFile parse(Path path)
    {
        RandomAccessReader raf  = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        BNKFile            file = new BNKFile();
        
        List<BNKHeader> headers = new ArrayList<>();
        while (!raf.isEOF())
        {
            BNKHeader header = parseHeader(raf);
            headers.add(header);
            raf.seek(raf.pos() + header.getLength());
        }
        
        headers.sort((a, b) -> order.indexOf(a.getSection()) > order.indexOf(b.getSection()) ? 1 : -1);
        
        for (BNKHeader header : headers)
        {
            parseSection(header, raf, file);
        }
        
        return file;
    }
    
    private void parseSection(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        switch (header.getSection())
        {
            case "BKHD":
                parseBKDH(header, raf, file);
                break;
            case "DIDX":
                parseDIDX(header, raf, file);
                break;
            case "DATA":
                parseDATA(header, raf, file);
                break;
            case "ENVS":
                parseENVS(header, raf, file);
                break;
            case "FXPR":
                parseFXPR(header, raf, file);
                break;
            case "HIRC":
                parseHIRC(header, raf, file);
                break;
            case "STID":
                parseSTID(header, raf, file);
                break;
            case "STMG":
                parseSTMG(header, raf, file);
                break;
            default:
                throw new RuntimeException("Invalid header section");
        }
    }
    
    private void parseSTMG(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        throw new UnsupportedOperationException("STMG PARSING NOT IMPLEMENTED");
    }
    
    private void parseSTID(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        throw new UnsupportedOperationException("STID PARSING NOT IMPLEMENTED");
    }
    
    private void parseHIRC(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        throw new UnsupportedOperationException("HIRC PARSING NOT IMPLEMENTED");
    }
    
    private void parseFXPR(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        throw new UnsupportedOperationException("FXPR PARSING NOT IMPLEMENTED");
    }
    
    private void parseENVS(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        throw new UnsupportedOperationException("ENVS PARSING NOT IMPLEMENTED");
    }
    
    
    private void parseDATA(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        BNKDATA data = new BNKDATA(header);
        raf.seek(header.getDataStart());
        int pos = raf.pos();
        
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
    
    private void parseDIDX(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        BNKDIDX didx = new BNKDIDX(header);
        raf.seek(header.getDataStart());
        int didxCount = didx.getLength() / 12;
        
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
    
    private void parseBKDH(BNKHeader header, RandomAccessReader raf, BNKFile file)
    {
        BNKBKHD bankHeader = new BNKBKHD(header);
        raf.seek(header.getDataStart());
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
        header.setDataStart(raf.pos());
        return header;
    }
}
