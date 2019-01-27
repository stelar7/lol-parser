package no.stelar7.cdragon.util.hashguessing;

import com.google.common.collect.Sets;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public class HashGuesser
{
    
    private Set<String> hashes;
    private HashFile    hashFile;
    
    private Map<String, String> known;
    private Set<String>         unknown;
    private Set<WADFile>        wads;
    private Set<String>         dirList;
    
    public HashGuesser(HashFile hashFile, Set<String> hashes)
    {
        this.hashFile = hashFile;
        this.hashes = hashes;
        
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
    
    public static Set<String> unknownFromExport(Path baseSearchPath)
    {
        try
        {
            Set<String> unknowns     = new HashSet<>();
            List<Path>  unknownFiles = Files.walk(baseSearchPath).filter(p -> p.getFileName().toString().contains(".unknown.txt")).collect(Collectors.toList());
            for (Path unk : unknownFiles)
            {
                unknowns.addAll(Files.readAllLines(unk));
            }
            
            return unknowns;
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return Collections.emptySet();
    }
    
    public void save()
    {
        this.hashFile.save();
    }
    
    public void addKnown(String hash, String path)
    {
        System.out.format("%s %s%n", hash, path);
        this.known.put(hash, path);
        this.unknown.remove(hash);
    }
    
    public void check(String path)
    {
        String hash = HashHandler.computeXXHash64(path);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path);
        }
    }
    
    public boolean isKnown(String path)
    {
        String hash = HashHandler.computeXXHash64(path);
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, path);
            return true;
        }
        
        return this.known.containsKey(hash);
    }
    
    public void check(String... paths)
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
                check(String.format("%s/%s", dir, name));
            }
        }
    }
    
    public Set<String> directoryList(boolean cached)
    {
        if (!cached || this.dirList == null)
        {
            Set<String> dirs  = new HashSet<>();
            Set<String> bases = new HashSet<>(this.known.values());
            
            while (!bases.isEmpty())
            {
                bases.removeAll(dirs);
                for (String p : bases)
                {
                    dirs.add(new File(p).getParent());
                }
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
            names.add(new File(p).getName());
        }
        
        System.out.format("substitute basenamses: %s basenames, %s directories%n", names.size(), dirs.size());
        
        for (String name : names)
        {
            for (String dir : dirs)
            {
                check(String.format("%s/%s", dir, name));
            }
        }
    }
    
    public void substituteBasenameWords(Set<String> paths, Set<String> words, int amount)
    {
        String formatPartPart = "{sep}%%s".repeat(amount - 1);
        String formatPart     = "%s%%s" + formatPartPart + "%s";
        
        Pattern     reExtract   = Pattern.compile("([^/_.-]+)(?=((?:[-_][^/_.-]+){{" + (amount - 1) + "}})[^/]*\\.[^/]+$)");
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
        
        System.out.format("substitute basenamses: %s formats, %s words%n", formats.size(), words.size());
        Set<String> product = UtilHandler.product(words, amount);
        for (String fmt : formats)
        {
            for (String p : product)
            {
                String hash = HashHandler.computeXXHash64(String.format(fmt, p));
                check(hash);
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
                    for (String sep : Arrays.asList("-", "_"))
                    {
                        formats.add(String.format("%s%s%%s%s", path.substring(0, m.start()), sep, path.substring(m.start())));
                        formats.add(String.format("%s%s%%s%s", path.substring(0, m.end()), sep, path.substring(m.end())));
                    }
                });
            }
        }
        
        System.out.format("add basename word: %s formats, %s words%n", formats.size(), words.size());
        
        for (String fmt : formats)
        {
            for (String w : words)
            {
                String hash = HashHandler.computeXXHash64(String.format(fmt, w));
                check(hash);
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
                    formats.add(String.format("%s%%s%s%%s%s", start, sep, end));
                }
            });
        }
        
        Set<String> product = UtilHandler.product(words, 2);
        System.out.format("double substitution: %s formats, %s words%n", formats.size(), words.size());
        for (String fmt : formats)
        {
            for (String p : product)
            {
                String hash = HashHandler.computeXXHash64(String.format(fmt, p));
                check(hash);
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
            reExtract = Pattern.compile(String.format("([0-9]{%d})(?=[^/]*\\.[^/]+$)", numbers.get()));
        }
        
        
        Set<String> formats = new HashSet<>();
        for (String path : paths)
        {
            String finalFmt = fmtTemp;
            reExtract.matcher(path).results().forEach(m -> {
                String start = path.substring(0, m.start());
                String end   = path.substring(m.end());
                formats.add(String.format("%s%s%s", start, finalFmt, end));
            });
        }
        
        
        System.out.format("substitute numbers: %s formats, nmax = %s%n", formats.size(), max);
        
        for (String fmt : formats)
        {
            IntStream.rangeClosed(0, max).forEach(n -> {
                String hash = HashHandler.computeXXHash64(String.format(fmt, n));
                check(hash);
            });
        }
    }
    
    public void substituteExtensions()
    {
        /*
         prefixes = set()
        extensions = set()
        for path in self.known.values():
            prefix, ext = os.path.splitext(path)
            prefixes.add(prefix)
            extensions.add(ext)

        logger.info(f"substitute extensions: {len(prefixes)} prefixes, {len(extensions)} extensions")
        for prefix in progress_iterator(sorted(prefixes)):
            self.check_iter(prefix + ext for ext in extensions)
         */
        
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
    
    public Set<String> getFilesAsText(WADFile wad)
    {
        Set<String> data = new HashSet<>();
        
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
            
            String content = new String(wad.readContentFromHeaderData(header));
            data.add(content);
        }
        
        return data;
    }
    
}
