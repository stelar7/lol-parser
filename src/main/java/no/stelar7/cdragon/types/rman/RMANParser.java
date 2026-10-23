package no.stelar7.cdragon.types.rman;

import com.google.gson.*;
import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;
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
                Set<Long> changedIds = new HashSet<>();
                newFile.getChangedFiles(oldFile).forEach(f -> changedIds.add(f.getFileId()));
                newFile.getBody().getFiles().removeIf(nfif -> !changedIds.contains(nfif.getFileId()));
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
    public RMANFile parse(RandomAccessReader raf)
    {
        raf.setEndian(ByteOrder.LITTLE_ENDIAN);

        RMANFileHeader header = parseHeader(raf);

        long bodyOffset = Integer.toUnsignedLong(header.getOffset());
        long bodyLength = Integer.toUnsignedLong(header.getLength());
        if (bodyOffset + bodyLength > raf.size())
        {
            throw new ParseException("RMAN body (offset " + bodyOffset + ", length " + bodyLength + ") is outside the file", raf);
        }

        raf.seek(bodyOffset);
        byte[] compressedBody = raf.readBytes(bodyLength);

        // the signature follows the body
        byte[] signature = null;
        if (header.getSignatureType() != 0)
        {
            signature = raf.readBytes(256);
        }

        RMANFile file = new RMANFile(header, parseCompressedBody(header, compressedBody), compressedBody, signature);
        file.buildChunkMap();
        file.buildBundleMap();

        return file;
    }

    private RMANFileBody parseCompressedBody(RMANFileHeader fileHeader, byte[] compressedBody)
    {
        byte[] uncompressed = CompressionHandler.uncompressZSTD(compressedBody, fileHeader.getDecompressedLength());
        if (uncompressed == null)
        {
            throw new ParseException("Failed to decompress RMAN body", fileHeader.getOffset());
        }

        RandomAccessReader raf = new RandomAccessReader(uncompressed, ByteOrder.LITTLE_ENDIAN);

        // FlatBuffers root: u32 offset to the root table
        int headerOffset = raf.readInt();
        raf.seek(Integer.toUnsignedLong(headerOffset));
        RMANVTable.Table root = RMANVTable.Table.read(raf);

        RMANFileBodyHeader header = new RMANFileBodyHeader(root.getVTableOffset(),
                                                           (int) root.offset(RMANVTable.BODY_BUNDLES),
                                                           (int) root.offset(RMANVTable.BODY_LANGUAGES),
                                                           (int) root.offset(RMANVTable.BODY_FILES),
                                                           (int) root.offset(RMANVTable.BODY_DIRECTORIES),
                                                           (int) root.offset(RMANVTable.BODY_KEYS),
                                                           (int) root.offset(RMANVTable.BODY_UNKNOWN));

        List<RMANFileBodyBundle>    bundles     = parseList(raf, header.getBundleListOffset(), RMANParser::parseBundle);
        List<RMANFileBodyLanguage>  languages   = parseList(raf, header.getLanguageListOffset(), RMANParser::parseLanguage);
        List<RMANFileBodyFile>      files       = parseList(raf, header.getFileListOffset(), RMANParser::parseFile);
        List<RMANFileBodyDirectory> directories = parseList(raf, header.getFolderListOffset(), RMANParser::parseDirectory);

        return new RMANFileBody(headerOffset, header, bundles, languages, files, directories);
    }

    /**
     * Parses a vector of tables, an absent vector (offset -1) is an empty list
     */
    private static <T> List<T> parseList(RandomAccessReader raf, long offset, java.util.function.Function<RandomAccessReader, T> parser)
    {
        if (offset < 0)
        {
            return new ArrayList<>();
        }

        raf.seek(offset);
        return RMANOffsetTable.parseOffsetTable(raf, parser);
    }

    private static RMANFileBodyDirectory parseDirectory(RandomAccessReader raf)
    {
        RMANVTable.Table table = RMANVTable.Table.read(raf);

        long   directoryId = table.u64(RMANVTable.DIRECTORY_ID, 0);
        long   parentId    = table.u64(RMANVTable.DIRECTORY_PARENT_ID, 0);
        String name        = table.string(RMANVTable.DIRECTORY_NAME, "");

        return new RMANFileBodyDirectory(name, directoryId, parentId);
    }

    private static RMANFileBodyFile parseFile(RandomAccessReader raf)
    {
        RMANVTable.Table table = RMANVTable.Table.read(raf);

        long       fileId        = table.u64(RMANVTable.FILE_ID, 0);
        long       directoryId   = table.u64(RMANVTable.FILE_DIRECTORY_ID, 0);
        long       fileSize      = table.u64(RMANVTable.FILE_SIZE, 0);
        String     name          = table.string(RMANVTable.FILE_NAME, "");
        long       languageFlags = table.u64(RMANVTable.FILE_LANGUAGES, 0);
        List<Long> chunkIds      = table.u64Vector(RMANVTable.FILE_CHUNKS);
        String     symlink       = table.string(RMANVTable.FILE_SYMLINK, "");
        int        permissions   = table.u8(RMANVTable.FILE_PERMISSIONS, 0);

        return new RMANFileBodyFile(name, symlink, fileId, directoryId, fileSize, permissions, languageFlags, chunkIds);
    }

    private static RMANFileBodyLanguage parseLanguage(RandomAccessReader raf)
    {
        RMANVTable.Table table = RMANVTable.Table.read(raf);

        int    id   = table.u8(RMANVTable.LANGUAGE_ID, 0);
        String name = table.string(RMANVTable.LANGUAGE_NAME, "");

        return new RMANFileBodyLanguage(id, name);
    }

    private static RMANFileBodyBundle parseBundle(RandomAccessReader raf)
    {
        RMANVTable.Table table    = RMANVTable.Table.read(raf);
        String           bundleId = HashHandler.toHex(table.u64(RMANVTable.BUNDLE_ID, 0), 16);
        return new RMANFileBodyBundle(bundleId, parseList(raf, table.offset(RMANVTable.BUNDLE_CHUNKS), RMANParser::parseChunk));
    }

    private static RMANFileBodyBundleChunk parseChunk(RandomAccessReader raf)
    {
        RMANVTable.Table table            = RMANVTable.Table.read(raf);
        long             chunkId          = table.u64(RMANVTable.CHUNK_ID, 0);
        long             compressedSize   = table.u32(RMANVTable.CHUNK_COMPRESSED_SIZE, 0);
        long             uncompressedSize = table.u32(RMANVTable.CHUNK_UNCOMPRESSED_SIZE, 0);
        return new RMANFileBodyBundleChunk(compressedSize, uncompressedSize, chunkId);
    }

    /**
     * u32 magic "RMAN", u8 major, u8 minor, u16 flags (we split it into unknown + signatureType), u32 body offset, u32 body length, u64 manifest id, u32 uncompressed body length
     */
    private RMANFileHeader parseHeader(RandomAccessReader raf)
    {
        if (raf.remaining() < 28)
        {
            throw new ParseException("File is too small to be a RMAN", raf);
        }

        String magic = raf.readString(4);
        byte   major = raf.readByte();
        byte   minor = raf.readByte();

        if (!"RMAN".equals(magic))
        {
            throw new ParseException("Invalid RMAN magic: " + magic, 0);
        }

        // only the major version changes the layout; CDTB accepts 2.0 and 2.1, moonshadow565/rman accepts any 2.x
        if (major != 2)
        {
            throw new ParseException("Unsupported RMAN version " + major + "." + minor, 4);
        }

        byte unknown            = raf.readByte();
        byte signatureType      = raf.readByte();
        int  offset             = raf.readInt();
        int  length             = raf.readInt();
        long manifestId         = raf.readLong();
        int  decompressedLength = raf.readInt();
        return new RMANFileHeader(magic, major, minor, unknown, signatureType, offset, length, manifestId, decompressedLength);

    }
}
