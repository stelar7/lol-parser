package no.stelar7.cdragon.wad.data;

import com.google.gson.*;
import lombok.*;
import no.stelar7.api.l4j8.basic.utils.Utils;
import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.WADHeaderBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Data
public class WADFile
{
    private String unknownHashContainer = "unknown.json";
    
    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.NONE)
    private final RAFReader     fileReader;
    private       WADHeaderBase header;
    
    private List<WADContentHeaderV1> contentHeaders = new ArrayList<>();
    
    public WADFile(RAFReader raf)
    {
        this.fileReader = raf;
    }
    
    public void extractFiles(String pluginName, String wadName, Path path)
    {
        if (wadName == null)
        {
            wadName = "assets.wad";
        }
        System.out.println("Extracting files from " + pluginName + "/" + wadName);
        final Path outputPath = path.resolve(pluginName);
        Path       ukp        = outputPath.resolve(unknownHashContainer);
        try
        {
            if (!Files.exists(ukp))
            {
                Files.createDirectories(ukp.getParent());
                Files.createFile(ukp);
            }
            Files.write(ukp, new byte[]{});
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        ExecutorService executor  = Executors.newFixedThreadPool(1);//Runtime.getRuntime().availableProcessors());
        final int       interval  = (int) Math.ceil(getContentHeaders().size() / 10f);
        String          legitName = pluginName;
        if (pluginName.contains("_"))
        {
            legitName = pluginName.substring(0, pluginName.indexOf('_'));
        }
        String realName = legitName;
        
        for (int index = 0; index < getContentHeaders().size(); index++)
        {
            final int selfIndex = index;
            // executor.submit(() -> {
            
            WADContentHeaderV1 fileHeader = getContentHeaders().get(selfIndex);
            
            if (contentHeaders.size() > 500)
            {
                if (selfIndex % interval == 0)
                {
                    System.out.println(selfIndex + "/" + getContentHeaders().size());
                }
            }
            
            if (getHeader().getMajor() > 1 && ((WADContentHeaderV2) fileHeader).isDuplicate())
            {
                // "continue" if not executor
                //System.out.println("Duplicate file, skipping");
                continue;
            }
            
            try
            {
                saveFile(fileHeader, outputPath, realName);
            } catch (IOException e)
            {
                e.printStackTrace();
            }

//            });
        }
        try
        {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            fileReader.close();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    private void saveFile(WADContentHeaderV1 header, Path savePath, String pluginName) throws IOException
    {
        String hash     = String.format("%016X", header.getPathHash()).toLowerCase(Locale.ENGLISH);
        String filename = UtilHandler.getKnownFileHashes(pluginName).getOrDefault(hash, "unknown\\" + hash);
        Path   self     = savePath.resolve(filename);
        
        self.getParent().toFile().mkdirs();
        String parentName = self.getParent().getFileName().toString();
        byte[] data       = readContentFromHeaderData(header);
        
        
        if (filename.endsWith("json"))
        {
            data = makePretty(data);
        }
        
        if ("unknown".equals(parentName))
        {
            Files.write(savePath.resolve(unknownHashContainer), (hash + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            findFileTypeAndRename(self, data, filename, savePath);
        } else
        {
            Files.write(self, data);
        }
    }
    
    private synchronized byte[] readContentFromHeaderData(WADContentHeaderV1 header) throws IOException
    {
        fileReader.seek(header.getOffset());
        if (header.isCompressed())
        {
            byte[] fileBytes = fileReader.readBytes(header.getCompressedFileSize());
            if (header.getCompressed() == 1)
            {
                return CompressionHandler.uncompressGZIP(fileBytes);
            }
            
            if (header.getCompressed() == 2)
            {
                return fileBytes;
            }
            
            if (header.getCompressed() == 3)
            {
                return CompressionHandler.uncompressZSTD(fileBytes, header.getFileSize());
            }
            
            Files.write(Paths.get("unknown.file"), fileBytes);
            System.out.println("Found file with unknown compression!");
            return fileBytes;
        } else
        {
            return fileReader.readBytes(header.getFileSize());
        }
    }
    
    private String findFileType(Path self, byte[] data)
    {
        ByteArrayWrapper magic  = new ByteArrayWrapper(Arrays.copyOf(data, 4));
        String           result = UtilHandler.getMagicNumbers().get(magic);
        
        if (result != null)
        {
            return result;
        }
        
        if (UtilHandler.isProbableBOM(magic.getData()))
        {
            return findFileType(self, Arrays.copyOfRange(data, 3, 7));
        }
        
        if (UtilHandler.isProbableJSON(magic.getData()))
        {
            return "json";
        }
        
        if (UtilHandler.isProbableJavascript(magic.getData()))
        {
            return "js";
        }
        
        if (UtilHandler.isProbableHTML(magic.getData()))
        {
            return "html";
        }
        
        if (UtilHandler.isProbableCSS(magic.getData()))
        {
            return "css";
        }
        
        if (UtilHandler.isProbableTXT(magic.getData()))
        {
            return "txt";
        }
        
        if (UtilHandler.isProbableIDX(magic.getData()))
        {
            return "idx";
        }
        
        if (UtilHandler.isProbable3DModelStuff(magic.getData()))
        {
            return "unknown3DModelStuff";
        }
        
        System.out.print("Unknown filetype: ");
        System.out.print(self.toString());
        System.out.println(magic.toString());
        return "txt";
    }
    
    private byte[] makePretty(byte[] jsonString)
    {
        String      dataString = new String(jsonString, StandardCharsets.UTF_8);
        JsonElement obj        = new JsonParser().parse(dataString);
        String      pretty     = Utils.getGson().toJson(obj);
        return pretty.getBytes(StandardCharsets.UTF_8);
    }
    
    private void findFileTypeAndRename(Path self, byte[] data, String filename, Path parent) throws IOException
    {
        String        fileType = findFileType(self, data);
        StringBuilder sb       = new StringBuilder(filename).append(".").append(fileType);
        Path          other    = parent.resolve(sb.toString());
        
        Files.write(other, data);
    }
}
