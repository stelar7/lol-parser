package types.filetypes;

import no.stelar7.cdragon.types.rman.RMANParser;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;
import org.junit.jupiter.api.Test;
import types.util.TestCDTBHashGuessing;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws Exception
    {
        List<RMANFile> files = new ArrayList<>();
        /*
        JsonObject     obj   = RMANParser.getPBEManifest();
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.GAME));
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.LCU));
         */
        
        files.addAll(RMANParser.getSieveManifests());
        //files.add(RMANParser.getFromURL("https://lol.secure.dyn.riotcdn.net/channels/public/releases/F5EFF59DBB492C6A.manifest"));
        
        System.out.println("Writing manifest content to file");
        files.forEach(RMANFile::printFileList);
        
        Path bundleFolder = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\patcher\\bundles");
        Files.createDirectories(bundleFolder);
        
        long    currentVersion = RMANParser.getSieveVersion();
        Path    lastVersion    = UtilHandler.CDRAGON_FOLDER.resolve("version");
        boolean shouldDownload = !Files.exists(lastVersion) || Long.parseLong(Files.readAllLines(lastVersion).get(0)) < currentVersion;
        if (shouldDownload)
        {
            List<String> removedBundles = getRemovedBundleIds(files, bundleFolder);
            removeOldBundles(removedBundles, bundleFolder);
            downloadAllBundles(files, bundleFolder);
            Files.writeString(lastVersion, String.valueOf(currentVersion));
        } else
        {
            System.out.println("This version is already downloaded, skipping download step");
        }
        
        Path    fileFolder   = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        boolean shouldExport = !Files.exists(fileFolder);
        if (shouldExport || shouldDownload)
        {
            Files.createDirectories(fileFolder);
            
            // use one thread per core, and leave one free for the OS
            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            
            Set<Pair<Integer, Function<Void, Void>>> extracts = new HashSet<>();
            files.forEach(manifest -> manifest.getBody()
                                              .getFiles()
                                              .forEach(f -> {
                                                  extracts.add(new Pair<>(f.getFileSize(), (v) -> {
                                                      manifest.extractFile(f, bundleFolder, fileFolder);
                                                      return null;
                                                  }));
                                              }));
            
            // extract content based on filesize (largest first)
            extracts.stream()
                    .sorted(Comparator.comparing(Pair::getA, Comparator.reverseOrder()))
                    .forEach(e -> service.submit(() -> e.getB().apply(null)));
            
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } else
        {
            System.out.println("This version is already merged, skipping merging step");
        }
        
        Path    extractPath  = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        boolean shouldUnpack = !Files.exists(extractPath) || shouldDownload || shouldExport;
        if (shouldUnpack)
        {
            TestWAD tw = new TestWAD();
            tw.testCDragonWAD();
        } else
        {
            System.out.println("This version is already unpacked, skipping unpack step");
        }
        
        TestCDTBHashGuessing hashes = new TestCDTBHashGuessing();
        hashes.doTests();
    }
    
    /**
     * set language to "" to download the basic files
     * <p>
     * all valid options;
     * ""   , ru_ru, it_it, el_gr, pl_pl, ro_ro, tr_tr,
     * pt_br, th_th, vn_vn, ja_jp, fr_fr, cs_cz, hu_hu,
     * de_de, zh_tw, ko_kr, en_us, es_mx, zh_cn, es_es
     */
    public void downloadChampionFiles(RMANFile file, String language, Path bundleFolder, Path outputFolder)
    {
        Map<String, List<RMANFileBodyFile>> files     = file.getChampionFilesByLanguage();
        List<RMANFileBodyFile>              langFiles = files.get(language);
        
        langFiles.stream().map(file::getBundlesForFile).forEach(list -> file.downloadBundles(list, bundleFolder));
        langFiles.forEach(f -> file.extractFile(f, bundleFolder, outputFolder));
    }
    
    private List<String> getRemovedBundleIds(Collection<RMANFile> datas, Path bundleFolder) throws IOException
    {
        System.out.println("Calculating removed bundles");
        Set<String> keep = datas.stream().map(RMANFile::getBundleMap).flatMap(a -> a.keySet().stream()).collect(Collectors.toSet());
        List<String> has = Files.walk(bundleFolder)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(s -> s.endsWith(".bundle"))
                                .map(s -> s.substring(0, 16))
                                .map(s -> s.toUpperCase(Locale.ENGLISH))
                                .collect(Collectors.toList());
        has.removeAll(keep);
        return has;
    }
    
    private List<String> getNewBundleIds(RMANFile data, Path bundleFolder) throws IOException
    {
        Set<String> keep = data.getBundleMap().keySet();
        List<String> has = Files.walk(bundleFolder)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(s -> s.endsWith(".bundle"))
                                .map(s -> s.substring(0, 16))
                                .map(s -> s.toUpperCase(Locale.ENGLISH))
                                .toList();
        has.forEach(keep::remove);
        return new ArrayList<>(keep);
    }
    
    public void removeOldBundles(List<String> bundleIds, Path bundleFolder)
    {
        System.out.println("Found " + bundleIds.size() + " bundles that are not used in this version, deleting...");
        bundleIds.stream()
                 .map(s -> s.toUpperCase(Locale.ENGLISH))
                 .map(s -> s + ".bundle")
                 .forEach(s -> {
                     try
                     {
                         Files.deleteIfExists(bundleFolder.resolve(s));
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });
    }
    
    private void downloadAllBundles(Collection<RMANFile> datas, Path bundleFolder) throws IOException
    {
        Files.createDirectories(bundleFolder);
        datas.forEach(f -> f.downloadBundles(f.getBody().getBundles(), bundleFolder));
    }
    
    private void downloadFileBundles(RMANFile manifest, RMANFileBodyFile file, Path bundleFolder)
    {
        Map<String, RMANFileBodyBundleChunkInfo> chunksById = manifest.getChunkMap();
        Set<RMANFileBodyBundle> bundles = file.getChunkIds()
                                              .stream()
                                              .map(c -> manifest.getBundleMap().get(chunksById.get(c).getBundleId()))
                                              .collect(Collectors.toSet());
        
        manifest.downloadBundles(bundles, bundleFolder);
    }
    
    @Test
    public void runTestStufF()
    {
        //RMANFile file = new RMANParser().parse(WebHandler.readBytes("https://ks-foundation.secure.dyn.riotcdn.net/channels/public/releases/6BEAC2167C663D6E.manifest"));
        //RMANFile file2 = new RMANParser().parse(WebHandler.readBytes("https://ks-foundation.dyn.riotcdn.net/channels/public/releases/AB41810D95E1599B.manifest"));
        
        //RMANFile file = new RMANParser().parse(WebHandler.readBytes("https://bacon.secure.dyn.riotcdn.net/channels/public/releases/2A3F9712EE141A58.manifest"));
        //RMANFile file = new RMANParser().parse(Paths.get("C:\\Riot Games\\LoR\\live\\PatcherData.manifest"));
        //RMANFile file = new RMANParser().parse(Paths.get("C:\\cdragon\\cdragon\\rman\\DC9F6F78A04934D6.manifest"));
        RMANFile file = new RMANParser().parse(Paths.get("C:\\cdragon\\cdragon\\patcher\\manifests\\sieve\\13165241453-game.rman"));
        System.out.println();
    }
}
