package no.stelar7.cdragon.util.hashguessing;

import com.google.common.collect.Sets;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.rst.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class BINHashGuesser extends HashGuesser
{
    private       List<BINFile> files = null;
    private final Path          dataPath;
    
    public BINHashGuesser(Collection<String> strings, Path dataPath)
    {
        super(HashGuesser.hashFileBIN, strings);
        this.dataPath = dataPath;
    }
    
    private List<BINFile> getFiles()
    {
        if (files == null)
        {
            try
            {
                System.out.println("Parsing bin files...");
                BINParser parser = new BINParser();
                files = Files.walk(dataPath)
                             .filter(UtilHandler.IS_BIN_PREDICATE)
                             .map(parser::parse)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
                
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        return files;
    }
    
    public void guessNewCharacters()
    {
        System.out.println("Guessing new characters");
        
        getFiles().stream()
                  .flatMap(b -> b.getEntries().stream())
                  .filter(b -> b.getType().equalsIgnoreCase("character"))
                  .forEach(e -> {
                      String name   = (String) e.getValues().get(0).getValue();
                      String toHash = "Characters/" + name;
                      this.check(toHash);
                  });
    }
    
    public void guessNewAnimations()
    {
        System.out.println("Guessing new animations");
        
        getFiles().stream()
                  .flatMap(b -> b.getEntries().stream())
                  .filter(b -> b.getType().equalsIgnoreCase("animationGraphData"))
                  .forEach(e -> {
                      Optional<BINValue> clipDataMap = e.get("mClipDataMap");
                      if (clipDataMap.isEmpty())
                      {
                          return;
                      }
            
                      BINMap clipData = (BINMap) clipDataMap.get().getValue();
                      for (Vector2<Object, Object> pairs : clipData.getData())
                      {
                          BINStruct clipContent = (BINStruct) pairs.getSecond();
                
                          Optional<BINValue> animationResourcePath = clipContent.get("mAnimationResourceData");
                          if (animationResourcePath.isEmpty())
                          {
                              return;
                          }
                
                          BINStruct          animationResourceData = (BINStruct) animationResourcePath.get().getValue();
                          Optional<BINValue> animationFilePath     = animationResourceData.get("mAnimationFilePath");
                          if (animationFilePath.isEmpty())
                          {
                              return;
                          }
                
                          String           path      = (String) animationFilePath.get().getValue();
                          String           filename  = UtilHandler.removeEnding(UtilHandler.getFilename(path));
                          Set<String>      parts     = new HashSet<>(Arrays.asList(filename.split("_")));
                          Set<Set<String>> powerSets = Sets.powerSet(parts);
                          for (Set<String> product : powerSets)
                          {
                              String toHash = String.join("", product);
                              if (toHash.isBlank())
                              {
                                  continue;
                        
                              }
                              this.check(toHash);
                          }
                      }
                  });
    }
    
    public void guessFromFile(Path file, String pattern)
    {
        System.out.println("Guessing from file " + file.toString() + " with pattern: \"" + pattern + "\"");
        
        try
        {
            if (!Files.exists(file))
            {
                return;
            }
            
            
            String  content = Files.readString(file);
            Pattern p       = Pattern.compile(pattern);
            Matcher m       = p.matcher(content);
            
            while (m.find())
            {
                int lastStart = 0;
                for (int i = 0; i <= m.groupCount(); i++)
                {
                    String data = m.group(1).toLowerCase();
                    check(data);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void guessFromFontFiles()
    {
        System.out.println("Guessing description variables");
        
        Map<String, Map<String, String>> descs = new HashMap<>();
        try
        {
            Files.walk(dataPath.resolve("data\\menu"))
                 .filter(p -> p.getFileName().toString().contains("fontconfig"))
                 .filter(UtilHandler.filetypePredicate(".txt"))
                 .forEach(p -> {
                     try
                     {
                         ByteArray ba = new ByteArray(Files.readAllBytes(p));
                         if (FileTypeHandler.isProbableRST(ba))
                         {
                             RSTParser parser = new RSTParser();
                             RSTFile   file   = parser.parse(p);
                             file.getEntries().values().forEach(this::checkLine);
                         } else
                         {
                             Map<String, String> desc = Files.readAllLines(p)
                                                             .stream()
                                                             .filter(s -> s.startsWith("tr "))
                                                             .map(s -> s.substring(s.indexOf(" ") + 1))
                                                             .collect(Collectors.toMap(s -> {
                                                                 String part = s.split("=")[0];
                                                                 part = part.substring(part.indexOf("\"") + 1);
                                                                 part = part.substring(0, part.indexOf("\""));
                                                                 return part;
                                                             }, s -> {
                                                                 String part = Arrays.stream(s.split("=")).skip(1).collect(Collectors.joining("="));
                                                                 part = part.substring(part.indexOf("\"") + 1);
                                                                 part = part.substring(0, part.lastIndexOf("\""));
                                                                 return part;
                                                             }));
                        
                             desc.values().forEach(this::checkLine);
                         }
                    
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
    }
    
    private void checkLine(String v)
    {
        String[] parts = v.split("@");
        for (int i = 1; i < parts.length; i += 2)
        {
            String toHash = parts[i];
            
            if (toHash.contains("*"))
            {
                toHash = toHash.substring(0, toHash.indexOf('*'));
            }
            
            this.check(toHash);
        }
    }
    
    public void pullCDTB()
    {
        System.out.println("Feching hashlists from CDTB");
        String hashA = "https://raw.githubusercontent.com/CommunityDragon/CDTB/master/cdragontoolbox/hashes.binentries.txt";
        String hashB = "https://raw.githubusercontent.com/CommunityDragon/CDTB/master/cdragontoolbox/hashes.binfields.txt";
        String hashC = "https://raw.githubusercontent.com/CommunityDragon/CDTB/master/cdragontoolbox/hashes.binhashes.txt";
        String hashD = "https://raw.githubusercontent.com/CommunityDragon/CDTB/master/cdragontoolbox/hashes.bintypes.txt";
        
        Set<String>  changedPlugins = new HashSet<>();
        List<String> data           = WebHandler.readWeb(hashA);
        data.addAll(WebHandler.readWeb(hashB));
        data.addAll(WebHandler.readWeb(hashC));
        data.addAll(WebHandler.readWeb(hashD));
        data.stream().map(line -> line.substring(line.indexOf(' ') + 1)).forEach(this::check);
    }
    
    @Override
    public String generateHash(String val)
    {
        long hashNum = HashHandler.computeFNV1A(val);
        return HashHandler.toHex(hashNum, 8);
    }
    
    
    /**
     * returns false if there are no more hashes
     */
    @Override
    public boolean check(String path)
    {
        String lowerCased = path.toLowerCase();
        if (lowerCased.isBlank())
        {
            return true;
        }
        
        String hash = generateHash(lowerCased);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path, lowerCased);
            return true;
        }
        
        if (this.unknown.isEmpty())
        {
            System.out.println("No more unknown hashes!");
            return false;
        }
        return true;
    }
    
    public void guessFromPets(Path dataPath)
    {
        System.out.println("Guessing new hashes based on characters");
        
        try
        {
            Path chars = dataPath.resolve("data\\characters");
            Files.walk(chars, 1).filter(f -> Files.isDirectory(f)).forEach(f -> check("characters/" + f.getFileName().toString()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
