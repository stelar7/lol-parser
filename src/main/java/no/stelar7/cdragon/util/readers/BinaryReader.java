package no.stelar7.cdragon.util.readers;

import java.nio.ByteOrder;

public interface BinaryReader
{
    String readString(long length);
    
    String readString();
    
    long readLong();
    
    int readInt();
    
    short readShort();
    
    double readDouble();
    
    float readFloat();
    
    byte[] readBytes(long i);
    
    byte readByte();
    
    char readChar();
    
    boolean readBoolean();
    
    byte[] readRemaining();
    
    long pos();
    
    void seek(long pos);
    
    void setEndian(ByteOrder order);
    
    void align();
    
}
