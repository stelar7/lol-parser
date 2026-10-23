package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.*;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.filemanifest.*;
import no.stelar7.cdragon.util.handlers.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
    }

    /**
     * Uses another hash file for the known hashes, eg. a small one for testing
     */
    public GameHashGuesser(HashFile hashFile, Set<String> hashes)
    {
        super(hashFile, hashes);
    }

    // a locale as a whole path segment / dot or underscore separated word: data/menu/en_us/, .en_us.wad, bootstrap_en_us.stringtable
    // zero-width so overlapping candidates are all found ("xx_en_us" finds both "xx_en" and "en_us")
    private static final Pattern LOCALE_PATTERN = Pattern.compile("(?<=[/._])(?=([a-z]{2}_[a-z]{2})(?:[/._]|$))");

    /**
     * For known paths containing a locale (vo/en_us/..., *.en_us.wad.client, ...), tries every other locale
     */
    public void substituteLanguages()
    {
        List<String> paths = new ArrayList<>(this.known.values());
        substituteTemplates("Substitute languages", paths, LOCALE_PATTERN, LANGUAGES::contains, LANGUAGES);
    }

    // skin0, skins/skin12/, skin12.bin, ahri_skin12_q.tex; not "reskin1" or "skin_1"
    private static final Pattern SKIN_PATTERN = Pattern.compile("(?<![a-z])skin(\\d+)(?!\\d)");

    /**
     * Replaces skin numbers in known paths with 0..max
     */
    public void substituteSkinNumbers(int max)
    {
        List<String> paths   = new ArrayList<>(this.known.values());
        List<String> numbers = IntStream.rangeClosed(0, max).mapToObj(String::valueOf).toList();

        // "skin" is kept in the template, only the number is replaced
        substituteTemplates("Substitute skin numbers", paths, SKIN_PATTERN, s -> true, numbers);
    }

    private static final Map<String, List<String>> SIBLING_ENDINGS = Map.of(
            ".dds", List.of(".tex"),
            ".tex", List.of(".dds"),
            ".skn", List.of(".skl"),
            ".skl", List.of(".skn"),
            ".scb", List.of(".sco"),
            ".sco", List.of(".scb"),
            "_audio.bnk", List.of("_events.bnk", "_audio.wpk"),
            "_events.bnk", List.of("_audio.bnk", "_audio.wpk"),
            "_audio.wpk", List.of("_audio.bnk", "_events.bnk")
                                                                           );

    /**
     * Tries the files that usually exist next to a known file: .dds/.tex, .skn/.skl, .scb/.sco, _audio.bnk/_events.bnk/_audio.wpk
     */
    public void substituteSiblingExtensions()
    {
        List<String> paths = new ArrayList<>(this.known.values());
        System.out.format("Substitute sibling extensions: %s paths%n", paths.size());

        paths.parallelStream().forEach(raw -> {
            String path = raw.toLowerCase(Locale.ENGLISH);
            for (Map.Entry<String, List<String>> e : SIBLING_ENDINGS.entrySet())
            {
                if (path.endsWith(e.getKey()))
                {
                    String base = path.substring(0, path.length() - e.getKey().length());
                    for (String ending : e.getValue())
                    {
                        check(base + ending);
                    }
                }
            }
        });
    }

    // content hashed names, shaders (guessShaderFiles covers them) and skin paths (substituteSkinNumbers covers them)
    private static final Pattern NUMBER_FILTER = Pattern.compile("/[0-9a-f]{8,}\\.[^/]+$|^data/shaders/|^assets/shaders/|/skin\\d+[^/]*$");

    /**
     * Replaces the numbers in the file name of known game paths with 0..max
     */
    public void substituteNumbers(int max, Optional<Integer> numbers)
    {
        Set<String> paths = new HashSet<>();
        for (String value : new ArrayList<>(this.known.values()))
        {
            String lower = value.toLowerCase(Locale.ENGLISH);
            if (!NUMBER_FILTER.matcher(lower).find())
            {
                paths.add(lower);
            }
        }

        super.substituteNumbers(paths, max, numbers);
    }
    
    public void guessBinByLinkedFiles(Path pbe)
    {
        System.out.println("Guessing bin files by linked file names");
        try
        {
            System.out.println("Parsing bin files...");
            BINParser parser = new BINParser();
            Files.walk(pbe).parallel().filter(UtilHandler.IS_BIN_PREDICATE).map(p -> BINHashGuesser.parseOrSkip(parser, p)).filter(Objects::nonNull).flatMap(b -> b.getLinkedFiles().stream()).forEach(this::check);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void guessAssetsBySearch(Path pbe) throws InterruptedException
    {
        System.out.println("Guessing assets by searching strings");
        
        // use one thread per core, and leave one free for the OS
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        
        List<Path> files = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        Pattern    p     = Pattern.compile("(/.*?/).*?\\..*\"");
        
        // read inside the task so file contents are not all held in memory while queued
        files.forEach(file -> service.execute(() -> {
                 String  e = UtilHandler.readAsString(file);
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
                         while (start > 0 && e.charAt(start - 1) != '"')
                         {
                             start--;
                         }

                         while (end < e.length() && e.charAt(end) != '"')
                         {
                             end++;
                         }
                         
                         String toCheck = e.substring(start, end).toLowerCase();
                         this.check(toCheck);
                     }
                 }
             }));
        
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
    
    private final Predicate<String> isGameHash = s -> !s.startsWith("plugins/");
    
    public void pullCDTB()
    {
        System.out.println("Feching hashlists from CommunityDragon");
        // streamed, the file is >200MB
        readCDragonHashList("hashes.game.txt", line -> {
            if (isGameHash.test(line.substring(line.indexOf(' ') + 1)))
            {
                checkPrehashed(line);
            }
        });
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
    
    public void guessShaderFiles(Path dataPath)
    {
        System.out.println("Guessing shaders by manifest");
        
        final List<String> prefixes = new ArrayList<>(Arrays.asList("data/shaders/hlsl/", "assets/shaders/generated/shaders/"));
        final List<String> suffixes = new ArrayList<>(Arrays.asList("", ".dx9", ".dx11", ".glsl", ".metal"));
        
        // not parallel, suffixes is a plain ArrayList
        Stream.of(".dx9_", ".dx11_", ".glsl_", ".metal_").forEach(s -> IntStream.rangeClosed(0, 100_000).filter(i -> i % 100 == 0).forEach(i -> suffixes.add(s + i)));
        
        try
        {
            Optional<Path> manifest = Files.find(dataPath, 100, (path, attr) -> path.toString().contains("shaderdefines.json")).findFirst();
            if (manifest.isPresent())
            {
                JsonObject shaders  = UtilHandler.getJsonParser().parse(Files.readString(manifest.get())).getAsJsonObject().getAsJsonObject("shaders");
                JsonArray  sections = shaders.getAsJsonArray("sections");
                
                for (JsonElement sectionEle : sections)
                {
                    JsonObject section = (JsonObject) sectionEle;
                    JsonArray  files   = section.getAsJsonArray("files");
                    
                    for (JsonElement fileEle : files)
                    {
                        for (String suffix : suffixes)
                        {
                            for (String prefix : prefixes)
                            {
                                String toCheck = prefix + fileEle.getAsString() + suffix;
                                check(toCheck);
                            }
                        }
                    }
                }
            }
            
            Function<JsonElement, String>      getFirstChildKey     = obj -> obj.getAsJsonObject().keySet().toArray(String[]::new)[0];
            Function<JsonElement, JsonElement> getFirstChildElement = obj -> obj.getAsJsonObject().get(getFirstChildKey.apply(obj));
            Function<JsonElement, JsonObject>  getFirstChildObject  = obj -> getFirstChildElement.apply(obj).getAsJsonObject();
            
            prefixes.clear();
            prefixes.addAll(Arrays.asList("data/shaders/hlsl/", "assets/shaders/generated/"));
            
            suffixes.clear();
            suffixes.addAll(Arrays.asList(".ps_2_0", ".vs_2_0", ".ps_2_0.dx9", ".vs_2_0.dx9", ".ps_2_0.dx11", ".vs_2_0.dx11", ".ps_2_0.glsl", ".vs_2_0.glsl", ".ps_2_0.metal", ".vs_2_0.metal"));
            
            Stream.of(".ps_2_0.dx9_", ".vs_2_0.dx9_", ".ps_2_0.dx11_", ".vs_2_0.dx11_", ".ps_2_0.glsl_", ".vs_2_0.glsl_", ".ps_2_0.metal_", ".vs_2_0.metal_").forEach(s -> IntStream.rangeClosed(0, 100_000).filter(i -> i % 100 == 0).forEach(i -> suffixes.add(s + i)));
            
            Path shaderJson = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\shaders\\shaders.json");
            if (!Files.exists(shaderJson))
            {
                return;
            }
            
            JsonArray shaderObj = JsonParser.parseString(Files.readString(shaderJson)).getAsJsonObject().getAsJsonArray("CustomShaderDef");
            for (JsonElement elem : shaderObj)
            {
                JsonObject obj     = (JsonObject) elem;
                JsonObject realObj = getFirstChildObject.apply(obj);
                String     name    = realObj.get("objectPath").getAsString();
                
                for (String prefix : prefixes)
                {
                    for (String suffix : suffixes)
                    {
                        String toCheck = prefix + name + suffix;
                        check(toCheck);
                    }
                }
                
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void guessStringTableFiles()
    {
        for (String language : LANGUAGES)
        {
            String hash = String.format("DATA/Menu/bootstrap_%s.stringtable", language);
            check(hash);
        }
    }
    
    public void guessHardcoded()
    {
        check("UX/RenderUI/Overrides/Default/PerkSummonerSpecialist.bin");
        check("UX/RenderUI/Overrides/Default/SB_LtoR_NoNames.bin");
        check("UX/RenderUI/Overrides/Default/SB_MirroredCenter_Names.bin");
        check("UX/RenderUI/Overrides/Default/SB_MirroredCenter_NoNames.bin");
    }
    
    public void guessScripts(Path dataPath)
    {
        System.out.println("Guessing scripts by manifest");
        
        Path                  luaManifest = dataPath.resolve("data/all_lua_files.manifest");
        ManifestContentParser parser      = new ManifestContentParser();
        ManifestContentFileV1 v1          = parser.parseV1(luaManifest);
        v1.getItems().forEach(this::check);
    }
    
    public void guessByExistingWords()
    {
        Set<String> prefixes = new HashSet<>();
        this.known.values().forEach(v -> {
            if (v.indexOf('/', 10) > -1)
            {
                prefixes.add(v.substring(0, v.indexOf('/', 10) + 1));
            }
        });
        
        super.guessByExistingWords(prefixes);
    }
}
