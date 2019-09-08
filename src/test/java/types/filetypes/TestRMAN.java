package types.filetypes;

import com.google.gson.JsonObject;
import no.stelar7.cdragon.types.rman.RMANParser;
import no.stelar7.cdragon.types.rman.RMANParser.RMANFileType;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.types.Pair;
import org.junit.jupiter.api.Test;
import types.util.TestCDTBHashGuessing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws Exception
    {
        List<RMANFile> files = new ArrayList<>();
        JsonObject     obj   = RMANParser.getPBEManifest();
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.GAME));
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.LCU));
        
        System.out.println("Writing manifest content to file");
        files.forEach(RMANFile::printFileList);
        
        Path bundleFolder = UtilHandler.CDRAGON_FOLDER.resolve("cdragon\\patcher\\bundles");
        Files.createDirectories(bundleFolder);
        
        int     currentVersion = obj.get("version").getAsInt();
        Path    lastVersion    = UtilHandler.CDRAGON_FOLDER.resolve("version");
        boolean shouldDownload = !Files.exists(lastVersion) || Integer.parseInt(Files.readAllLines(lastVersion).get(0)) < currentVersion;
        if (shouldDownload)
        {
            List<String> removedBundles = getRemovedBundleIds(files, bundleFolder);
            removeOldBundles(removedBundles, bundleFolder);
            downloadAllBundles(files, bundleFolder);
            Files.write(lastVersion, String.valueOf(currentVersion).getBytes(StandardCharsets.UTF_8));
        } else
        {
            System.out.println("This version is already downloaded, skipping download step");
        }
        
        Path    fileFolder   = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        boolean shouldExport = !Files.exists(fileFolder);
        if (shouldDownload || shouldExport)
        {
            Files.createDirectories(fileFolder);
            
            // use one thread per core, and leave one free for the OS
            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            
            // sort content based on filesize
            Set<Pair<Integer, Function<Void, Void>>> extracts = new TreeSet<>(Comparator.comparingInt((ToIntFunction<Pair<Integer, Function<Void, Void>>>) Pair::getA).reversed());
            files.forEach(manifest -> manifest.getBody()
                                              .getFiles()
                                              .forEach(f -> {
                                                  extracts.add(new Pair<>(f.getFileSize(), (v) -> {
                                                      manifest.extractFile(f, bundleFolder, fileFolder);
                                                      return null;
                                                  }));
                                              }));
            
            extracts.forEach(e -> service.submit(() -> e.getB().apply(null)));
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } else
        {
            System.out.println("This version is already extracted, skipping extract step");
        }
        
        boolean shouldUnpack = shouldDownload || shouldExport;
        if (shouldUnpack)
        {
            TestWAD tw = new TestWAD();
            tw.testCDragonWAD();
            
            TestCDTBHashGuessing hashes = new TestCDTBHashGuessing();
            hashes.doBINTest();
            hashes.doGameTest();
            hashes.doLCUTest();
        }
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
                                .collect(Collectors.toList());
        keep.removeAll(has);
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
}
