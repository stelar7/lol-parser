package no.stelar7.cdragon.util.hashguessing;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.types.rst.*;
import no.stelar7.cdragon.util.ProgressBar;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

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
                BINParser  parser = new BINParser();
                List<Path> bins;
                try (Stream<Path> paths = Files.walk(dataPath))
                {
                    bins = paths.filter(UtilHandler.IS_BIN_PREDICATE).toList();
                }

                ProgressBar progress = new ProgressBar("Parsing bin files", bins.size());
                files = bins.parallelStream()
                            .map(p -> {
                                BINFile parsed = parseOrSkip(parser, p);
                                progress.step();
                                return parsed;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                progress.finish();

            } catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        return files;
    }

    /**
     * Parses a .bin file, returns null for files that are not bins, and skips (with a message) corrupt ones,
     * so one bad file does not stop a whole guessing run
     */
    static BINFile parseOrSkip(BINParser parser, Path path)
    {
        try
        {
            return parser.tryParse(path);
        } catch (RuntimeException e)
        {
            System.out.println("Skipping " + path + ": " + e.getMessage());
            return null;
        }
    }
    
    public void guessTFTItems()
    {
        System.out.println("Guessing TFT items");
        
        String prefix = "maps/shipping/map22/items/";
        
        BINParser parser = new BINParser();
        BINFile   parsed = parser.parse(UtilHandler.CDRAGON_FOLDER.resolve(Paths.get("pbe/data/maps/shipping/map22/map22.bin")));
        
        parsed.getEntries().stream()
              .filter(b -> b.getType().equalsIgnoreCase("tftitemdata"))
              .forEach(e -> {
                  String name = (String) e.getIfPresent("mname").getValue();
                  this.check(prefix + name);
              });
    }
    
    
    public void guessTFTParticles()
    {
        System.out.println("Guessing TFT Particles");
        
        String prefix = "maps/shipping/map22/particles/";
        
        BINParser parser = new BINParser();
        BINFile   parsed = parser.parse(UtilHandler.CDRAGON_FOLDER.resolve(Paths.get("pbe/data/maps/shipping/map22/map22.bin")));
        
        parsed.getEntries().stream()
              .filter(b -> b.getType().equalsIgnoreCase("vfxsystemdefinitiondata"))
              .forEach(e -> {
                  String name = (String) e.getIfPresent("particleName").getValue();
                  this.check(prefix + name);
                  this.check(e.getValue("particlePath", ""));
              });
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
                              continue;
                          }

                          BINStruct          animationResourceData = (BINStruct) animationResourcePath.get().getValue();
                          Optional<BINValue> animationFilePath     = animationResourceData.get("mAnimationFilePath");
                          if (animationFilePath.isEmpty())
                          {
                              continue;
                          }

                          // clip names are runs of the filename parts in order, e.g. ahri_idle1_base -> idle1, idle1_base, idle1base, ...
                          String   path     = (String) animationFilePath.get().getValue();
                          String   filename = UtilHandler.removeEnding(UtilHandler.getFilename(path));
                          String[] parts    = filename.split("_");
                          for (int start = 0; start < parts.length; start++)
                          {
                              for (int end = start + 1; end <= parts.length; end++)
                              {
                                  String[] run = Arrays.copyOfRange(parts, start, end);
                                  this.check(String.join("_", run));
                                  this.check(String.join("", run));
                              }
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
                    String data = m.group(1).toLowerCase(Locale.ROOT);
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
            List<Path> fontFiles;
            try (Stream<Path> paths = Files.walk(dataPath.resolve("data").resolve("menu")))
            {
                fontFiles = paths.filter(p -> p.getFileName().toString().contains("fontconfig"))
                                 .filter(UtilHandler.filetypePredicate(".txt"))
                                 .toList();
            }

            ProgressBar progress = new ProgressBar("Guessing from fontconfig files", fontFiles.size());
            fontFiles.parallelStream()
                 .forEach(p -> {
                     progress.step();
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
                                                             .parallel()
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
                        
                             desc.values().stream().parallel().forEach(this::checkLine);
                         }
                    
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });
            progress.finish();
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
        System.out.println("Feching hashlists from CommunityDragon");

        // the CDTB repo no longer has these, and the moonshadow565 fork stopped updating in 2022
        List<String> files = Arrays.asList("hashes.binentries.txt",
                                           "hashes.binfields.txt",
                                           "hashes.binhashes.txt",
                                           "hashes.bintypes.txt");

        for (String file : files)
        {
            HashLists.readCDragonHashList(file, this::checkPrehashed);
        }
    }

    @Override
    public long hashOf(String lowerCased)
    {
        return HashHandler.computeFNV1ALowercase(lowerCased);
    }

    @Override
    protected int hexLength()
    {
        return 8;
    }


    /**
     * returns false if there are no more hashes
     */
    @Override
    public boolean check(String path)
    {
        String lowerCased = path.toLowerCase(Locale.ROOT);
        if (lowerCased.isBlank())
        {
            return true;
        }

        return checkHash(hashOf(lowerCased), path, lowerCased);
    }

    /**
     * returns false if there are no more hashes
     */
    @Override
    protected boolean checkHash(long hash, String printPath, String savePath)
    {
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, printPath, savePath);
            return true;
        }

        if (!this.knownHashes.contains(hash))
        {
            this.addKnown(hash, printPath, savePath);
        }

        if (this.unknown.isEmpty())
        {
            return noMoreHashes();
        }
        return true;
    }
    
    public void guessFromPets(Path dataPath)
    {
        System.out.println("Guessing new hashes based on characters");
        
        try
        {
            Path chars = dataPath.resolve("data").resolve("characters");
            Files.walk(chars, 1).filter(Files::isDirectory).forEach(f -> check("characters/" + f.getFileName().toString()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void guessSpellNames()
    {
        getFiles().stream()
                  .flatMap(b -> b.getEntries().stream())
                  .filter(b -> b.getType().equalsIgnoreCase("SpellObject"))
                  .map(e -> e.get("mSpell"))
                  .filter(Optional::isPresent)
                  .map(e -> (BINStruct) e.get().getValue())
                  .filter(Objects::nonNull)
                  .map(s -> s.get("mDataValues"))
                  .filter(Optional::isPresent)
                  .map(s -> (BINContainer) s.get().getValue())
                  .flatMap(s -> s.getData().stream())
                  .map(s -> (BINStruct) s)
                  .map(s -> s.get("mName"))
                  .filter(Optional::isPresent)
                  .map(s -> (String) s.get().getValue())
                  .forEach(this::check);
    }
}
