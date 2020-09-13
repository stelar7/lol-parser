package no.stelar7.cdragon.util.readers;

import java.nio.ByteOrder;

public interface BinaryReader
{
    String readString(int length);
    
    String readString();
    
    long readLong();
    
    int readInt();
    
    short readShort();
    
    byte[] readBytes(int i);
    
    byte readByte();
    
    char readChar();
    
    boolean readBoolean();
    
    byte[] readRemaining();
    
    int pos();
    
    void seek(int pos);
    
    void setEndian(ByteOrder order);
    
}
