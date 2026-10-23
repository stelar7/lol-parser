package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.JsonElement;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.ProgressBar;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.Triplet;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.*;
import java.util.stream.*;

public class LCUHashGuesser extends HashGuesser
{
    
    public LCUHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileLCU, hashes);
    }

    /**
     * Uses another hash file for the known hashes, eg. a small one for testing
     */
    public LCUHashGuesser(HashFile hashFile, Set<String> hashes)
    {
        super(hashFile, hashes);
    }
    
    @Override
    public long hashOf(String lowerCased)
    {
        return HashHandler.computeXXHash64AsLong(lowerCased);
    }

    @Override
    protected int hexLength()
    {
        return 16;
    }
    
    private static Set<Triplet<String, String, String>> getFilesAsText(WADFile wad)
    {
        Set<Triplet<String, String, String>> data = new HashSet<>();
        
        Set<String> exts = new HashSet<>(Arrays.asList("png", "jpg", "ttf", "webm", "ogg", "dds", "tga"));
        for (WADContentHeaderV1 header : wad.getContentHeaders())
        {
            String unhashed = HashHandler.getWadHash(header.getPathHash());
            if (!unhashed.equals(header.getPathHash()))
            {
                String ext = UtilHandler.getEnding(unhashed);
                if (exts.contains(ext))
                {
                    continue;
                }
            }
            
            String ext     = UtilHandler.getEnding(unhashed);
            String content = new String(wad.readContentFromHeaderData(header));
            data.add(new Triplet<>(unhashed, ext, content));
        }
        
        return data;
    }
    
    
    public Set<String> buildWordlist()
    {
        Pattern     reFilterPath = Pattern.compile("^plugins/rcp-be-lol-game-data/global/default/data/characters/|/[0-9a-f]{32}\\.");
        Set<String> paths        = new HashSet<>();
        for (String p : this.known.values())
        {
            if (!reFilterPath.matcher(p).find())
            {
                paths.add(p);
            }
        }
        
        return buildWordlist(paths);
    }
    
    private int ordinalIndexOf(String str, String substr, int n)
    {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
        {
            pos = str.indexOf(substr, pos + 1);
        }
        return pos;
    }
    
    public void substituteRegionLang()
    {
        Set<String> languages = new HashSet<>(this.LANGUAGES);
        languages.add("default");
        Set<String> regionLang = WordHandler.product("/", this.REGIONS, languages);

        // paths are plugins/<plugin>/<region>/<lang>/<rest>, most of them only differ by region/lang,
        // so collapse them to unique (prefix, rest) pairs before substituting
        Set<List<String>> templates = new HashSet<>();
        for (String value : this.known.values())
        {
            int second = ordinalIndexOf(value, "/", 2);
            int fourth = ordinalIndexOf(value, "/", 4);
            if (value.startsWith("plugins/") && fourth > 0)
            {
                templates.add(List.of(value.substring(0, second + 1), value.substring(fourth + 1)));
            }
        }

        System.out.format("Substitute region, and language: %s region/languages, %s templates (from %s hashes)%n", regionLang.size(), templates.size(), this.known.size());

        ProgressBar progress = new ProgressBar("Substitute region, and language", templates.size());
        templates.parallelStream().forEach(t -> {
            for (String rl : regionLang)
            {
                check(t.get(0) + rl + "/" + t.get(1));
            }
            progress.step();
        });
        progress.finish();
    }

    public void substitutePlugins()
    {
        List<String> pluginPaths = this.known.values().stream()
                                             .filter(a -> a.startsWith("plugins/") && a.indexOf('/', "plugins/".length()) > 0)
                                             .toList();

        Set<String> plugins = pluginPaths.stream()
                                         .map(a -> a.substring("plugins/".length(), a.indexOf('/', "plugins/".length())))
                                         .collect(Collectors.toSet());


        System.out.format("Substitute plugins: %s plugins, %s hashes%n", plugins.size(), pluginPaths.size());

        ProgressBar progress = new ProgressBar("Substitute plugins", plugins.size());
        for (String p : plugins)
        {
            for (String value : pluginPaths)
            {
                String prefix  = value.substring(0, ordinalIndexOf(value, "/", 1) + 1);
                String suffix  = value.substring(ordinalIndexOf(value, "/", 2) + 1);
                String toCheck = prefix + p + "/" + suffix;

                check(toCheck);
            }
            progress.step();
        }
        progress.finish();
    }
    
    public void substituteBasenameWords(String plugin, String ext, Set<String> words, int amount)
    {
        Set<String> paths = new HashSet<>(this.known.values());
        if (plugin != null)
        {
            paths.removeIf(path -> !path.startsWith("plugins/" + plugin));
        }
        
        if (ext != null)
        {
            paths.removeIf(path -> !path.endsWith(ext));
        }
        
        if (words == null)
        {
            words = this.buildWordlist();
        }
        
        super.substituteBasenameWords(paths, words, amount);
    }
    
    public void addBasenameWord()
    {
        super.addBasenameWord(new HashSet<>(this.known.values()), this.buildWordlist());
    }
    
    public void doubleSubstitution(String plugin, String ext, Set<String> words)
    {
        Set<String> paths = new HashSet<>(this.known.values());
        if (plugin != null)
        {
            paths.removeIf(path -> !path.startsWith("plugins/" + plugin));
        }
        
        if (ext != null)
        {
            paths.removeIf(path -> !path.endsWith(ext));
        }
        
        super.doubleSubstitution(paths, words);
    }
    
    public void substituteNumbers(int max, Optional<Integer> numbers)
    {
        // paths that are already fully covered elsewhere, or that only differ by a content hash
        Pattern filter = Pattern.compile("^(?:plugins/rcp-be-lol-game-data/[^/]+/[^/]+/v1/champion-" +
                                         "|plugins/rcp-be-lol-game-data/global/default/(?:data|assets)/characters/" +
                                         "|plugins/rcp-be-lol-game-data/global/default/data/items/icons2d/\\d+_" +
                                         "|plugins/rcp-be-lol-game-data/[^/]+/[^/]+/v1/champions/-1\\.json" +
                                         ")|/[0-9a-f]{32}\\.");
        
        Set<String> paths = new HashSet<>();
        for (String value : new ArrayList<>(this.known.values()))
        {
            if (!filter.matcher(value).find())
            {
                paths.add(value);
            }
        }
        
        super.substituteNumbers(paths, max, numbers);
    }
    
    public void grepWad(WADFile file)
    {
        System.out.println("Loading from wad!");
        
        Set<String> relative = new HashSet<>();
        
        for (Triplet<String, String, String> pair : this.getFilesAsText(file))
        {
            String      unhashed = pair.getA();
            String      ext      = pair.getB();
            String      content  = pair.getC();
            JsonElement jsonData = null;
            
            if (ext.equalsIgnoreCase("json"))
            {
                jsonData = JsonHandler.getJsonParser().parse(content);
            }
            
            if (jsonData != null)
            {
                if (unhashed.equalsIgnoreCase("plugins/rcp-fe-lol-loot/global/default/trans.json"))
                {
                    for (String key : jsonData.getAsJsonObject().keySet())
                    {
                        String toCheck = "plugins/rcp-be-lol-game-data/global/default/v1/hextech-images/" + key + ".png";
                        check(toCheck);
                    }
                } else if (jsonData.isJsonObject() && jsonData.getAsJsonObject().has("pluginDependencies") && jsonData.getAsJsonObject().has("name"))
                {
                    String      name     = jsonData.getAsJsonObject().get("name").getAsString();
                    Set<String> subPaths = new HashSet<>(Arrays.asList("index.html", "init.js", "init.js.map", "bundle.js", "trans.json", "css/main.css", "license.json"));
                    for (String sub : subPaths)
                    {
                        String toCheck = "plugins/" + name + "/global/default/" + sub;
                        check(toCheck);
                    }
                } else if (jsonData.isJsonObject() && jsonData.getAsJsonObject().has("musicVolume") && jsonData.getAsJsonObject().has("files"))
                {
                    Set<String> names = new HashSet<>();
                    for (JsonElement path : jsonData.getAsJsonObject().get("files").getAsJsonArray())
                    {
                        Pattern p = Pattern.compile("-splash-([^.]+)");
                        Matcher m = p.matcher(path.getAsString());
                        if (m.find())
                        {
                            names.add(m.group(1));
                        }
                    }
                    
                    for (String name : names)
                    {
                        String toCheck = "plugins/rcp-fe-lol-splash/global/default/splash-assets/" + name + "/config.json";
                        check(toCheck);
                        
                        for (JsonElement path : jsonData.getAsJsonObject().get("files").getAsJsonArray())
                        {
                            toCheck = "plugins/rcp-fe-lol-splash/global/default/splash-assets/" + name + "/" + path;
                            check(toCheck);
                        }
                    }
                    
                } else if (unhashed.equalsIgnoreCase("plugins/rcp-be-lol-game-data/global/default/v1/champion-summary.json"))
                {
                    Set<Integer> ids = new HashSet<>();
                    for (JsonElement ch : jsonData.getAsJsonArray())
                    {
                        ids.add(ch.getAsJsonObject().get("id").getAsInt());
                    }
                    
                    for (Integer id : ids)
                    {
                        String toCheck = "plugins/rcp-be-lol-game-data/global/default/v1/champions/" + id + ".json";
                        check(toCheck);
                        
                        toCheck = "plugins/rcp-be-lol-game-data/global/default/v1/champion-splashes/" + id + "/metadata.json";
                        check(toCheck);
                    }
                } else if (jsonData.isJsonObject() && jsonData.getAsJsonObject().has("recommendedItemDefaults"))
                {
                    for (JsonElement rec : jsonData.getAsJsonObject().getAsJsonArray("recommendedItemDefaults"))
                    {
                        String toCheck = "plugins/rcp-be-lol-game-data/global/default" + rec;
                        check(toCheck);
                    }
                }
            }
            
            
            Pattern p = Pattern.compile("/fe/([^/]+)/([a-zA-Z0-9/_.@-]+)");
            Matcher m = p.matcher(content);
            while (m.find())
            {
                String toCheck = "plugins/rcp-fe-" + m.group(1) + "/global/default/" + m.group(2);
                check(toCheck);
            }
            
            p = Pattern.compile("/DATA/([a-zA-Z0-9/_.@-]+)");
            m = p.matcher(content);
            while (m.find())
            {
                String toCheck = "plugins/rcp-be-lol-game-data/global/default/data/" + m.group(1);
                check(toCheck);
            }
            
            p = Pattern.compile("[/\"]lol-game-data/assets/([a-zA-Z0-9/_.@-]+)");
            m = p.matcher(content);
            while (m.find())
            {
                String toCheck = "plugins/rcp-be-lol-game-data/global/default/" + m.group(1);
                check(toCheck);
            }
            
            
            p = Pattern.compile("[^a-zA-Z0-9/_.\\\\-]((?:\\.|\\.\\.)/[a-zA-Z0-9/_.-]+)");
            m = p.matcher(content);
            while (m.find())
            {
                relative.add(m.group(1));
            }
            
            p = Pattern.compile("[\"']([a-zA-Z0-9][a-zA-Z0-9/_.@-]*\\.(?:js|json|webm|html|[a-z]{3}))[\"']");
            m = p.matcher(content);
            while (m.find())
            {
                relative.add(m.group(1));
            }
            
            p = Pattern.compile("<template id=\"[^\"]*-template-([^\"]+)\"");
            m = p.matcher(content);
            while (m.find())
            {
                relative.add(m.group(1) + "/template.html");
            }
            
            p = Pattern.compile("sourceMappingURL=(.*?\\.js)\\.map");
            m = p.matcher(content);
            while (m.find())
            {
                relative.add(m.group(1));
            }
        }
        
        checkBasenames(relative);
    }
    
    public void guessFromGameHashes()
    {
        // game textures are served by the client with the same path, converted to png/jpg
        String             base      = "plugins/rcp-be-lol-game-data/global/default";
        Collection<String> gamePaths = HashGuesser.hashFileGAME.load(true).values();
        ProgressBar        progress  = new ProgressBar("Guessing from game hashes", gamePaths.size());
        for (String path : gamePaths)
        {
            progress.step();
            String ext = UtilHandler.getEnding(path);

            if (ext.equals("dds") || ext.equals("tex"))
            {
                String withoutExt = path.substring(0, path.lastIndexOf('.'));
                check(base + "/" + withoutExt + ".jpg");
                check(base + "/" + withoutExt + ".png");
            } else if (ext.equals("json"))
            {
                check(base + "/" + path);
            }
        }
        progress.finish();
    }
    
    public void guessPatterns()
    {
        Set<Integer> primary = IntStream.iterate(8000, i -> i < 8700, i -> i + 100).boxed().collect(Collectors.toSet());
        primary.add(0);
        
        primary.forEach(i -> {
            Set<Integer> secondary = IntStream.rangeClosed(i, i + 100).boxed().collect(Collectors.toSet());
            secondary.add(0);
            
            for (Integer j : primary)
            {
                for (Integer k : secondary)
                {
                    String toCheck = "plugins/rcp-fe-lol-perks/global/default/images/inventory-card/" + i + "/p" + i + "_s" + j + "_k" + k + ".jpg";
                    check(toCheck);
                }
            }
            
            Set<String> paths = new HashSet<>(Arrays.asList("environment.jpg", "construct.png"));
            for (Integer j : secondary)
            {
                paths.add("keystones/" + j + ".png");
            }
            
            for (Integer j : primary)
            {
                paths.add("f'second/" + j + ".png");
            }
            
            for (String path : paths)
            {
                String toCheck = "plugins/rcp-fe-lol-perks/global/default/images/construct/" + i + "/" + path;
                check(toCheck);
            }
        });
        
        Set<String> paths = new HashSet<>();
        IntStream.range(0, 5).forEach(i -> {
            Set<String> actions = new HashSet<>(Arrays.asList("filter", "unfilter"));
            for (String action : actions)
            {
                paths.add(i + "." + action + ".csv");
                
                for (String lang : this.LANGUAGES)
                {
                    paths.add(i + "." + action + ".language." + lang.split("_")[0] + ".csv");
                    paths.add(i + "." + action + ".country." + lang.split("_")[1] + ".csv");
                    paths.add(i + "." + action + ".locale." + lang + ".csv");
                }
                
                for (String region : this.REGIONS)
                {
                    paths.add(i + "." + action + ".region." + region + "c.csv");
                }
            }
        });
        
        Set<String> divs = new HashSet<>(Arrays.asList("allowedchars", "breakingchars", "projectedchars", "projectedchars1337", "punctuationchars", "variantaliases"));
        for (String p : divs)
        {
            for (String lang : this.LANGUAGES)
            {
                paths.add(p + ".locale." + lang + ".txt");
                paths.add(p + ".language." + lang.split("_")[0] + ".txt");
            }
        }
        
        for (String path : paths)
        {
            String toCheck = "plugins/rcp-be-sanitizer/global/default/" + path;
            check(toCheck);
        }
        
        
        for (String p : new ArrayList<>(this.known.values()))
        {
            if (p.startsWith("plugins/rcp-fe-lol-loot/global/default/assets/loot_item_icons/"))
            {
                String toCheck = p.replace(".png", "_splash.png");
                check(toCheck);
            }
        }
    }
    
    public void guessAssetsBySearch(Path pbe)
    {
        System.out.println("Guessing assets by searching strings");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.WEB_FILE_PREDICATE);
        
        // need a better regex for this :thinking:
        Pattern p = Pattern.compile("/(rcp-(?:fe|be)-.{1,40}\\.(?:css|js))\"");

        ProgressBar progress = new ProgressBar("Searching web files for assets", readMe.size());
        readMe.stream()
              .parallel()
              .map(UtilHandler::readAsString)
              .forEach(e -> {
                  progress.step();
                  Matcher m = p.matcher(e);
                  while (m.find())
                  {
                      int lastStart = 0;
                      for (int i = 0; i <= m.groupCount(); i++)
                      {
                          int start = m.start(i);
                          int end   = m.end(i) - 1;
                    
                          if (start == lastStart)
                          {
                              continue;
                          }
                    
                          lastStart = start;
                          while (start > 0 && e.charAt(start - 1) != '"' && e.charAt(start - 1) != '\'')
                          {
                              start--;
                          }
                    
                          while (end < e.length() && e.charAt(end) != '"' && e.charAt(end) != '\'')
                          {
                              end++;
                          }
                    
                          String toCheck = e.substring(start, end).toLowerCase(Locale.ROOT);
                          if (toCheck.contains("/"))
                          {
                              String prefix = "plugins/";
                              String plugin = toCheck.substring(toCheck.lastIndexOf("/") + 1, toCheck.indexOf("."));
                              String suffix = "/global/default/";
                        
                              List<String> endings = Arrays.asList("index.html", "init.js", plugin + ".css", plugin + ".js", "trans.json", "license.json");
                              List<String> full    = endings.stream().map(att -> prefix + plugin + suffix + att).collect(Collectors.toList());
                              this.check(full);
                          } else
                          {
                              System.out.println("failed on " + toCheck);
                              this.check(toCheck);
                          }
                      }
                  }
              });
        progress.finish();
    }
    
    private final Predicate<String> isLCUHash = s -> s.startsWith("plugins/");
    
    public void pullCDTB()
    {
        System.out.println("Feching hashlists from CommunityDragon");
        HashLists.readCDragonHashList("hashes.lcu.txt", line -> {
            if (isLCUHash.test(line.substring(line.indexOf(' ') + 1)))
            {
                checkPrehashed(line);
            }
        });
    }

    /**
     * Runs {@link #grepWad} on every LCU wad (*.wad, not the game *.wad.client) in the folder.
     * Does nothing if the folder does not exist.
     */
    public void grepWads(Path folder)
    {
        if (folder == null || !Files.isDirectory(folder))
        {
            System.out.println("No wad folder at " + folder + ", skipping grepWads");
            return;
        }

        List<Path> wadFiles;
        try (Stream<Path> files = Files.find(folder, 50, (p, attr) -> attr.isRegularFile() && p.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".wad")))
        {
            wadFiles = files.toList();
        } catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

        System.out.format("Grepping %s LCU wads%n", wadFiles.size());
        WADParser   parser   = new WADParser();
        ProgressBar progress = new ProgressBar("Grepping LCU wads", wadFiles.size());
        for (Path wadFile : wadFiles)
        {
            try
            {
                WADFile wad = parser.parse(wadFile);
                if (wad != null)
                {
                    grepWad(wad);
                }
            } catch (Exception e)
            {
                System.err.println("Unable to grep " + wadFile + ": " + e.getMessage());
            }
            progress.step();
        }
        progress.finish();
    }
    
    public void guessSanitizerHashes()
    {
        System.out.println("Guessing new sanitizer hashes");
        
        String       base        = "plugins/rcp-be-sanitizer/global/default/";
        List<String> types       = Arrays.asList("filter", "unfilter", "whitelist", "allowedchars", "breakingchars", "projectedchars", "projectedchars1337", "punctuationchars", "variantaliases");
        List<String> prefixes    = Arrays.asList("", "0", "1", "2", "3", "4");
        List<String> subtypes    = Arrays.asList("", "country", "language", "region");
        Set<String>  languageSet = this.LANGUAGES.stream().map(l -> l.contains("_") ? l.split("_")[0] : "").collect(Collectors.toSet());
        languageSet.addAll(this.REGIONS);
        languageSet.add("");
        
        for (String prefix : prefixes)
        {
            for (String type : types)
            {
                for (String subtype : subtypes)
                {
                    for (String lang : languageSet)
                    {
                        StringBuilder test = new StringBuilder(base);
                        test.append(prefix);
                        if (!test.toString().endsWith("/"))
                        {
                            test.append(".");
                        }
                        
                        test.append(type);
                        test.append(".");
                        test.append(subtype);
                        
                        if (!test.toString().endsWith("."))
                        {
                            test.append(".");
                        }
                        
                        test.append(lang);
                        test.append(".csv");
                        
                        check(test.toString());
                    }
                }
            }
        }
    }
    
    public void guessManifestFiles()
    {
        check("DATA/FINAL/DATA.wad.LegacyDirListInfo");
        for (int i = 0; i < 1000; i++)
        {
            String toCheck = "DATA/FINAL/Maps/Shipping/Map" + i + "LEVELS.wad.LegacyDirListInfo";
            check(toCheck);
        }
    }
    
    public void guessByExistingWords()
    {
        Set<String> plugins = new HashSet<>();
        this.known.values().forEach(v -> plugins.add(v.substring(0, v.indexOf('/', 10)) + "/global/default/"));
        plugins.removeIf(v -> !v.startsWith("plugin"));
        
        super.guessByExistingWords(plugins);
    }
}
