package no.stelar7.cdragon.util.hashguessing;

import com.google.gson.*;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.filemanifest.*;
import no.stelar7.cdragon.util.handlers.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

public class GameHashGuesser extends HashGuesser
{
    
    public GameHashGuesser(Set<String> hashes)
    {
        super(HashGuesser.hashFileGAME, hashes);
        System.out.println("Started guessing GAME hashes");
    }
    
    public void guessBinByLinkedFiles(Path pbe)
    {
        System.out.println("Guessing bin files by linked file names");
        try
        {
            System.out.println("Parsing bin files...");
            BINParser parser = new BINParser();
            Files.walk(pbe)
                 .parallel()
                 .filter(UtilHandler.IS_BIN_PREDICATE)
                 .map(parser::parse)
                 .filter(Objects::nonNull)
                 .flatMap(b -> b.getLinkedFiles().stream())
                 .forEach(this::check);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public void guessAssetsBySearch(Path pbe)
    {
        System.out.println("Guessing assets by searching strings");
        List<Path> readMe = UtilHandler.getFilesMatchingPredicate(pbe, UtilHandler.IS_JSON_PREDICATE);
        
        // need a better regex for this :thinking:
        Pattern p = Pattern.compile("(/.*?/).*?\\..*\"");
        
        readMe.stream()
              .parallel()
              .map(UtilHandler::readAsString)
              .forEach(e -> {
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
                          while (e.charAt(start - 1) != '"')
                          {
                              start--;
                          }
                    
                          while (e.charAt(end) != '"')
                          {
                              end++;
                          }
                    
                          String toCheck = e.substring(start, end).toLowerCase();
                          this.check(toCheck);
                      }
                  }
              });
    }
    
    private final Predicate<String> isGameHash = s -> !s.startsWith("plugins/");
    
    public void pullCDTB()
    {
        System.out.println("Feching hashlists from CDTB");
        String hashA = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.game.txt";
        String hashB = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.game.txt";
        
        Stream.of(WebHandler.readWeb(hashA).stream(), WebHandler.readWeb(hashB).stream())
              .flatMap(a -> a)
              .map(line -> line.substring(line.indexOf(' ') + 1))
              .filter(isGameHash)
              .forEach(this::check);
    }
    
    @Override
    public String generateHash(String val)
    {
        Long hashNum = HashHandler.computeXXHash64AsLong(val);
        return HashHandler.toHex(hashNum, 16);
    }
    
    public void guessShaderFiles(Path dataPath)
    {
        System.out.println("Guessing shaders by manifest");
        
        final List<String> prefixes = new ArrayList<>(Arrays.asList("data/shaders/hlsl/", "assets/shaders/generated/shaders/"));
        final List<String> suffixes = new ArrayList<>(Arrays.asList("", ".dx9", ".dx11", ".glsl", ".metal"));
        
        Stream.of(".dx9_", ".dx11_", ".glsl_", ".metal_")
              .parallel()
              .forEach(s -> IntStream
                      .rangeClosed(0, 100_000)
                      .parallel()
                      .filter(i -> i % 100 == 0)
                      .forEach(i -> suffixes.add(s + i)));
        
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
            
            Stream.of(".ps_2_0.dx9_", ".vs_2_0.dx9_", ".ps_2_0.dx11_", ".vs_2_0.dx11_", ".ps_2_0.glsl_", ".vs_2_0.glsl_", ".ps_2_0.metal_", ".vs_2_0.metal_")
                  .forEach(s -> IntStream
                          .rangeClosed(0, 100_000)
                          .filter(i -> i % 100 == 0)
                          .forEach(i -> suffixes.add(s + i)));
            
            Path      shaderJson = UtilHandler.CDRAGON_FOLDER.resolve("pbe\\data\\shaders\\shaders.json");
            JsonArray shaderObj  = UtilHandler.getJsonParser().parse(Files.readString(shaderJson)).getAsJsonObject().getAsJsonArray("CustomShaderDef");
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
                prefixes.add(v.substring(0, v.indexOf('/', 10)));
            }
        });
        
        super.guessByExistingWords(Set.of(""));
    }
}
