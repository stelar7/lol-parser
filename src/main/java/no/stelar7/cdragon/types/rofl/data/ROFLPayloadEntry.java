package no.stelar7.cdragon.types.rofl.data;

import no.stelar7.cdragon.util.readers.ParseException;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class ROFLPayloadEntry
{
    public static final int HEADER_SIZE = 17;

    private int    id;
    private int    length;
    private int    nextChunkId;
    private int    offset;
    private byte   type;
    private byte[] data;

    // the chunk as stored in the file, decrypted and decompressed on the first call to getData()
    private transient byte[] encryptedData;
    private transient byte[] chunkKey;

    public static int getHeaderSize()
    {
        return HEADER_SIZE;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public int getNextChunkId()
    {
        return nextChunkId;
    }

    public void setNextChunkId(int nextChunkId)
    {
        this.nextChunkId = nextChunkId;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    public byte getType()
    {
        return type;
    }

    public void setType(byte type)
    {
        this.type = type;
    }

    /**
     * Sets the already decrypted and decompressed chunk data
     */
    public void setData(byte[] data)
    {
        this.data = data;
        this.encryptedData = null;
        this.chunkKey = null;
    }

    /**
     * Sets the chunk as stored in the file, it is decrypted and decompressed when {@link #getData()} is first called
     *
     * @param chunkKey the decrypted chunk encryption key, see {@link #decryptChunkKey(long, byte[])}
     */
    public void setEncryptedData(byte[] encryptedData, byte[] chunkKey)
    {
        this.data = null;
        this.encryptedData = encryptedData;
        this.chunkKey = chunkKey;
    }

    /**
     * The decrypted and decompressed chunk data
     *
     * @throws ParseException if the chunk can not be decrypted or decompressed
     */
    public byte[] getData()
    {
        if (data == null && encryptedData != null)
        {
            byte[] decryptedChunk = depad(decrypt(chunkKey, encryptedData), "chunk " + id);
            this.data = decompress(decryptedChunk);
            this.encryptedData = null;
            this.chunkKey = null;
        }
        return data;
    }

    /**
     * Decrypts the payload header's encryption key with the game id; the result is the key for every chunk in the file
     */
    public static byte[] decryptChunkKey(long gameId, byte[] encryptionKey)
    {
        byte[] key = String.valueOf(gameId).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return depad(decrypt(key, encryptionKey), "chunk encryption key");
    }

    private static byte[] decrypt(byte[] key, byte[] data)
    {
        if (data.length == 0 || data.length % 8 != 0)
        {
            throw new ParseException("Encrypted ROFL data length " + data.length + " is not a multiple of the Blowfish block size", -1);
        }

        try
        {
            BufferedBlockCipher cipher = new BufferedBlockCipher(new BlowfishEngine());
            cipher.init(false, new KeyParameter(key));
            byte[] output  = new byte[cipher.getOutputSize(data.length)];
            int    written = cipher.processBytes(data, 0, data.length, output, 0);
            written += cipher.doFinal(output, written);
            if (written != output.length)
            {
                output = java.util.Arrays.copyOf(output, written);
            }
            return output;
        } catch (CryptoException | RuntimeException e)
        {
            throw new ParseException("Failed to decrypt ROFL data: " + e.getMessage(), -1, e);
        }
    }

    /**
     * Removes PKCS#5 padding
     */
    private static byte[] depad(byte[] data, String what)
    {
        if (data.length == 0)
        {
            throw new ParseException("Decrypted ROFL " + what + " is empty", -1);
        }

        int paddingLength = Byte.toUnsignedInt(data[data.length - 1]);
        if (paddingLength < 1 || paddingLength > 8 || paddingLength > data.length)
        {
            throw new ParseException("Invalid padding in decrypted ROFL " + what + " (wrong key or corrupt data)", -1);
        }

        for (int i = data.length - paddingLength; i < data.length; i++)
        {
            if (Byte.toUnsignedInt(data[i]) != paddingLength)
            {
                throw new ParseException("Invalid padding in decrypted ROFL " + what + " (wrong key or corrupt data)", -1);
            }
        }

        return java.util.Arrays.copyOf(data, data.length - paddingLength);
    }

    private byte[] decompress(byte[] decryptedChunk)
    {
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(decryptedChunk)))
        {
            return input.readAllBytes();
        } catch (IOException e)
        {
            throw new ParseException("Failed to decompress ROFL chunk " + id + ": " + e.getMessage(), offset, e);
        }
    }
}
