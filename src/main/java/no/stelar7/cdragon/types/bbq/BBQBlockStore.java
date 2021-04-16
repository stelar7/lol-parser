package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.ByteArray;

import java.util.List;

public class BBQBlockStore
{
    private List<BBQBlockInfo> blockList;
    private BinaryReader       raf;
    private int                cursor            = 0;
    private int                basePos;
    private int                maxPos;
    private BBQBlockInfo       currentBlock;
    private int                currentBlockStart = 0;
    private BinaryReader       currentStream;
    
    public BBQBlockStore(List<BBQBlockInfo> blockList, RandomAccessReader raf)
    {
        this.blockList = blockList;
        this.raf = raf;
        this.basePos = raf.pos();
        this.maxPos = blockList.stream().mapToInt(BBQBlockInfo::getUncompressedSize).sum();
        seekInternal(0);
    }
    
    public void seekInternal(int pos)
    {
        this.cursor = pos;
        if (!inCurrentBlock(pos))
        {
            seekToBlock(pos);
        }
        this.currentStream.seek(pos - this.currentBlockStart);
    }
    
    public byte[] read(int size)
    {
        ByteArray buffer = new ByteArray();
        while (size != 0 && this.cursor < this.maxPos)
        {
            if (!inCurrentBlock(this.cursor))
            {
                seekToBlock(this.cursor);
            }
            
            byte[] part = this.currentStream.readBytes(size);
            
            if (size > 0)
            {
                if (part.length == 0)
                {
                    throw new RuntimeException("Unexpected end of file?");
                }
                
                size -= part.length;
            }
            
            this.cursor += part.length;
            buffer.append(part);
        }
        
        return buffer.getDataRaw();
    }
    
    public int pos()
    {
        return this.cursor;
    }
    
    public void seek(int pos, int direction)
    {
        if (direction == 0)
        {
            seekFromStart(pos);
        } else if (direction == 1)
        {
            seekFromCurrent(pos);
        } else if (direction == 2)
        {
            seekFromEnd(pos);
        }
    }
    
    public void seekFromCurrent(int pos)
    {
        int newCursor = this.cursor + pos;
        if (newCursor != this.cursor)
        {
            seekInternal(newCursor);
        }
    }
    
    public void seekFromEnd(int pos)
    {
        int newCursor = this.maxPos + pos;
        if (newCursor != this.cursor)
        {
            seekInternal(newCursor);
        }
    }
    
    public void seekFromStart(int pos)
    {
        int newCursor = pos;
        if (newCursor != this.cursor)
        {
            seekInternal(newCursor);
        }
    }
    
    
    private void seekToBlock(int pos)
    {
        int     baseOffset = 0;
        int     offset     = 0;
        boolean didBreak   = false;
        
        for (BBQBlockInfo block : blockList)
        {
            if (offset + block.getUncompressedSize() > pos)
            {
                this.currentBlock = block;
                didBreak = true;
                break;
            }
            
            baseOffset += block.getCompressedSize();
            offset += block.getUncompressedSize();
        }
        
        if (!didBreak)
        {
            this.currentBlock = null;
            this.currentStream = new RandomAccessReader(new byte[0]);
            return;
        }
        
        this.currentBlockStart = offset;
        this.raf.seek(this.basePos + baseOffset);
        byte[] buffer = this.raf.readBytes(this.currentBlock.getCompressedSize());
        this.currentStream = new RandomAccessReader(this.currentBlock.decompress(buffer));
    }
    
    private boolean inCurrentBlock(int pos)
    {
        if (this.currentBlock == null)
        {
            return false;
        }
        
        int end = this.currentBlockStart + this.currentBlock.getUncompressedSize();
        return this.currentBlockStart <= pos && pos < end;
    }
    
    public int getMaxPos()
    {
        return maxPos;
    }
}
