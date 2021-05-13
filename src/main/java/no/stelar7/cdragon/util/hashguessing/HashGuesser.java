package no.stelar7.cdragon.util.hashguessing;

import com.google.common.collect.Sets;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;

import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public abstract class HashGuesser
{
    public static HashFile hashFileLCU  = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("lcu.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\wad\\lcu.json"));
    public static HashFile hashFileGAME = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("game.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\wad\\game.json"));
    public static HashFile hashFileBIN  = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("bin.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\bin\\binhash.json"));
    
    
    protected Set<String> REGIONS = new HashSet<>(Arrays.asList("global", "br", "cn", "eun", "eune", "euw", "garena2", "garena3", "id", "jp", "kr", "la", "la1", "la2",
                                                                "lan", "las", "na", "oc", "oc1", "oce", "pbe", "ph", "ru", "sg", "tencent", "th", "tr", "tw",
                                                                "vn"));
    
    protected Set<String> LANGUAGES = new HashSet<>(Arrays.stream(DateFormat.getAvailableLocales())
                                                          .map(l -> l.toString().toLowerCase(Locale.ENGLISH))
                                                          .filter(s -> s.contains("_"))
                                                          .collect(Collectors.toSet()));
    
    
    protected Set<String> hashes;
    protected HashFile    hashFile;
    
    private boolean foundNew = false;
    
    protected Map<String, String> known;
    protected Set<String>         unknown;
    protected Set<WADFile>        wads;
    protected Set<String>         dirList;
    
    public HashGuesser(HashFile hashFile, Collection<String> hashes)
    {
        this.hashFile = hashFile;
        this.hashes = new HashSet<>(hashes);
        
        this.known = this.hashFile.load(true);
        this.unknown = new HashSet<>(hashes);
        this.unknown.removeAll(this.known.keySet());
    }
    
    public HashGuesser fromWads(Set<WADFile> wads)
    {
        this.wads = wads;
        this.hashes = new HashSet<>();
        for (WADFile wad : wads)
        {
            for (WADContentHeaderV1 header : wad.getContentHeaders())
            {
                hashes.add(header.getPathHash());
            }
        }
        return this;
    }
    
    public static Set<String> unknownFromExportWAD(Path unknownFile)
    {
        try
        {
            return Files.readAllLines(unknownFile).stream().map(a -> a.substring(0, 16)).collect(Collectors.toSet());
            
        } catch (IOException e)
        {
            System.err.println(e.getMessage());
            System.out.println("Unable to load unknown hash file");
        }
        
        return Collections.emptySet();
    }
    
    public static Set<String> unknownFromExportBIN(Path unknownFile)
    {
        try
        {
            return new HashSet<>(Files.readAllLines(unknownFile));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return Collections.emptySet();
    }
    
    public boolean didFindNewHash()
    {
        return foundNew;
    }
    
    public void save()
    {
        System.out.println("Saving current hashes as CDTB format");
        this.hashFile.save(this.known);
    }
    
    public void saveAsJson()
    {
        System.out.println("Saving current hashes as JSON");
        this.hashFile.saveAsJson(this.known, hashFile.file);
    }
    
    public void saveAsJson(Path output)
    {
        System.out.println("Saving current hashes as JSON");
        this.hashFile.saveAsJson(this.known, output);
    }
    
    public void saveToBackup()
    {
        System.out.println("Saving current hashes as JSON");
        this.hashFile.saveToBackupAsJson(this.known);
    }
    
    public void addKnown(String hash, String printPath, String savePath)
    {
        System.out.format(hashFile.printFormat, hash, printPath);
        this.known.put(hash, savePath);
        this.unknown.remove(hash);
        this.foundNew = true;
        
        if (this.unknown.isEmpty())
        {
            System.out.println("No more unknown hashes!");
            System.exit(0);
        }
    }
    
    public abstract String generateHash(String val);
    
    boolean hasPrinted = false;
    
    /**
     * returns false if there are no more hashes
     */
    public boolean check(String path)
    {
        String lowerCased = path.toLowerCase();
        if (lowerCased.isBlank())
        {
            return true;
        }
        
        if (lowerCased.endsWith(".lua"))
        {
            String test = lowerCased.substring(0, lowerCased.lastIndexOf(".lua"));
            check(test + ".luabin");
            check(test + ".luabin64");
            check(test + ".preload");
        }
        
        String startPart = path.substring(0, path.lastIndexOf('/') + 1);
        String lastPart  = path.substring(path.lastIndexOf('/') + 1);
        if (!lastPart.startsWith("2x") && !lastPart.startsWith("4x"))
        {
            check(startPart + "2x_" + lastPart);
            check(startPart + "4x_" + lastPart);
        }
        
        String hash = generateHash(lowerCased);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path, lowerCased);
            return true;
        }
        
        if (this.unknown.isEmpty())
        {
            if (!hasPrinted)
            {
                System.out.println("No more unknown hashes!");
                hasPrinted = false;
            }
            return false;
        }
        return true;
    }
    
    public void check(String... paths)
    {
        for (String path : paths)
        {
            check(path);
        }
    }
    
    public void check(Collection<String> paths)
    {
        for (String path : paths)
        {
            check(path);
        }
    }
    
    public void checkTextList(String text)
    {
        check(text.split(" "));
    }
    
    public void checkXDBGHashes(Path file)
    {
        try
        {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines)
            {
                if (line.startsWith("hash: "))
                {
                    check(line.split("\"")[1]);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void checkBasenames(Set<String> names)
    {
        Set<String> dirs = directoryList(true);
        for (String name : names)
        {
            for (String dir : dirs)
            {
                check(dir + "/" + name);
            }
        }
    }
    
    public Set<String> directoryList(boolean cached)
    {
        if (!cached || this.dirList == null)
        {
            Set<String> dirs  = new TreeSet<>();
            Set<String> bases = new TreeSet<>(this.known.values());
            
            for (String p : bases)
            {
                dirs.add(p.substring(0, p.lastIndexOf('/')));
            }
            
            this.dirList = dirs;
        }
        
        return this.dirList;
    }
    
    public void substituteBasenames()
    {
        Set<String> dirs  = directoryList(true);
        Set<String> names = new HashSet<>();
        for (String p : this.known.values())
        {
            names.add(UtilHandler.getFilename(p));
        }
        
        System.out.format("substitute basenamses: %s basenames, %s directories%n", names.size(), dirs.size());
        
        for (String name : names)
        {
            for (String dir : dirs)
            {
                check(dir + "/" + name);
            }
        }
    }
    
    public void substituteBasenameWords(Set<String> paths, Set<String> words, int amount)
    {
        String formatPartPart = "{sep}%%s".repeat(amount - 1);
        String formatPart     = "%s%%s" + formatPartPart + "%s";
        
        Pattern     reExtract   = Pattern.compile("([^/_.-]+)(?=((?:[-_][^/_.-]+){" + (amount - 1) + "})[^/]*\\.[^/]+$)");
        Set<String> tempFormats = new HashSet<>();
        for (String path : paths)
        {
            reExtract.matcher(path).results().forEach(m -> {
                String match     = m.group(1) + m.group(2);
                String pre       = path.substring(0, m.start());
                String post      = path.substring(m.start() + match.length());
                String formatted = String.format(formatPart, pre, post);
                tempFormats.add(formatted);
            });
        }
        
        Set<String> formats = new HashSet<>();
        for (String sep : Arrays.asList("-", "_"))
        {
            for (String fmt : tempFormats)
            {
                formats.add(fmt.replace("{sep}", sep));
            }
        }
        
        System.out.format("substitute basenames words: %s formats, %s words%n", formats.size(), words.size());
        Set<String> product = UtilHandler.product(words, amount);
        for (String fmt : formats)
        {
            for (String p : product)
            {
                String[] parts   = fmt.split("%s");
                String   toCheck = parts[0] + p + parts[1];
                check(toCheck);
            }
        }
    }
    
    public void addBasenameWord(Set<String> paths, Set<String> words)
    {
        Pattern     reExtract  = Pattern.compile("([^/_.-]+)(?=[^/]*\\.[^/]+$)");
        Set<String> formats    = new HashSet<>();
        Set<String> knownPaths = new HashSet<>(Arrays.asList("assets/characters/", "vo/", "sfx/", "skins_skin"));
        for (String path : paths)
        {
            if (knownPaths.stream().noneMatch(path::contains))
            {
                reExtract.matcher(path).results().forEach(m -> {
                    for (String sep : Arrays.asList("-", "_", "."))
                    {
                        String pre     = path.substring(0, m.start());
                        String preEnd  = path.substring(m.start());
                        String post    = path.substring(0, m.end());
                        String postEnd = path.substring(m.end());
                        
                        formats.add(pre + sep + "%s" + preEnd);
                        formats.add(post + sep + "%s" + postEnd);
                    }
                });
            }
        }
        
        System.out.format("add basename word: %s formats, %s words%n", formats.size(), words.size());
        
        for (String fmt : formats)
        {
            for (String w : words)
            {
                String[] parts   = fmt.split("%s");
                String   toCheck = parts[0] + w + parts[1];
                check(toCheck);
            }
        }
    }
    
    
    public void doubleSubstitution(Set<String> paths, Set<String> words)
    {
        Pattern     reExtract = Pattern.compile("([^/_.-]+)(?=[^/]*\\.[^/]+$)");
        Set<String> formats   = new HashSet<>();
        for (String path : paths)
        {
            reExtract.matcher(path).results().forEach(m -> {
                for (String sep : Arrays.asList("-", "_"))
                {
                    String start = path.substring(0, m.start());
                    String end   = path.substring(m.end());
                    
                    formats.add(start + "%s" + sep + "%s" + end);
                }
            });
        }
        
        Set<String> product = UtilHandler.product(words, 2);
        System.out.format("double substitution: %s formats, %s words%n", formats.size(), words.size());
        for (String fmt : formats)
        {
            for (String p : product)
            {
                String[] parts   = fmt.split("%s");
                String   toCheck = parts[0] + p + parts[1];
                check(toCheck);
            }
        }
    }
    
    public void substituteNumbers(Set<String> paths, int max, Optional<Integer> numbers)
    {
        int     digits;
        String  fmtTemp   = null;
        Pattern reExtract = null;
        
        if (numbers.isPresent() && numbers.get() == null)
        {
            numbers = Optional.of(String.valueOf(max).length());
        }
        
        if (numbers.isEmpty())
        {
            fmtTemp = "%d";
            reExtract = Pattern.compile("([0-9]+)(?=[^/]*\\.[^/]+$)");
        } else
        {
            fmtTemp = "%0" + numbers.get() + "d";
            reExtract = Pattern.compile("([0-9]{" + numbers.get() + "})( ? =[^/]*\\.[^/]+$)");
        }
        
        
        Set<String> formats = new HashSet<>();
        for (
                String path : paths)
        
        {
            String finalFmt = fmtTemp;
            reExtract.matcher(path).results().forEach(m -> {
                String start = path.substring(0, m.start());
                String end   = path.substring(m.end());
                formats.add(start + finalFmt + end);
            });
        }
        
        
        System.out.format("substitute numbers: %s formats, nmax = %s%n", formats.size(), max);
        for (String fmt : formats)
        {
            IntStream.rangeClosed(0, max).forEach(n -> check(String.format(fmt, n)));
        }
        
    }
    
    public void substituteExtensions()
    {
        Set<String> prefixes   = new HashSet<>();
        Set<String> extensions = new HashSet<>();
        
        for (String path : this.known.values())
        {
            String filename = UtilHandler.getFilename(path);
            String ext      = UtilHandler.getEnding(filename);
            String prefix   = filename.substring(0, filename.lastIndexOf('.'));
            
            prefixes.add(prefix);
            extensions.add(ext);
        }
        
        System.out.format("substitute extensions: %s prefixes, %s extensions%n", prefixes.size(), extensions.size());
        
        Set<String> paths = new HashSet<>();
        for (List<String> strings : Sets.cartesianProduct(prefixes, extensions))
        {
            paths.add(String.join("", strings));
        }
        
        check(paths.toArray(new String[0]));
    }
    
    public Set<Triplet<String, String, String>> getFilesAsText(WADFile wad)
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
    
    
    public Set<String> buildWordlist(Set<String> paths)
    {
        Set<String> words   = new HashSet<>();
        Pattern     reSplit = Pattern.compile("[/_.-]");
        for (String path : paths)
        {
            words.addAll(Arrays.asList(reSplit.split(path)));
        }
        
        Pattern reFilterWords = Pattern.compile("^[0-9]{3,}$");
        words.removeIf(word -> reFilterWords.matcher(word).find());
        
        return words;
    }
    
    public Set<String> generateWordListFromKnownHashes()
    {
        System.out.println("Generating word list from known hashes");
        
        Set<String> data = new HashSet<>();
        this.known.values().forEach(v -> {
            String[] parts = v.split("_|\\.|/|\\|-|-");
            
            for (String part : parts)
            {
                try
                {
                    // if its a number, add some numbers above and below it
                    int value = Integer.parseInt(part);
                    for (int i = value - 100; i < value + 100; i++)
                    {
                        if (i < 0)
                        {
                            continue;
                        }
                        
                        data.add(String.valueOf(i));
                    }
                } catch (NumberFormatException e)
                {
                    // ignore
                }
                
                data.add(part);
            }
        });
        
        return data;
    }
    
    /**
     * returns true if this hash is already known, or is a new valid hash
     */
    public boolean checkKnown(String test)
    {
        String lowerCased = test.toLowerCase();
        if (lowerCased.isBlank())
        {
            return true;
        }
        
        String hash = generateHash(lowerCased);
        if (this.unknown.contains(hash))
        {
            return true;
        }
        
        return this.known.containsKey(hash);
    }
    
    public void guessByExistingWords(Set<String> prefixes)
    {
        Set<String> separators = Set.of("", "_", ".", "/", "-");
        Set<String> endings    = new HashSet<>();
        Set<String> words      = this.generateWordListFromKnownHashes();
        this.known.values().forEach(v -> endings.add(v.substring(v.lastIndexOf('.'))));
        
        System.out.println("Guessing by existing words");
        
        for (String plugin : prefixes)
        {
            for (String word : words)
            {
                for (String ending : endings)
                {
                    this.check(plugin + word + ending);
                }
                
                if (word.isEmpty())
                {
                    continue;
                }
                
                for (String separator : separators)
                {
                    String path1 = word + separator;
                    
                    for (String word2 : words)
                    {
                        for (String ending : endings)
                        {
                            this.check(plugin + path1 + word2 + ending);
                        }
                        
                        if (word2.isEmpty())
                        {
                            continue;
                        }
                        
                        for (String separator2 : separators)
                        {
                            String path2 = word2 + separator2;
                            
                            for (String word3 : words)
                            {
                                for (String ending : endings)
                                {
                                    this.check(plugin + path1 + path2 + word3 + ending);
                                }
                                
                                if (word3.isEmpty())
                                {
                                    continue;
                                }
                                
                                for (String separator3 : separators)
                                {
                                    String path3 = word3 + separator3;
                                    
                                    for (String word4 : words)
                                    {
                                        for (String ending : endings)
                                        {
                                            this.check(plugin + path1 + word2 + path3 + word4 + ending);
                                        }
                                        
                                        if (word4.isEmpty())
                                        {
                                            continue;
                                        }
                                        
                                        for (String separator4 : separators)
                                        {
                                            String path4 = word4 + separator4;
                                            
                                            for (String word5 : words)
                                            {
                                                for (String ending : endings)
                                                {
                                                    this.check(plugin + path1 + word2 + path3 + path4 + word5 + ending);
                                                }
                                                
                                                if (word5.isEmpty())
                                                {
                                                    continue;
                                                }
                                                
                                                for (String separator5 : separators)
                                                {
                                                    String path5 = word5 + separator5;
                                                    
                                                    for (String word6 : words)
                                                    {
                                                        for (String ending : endings)
                                                        {
                                                            this.check(plugin + path1 + word2 + path3 + path4 + path5 + word6 + ending);
                                                        }
                                                        
                                                        if (word6.isEmpty())
                                                        {
                                                            continue;
                                                        }
                                                        
                                                        for (String separator6 : separators)
                                                        {
                                                            String path6 = word6 + separator6;
                                                            
                                                            for (String word7 : words)
                                                            {
                                                                for (String ending : endings)
                                                                {
                                                                    this.check(plugin + path1 + word2 + path3 + path4 + path5 + path6 + word7 + ending);
                                                                }
                                                                
                                                                if (word7.isEmpty())
                                                                {
                                                                    continue;
                                                                }
                                                                
                                                                for (String separator7 : separators)
                                                                {
                                                                    String path7 = word7 + separator7;
                                                                    
                                                                    for (String word8 : words)
                                                                    {
                                                                        for (String ending : endings)
                                                                        {
                                                                            this.check(plugin + path1 + word2 + path3 + path4 + path5 + path6 + path7 + word8 + ending);
                                                                        }
                                                                        
                                                                        if (word8.isEmpty())
                                                                        {
                                                                            continue;
                                                                        }
                                                                        
                                                                        for (String separator8 : separators)
                                                                        {
                                                                            String path8 = word8 + separator8;
                                                                            
                                                                            for (String word9 : words)
                                                                            {
                                                                                for (String ending : endings)
                                                                                {
                                                                                    this.check(plugin + path1 + word2 + path3 + path4 + path5 + path6 + path7 + path8 + word9 + ending);
                                                                                }
                                                                                
                                                                                if (word9.isEmpty())
                                                                                {
                                                                                    continue;
                                                                                }
                                                                                
                                                                                for (String separator9 : separators)
                                                                                {
                                                                                    String path9 = word9 + separator9;
                                                                                    
                                                                                    for (String word10 : words)
                                                                                    {
                                                                                        for (String ending : endings)
                                                                                        {
                                                                                            this.check(plugin + path1 + word2 + path3 + path4 + path5 + path6 + path7 + path8 + path9 + word10 + ending);
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
