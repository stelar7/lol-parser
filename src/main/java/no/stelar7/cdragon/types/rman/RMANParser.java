package no.stelar7.cdragon.types.rman;

import com.google.gson.*;
import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
            Path downloadPath = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            Path realPath     = downloadPath.resolveSibling(version + ".json");
            if (!Files.exists(realPath))
            {
                System.out.println("Manifest not in cache!!");
                return null;
            }
            
            String     patchManifest = String.join("\n", Files.readAllLines(realPath));
            JsonObject obj           = JsonParser.parseString(patchManifest).getAsJsonObject();
            
            Path usedManfest = null;
            switch (type)
            {
                case GAME ->
                {
                    System.out.println("Downloading game manifest");
                    String url = obj.get("game_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                }
                case LCU ->
                {
                    System.out.println("Downloading lcu manifest");
                    String url = obj.get("client_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                    
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
    
    public static Long getSieveVersion()
    {
        Pair<String, String> urls = getPBEManifestFromSieve();
        return Long.parseLong(urls.getA());
    }
    
    public static List<RMANFile> getSieveManifests()
    {
        List<RMANFile>       files      = new ArrayList<>();
        Pair<String, String> gameData   = getPBEManifestFromSieve();
        String               clientData = getPBEManifestFromClientConfig();
        RMANParser           parser     = new RMANParser();
        
        String version = gameData.getA();
        String url     = gameData.getB();
        
        System.out.println("Downloading manifest " + version);
        Path usedManfest = UtilHandler.CDRAGON_FOLDER.resolve("cdragon").resolve("patcher").resolve("manifests").resolve("sieve\\" + version + "-game.rman");
        System.out.println(usedManfest);
        WebHandler.downloadFile(usedManfest, url);
        files.add(parser.parse(usedManfest));
        
        url = clientData;
        usedManfest = UtilHandler.CDRAGON_FOLDER.resolve("cdragon").resolve("patcher").resolve("manifests").resolve("sieve\\" + version + "-client.rman");
        System.out.println(usedManfest);
        WebHandler.downloadFile(usedManfest, url);
        files.add(parser.parse(usedManfest));
        
        return files;
    }
    
    public static String getPBEManifestFromClientConfig()
    {
        System.out.println("Getting PBE manifest from client config");
        String     url       = "https://clientconfig.rpg.riotgames.com/api/v1/config/public";
        String     content   = String.join("\n", WebHandler.readWeb(url));
        JsonObject obj       = (JsonObject) JsonParser.parseString(content);
        JsonObject patchline = obj.getAsJsonObject("keystone.products.league_of_legends.patchlines.pbe");
        JsonObject windows   = patchline.getAsJsonObject("platforms").getAsJsonObject("win");
        JsonObject config    = (JsonObject) windows.getAsJsonArray("configurations").get(0);
        String     patchUrl  = config.get("patch_url").getAsString();
        return patchUrl;
    }
    
    public static Pair<String, String> getPBEManifestFromSieve()
    {
        System.out.println("Getting PBE manifest from sieve");
        String     url      = "https://sieve.services.riotcdn.net/api/v1/products/lol/version-sets/PBE1?q[platform]=windows";
        String     content  = String.join("\n", WebHandler.readWeb(url));
        JsonObject obj      = (JsonObject) JsonParser.parseString(content);
        JsonArray  releases = obj.getAsJsonArray("releases");
        
        final long[]                            maxVersion      = {0};
        Map<String, List<Pair<String, String>>> patchToManifest = new HashMap<>();
        releases.forEach(e -> {
            String type     = e.getAsJsonObject().getAsJsonObject("release").getAsJsonObject("labels").getAsJsonObject("riot:artifact_type_id").getAsJsonArray("values").get(0).getAsString();
            String version  = e.getAsJsonObject().getAsJsonObject("compat_version").get("id").getAsString();
            String manifest = e.getAsJsonObject().getAsJsonObject("download").get("url").getAsString();
            
            long intVersion = Long.parseLong(version.split("\\+")[0].replace(".", ""));
            if (intVersion > maxVersion[0])
            {
                maxVersion[0] = intVersion;
            }
            
            patchToManifest.putIfAbsent(version, new ArrayList<>());
            patchToManifest.get(version).add(new Pair<>(type, manifest));
        });
        
        AtomicReference<Pair<String, String>> target = new AtomicReference<>();
        patchToManifest.forEach((k, v) -> {
            if (k.contains("releasedbg"))
            {
                return;
            }
            
            long intVersion = Long.parseLong(k.split("\\+")[0].replace(".", ""));
            if (intVersion == maxVersion[0])
            {
                v.forEach(val -> {
                    if (val.getA().equalsIgnoreCase("lol-game-client"))
                    {
                        target.set(new Pair<>(String.valueOf(intVersion), val.getB()));
                    }
                });
            }
        });
        
        return target.get();
    }
    
    
    public static JsonObject getPBEManifest()
    {
        try
        {
            Path downloadPath = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            Files.createDirectories(downloadPath.getParent());
            
            System.out.println("Downloading patcher manifest");
            //String patcherUrl = "https://ks-foundation.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
            //String patcherUrl = "https://sieve.services.riotcdn.net/api/v1/products/lol/version-sets/PBE1?q[artifact_type_id]=lol-game-client&q[platform]=windows&q[published]=true";
            String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
            //String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/macpbe-pbe-mac.json";
            WebHandler.downloadFile(downloadPath, patcherUrl);
            
            String     patchManifest = String.join("\n", Files.readAllLines(downloadPath));
            JsonObject obj           = JsonParser.parseString(patchManifest).getAsJsonObject();
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
            
            obj.addProperty("localsavepath", realPath.toString());
            return obj;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static RMANFile loadFromPBE(JsonObject obj, RMANFileType type)
    {
        Path   downloadPath = Paths.get(obj.get("localsavepath").getAsString());
        String version      = obj.get("version").getAsString();
        
        Path usedManfest = null;
        switch (type)
        {
            case GAME ->
            {
                System.out.println("Downloading game manifest");
                String url = obj.get("game_patch_url").getAsString();
                usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                WebHandler.downloadFile(usedManfest, url);
            }
            case LCU ->
            {
                System.out.println("Downloading lcu manifest");
                String url = obj.get("client_patch_url").getAsString();
                usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                WebHandler.downloadFile(usedManfest, url);
                
            }
        }
        
        System.out.println("Parsing...");
        return new RMANParser().parse(usedManfest);
    }
    
    /**
     * the files list on this object only contains the files that have changed between this and last patch
     */
    public static RMANFile loadFromPBEIgnoreOld(RMANFileType type)
    {
        try
        {
            Path downloadPath = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\patcher\\manifests").resolve(UUID.randomUUID().toString());
            
            System.out.println("Downloading patcher manifest");
            String patcherUrl = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
            WebHandler.downloadFile(downloadPath, patcherUrl);
            
            String     patchManifest = String.join("\n", Files.readAllLines(downloadPath));
            JsonObject obj           = JsonParser.parseString(patchManifest).getAsJsonObject();
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
                case GAME ->
                {
                    System.out.println("Downloading game manifest");
                    String url = obj.get("game_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("game\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
                }
                case LCU ->
                {
                    System.out.println("Downloading lcu manifest");
                    String url = obj.get("client_patch_url").getAsString();
                    usedManfest = downloadPath.resolveSibling("lcu\\" + version + ".rman");
                    WebHandler.downloadFile(usedManfest, url);
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
    
    public static RMANFile getFromURL(String url)
    {
        ByteArrayOutputStream stream = WebHandler.downloadFileToMemory(url);
        ByteArray             data   = new ByteArray(stream.toByteArray());
        
        return new RMANParser().parse(data);
    }
    
    @Override
    public RMANFile parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public RMANFile parse(ByteArray data)
    {
        return parse(new RandomAccessReader(data.getDataRaw(), ByteOrder.LITTLE_ENDIAN));
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
        
        header.setBundleListOffset(raf.pos() + raf.readInt());
        header.setLanguageListOffset(raf.pos() + raf.readInt());
        header.setFileListOffset(raf.pos() + raf.readInt());
        header.setFolderListOffset(raf.pos() + raf.readInt());
        header.setKeyHeaderOffset(raf.pos() + raf.readInt());
        header.setUnknownOffset(raf.pos() + raf.readInt());
        
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
        
        List<Map<String, Object>> unmappedDirectories = RMANOffsetTable.parseOffsetTable(raf, RMANVTable.getDirectoryFields(), RMANVTable::parseVTable);
        for (Map<String, Object> entry : unmappedDirectories)
        {
            RMANFileBodyDirectory dir = new RMANFileBodyDirectory();
            
            dir.setDirectoryId((Long) entry.get("directory_id"));
            dir.setParentId((Long) entry.get("parent_id"));
            dir.setName((String) entry.get("name"));
            
            data.add(dir);
        }
        
        return data;
    }
    
    private List<RMANFileBodyFile> parseFiles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyFile> data = new ArrayList<>();
        raf.seek(header.getFileListOffset());
        
        List<Map<String, Object>> unmappedDirectories = RMANOffsetTable.parseOffsetTable(raf, RMANVTable.getFileFields(), RMANVTable::parseVTable);
        for (Map<String, Object> entry : unmappedDirectories)
        {
            RMANFileBodyFile file = new RMANFileBodyFile();
            
            file.setFileId((Long) entry.get("file_id"));
            file.setDirectoryId((Long) entry.get("directory_id"));
            file.setFileSize((Integer) entry.get("file_size"));
            file.setName((String) entry.get("name"));
            file.setSymlink((String) entry.get("symlink"));
            file.setChunkIds((List<Long>) entry.get("chunks"));
            
            data.add(file);
        }
        
        return data;
    }
    
    private List<RMANFileBodyLanguage> parseLanguages(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyLanguage> data = new ArrayList<>();
        raf.seek(header.getLanguageListOffset());
        
        List<Map<String, Object>> unmappedLanguages = RMANOffsetTable.parseOffsetTable(raf, RMANVTable.getLanguageFields(), RMANVTable::parseVTable);
        for (Map<String, Object> entry : unmappedLanguages)
        {
            RMANFileBodyLanguage dir = new RMANFileBodyLanguage();
            
            dir.setId((Integer) entry.get("language_id"));
            dir.setName((String) entry.get("name"));
            
            data.add(dir);
        }
        
        return data;
    }
    
    private List<RMANFileBodyBundle> parseBundles(RandomAccessReader raf, RMANFileBodyHeader header)
    {
        List<RMANFileBodyBundle> data = new ArrayList<>();
        raf.seek(header.getBundleListOffset());
        
        List<Map<String, Object>> unmappedBundles = RMANOffsetTable.parseOffsetTable(raf, RMANVTable.getBundleFields(), RMANVTable::parseVTable);
        for (Map<String, Object> entry : unmappedBundles)
        {
            RMANFileBodyBundle bundle = new RMANFileBodyBundle();
            bundle.setBundleId(HashHandler.toHex((Long) entry.get("bundle_id"), 16));
            
            List<RMANFileBodyBundleChunk> chunks = new ArrayList<>();
            raf.seek((Integer) entry.get("chunks_offset"));
            List<Map<String, Object>> unmappedChunks = RMANOffsetTable.parseOffsetTable(raf, RMANVTable.getChunkFields(), RMANVTable::parseVTable);
            for (Map<String, Object> chunkInfo : unmappedChunks)
            {
                RMANFileBodyBundleChunk chunk = new RMANFileBodyBundleChunk();
                chunk.setCompressedSize((Integer) chunkInfo.get("compressed_size"));
                chunk.setUncompressedSize((Integer) chunkInfo.get("uncompressed_size"));
                chunk.setChunkId(HashHandler.toHex((Long) chunkInfo.get("chunk_id"), 16));
                chunks.add(chunk);
            }
            
            bundle.setChunks(chunks);
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