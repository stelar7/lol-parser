package offline;

import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RandomAccessReader on synthetic data: byte orders, unaligned reads, strings, seeking helpers
 */
public class ReaderTest
{
    private static final byte[] DATA = {1, 2, 3, 4, 5, 6, 7, 8, 9, 'a', 'b', 0, 'c', 0};

    @Test
    public void testByteOrders()
    {
        RandomAccessReader r = new RandomAccessReader(DATA);
        assertEquals(0x04030201, r.readInt());
        r.seek(1);
        assertEquals(0x05040302, r.readInt(), "unaligned little endian read");

        RandomAccessReader b = new RandomAccessReader(DATA, ByteOrder.BIG_ENDIAN);
        assertEquals(0x01020304, b.readInt());
        b.setEndian(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x08070605, b.readInt(), "endian switch keeps the position");

        RandomAccessReader l = new RandomAccessReader(new LE().i64(0x1122334455667788L).i16(0xBEEF).f32(1.5f).toBytes());
        assertEquals(0x1122334455667788L, l.readLong());
        assertEquals((short) 0xBEEF, l.readShort());
        assertEquals(1.5f, l.readFloat());
        assertFalse(l.hasMoreBytes());
    }

    @Test
    public void testPositioning()
    {
        RandomAccessReader r = new RandomAccessReader(DATA);
        r.readBytes(2);
        assertEquals(2, r.pos(), "readBytes advances");

        r.seek(9);
        assertEquals("ab", r.readString());
        assertEquals(12, r.pos(), "position after the terminator");

        assertEquals("c", r.readFromOffset(12));
        assertEquals(12, r.pos(), "readFromOffset restores the position");

        r.seek(5);
        assertTrue(r.hasMoreBytes());
        r.seek(DATA.length);
        assertFalse(r.hasMoreBytes());

        r.seek(4);
        assertEquals(0x04030201, r.readIntReverse());
        assertEquals(0, r.pos(), "reverse read moves backwards");

        assertEquals(1, r.readChar(), "readChar reads a single byte");
        assertEquals(1, r.pos());
    }

    @Test
    public void testSearchAndStrings()
    {
        RandomAccessReader r = new RandomAccessReader(DATA);
        assertTrue(r.readUntillString("ab"));
        assertEquals(9, r.pos(), "positioned at the match");
        assertFalse(r.readUntillString("zz"));
        assertEquals(9, r.pos(), "position kept when nothing is found");

        assertEquals("hi", new RandomAccessReader(new byte[]{'h', 'i', 0, 'x'}).readFixedString(4));
        assertEquals(65535, new RandomAccessReader(new byte[]{(byte) 0xFF, (byte) 0xFF}).readU16());
    }

    @Test
    public void testFileReaderDoesNotLock() throws Exception
    {
        Path dir = Files.createTempDirectory("cdragon-reader");
        Path f   = dir.resolve("reader.bin");
        Files.write(f, DATA);
        RandomAccessReader fr = new RandomAccessReader(f);
        fr.seek(1);
        assertEquals(0x05040302, fr.readInt());
        Files.delete(f);
        assertFalse(Files.exists(f), "file can be deleted while the reader is alive");
        Files.delete(dir);
    }
}
