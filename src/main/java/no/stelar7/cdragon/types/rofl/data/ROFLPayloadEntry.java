package no.stelar7.cdragon.types.rofl.data;

import lombok.*;
import no.stelar7.cdragon.util.handlers.CompressionHandler;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;

@Data
public class ROFLPayloadEntry
{
    public static final int HEADER_SIZE = 17;
    
    private int    id;
    private int    length;
    private int    nextChunkId;
    private int    offset;
    private byte   type;
    private byte[] data;
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data, ROFLFile file)
    {
        byte[] gameId             = String.valueOf(file.getPayloadHeader().getGameId()).getBytes(StandardCharsets.UTF_8);
        byte[] chunkEncryptionKey = depad(decrypt(gameId, file.getPayloadHeader().getEncryptionKey()));
        byte[] decryptedChunk     = depad(decrypt(chunkEncryptionKey, data));
        this.data = decompress(decryptedChunk);
    }
    
    private byte[] decrypt(byte[] key, byte[] data)
    {
        BufferedBlockCipher cipher = new BufferedBlockCipher(new BlowfishEngine());
        cipher.init(false, new KeyParameter(key));
        byte[] output = new byte[cipher.getOutputSize(data.length)];
        cipher.processBytes(data, 0, data.length, output, 0);
        return output;
    }
    
    private byte[] depad(byte[] data)
    {
        int    paddingLength = Byte.toUnsignedInt(data[data.length - 1]);
        byte[] holder        = new byte[data.length - paddingLength];
        System.arraycopy(data, 0, holder, 0, holder.length);
        return holder;
    }
    
    private byte[] decompress(byte[] decryptedChunk)
    {
        return CompressionHandler.uncompressGZIP(decryptedChunk);
    }
}
