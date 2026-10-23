package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Decrypts the blocks of "UnityCN" encrypted asset bundles (Unity China's AssetBundle encryption).
 * <p>
 * The bundle header is followed (before the 16 byte alignment) by:
 * <pre>
 * u32 unknown
 * u8[16] data,     u8[16] key,     u8 skipped
 * u8[16] dataSig,  u8[16] keySig,  u8 skipped
 * </pre>
 * With the per-game 16 byte key K: AES-ECB_K(keySig) ^ dataSig must be "#$unity3dchina!@",
 * and the nibbles of AES-ECB_K(key) ^ data give the 16 entry index table and the 16 entry substitution table.
 * <p>
 * Only the structure bytes of the LZ4 sequences in blocks with flag 0x100 are encrypted (token, length extensions and match offset),
 * literals are stored in the clear.
 * <p>
 * See https://github.com/K0lb3/UnityPy/blob/master/UnityPy/helpers/ArchiveStorageManager.py
 * and UnityPyBoost/ArchiveStorageDecryptor.cpp (based on RazTools/Studio UnityCN.cs)
 */
public class BBQUnityCN
{
    public static final  byte[] SIGNATURE   = "#$unity3dchina!@".getBytes(StandardCharsets.US_ASCII);
    /**
     * Block flag marking an encrypted block
     */
    public static final  int    BLOCK_FLAG  = 0x100;
    /**
     * Size of the encryption header in the bundle
     */
    public static final  int    HEADER_SIZE = 4 + 2 * (16 + 16 + 1);

    private final long   unknown;
    private final byte[] indexTable = new byte[16];
    private final byte[] substitute = new byte[16];

    /**
     * Reads the encryption header at the current position of the reader
     *
     * @param key the 16 byte per-game key
     */
    public BBQUnityCN(RandomAccessReader raf, byte[] key)
    {
        long start = raf.pos();
        if (key == null)
        {
            throw new ParseException("The bundle is UnityCN encrypted, but no key was provided (see BBQParser#setUnityCNKey)", start);
        }
        if (key.length != 16)
        {
            throw new ParseException("UnityCN keys are 16 bytes, got " + key.length, start);
        }

        this.unknown = raf.readU32();
        byte[] data = raf.readBytes(16);
        byte[] vKey = raf.readBytes(16);
        raf.readByte();
        byte[] dataSig = raf.readBytes(16);
        byte[] keySig  = raf.readBytes(16);
        raf.readByte();

        byte[] signature = decryptKey(keySig, dataSig, key);
        if (!Arrays.equals(signature, SIGNATURE))
        {
            throw new ParseException("Invalid UnityCN key: the decrypted signature does not match", start);
        }

        byte[] table = decryptKey(vKey, data, key);
        byte[] nibbles = new byte[32];
        for (int i = 0; i < 16; i++)
        {
            nibbles[i * 2] = (byte) ((table[i] >> 4) & 0xF);
            nibbles[i * 2 + 1] = (byte) (table[i] & 0xF);
        }

        System.arraycopy(nibbles, 0, indexTable, 0, 16);
        // the substitution table is stored transposed
        for (int j = 0; j < 4; j++)
        {
            for (int i = 0; i < 4; i++)
            {
                substitute[j * 4 + i] = nibbles[0x10 + i * 4 + j];
            }
        }
    }

    /**
     * AES-ECB_keyBytes(key) ^ data
     */
    public static byte[] decryptKey(byte[] key, byte[] data, byte[] keyBytes)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));
            byte[] encrypted = cipher.doFinal(key);
            byte[] result    = new byte[16];
            for (int i = 0; i < 16; i++)
            {
                result[i] = (byte) (data[i] ^ encrypted[i]);
            }
            return result;
        } catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("AES is not available", e);
        }
    }

    public long getUnknown()
    {
        return unknown;
    }

    /**
     * Decrypts one block in place and returns it
     *
     * @param blockIndex the index of the block in the bundle (0 for the block info)
     */
    public byte[] decryptBlock(byte[] data, int blockIndex)
    {
        long index  = Integer.toUnsignedLong(blockIndex);
        int  offset = 0;
        while (offset < data.length)
        {
            offset += decryptSequence(data, offset, index++, data.length - offset);
        }
        return data;
    }

    private int decryptByte(byte[] bytes, int offset, long index)
    {
        if (offset >= bytes.length)
        {
            throw new ParseException("UnityCN block ended inside an LZ4 sequence", offset);
        }

        int count = substitute[(int) (((index >> 2) & 3) + 4)]
                    + substitute[(int) (index & 3)]
                    + substitute[(int) (((index >> 4) & 3) + 8)]
                    + substitute[(int) (((index & 0xFF) >> 6) + 12)];
        int value = bytes[offset] & 0xFF;
        bytes[offset] = (byte) (((indexTable[value & 0xF] - count) & 0xF) | ((indexTable[value >> 4] - count) << 4));
        return bytes[offset] & 0xFF;
    }

    /**
     * Decrypts the structure bytes of one LZ4 sequence; returns the number of bytes it spans
     */
    private int decryptSequence(byte[] bytes, int start, long index, int remaining)
    {
        int offset = start;

        int current = decryptByte(bytes, offset++, index++);
        long high   = current >> 4;
        int  low    = current & 0xF;

        if (high == 0xF)
        {
            int b;
            do
            {
                b = decryptByte(bytes, offset++, index++);
                high += b;
            } while (b == 0xFF);
        }

        long skipped = (offset - start) + high;
        if (skipped < remaining)
        {
            offset = (int) (start + skipped);
            decryptByte(bytes, offset++, index++);
            decryptByte(bytes, offset++, index++);
            if (low == 0xF)
            {
                int b;
                do
                {
                    b = decryptByte(bytes, offset++, index++);
                } while (b == 0xFF);
            }
            return offset - start;
        }

        return (int) Math.min(Integer.MAX_VALUE, skipped);
    }
}
