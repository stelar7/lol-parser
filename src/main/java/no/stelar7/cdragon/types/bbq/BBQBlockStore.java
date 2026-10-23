package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

import java.util.*;

/**
 * Presents the (compressed) blocks of a bundle as one continuous stream of uncompressed data.
 * Blocks are decompressed on demand, and the most recently used ones are kept in a small cache.
 */
public class BBQBlockStore
{
    private static final int CACHE_SIZE = 8;

    private final List<BBQBlockInfo> blockList;
    private final RandomAccessReader raf;
    private final long[]             compressedStarts;
    private final long[]             uncompressedStarts;
    private final long               maxPos;
    private final BBQUnityCN         decryptor;
    private       long               cursor = 0;

    private final Map<Integer, byte[]> cache = new LinkedHashMap<>(16, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest)
        {
            return size() > CACHE_SIZE;
        }
    };

    private int    lastIndex = -1;
    private byte[] lastBlock;

    /**
     * @param basePos the absolute file offset of the first block
     */
    public BBQBlockStore(List<BBQBlockInfo> blockList, RandomAccessReader raf, long basePos)
    {
        this(blockList, raf, basePos, null);
    }

    /**
     * @param basePos   the absolute file offset of the first block
     * @param decryptor decrypts blocks with flag 0x100 in UnityCN encrypted bundles, null if the bundle is not encrypted
     */
    public BBQBlockStore(List<BBQBlockInfo> blockList, RandomAccessReader raf, long basePos, BBQUnityCN decryptor)
    {
        this.decryptor = decryptor;
        this.blockList = blockList;
        this.raf = raf;
        this.compressedStarts = new long[blockList.size()];
        this.uncompressedStarts = new long[blockList.size() + 1];

        long compressed   = 0;
        long uncompressed = 0;
        for (int i = 0; i < blockList.size(); i++)
        {
            BBQBlockInfo block = blockList.get(i);

            // bundle format 9+ stores the offset of each block, older ones store them back to back
            compressedStarts[i] = basePos + (block.getOffset() >= 0 ? block.getOffset() : compressed);
            uncompressedStarts[i] = uncompressed;

            compressed += Integer.toUnsignedLong(block.getCompressedSize());
            uncompressed += Integer.toUnsignedLong(block.getUncompressedSize());
        }

        this.uncompressedStarts[blockList.size()] = uncompressed;
        this.maxPos = uncompressed;
    }

    /**
     * Copies up to len bytes starting at the absolute (uncompressed) position into dst.
     * Returns the number of bytes copied, which is only less than len at the end of the data.
     */
    public int read(long position, byte[] dst, int off, int len)
    {
        int copied = 0;
        while (copied < len && position < maxPos)
        {
            int    index       = blockIndex(position);
            byte[] block       = block(index);
            int    inBlock     = (int) (position - uncompressedStarts[index]);
            int    count       = Math.min(len - copied, block.length - inBlock);

            System.arraycopy(block, inBlock, dst, off + copied, count);
            copied += count;
            position += count;
        }
        return copied;
    }

    /**
     * Reads the byte at the absolute (uncompressed) position
     */
    public byte get(long position)
    {
        if (lastBlock != null && position >= uncompressedStarts[lastIndex] && position < uncompressedStarts[lastIndex + 1])
        {
            return lastBlock[(int) (position - uncompressedStarts[lastIndex])];
        }

        if (position < 0 || position >= maxPos)
        {
            throw new ParseException("Read past the end of the bundle data", position);
        }

        int index = blockIndex(position);
        return block(index)[(int) (position - uncompressedStarts[index])];
    }

    private int blockIndex(long position)
    {
        int index = Arrays.binarySearch(uncompressedStarts, position);
        if (index < 0)
        {
            index = -index - 2;
        }

        // skip empty blocks
        while (index < blockList.size() - 1 && uncompressedStarts[index + 1] <= position)
        {
            index++;
        }

        return index;
    }

    private byte[] block(int index)
    {
        if (index == lastIndex)
        {
            return lastBlock;
        }

        byte[] data = cache.get(index);
        if (data == null)
        {
            BBQBlockInfo info = blockList.get(index);
            if (compressedStarts[index] + Integer.toUnsignedLong(info.getCompressedSize()) > raf.size())
            {
                throw new ParseException("Bundle block " + index + " is truncated", compressedStarts[index]);
            }
            raf.seek(compressedStarts[index]);
            byte[] compressed = raf.readBytes(Integer.toUnsignedLong(info.getCompressedSize()));
            if (compressed.length != info.getCompressedSize())
            {
                throw new ParseException("Bundle block " + index + " is truncated", compressedStarts[index]);
            }

            if (decryptor != null && (info.getFlags() & BBQUnityCN.BLOCK_FLAG) != 0 && info.getCompressionType() != BBQCompressionType.NONE && info.getUncompressedSize() != 0)
            {
                compressed = decryptor.decryptBlock(compressed, index);
            }

            data = info.decompress(compressed);
            cache.put(index, data);
        }

        lastIndex = index;
        lastBlock = data;
        return data;
    }

    /**
     * Reads up to size bytes from the current cursor, fewer at the end of the data
     */
    public byte[] read(int size)
    {
        int    count  = (int) Math.max(0, Math.min(size, maxPos - cursor));
        byte[] buffer = new byte[count];
        int    read   = read(cursor, buffer, 0, count);
        cursor += read;
        return buffer;
    }

    public long pos()
    {
        return this.cursor;
    }

    public void seek(long pos, int direction)
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

    public void seekFromCurrent(long pos)
    {
        this.cursor += pos;
    }

    public void seekFromEnd(long pos)
    {
        this.cursor = this.maxPos + pos;
    }

    public void seekFromStart(long pos)
    {
        this.cursor = pos;
    }

    public long getMaxPos()
    {
        return maxPos;
    }
}
