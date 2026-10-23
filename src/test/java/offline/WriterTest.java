package offline;

import no.stelar7.cdragon.util.handlers.FileTypeHandler;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.Vector4b;
import no.stelar7.cdragon.util.writers.ByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ByteWriter output, ByteArray comparisons and file type detection of tiny / BOM prefixed inputs
 */
public class WriterTest
{
    @Test
    public void testVec4BIsFourBytes()
    {
        Vector4b v = new Vector4b();
        v.setX((byte) 1);
        v.setY((byte) 2);
        v.setZ((byte) 3);
        v.setW((byte) -1);

        ByteWriter bw = new ByteWriter();
        bw.writeVec4B(v);
        assertArrayEquals(new byte[]{1, 2, 3, -1}, bw.toByteArray());
    }

    @Test
    public void testStringLengthIsUtf8Bytes()
    {
        ByteWriter bw = new ByteWriter();
        bw.writeStringWithLength("æøå");
        byte[] out = bw.toByteArray();
        assertEquals(2 + 6, out.length);
        assertEquals(6, out[0], "length prefix counts utf-8 bytes, not chars");
        assertEquals("æøå", new String(out, 2, 6, StandardCharsets.UTF_8));
    }

    @Test
    public void testSaveTruncates() throws Exception
    {
        Path file = Files.createTempFile("cdragon-writer", ".bin");
        try
        {
            Files.write(file, new byte[100]);
            ByteWriter bw = new ByteWriter();
            bw.writeInt(7);
            bw.save(file);
            assertEquals(4, Files.size(file), "old tail must not be left behind");
        } finally
        {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testByteArrayComparisons()
    {
        ByteArray abc = new ByteArray("abc".getBytes(StandardCharsets.UTF_8));
        ByteArray ab  = new ByteArray("ab".getBytes(StandardCharsets.UTF_8));

        assertNotEquals(abc, ab, "a prefix is not equal");
        assertNotEquals(ab, abc);
        assertEquals(abc, new ByteArray("abc".getBytes(StandardCharsets.UTF_8)));
        assertTrue(abc.startsWith(ab));
        assertFalse(ab.startsWith(abc), "shorter data can not start with longer data");
    }

    @Test
    public void testTinyAndBOMFiles()
    {
        for (int len = 1; len <= 3; len++)
        {
            byte[] data = new byte[len];
            data[0] = (byte) 0xEF;
            assertNotNull(FileTypeHandler.findFileType(new ByteArray(data)), "length " + len);
        }

        // a bare BOM and the shortest wrong-case magic must not throw
        assertNotNull(FileTypeHandler.findFileType(new ByteArray(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})));
        assertNotNull(FileTypeHandler.findFileType(new ByteArray(new byte[]{(byte) 0xFE, (byte) 0xFF})));
        assertNotEquals("wpk", FileTypeHandler.findFileType(new ByteArray("r3d1xxxx".getBytes(StandardCharsets.UTF_8))));

        byte[] json = "﻿{\"a\": 1}".getBytes(StandardCharsets.UTF_8);
        assertEquals(FileTypeHandler.findFileType(new ByteArray("{\"a\": 1}".getBytes(StandardCharsets.UTF_8))), FileTypeHandler.findFileType(new ByteArray(json)));
    }
}
