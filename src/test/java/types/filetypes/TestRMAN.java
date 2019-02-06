package types.filetypes;

import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.types.rman.RMANParser.RMANFileType;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws Exception
    {
        List<RMANFile> files = new ArrayList<>();
        files.add(RMANParser.loadFromPBE(RMANFileType.GAME));
        files.add(RMANParser.loadFromPBE(RMANFileType.LCU));
        files.forEach(RMANFile::printFileList);
        
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\patcher\\bundles");
        Path fileFolder   = UtilHandler.DOWNLOADS_FOLDER.resolve("extractedFiles");
        Files.createDirectories(bundleFolder);
        Files.createDirectories(fileFolder);
        
        List<String> removedBundles = getRemovedBundleIds(files, bundleFolder);
        removeOldBundles(removedBundles, bundleFolder);
        downloadAllBundles(files, bundleFolder);
        
        boolean shouldExport = true;
        if (shouldExport)
        {
            long maxFileSize = files.stream().flatMap(f -> f.getBody().getFiles().stream()).mapToLong(RMANFileBodyFile::getFileSize).max().getAsLong();
            
            long allocatedMemory      = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
            int  suggestedThreadCount = (int) (Math.floorDiv(presumableFreeMemory, maxFileSize));
            
            // This is ran in a pool to limit the memory usage (sets the concurrent threads to suggestedThreadCount, instead of core count (12 in my case))
            // well, it was, but not all files got extracted
            AtomicInteger counter      = new AtomicInteger(0);
            ForkJoinPool  forkJoinPool = new ForkJoinPool(5);
            
            files.forEach(manifest -> manifest.getBody().getFiles().forEach(f -> forkJoinPool.submit(() -> {
                System.out.format("Extracting file %s of %s%n", counter.incrementAndGet(), manifest.getBody().getFiles().size());
                manifest.extractFile(f, bundleFolder, fileFolder);
            })));
            
            forkJoinPool.shutdown();
            forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
        
        TestWAD tw = new TestWAD();
        tw.testPullCDTB();
        tw.testCDragonWAD();
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
