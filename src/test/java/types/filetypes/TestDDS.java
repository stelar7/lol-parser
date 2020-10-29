package types.filetypes;

import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.*;

import javax.crypto.Cipher;
import javax.crypto.spec.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TestDDS
{
    
    @Test
    public void testDDS()
    {
        try
        {
            DDSParser parser = new DDSParser();
            
            Path file = UtilHandler.CDRAGON_FOLDER.resolve("temp");
            
            Files.walkFileTree(file, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    BufferedImage img = null;
                    if (file.toString().endsWith(".dds.compressed"))
                    {
                        img = parser.parseCompressed(file);
                    } else if (file.toString().endsWith(".dds"))
                    {
                        img = parser.parse(file);
                    }
                    
                    if (img == null)
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    Path output = UtilHandler.CDRAGON_FOLDER.resolve("png/" + UtilHandler.pathToFilename(file) + ".png");
                    Files.createDirectories(output.getParent());
                    
                    ImageIO.write(img, "png", output.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testSingle() throws IOException
    {
        DDSParser     parser = new DDSParser();
        Path          file   = Paths.get("D:\\pbe\\assets\\loadouts\\summoneremotes\\flairs\\penguin_meme_dab_vfx.dds");
        BufferedImage img    = parser.parse(file);
        ImageIO.write(img, "png", file.resolveSibling("out.png").toFile());
    }
    
    @Test
    @Disabled
    public void testEncrypted() throws Exception
    {
        byte[] dataIn  = Files.readAllBytes(Paths.get("D:\\cdragon\\dds\\00DFBFE26ACB33FC.encrypted.dds"));
        
        // AES_256-CBC, it only uses 256 bits of the key, aka. 32bytes. The key Riot sends over is 64bytes, so skip the last part...
        byte[] keyBytes = UtilHandler.AES_KEY.substring(0, 32).getBytes();
        byte[] ivBytes  = UtilHandler.AES_IV.substring(0, 16).getBytes();
        
        SecretKeySpec   keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec  = new IvParameterSpec(ivBytes);
        Cipher          cipher  = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] dataOut = cipher.doFinal(dataIn);
     
        Files.write(Paths.get("D:\\cdragon\\dds\\00DFBFE26ACB33FC.decrypted.dds"), dataOut);
    }
}
