package no.stelar7.cdragon.types.rman;

import com.google.gson.JsonObject;
import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class RMANParser implements Parseable<RMANFile>
{
    
    public enum RMANFileType
    {
        GAME, LCU
    }
    
    public static RMANFile loadFromCache(int version, RMANFileType type)
    {
        try
        {
            Path downloadPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            Path realPath     = downloadPath.resolveSibling(version + ".json");
            if (!Files.exists(realPath))
            {
                System.out.println("Manifest not in cache!!");
                return null;
            }
            
            String     patchManifest = String.join("\n", Files.readAllLines(realPath));
            JsonObject obj           = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject();
            
            Path usedManfest = null;
            switch (type)
            {
                case GAME:
                {
                    System.out.println("Downloading game manifest");
                    String url = obj.get("game_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    break;
                }
                
                case LCU:
                {
                    System.out.println("Downloading lcu manifest");
                    String url = obj.get("client_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    
                    break;
                }
            }
            
            System.out.println("Parsing...");
            return new RMANParser().parse(usedManfest);
            
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static RMANFile loadFromPBE(RMANFileType type)
    {
        try
        {
            Path downloadPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            Files.createDirectories(downloadPath.getParent());
            
            System.out.println("Downloading patcher manifest");
            String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
            //String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/macpbe-pbe-mac.json";
            WebHandler.downloadFile(downloadPath, patcherUrl);
            
            String     patchManifest = String.join("\n", Files.readAllLines(downloadPath));
            JsonObject obj           = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject();
            int        version       = obj.get("version").getAsInt();
            System.out.println("Found patch version " + version);
            
            Path realPath = downloadPath.resolveSibling(version + ".json");
            if (Files.exists(realPath))
            {
                System.out.println("Manifest already saved!");
                Files.deleteIfExists(downloadPath);
            } else
            {
                System.out.println("Saving file");
                Files.move(downloadPath, realPath);
            }
            
            Path usedManfest = null;
            switch (type)
            {
                case GAME:
                {
                    System.out.println("Downloading game manifest");
                    String url = obj.get("game_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    break;
                }
                
                case LCU:
                {
                    System.out.println("Downloading lcu manifest");
                    String url = obj.get("client_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    
                    break;
                }
            }
            
            System.out.println("Parsing...");
            return new RMANParser().parse(usedManfest);
            
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * the files list on this object only contains the files that have changed between this and last patch
     */
    public static RMANFile loadFromPBEIgnoreOld(RMANFileType type)
    {
        try
        {
            Path downloadPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            
            System.out.println("Downloading patcher manifest");
            String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
            WebHandler.downloadFile(downloadPath, patcherUrl);
            
            String     patchManifest = String.join("\n", Files.readAllLines(downloadPath));
            JsonObject obj           = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject();
            int        version       = obj.get("version").getAsInt();
            System.out.println("Found patch version " + version);
            
            Path realPath = downloadPath.resolveSibling(version + ".json");
            if (Files.exists(realPath))
            {
                System.out.println("Manifest already saved!");
                Files.deleteIfExists(downloadPath);
            } else
            {
                System.out.println("Saving file");
                Files.move(downloadPath, realPath);
            }
            
            Path usedManfest = null;
            switch (type)
            {
                case GAME:
                {
                    System.out.println("Downloading game manifest");
                    String url = obj.get("game_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    break;
                }
                
                case LCU:
                {
                    System.out.println("Downloading lcu manifest");
                    String url = obj.get("client_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    break;
                }
            }
            
            System.out.println("Parsing...");
            RMANFile newFile = new RMANParser().parse(usedManfest);
            RMANFile oldFile = RMANParser.loadFromCache(version - 1, type);
            if (oldFile != null)
            {
                System.out.println("Found version " + (version - 1) + " in cache");
                List<RMANFileBodyFile> oldFiles = newFile.getChangedFiles(oldFile);
                newFile.getBody().getFiles().removeIf(nfif -> oldFiles.stream().map(RMANFileBodyFile::getFileId).noneMatch(i -> nfif.getFileId() == i));
                System.out.println();
            }
            
            return newFile;
            
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public RMANFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getData(), ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(RandomAccessReader raf)
    {
        RMANFile file = new RMANFile();
        file.setHeader(parseHeader(raf));
        
        raf.seek(file.getHeader().getOffset());
        file.setCompressedBody(raf.readBytes(file.getHeader().getLength()));
        
        if (file.getHeader().getSignatureType() != 0)
        {
            file.setSignature(raf.readBytes(256));
        }
        
        file.setBody(parseCompressedBody(file));
        file.buildChunkMap();
        file.buildBundleMap();
        return file;
    }
    
    private RMANFileBody parseCompressedBody(RMANFile file)
    {
        byte[]             uncompressed = CompressionHandler.uncompressZSTD(file.getCompressedBody(), file.getHeader().getDecompressedLength());
        RandomAccessReader raf          = new RandomAccessReader(uncompressed, ByteOrder.LITTLE_ENDIAN);
        RMANFileBody       body         = new RMANFileBody();
        body.setHeaderOffset(raf.readInt());
        raf.seek(body.getHeaderOffset());
        
        RMANFileBodyHeader header = new RMANFileBodyHeader();
        header.setOffsetTableOffset(raf.readInt());
        
        int offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setBundleListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setLanguageListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setFileListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setFolderListOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setKeyHeaderOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        offset = raf.readInt();
        raf.seek(raf.pos() + offset);
        header.setUnknownOffset(raf.pos() - 4);
        raf.seek(raf.pos() - offset);
        
        body.setHeader(header);
        body.setBundles(parseBundles(raf, header));
        body.setLanguages(parseLanguages(raf, header));
        body.setFiles(parseFiles(raf, header));
        body.setDirectories(parseDirectories(raf, header));
        
        return body;
    }
    
    private List<RMANFileBodyDirectory> parseDirectories(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyDirectory> data = new ArrayList<>();
        raf.seek(header.getFolderListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyDirectory dir = new RMANFileBodyDirectory();
            
            dir.setOffset(raf.readInt());
            int nextFileOffset = raf.pos();
            raf.seek(nextFileOffset + dir.getOffset() - 4);
            
            dir.setOffsetTableOffset(raf.readInt());
            int resumeOffset = raf.pos();
            raf.seek(raf.pos() - dir.getOffsetTableOffset());
            dir.setDirectoryIdOffset(raf.readShort());
            dir.setParentIdOffset(raf.readShort());
            raf.seek(resumeOffset);
            dir.setNameOffset(raf.readInt());
            raf.seek(raf.pos() + dir.getNameOffset() - 4);
            dir.setName(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + dir.getOffset() + 4);
            
            if (dir.getDirectoryIdOffset() > 0)
            {
                dir.setDirectoryId(raf.readLong());
            }
            
            if (dir.getParentIdOffset() > 0)
            {
                dir.setParentId(raf.readLong());
            }
            
            raf.seek(nextFileOffset);
            data.add(dir);
        }
        
        return data;
    }
    
    private List<RMANFileBodyFile> parseFiles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyFile> data = new ArrayList<>();
        raf.seek(header.getFileListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyFile bfile = new RMANFileBodyFile();
            
            bfile.setOffset(raf.readInt());
            int nextFileOffset = raf.pos();
            raf.seek(nextFileOffset + bfile.getOffset() - 4);
            
            bfile.setOffsetTableOffset(raf.readInt());
            
            int tempA         = raf.readInt();
            int restoreOffset = 4;
            
            bfile.setCustomNameOffset(tempA & 0xFFFFFF);
            bfile.setFiletypeFlag(tempA >> 24);
            boolean hasInlineName = raf.readInt() < 100;
            raf.seek(raf.pos() - 4);
            if (hasInlineName)
            {
                bfile.setNameOffset(bfile.getCustomNameOffset());
            } else
            {
                bfile.setNameOffset(raf.readInt());
                restoreOffset = 8;
            }
            
            raf.seek(raf.pos() + bfile.getNameOffset() - 4);
            bfile.setName(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + bfile.getOffset() + restoreOffset);
            
            bfile.setStructSize(raf.readInt());
            
            bfile.setSymlinkOffset(raf.readInt());
            raf.seek(raf.pos() + bfile.getSymlinkOffset() - 4);
            bfile.setSymlink(raf.readString(raf.readInt()));
            raf.seek(nextFileOffset + bfile.getOffset() + 8 + restoreOffset);
            
            bfile.setFileId(raf.readLong());
            
            if (bfile.getStructSize() > 28)
            {
                bfile.setDirectoryId(raf.readLong());
            }
            
            bfile.setFileSize(raf.readInt());
            bfile.setPermissions(raf.readInt());
            
            if (bfile.getStructSize() > 36)
            {
                bfile.setLanguageId(raf.readInt());
                bfile.setUnknown2(raf.readInt());
            }
            
            
            bfile.setSingleChunk(raf.readInt());
            if (!bfile.isSingleChunk())
            {
                bfile.setChunkIds(raf.readLongs(raf.readInt()));
            } else
            {
                bfile.setChunkIds(Collections.singletonList(raf.readLong()));
                bfile.setUnknown3(raf.readInt());
            }
            
            raf.seek(nextFileOffset);
            data.add(bfile);
        }
        
        return data;
    }
    
    private List<RMANFileBodyLanguage> parseLanguages(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyLanguage> data = new ArrayList<>();
        raf.seek(header.getLanguageListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyLanguage language = new RMANFileBodyLanguage();
            
            language.setOffset(raf.readInt());
            int nextLanguageOffset = raf.pos();
            raf.seek(nextLanguageOffset + language.getOffset() - 4);
            
            language.setOffsetTableOffset(raf.readInt());
            language.setId(raf.readInt());
            language.setNameOffset(raf.readInt());
            raf.seek(raf.pos() + language.getNameOffset() - 4);
            language.setName(raf.readString(raf.readInt()));
            
            raf.seek(nextLanguageOffset);
            data.add(language);
        }
        
        return data;
    }
    
    private List<RMANFileBodyBundle> parseBundles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyBundle> data = new ArrayList<>();
        raf.seek(header.getBundleListOffset());
        
        int count = raf.readInt();
        for (int i = 0; i < count; i++)
        {
            RMANFileBodyBundle bundle = new RMANFileBodyBundle();
            
            bundle.setOffset(raf.readInt());
            int nextBundleOffset = raf.pos();
            raf.seek(nextBundleOffset + bundle.getOffset() - 4);
            
            bundle.setOffsetTableOffset(raf.readInt());
            bundle.setHeaderSize(raf.readInt());
            bundle.setBundleId(HashHandler.toHex(raf.readLong(), 16));
            if (bundle.getHeaderSize() > 12)
            {
                bundle.setSkipped(raf.readBytes(bundle.getHeaderSize() - 12));
            }
            
            List<RMANFileBodyBundleChunk> chunks     = new ArrayList<>();
            int                           chunkCount = raf.readInt();
            for (int j = 0; j < chunkCount; j++)
            {
                int chunkOffset     = raf.readInt();
                int nextChunkOffset = raf.pos();
                raf.seek(chunkOffset + nextChunkOffset - 4);
                
                RMANFileBodyBundleChunk chunk = new RMANFileBodyBundleChunk();
                chunk.setOffsetTableOffset(raf.readInt());
                chunk.setCompressedSize(raf.readInt());
                chunk.setUncompressedSize(raf.readInt());
                chunk.setChunkId(HashHandler.toHex(raf.readLong(), 16));
                chunks.add(chunk);
                
                raf.seek(nextChunkOffset);
            }
            
            bundle.setChunks(chunks);
            raf.seek(nextBundleOffset);
            data.add(bundle);
        }
        
        return data;
    }
    
    private RMANFileHeader parseHeader(RandomAccessReader raf)
    {
        RMANFileHeader header = new RMANFileHeader();
        header.setMagic(raf.readString(4));
        header.setMajor(raf.readByte());
        header.setMinor(raf.readByte());
        header.setUnknown(raf.readByte());
        header.setSignatureType(raf.readByte());
        header.setOffset(raf.readInt());
        header.setLength(raf.readInt());
        header.setManifestId(raf.readLong());
        header.setDecompressedLength(raf.readInt());
        return header;
    }
}