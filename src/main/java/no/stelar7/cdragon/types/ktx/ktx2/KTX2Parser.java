package no.stelar7.cdragon.types.ktx.ktx2;


import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.ktx.ktx2.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * https://registry.khronos.org/KTX/specs/2.0/ktxspec.v2.html
 */
public class KTX2Parser implements Parseable<KTX2File>
{
    public static final byte[] MAGIC = {(byte) 0xAB, 0x4B, 0x54, 0x58, 0x20, 0x32, 0x30, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final int HEADER_SIZE      = 80;
    private static final int LEVEL_INDEX_SIZE = 24;

    @Override
    public KTX2File parse(RandomAccessReader raf)
    {
        // KTX2 is always little endian
        raf.setEndian(ByteOrder.LITTLE_ENDIAN);

        if (raf.size() < HEADER_SIZE)
        {
            throw new ParseException("File is too small to be a KTX2 file", 0);
        }

        KTX2File data = new KTX2File();
        data.setHeader(parseHeader(raf));
        data.setIndecies(parseIndex(raf));
        data.setLevels(parseLevels(raf, data.getHeader()));

        KTX2FileIndecies index = data.getIndecies();
        parseDataFormatDescriptor(raf, data, index.getDfdByteOffset(), index.getDfdByteLength());
        data.setKeyValueData(parseKeyValueData(raf, index.getKvdByteOffset(), index.getKvdByteLength()));
        data.setSupercompressionGlobalData(readRange(raf, index.getSgdByteOffset(), index.getSgdByteLength(), "supercompression global data"));

        for (int i = 0; i < data.getLevels().size(); i++)
        {
            KTX2FileLevel level = data.getLevels().get(i);
            level.setData(readRange(raf, level.getByteOffset(), level.getByteLength(), "level " + i));
        }

        return data;
    }

    private byte[] readRange(RandomAccessReader raf, long offset, long length, String what)
    {
        if (length == 0)
        {
            return new byte[0];
        }

        if (offset < 0 || length < 0 || offset + length > raf.size() || offset + length < 0)
        {
            throw new ParseException("KTX2 " + what + " (offset " + offset + ", length " + length + ") is outside the file (size " + raf.size() + ")", offset);
        }

        if (length > Integer.MAX_VALUE)
        {
            throw new ParseException("KTX2 " + what + " is too large (" + length + " bytes)", offset);
        }

        raf.seek(offset);
        return raf.readBytes(length);
    }

    private void parseDataFormatDescriptor(RandomAccessReader raf, KTX2File file, long offset, long length)
    {
        List<KTX2FileDataFormatDescriptorBlock> blocks = new ArrayList<>();
        file.setDataFormatBlocks(blocks);
        if (length == 0)
        {
            return;
        }

        readRange(raf, offset, length, "data format descriptor");
        raf.seek(offset);

        long totalSize = raf.readU32();
        if (totalSize != length)
        {
            throw new ParseException("KTX2 dfdTotalSize (" + totalSize + ") does not match dfdByteLength (" + length + ")", offset);
        }
        file.setDfdTotalSize((int) totalSize);

        long end = offset + totalSize;
        while (raf.pos() + 8 <= end)
        {
            long blockStart = raf.pos();

            KTX2FileDataFormatDescriptorBlock dfd = new KTX2FileDataFormatDescriptorBlock();

            // vendorId is the low 17 bits, descriptorType the high 15 bits
            int vendorAndType = raf.readInt();
            dfd.setVendorId(vendorAndType & 0x1FFFF);
            dfd.setDescriptorType(vendorAndType >>> 17);
            dfd.setVersionNumber(raf.readShort());
            dfd.setDescriptorBlockSize(raf.readShort());

            int blockSize = Short.toUnsignedInt(dfd.getDescriptorBlockSize());
            if (blockSize < 8 || blockStart + blockSize > end)
            {
                throw new ParseException("Invalid KTX2 data format descriptor block size " + blockSize, blockStart);
            }

            // the basic descriptor block (Khronos vendor 0, type 0) is the only one with a known layout
            if (dfd.getVendorId() == 0 && dfd.getDescriptorType() == 0)
            {
                if (blockSize < 24 || (blockSize - 24) % 16 != 0)
                {
                    throw new ParseException("Invalid KTX2 basic data format descriptor block size " + blockSize, blockStart);
                }

                dfd.setColorModel(raf.readByte());
                dfd.setColorPrimaries(raf.readByte());
                dfd.setTransferFunction(raf.readByte());
                dfd.setFlags(raf.readByte());
                dfd.setTexelBlockDimension0(raf.readByte());
                dfd.setTexelBlockDimension1(raf.readByte());
                dfd.setTexelBlockDimension2(raf.readByte());
                dfd.setTexelBlockDimension3(raf.readByte());
                dfd.setBytesPlane0(raf.readByte());
                dfd.setBytesPlane1(raf.readByte());
                dfd.setBytesPlane2(raf.readByte());
                dfd.setBytesPlane3(raf.readByte());
                dfd.setBytesPlane4(raf.readByte());
                dfd.setBytesPlane5(raf.readByte());
                dfd.setBytesPlane6(raf.readByte());
                dfd.setBytesPlane7(raf.readByte());

                int sampleCount = (blockSize - 24) / 16;
                for (int i = 0; i < sampleCount; i++)
                {
                    KTX2FileDataFormatDescriptorBlockSample sample = new KTX2FileDataFormatDescriptorBlockSample();
                    sample.setBitOffset(raf.readShort());
                    sample.setBitLength(raf.readByte());
                    sample.setChannelType(raf.readByte());
                    sample.setSamplePosition0(raf.readByte());
                    sample.setSamplePosition1(raf.readByte());
                    sample.setSamplePosition2(raf.readByte());
                    sample.setSamplePosition3(raf.readByte());
                    sample.setSampleLower(raf.readInt());
                    sample.setSampleUpper(raf.readInt());
                    dfd.addSample(sample);
                }
            }

            blocks.add(dfd);
            raf.seek(blockStart + blockSize);
        }
    }

    private Map<String, byte[]> parseKeyValueData(RandomAccessReader raf, long offset, long length)
    {
        Map<String, byte[]> values = new LinkedHashMap<>();
        if (length == 0)
        {
            return values;
        }

        readRange(raf, offset, length, "key/value data");
        raf.seek(offset);

        long end = offset + length;
        while (raf.pos() + 4 <= end)
        {
            long entryStart = raf.pos();
            long size       = raf.readU32();
            if (size == 0 || raf.pos() + size > end)
            {
                throw new ParseException("Invalid KTX2 key/value entry length " + size, entryStart);
            }

            byte[] keyAndValue = raf.readBytes(size);
            int    keyEnd      = 0;
            while (keyEnd < keyAndValue.length && keyAndValue[keyEnd] != 0)
            {
                keyEnd++;
            }

            if (keyEnd == keyAndValue.length)
            {
                throw new ParseException("KTX2 key/value entry key is not 0x00 terminated", entryStart);
            }

            String key   = new String(keyAndValue, 0, keyEnd, StandardCharsets.UTF_8);
            byte[] value = Arrays.copyOfRange(keyAndValue, keyEnd + 1, keyAndValue.length);
            values.put(key, value);

            // each entry is padded to a multiple of 4 bytes, except possibly the last one
            raf.seek(Math.min(end, (raf.pos() + 3) & ~3L));
        }

        return values;
    }

    private List<KTX2FileLevel> parseLevels(RandomAccessReader raf, KTX2FileHeader header)
    {
        List<KTX2FileLevel> levels = new ArrayList<>();

        // a levelCount of 0 means a single level is stored, and the loader should generate the rest
        int count = Math.max(1, header.getLevelCount());
        if (HEADER_SIZE + (long) count * LEVEL_INDEX_SIZE > raf.size())
        {
            throw new ParseException("KTX2 level index for " + count + " levels is outside the file", raf);
        }

        for (int i = 0; i < count; i++)
        {
            KTX2FileLevel level = new KTX2FileLevel();
            level.setByteOffset(raf.readLong());
            level.setByteLength(raf.readLong());
            level.setUncompressedByteLength(raf.readLong());

            if (header.getSupercompressionScheme() == KTX2File.SUPERCOMPRESSION_NONE && level.getByteLength() != level.getUncompressedByteLength())
            {
                throw new ParseException("KTX2 level " + i + " byteLength does not match uncompressedByteLength for an uncompressed file", raf);
            }

            levels.add(level);
        }

        return levels;
    }

    private KTX2FileIndecies parseIndex(RandomAccessReader raf)
    {
        KTX2FileIndecies index = new KTX2FileIndecies();
        index.setDfdByteOffset(raf.readInt());
        index.setDfdByteLength(raf.readInt());
        index.setKvdByteOffset(raf.readInt());
        index.setKvdByteLength(raf.readInt());
        index.setSgdByteOffset(raf.readLong());
        index.setSgdByteLength(raf.readLong());
        return index;
    }

    private KTX2FileHeader parseHeader(RandomAccessReader raf)
    {
        KTX2FileHeader header = new KTX2FileHeader();
        header.setMagic(raf.readBytes(12));
        if (!Arrays.equals(header.getMagic(), MAGIC))
        {
            throw new ParseException("Not a KTX2 file (invalid identifier)", 0);
        }

        header.setFormat(raf.readInt());
        header.setTypeSize(raf.readInt());
        header.setPixelWidth(raf.readInt());
        header.setPixelHeight(raf.readInt());
        header.setPixelDepth(raf.readInt());
        header.setArrayElementCount(raf.readInt());
        header.setFaceCount(raf.readInt());
        header.setLevelCount(raf.readInt());
        header.setSupercompressionScheme(raf.readInt());

        if (header.getPixelWidth() <= 0 || header.getPixelHeight() < 0 || header.getPixelDepth() < 0)
        {
            throw new ParseException("Invalid KTX2 dimensions " + header.getPixelWidth() + "x" + header.getPixelHeight() + "x" + header.getPixelDepth(), 20);
        }

        if (header.getFaceCount() != 1 && header.getFaceCount() != 6)
        {
            throw new ParseException("Invalid KTX2 faceCount " + header.getFaceCount(), 36);
        }

        if (header.getLevelCount() < 0 || header.getLevelCount() > 32)
        {
            throw new ParseException("Invalid KTX2 levelCount " + header.getLevelCount(), 40);
        }

        return header;
    }
}
