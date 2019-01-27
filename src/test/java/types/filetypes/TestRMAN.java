package types.filetypes;

import com.google.gson.JsonObject;
import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws Exception
    {
        RMANParser parser = new RMANParser();
        
        System.out.println("Downloading patcher manifest");
        String patcherUrl    = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
        String patchManifest = String.join("\n", WebHandler.readWeb(patcherUrl));
        
        JsonObject obj     = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject();
        int        version = obj.get("version").getAsInt();
        System.out.println("Found patch version " + version);
        
        System.out.println("Downloading bundle manifest");
        String manifestUrl = obj.get("game_patch_url").getAsString();
        
        System.out.println("Parsing Manifest");
        RMANFile data = parser.parse(WebHandler.readBytes(manifestUrl));
        
        int[] counter = {0};
        
        List<String> removedBundles = getRemovedBundleIds(data);
        removeOldBundles(removedBundles);
        downloadAllBundles(data);
        
        if (removedBundles.size() != 0)
        {
            long allocatedMemory      = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
            int  suggestedThreadCount = (int) (Math.floorDiv(presumableFreeMemory, 500_000_000) / 4);
            
            // This is ran in a pool to limit the memory usage (sets the concurrent threads to suggestedThreadCount, instead of core count (12 in my case))
            ForkJoinPool forkJoinPool = new ForkJoinPool(Math.max(suggestedThreadCount, 1));
            forkJoinPool.submit(() -> data.getBody()
                                          .getFiles()
                                          .parallelStream()
                                          .forEach(f ->
                                                   {
                                                       counter[0]++;
                                                       data.extractFile(f);
                                                       System.out.format("Extracting file %s of %s%n", counter[0], data.getBody().getFiles().size());
                                                   })).get();
            forkJoinPool.shutdown();
        }
        
        TestWAD tw = new TestWAD();
        tw.testPullCDTB();
        tw.testCDragonWAD();
    }
    
    private List<String> getRemovedBundleIds(RMANFile data) throws IOException
    {
        Path        bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Set<String> keep         = data.getBundleMap().keySet();
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
    
    private List<String> getNewBundleIds(RMANFile data) throws IOException
    {
        Path        bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Set<String> keep         = data.getBundleMap().keySet();
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
    
    public void removeOldBundles(List<String> bundleIds)
    {
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        System.out.println(bundleIds.size() + " are not used in the current version, so we delete them");
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
    
    private void downloadAllBundles(RMANFile manifest) throws IOException
    {
        manifest.downloadBundles(manifest.getBody().getBundles());
    }
    
    private void downloadFileBundles(RMANFile manifest, RMANFileBodyFile file) throws IOException
    {
        Map<String, RMANFileBodyBundleChunkInfo> chunksById = manifest.getChunkMap();
        List<RMANFileBodyBundle> bundles = file.getChunkIds()
                                               .stream()
                                               .map(c -> manifest.getBundleMap().get(chunksById.get(c).getBundleId()))
                                               .collect(Collectors.toList());
        
        manifest.downloadBundles(bundles);
    }
}
