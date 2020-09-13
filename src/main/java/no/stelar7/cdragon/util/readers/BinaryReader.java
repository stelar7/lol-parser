package no.stelar7.cdragon.util.readers;

import java.nio.ByteOrder;

public interface BinaryReader
{
    String readString(int length);
    
    String readString();
    
    long readLong();
    
    int readInt();
    
    short readShort();
    
    byte readByte();
    
    byte[] readRemaining();
    
    int pos();
    
    void seek(int pos);
    
    void setEndian(ByteOrder order);
}
