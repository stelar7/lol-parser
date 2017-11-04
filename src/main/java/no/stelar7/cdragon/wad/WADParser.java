package no.stelar7.cdragon.wad;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sf.jmimemagic.*;
import no.stelar7.cdragon.wad.data.WADFile;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

public class WADParser
{
    
    private static Map<String, String> hashName;
    
    static
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            
            try (InputStream is = new URL("https://raw.githubusercontent.com/CommunityDragon/RADS-CDragon/Python-Version/wad_parser/hashes.json").openConnection().getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                 Stream<String> stream = reader.lines())
            {
                stream.forEach(sb::append);
            }
            
            hashName = new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Downloads and parses the latest WAD file;
     * if the file already exists, it parses that file
     *
     * @param path path to store the file
     * @return WADFile
     * @throws Exception yes :kappa:
     */
    public WADFile parseLatest(Path path) throws Exception
    {
        if (Files.exists(path))
        {
            return parse(path);
        }
        
        for (int i = 150; i > 50; i--)
        {
            String            url = String.format("http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/0.0.1.%s/files/Plugins/rcp-be-lol-game-data/default-assets.wad.compressed", i);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (con.getResponseCode() == 200)
            {
                System.out.println("Downloading file: " + url);
                ReadableByteChannel rbc = Channels.newChannel(new URL(url).openStream());
                FileOutputStream    fos = new FileOutputStream(path.toFile());
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                return parse(Paths.get("default-assets.wad.compressed"));
            }
        }
        return null;
    }
    
    /**
     * Parses the specified WAD file
     *
     * @param path path to the file
     * @return WADFile
     * @throws Exception yes :kappa:
     */
    public WADFile parse(Path path) throws Exception
    {
        String filename = path.getFileName().toString();
        if (filename.endsWith(".compressed"))
        {
            filename = filename.substring(0, filename.lastIndexOf(".compressed"));
            Path uncompressPath = path.getParent().resolve(filename);
            Files.write(uncompressPath, uncompressDEFLATE(Files.readAllBytes(path)));
            return parse(uncompressPath);
        }
        
        WADFile          file = new WADFile();
        RandomAccessFile raf  = new RandomAccessFile(path.toFile(), "r");
        
        file.setHeader(parseHeader(raf));
        file.setFileHeaders(parseContent(raf, file));
        
        extractFiles(path, raf, file);
        
        raf.close();
        
        return file;
        
    }
    
    private void extractFiles(Path path, final RandomAccessFile raf, final WADFile file)
    {
        for (int index = 0; index < file.getFileHeaders().size(); index++)
        {
            
            if (index % 100 == 0)
            {
                System.out.println(index + "/" + file.getFileHeaders().size());
            }
            
            
            WADContentHeaderV1 header = file.getFileHeaders().get(index);
            
            if (file.getHeader().getMajor() > 1 && isDuplicate((WADContentHeaderV2) header))
            {
                continue;
            }
            
            saveFile(raf, header, path);
        }
    }
    
    private boolean isDuplicate(WADContentHeaderV2 header)
    {
        return header.getDuplicate() > 0;
    }
    
    private void saveFile(RandomAccessFile raf, WADContentHeaderV1 header, Path path)
    {
        try
        {
            String hash     = Long.toUnsignedString(header.getPathHash(), 16);
            String filename = hashName.getOrDefault(hash, "unknown/" + hash);
            Path   self;
            byte[] data;
            
            raf.seek(header.getOffset());
            
            if (header.getCompressed() > 0)
            {
                data = uncompressGZIP(readBytes(raf, (int) header.getCompressedFileSize()));
            } else
            {
                data = readBytes(raf, (int) header.getFileSize());
            }
            
            String addMe  = filename.startsWith("/") ? ("." + filename) : filename;
            Path   parent = path.getParent();
            self = parent.resolve(addMe).normalize();
            self.getParent().toFile().mkdirs();
            Files.write(self, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            
            if ("unknown".equals(self.getParent().getFileName().toString()))
            {
                findFileTypeAndRename(self, data, addMe, parent);
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void findFileTypeAndRename(Path self, byte[] data, String filename, Path parent)
    {
        try
        {
            StringBuilder sb = new StringBuilder(filename);
            
            MagicMatch match = Magic.getMagicMatch(data);
            if (match != null)
            {
                sb.append(".");
                if (!match.getExtension().isEmpty())
                {
                    sb.append(match.getExtension());
                } else
                {
                    // JMimeMagic can find _most_ types, but not newer ones, so we check the magic number for those
                    RandomAccessFile raf2  = new RandomAccessFile(self.toFile(), "r");
                    byte[]           magic = readBytes(raf2, 4);
                    raf2.close();
                    
                    byte[] oggMagic  = new byte[]{79, 103, 103, 83};
                    byte[] webmMagic = new byte[]{26, 69, -33, -93};
                    byte[] ddsMagic  = new byte[]{68, 68, 83, 32};
                    
                    if (Arrays.equals(magic, oggMagic))
                    {
                        sb.append("ogg");
                    } else if (Arrays.equals(magic, webmMagic))
                    {
                        sb.append("webm");
                    } else if (Arrays.equals(magic, ddsMagic))
                    {
                        sb.append("dds");
                    } else
                    {
                        sb.append("txt");
                    }
                }
                
                Path other = parent.resolve(sb.toString()).normalize();
                Files.move(self, other, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException | IOException e)
        {
            System.err.println("Magic didnt find extension for hash: " + filename + ", you should try with FILE");
            e.printStackTrace();
        }
    }
    
    private List<WADContentHeaderV1> parseContent(RandomAccessFile raf, WADFile file) throws Exception
    {
        List<WADContentHeaderV1> content = new ArrayList<>();
        
        
        Field field = WADHeaderBase.class.getDeclaredField("fileCount");
        field.setAccessible(true);
        long fileCount = (long) field.get(file.getHeader());
        
        for (int i = 0; i < fileCount; i++)
        {
            WADContentHeaderV1 header = new WADContentHeaderV1();
            header.setPathHash(readULong(raf));
            header.setOffset(readUInt(raf));
            header.setCompressedFileSize(readUInt(raf));
            header.setFileSize(readUInt(raf));
            header.setCompressed(readUByte(raf));
            
            if (file.getHeader().getMajor() == 2 || file.getHeader().getMajor() == 3)
            {
                WADContentHeaderV2 headerv2 = new WADContentHeaderV2(header);
                headerv2.setDuplicate(readUByte(raf));
                headerv2.setUnknown(readUByte(raf));
                headerv2.setUnknown2(readUByte(raf));
                headerv2.setSha256(readULong(raf));
                
                content.add(headerv2);
                continue;
            }
            
            content.add(header);
        }
        
        return content;
    }
    
    /**
     * Parse the WAD header by version number
     *
     * @param raf wad file
     * @return WADHeaderBase
     */
    
    private WADHeaderBase parseHeader(RandomAccessFile raf) throws IOException
    {
        WADHeaderBase base = new WADHeaderBase();
        base.setMagic(readString(raf, 2));
        base.setMajor(readUByte(raf));
        base.setMinor(readUByte(raf));
        
        switch (base.getMajor())
        {
            case 1:
            {
                WADHeaderV1 head = new WADHeaderV1(base);
                head.setEntryHeaderOffset(readUShort(raf));
                head.setEntryHeaderCellSize(readUShort(raf));
                head.setFileCount(readUInt(raf));
                return head;
            }
            case 2:
            {
                WADHeaderV2 head = new WADHeaderV2(base);
                head.setECDSALength(readUByte(raf));
                head.setECDSA(readBytes(raf, head.getECDSALength()));
                head.setECDSAPadding(readBytes(raf, 83 - head.getECDSALength()));
                head.setFileChecksum(readULong(raf));
                head.setEntryHeaderOffset(readUShort(raf));
                head.setEntryHeaderCellSize(readUShort(raf));
                head.setFileCount(readUInt(raf));
                return head;
            }
            case 3:
            {
                WADHeaderV3 head = new WADHeaderV3(base);
                head.setECDSA(readBytes(raf, 256));
                head.setChecksum(readULong(raf));
                head.setFileCount(readUInt(raf));
                return head;
            }
            default:
                throw new RuntimeException("Invalid major version! " + base.getMajor());
        }
    }
    
    private long readULong(RandomAccessFile raf) throws IOException
    {
        return Long.reverseBytes(raf.readLong());
    }
    
    private byte[] readBytes(RandomAccessFile raf, int length) throws IOException
    {
        byte[] tempData = new byte[length];
        raf.readFully(tempData, 0, length);
        return Arrays.copyOf(tempData, length);
    }
    
    private long readUInt(RandomAccessFile raf) throws IOException
    {
        return Integer.reverseBytes(raf.readInt());
    }
    
    private int readUShort(RandomAccessFile raf) throws IOException
    {
        return Short.reverseBytes(raf.readShort());
    }
    
    private int readUByte(RandomAccessFile raf) throws IOException
    {
        return raf.readUnsignedByte();
    }
    
    private byte[] uncompressGZIP(byte[] data)
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream os = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[65535];
            
            int length;
            while ((length = gis.read(buffer)) != -1)
            {
                os.write(buffer, 0, length);
            }
            os.flush();
            return os.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private byte[] uncompressDEFLATE(byte[] data)
    {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             InflaterInputStream gis = new InflaterInputStream(bis);
             ByteArrayOutputStream os = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[65535];
            
            int length;
            while ((length = gis.read(buffer)) != -1)
            {
                os.write(buffer, 0, length);
            }
            os.flush();
            return os.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    
    private String readString(RandomAccessFile raf, int length) throws IOException
    {
        byte[] tempData = new byte[65535];
        raf.read(tempData, 0, length);
        return new String(tempData, StandardCharsets.UTF_8).trim();
    }
}
