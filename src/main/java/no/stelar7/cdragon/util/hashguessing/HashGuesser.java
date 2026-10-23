package no.stelar7.cdragon.util.hashguessing;

import com.google.common.collect.Sets;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

public abstract class HashGuesser
{
    public static HashFile hashFileLCU  = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("lcu.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\wad\\lcu.json"));
    public static HashFile hashFileGAME = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("game.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\wad\\game.json"));
    public static HashFile hashFileBIN  = new HashFile(UtilHandler.CDRAGON_FOLDER.resolve("bin.json"), Paths.get("C:\\Dropbox\\Private\\workspace\\cdragon\\src\\main\\resources\\hashes\\bin\\binhash.json"));
    
    
    protected Set<String> REGIONS = new HashSet<>(Arrays.asList("global", "br", "cn", "eun", "eune", "euw", "garena2", "garena3", "id", "jp", "kr", "la", "la1", "la2",
                                                                "lan", "las", "na", "oc", "oc1", "oce", "pbe", "ph", "ru", "sg", "tencent", "th", "tr", "tw",
                                                                "vn"));
    
    // the locales the client actually ships, the JVM locale list is ~5x larger and full of variants like "ja_jp_jp_#u-ca-japanese"
    protected Set<String> LANGUAGES = new HashSet<>(Arrays.asList("ar_ae", "cs_cz", "de_de", "el_gr", "en_au", "en_gb", "en_ph", "en_sg", "en_us",
                                                                  "es_ar", "es_es", "es_mx", "fr_fr", "hu_hu", "id_id", "it_it", "ja_jp", "ko_kr",
                                                                  "ms_my", "pl_pl", "pt_br", "ro_ro", "ru_ru", "th_th", "tr_tr", "vi_vn", "vn_vn",
                                                                  "zh_cn", "zh_my", "zh_tw"));
    
    
    protected Set<String> hashes;
    protected HashFile    hashFile;

    private volatile boolean foundNew = false;
    // starts dirty so the first save syncs the backup with the loaded file
    private volatile boolean dirty    = true;

    // hashes are kept as longs so checking a guess never has to build a hex string;
    // the collections are concurrent since check() is called from parallel streams
    protected Map<String, String> known;
    protected Set<Long>           knownHashes;
    protected Set<Long>           unknown;
    protected Set<WADFile>        wads;
    protected Set<String>         dirList;

    public HashGuesser(HashFile hashFile, Collection<String> hashes)
    {
        this.hashFile = hashFile;
        this.hashes = new HashSet<>(hashes);

        this.known = new ConcurrentHashMap<>();
        this.knownHashes = ConcurrentHashMap.newKeySet();
        this.hashFile.load(true).forEach((k, v) -> {
            Long parsed = parseHash(k);
            if (parsed != null && v != null)
            {
                this.known.put(k, v);
                this.knownHashes.add(parsed);
            }
        });

        this.unknown = ConcurrentHashMap.newKeySet();
        for (String hash : hashes)
        {
            Long parsed = parseHash(hash);
            if (parsed != null && !this.knownHashes.contains(parsed))
            {
                this.unknown.add(parsed);
            }
        }
    }

    private static Long parseHash(String hex)
    {
        try
        {
            return Long.parseUnsignedLong(hex.trim(), 16);
        } catch (NumberFormatException e)
        {
            return null;
        }
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
        // sorting and writing the full hash list is slow, so skip it when nothing changed
        if (!this.dirty)
        {
            System.out.println("No new hashes, skipping save");
            return;
        }

        System.out.println("Saving current hashes as JSON");
        this.dirty = false;
        this.hashFile.saveToBackupAsJson(this.known);
    }
    
    public void addKnown(long hash, String printPath, String savePath)
    {
        String hex = HashHandler.toHex(hash, hexLength());
        System.out.format(hashFile.printFormat, hex, printPath);
        this.known.put(hex, savePath);
        this.knownHashes.add(hash);
        this.unknown.remove(hash);
        this.foundNew = true;
        this.dirty = true;
    }

    /**
     * Hashes an already lowercased value
     */
    public abstract long hashOf(String lowerCased);

    /**
     * Amount of hex digits used for hashes in the hash file
     */
    protected abstract int hexLength();

    public String generateHash(String val)
    {
        return HashHandler.toHex(hashOf(val), hexLength());
    }

    volatile boolean hasPrinted = false;

    /**
     * returns false if there are no more hashes
     */
    public boolean check(String path)
    {
        if (this.unknown.isEmpty())
        {
            return noMoreHashes();
        }

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

        // scaled variants only exist for images
        if (lowerCased.endsWith(".png") || lowerCased.endsWith(".jpg"))
        {
            String startPart = path.substring(0, path.lastIndexOf('/') + 1);
            String lastPart  = path.substring(path.lastIndexOf('/') + 1);
            if (!lastPart.startsWith("2x") && !lastPart.startsWith("4x"))
            {
                check(startPart + "2x_" + lastPart);
                check(startPart + "4x_" + lastPart);
            }
        }

        return checkHash(hashOf(lowerCased), path, lowerCased);
    }

    /**
     * Checks a "hash path" line (as found in the CDTB hash lists) without rehashing the path
     */
    public boolean checkPrehashed(String line)
    {
        int split = line.indexOf(' ');
        if (split <= 0)
        {
            return true;
        }

        String path = line.substring(split + 1);
        try
        {
            long hash = Long.parseUnsignedLong(line, 0, split, 16);
            return checkHash(hash, path, path.toLowerCase());
        } catch (NumberFormatException e)
        {
            return check(path);
        }
    }

    /**
     * returns false if there are no more hashes
     */
    protected boolean checkHash(long hash, String printPath, String savePath)
    {
        if (this.unknown.contains(hash))
        {
            this.addKnown(hash, printPath, savePath);
            return true;
        }

        if (this.unknown.isEmpty())
        {
            return noMoreHashes();
        }
        return true;
    }

    /**
     * Prints the "done" message once; always returns false so callers can stop
     */
    protected boolean noMoreHashes()
    {
        if (!hasPrinted)
        {
            hasPrinted = true;
            System.out.println("No more unknown hashes!");
        }
        return false;
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
        // only combine names and directories that share the first two path segments (plugin / top level folder),
        // the full cross product of every name with every directory is too large to finish
        Map<String, Set<String>> dirsByRoot  = new HashMap<>();
        Map<String, Set<String>> namesByRoot = new HashMap<>();
        for (String dir : directoryList(true))
        {
            dirsByRoot.computeIfAbsent(pathRoot(dir), k -> new HashSet<>()).add(dir);
        }
        for (String p : this.known.values())
        {
            namesByRoot.computeIfAbsent(pathRoot(p), k -> new HashSet<>()).add(UtilHandler.getFilename(p));
        }
        
        long total = 0;
        for (Map.Entry<String, Set<String>> e : namesByRoot.entrySet())
        {
            total += (long) e.getValue().size() * dirsByRoot.getOrDefault(e.getKey(), Set.of()).size();
        }
        System.out.format("substitute basenames: %s roots, %s combinations%n", namesByRoot.size(), total);
        
        for (Map.Entry<String, Set<String>> e : namesByRoot.entrySet())
        {
            Set<String> dirs = dirsByRoot.getOrDefault(e.getKey(), Set.of());
            for (String name : e.getValue())
            {
                for (String dir : dirs)
                {
                    check(dir + "/" + name);
                }
            }
        }
    }
    
    /**
     * The path up to (not including) the second '/', or the whole path if it has fewer
     */
    private static String pathRoot(String path)
    {
        int first = path.indexOf('/');
        if (first < 0)
        {
            return path;
        }
        
        int second = path.indexOf('/', first + 1);
        return second < 0 ? path : path.substring(0, second);
    }
    
    /**
     * The text before and after the part of a path that gets substituted
     */
    private record Slot(String pre, String post) {}
    
    public void substituteBasenameWords(Set<String> paths, Set<String> words, int amount)
    {
        Pattern   reExtract = Pattern.compile("([^/_.-]+)(?=((?:[-_][^/_.-]+){" + (amount - 1) + "})[^/]*\\.[^/]+$)");
        Set<Slot> slots     = new HashSet<>();
        for (String path : paths)
        {
            reExtract.matcher(path).results().forEach(m -> {
                String match = m.group(1) + m.group(2);
                slots.add(new Slot(path.substring(0, m.start()), path.substring(m.start() + match.length())));
            });
        }
        
        System.out.format("substitute basenames words: %s formats, %s words%n", slots.size() * 2, words.size());
        
        // lazy view, the product is never materialized
        Set<List<String>> product = Sets.cartesianProduct(Collections.nCopies(amount, words));
        for (String sep : Arrays.asList("-", "_"))
        {
            for (Slot slot : slots)
            {
                for (List<String> p : product)
                {
                    check(slot.pre() + String.join(sep, p) + slot.post());
                }
            }
        }
    }
    
    public void addBasenameWord(Set<String> paths, Set<String> words)
    {
        Pattern     reExtract  = Pattern.compile("([^/_.-]+)(?=[^/]*\\.[^/]+$)");
        Set<Slot>   slots      = new HashSet<>();
        Set<String> knownPaths = new HashSet<>(Arrays.asList("assets/characters/", "vo/", "sfx/", "skins_skin"));
        for (String path : paths)
        {
            if (knownPaths.stream().noneMatch(path::contains))
            {
                reExtract.matcher(path).results().forEach(m -> {
                    for (String sep : Arrays.asList("-", "_", "."))
                    {
                        // word before the match, and word after the match
                        slots.add(new Slot(path.substring(0, m.start()), sep + path.substring(m.start())));
                        slots.add(new Slot(path.substring(0, m.end()) + sep, path.substring(m.end())));
                    }
                });
            }
        }
        
        System.out.format("add basename word: %s formats, %s words%n", slots.size(), words.size());
        
        for (Slot slot : slots)
        {
            for (String w : words)
            {
                check(slot.pre() + w + slot.post());
            }
        }
    }
    
    
    public void doubleSubstitution(Set<String> paths, Set<String> words)
    {
        Pattern   reExtract = Pattern.compile("([^/_.-]+)(?=[^/]*\\.[^/]+$)");
        Set<Slot> slots     = new HashSet<>();
        for (String path : paths)
        {
            reExtract.matcher(path).results().forEach(m -> slots.add(new Slot(path.substring(0, m.start()), path.substring(m.end()))));
        }
        
        System.out.format("double substitution: %s formats, %s words%n", slots.size() * 2, words.size());
        for (String sep : Arrays.asList("-", "_"))
        {
            for (Slot slot : slots)
            {
                for (String first : words)
                {
                    String pre = slot.pre() + first + sep;
                    for (String second : words)
                    {
                        check(pre + second + slot.post());
                    }
                }
            }
        }
    }
    
    public void substituteNumbers(Set<String> paths, int max, Optional<Integer> numbers)
    {
        Pattern reExtract = numbers.isEmpty()
                            ? Pattern.compile("([0-9]+)(?=[^/]*\\.[^/]+$)")
                            : Pattern.compile("([0-9]{" + numbers.get() + "})(?=[^/]*\\.[^/]+$)");
        
        Set<Slot> slots = new HashSet<>();
        for (String path : paths)
        {
            reExtract.matcher(path).results().forEach(m -> slots.add(new Slot(path.substring(0, m.start()), path.substring(m.end()))));
        }
        
        // format the numbers once instead of once per path
        String       fmt    = numbers.isEmpty() ? "%d" : "%0" + numbers.get() + "d";
        List<String> values = IntStream.rangeClosed(0, max).mapToObj(n -> String.format(fmt, n)).toList();
        
        System.out.format("substitute numbers: %s formats, nmax = %s%n", slots.size(), max);
        for (Slot slot : slots)
        {
            for (String n : values)
            {
                check(slot.pre() + n + slot.post());
            }
        }
    }
    
    /**
     * Tries every known extension on every known path (directory and name kept, extension swapped).
     * Only sensible for the smaller hash lists (LCU), for game paths use the sibling extension guesser.
     */
    public void substituteExtensions()
    {
        Set<String> stems      = new HashSet<>();
        Set<String> extensions = new HashSet<>();

        for (String path : this.known.values())
        {
            int slash = path.lastIndexOf('/');
            int dot   = path.lastIndexOf('.');
            if (dot <= slash)
            {
                continue;
            }

            stems.add(path.substring(0, dot + 1).toLowerCase(Locale.ENGLISH));
            extensions.add(path.substring(dot + 1).toLowerCase(Locale.ENGLISH));
        }

        System.out.format("substitute extensions: %s paths, %s extensions%n", stems.size(), extensions.size());

        stems.parallelStream().forEach(stem -> {
            for (String ext : extensions)
            {
                check(stem + ext);
            }
        });
    }

    /**
     * The locales this guesser substitutes
     */
    public Set<String> getLanguages()
    {
        return Collections.unmodifiableSet(LANGUAGES);
    }

    /**
     * Builds templates from the known paths by cutting out every occurrence of a matched value
     * (all occurrences of the same value are replaced together, so {@code vo/en_us/x_en_us.bnk} keeps both in sync),
     * then fills each template with every replacement.
     * Templates are deduplicated, so paths that only differ by the substituted value are only tried once.
     *
     * @param name         printed in the progress message
     * @param paths        the paths to build templates from
     * @param pattern      finds the values to substitute, group 1 is the value that is replaced
     * @param accept       filters the matched values, eg. to only accept real locales
     * @param replacements the values put into the templates
     */
    protected void substituteTemplates(String name, Collection<String> paths, Pattern pattern, Predicate<String> accept, Collection<String> replacements)
    {
        Set<List<String>> templates = buildTemplates(paths, pattern, accept);

        System.out.format("%s: %s templates (from %s paths), %s replacements%n", name, templates.size(), paths.size(), replacements.size());

        templates.parallelStream().forEach(parts -> {
            StringBuilder sb = new StringBuilder();
            for (String value : replacements)
            {
                sb.setLength(0);
                sb.append(parts.get(0));
                for (int i = 1; i < parts.size(); i++)
                {
                    sb.append(value).append(parts.get(i));
                }
                check(sb.toString());
            }
        });
    }

    /**
     * Splits each path around the occurrences of the matched values, see {@link #substituteTemplates}
     */
    static Set<List<String>> buildTemplates(Collection<String> paths, Pattern pattern, Predicate<String> accept)
    {
        Set<List<String>> templates = ConcurrentHashMap.newKeySet();
        paths.parallelStream().forEach(raw -> {
            String  path   = raw.toLowerCase(Locale.ENGLISH);
            Matcher m      = pattern.matcher(path);
            Set<String> values = new HashSet<>();
            while (m.find())
            {
                if (accept.test(m.group(1)))
                {
                    values.add(m.group(1));
                }
            }

            for (String value : values)
            {
                // split on exact occurrences found by the pattern, not on any substring equal to the value
                List<String> parts = new ArrayList<>();
                int          last  = 0;
                m.reset();
                while (m.find())
                {
                    if (m.group(1).equals(value))
                    {
                        parts.add(path.substring(last, m.start(1)));
                        last = m.end(1);
                    }
                }
                parts.add(path.substring(last));
                templates.add(List.copyOf(parts));
            }
        });
        return templates;
    }

    /**
     * The maintained hash lists from CommunityDragon
     */
    public static final String CDRAGON_HASH_URL = "https://raw.communitydragon.org/data/hashes/lol/";
    /**
     * Mirror of the same lists on GitHub, large lists are split into name.0, name.1, ...
     */
    public static final String GITHUB_HASH_URL  = "https://raw.githubusercontent.com/CommunityDragon/Data/master/hashes/lol/";

    /**
     * Streams a CommunityDragon hash list (eg. "hashes.game.txt") line by line into the consumer.
     * Uses raw.communitydragon.org, falling back to the GitHub mirror (and its split parts) if that fails.
     *
     * @return false if the list could not be loaded from anywhere
     */
    public static boolean readCDragonHashList(String name, Consumer<String> consumer)
    {
        if (readWebLines(CDRAGON_HASH_URL + name, consumer))
        {
            return true;
        }

        System.out.println("Unable to load " + name + " from raw.communitydragon.org, trying GitHub");
        if (readWebLines(GITHUB_HASH_URL + name, consumer))
        {
            return true;
        }

        boolean any = false;
        for (int part = 0; readWebLines(GITHUB_HASH_URL + name + "." + part, consumer); part++)
        {
            any = true;
        }

        if (!any)
        {
            System.out.println("Unable to load hash list " + name);
        }
        return any;
    }

    /**
     * Streams the lines of a text url without holding the whole file in memory (hashes.game.txt is >200MB)
     *
     * @return false if the url did not answer with 200
     */
    public static boolean readWebLines(String url, Consumer<String> consumer)
    {
        try
        {
            HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
            con.setConnectTimeout(30_000);
            con.setReadTimeout(120_000);
            if (con.getResponseCode() != 200)
            {
                con.disconnect();
                return false;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = in.readLine()) != null)
                {
                    if (!line.isEmpty())
                    {
                        consumer.accept(line);
                    }
                }
            }
            return true;
        } catch (IOException | IllegalArgumentException e)
        {
            System.err.println("Unable to read " + url + ": " + e.getMessage());
            return false;
        }
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
        
        long hash = hashOf(lowerCased);
        return this.unknown.contains(hash) || this.knownHashes.contains(hash);
    }
    
    public void guessByExistingWords(Set<String> prefixes)
    {
        guessByExistingWords(prefixes, 3);
    }
    
    /**
     * Tries prefix + word (+ separator + word)* + ending, with up to maxWords words
     */
    public void guessByExistingWords(Set<String> prefixes, int maxWords)
    {
        Set<String> endings = new HashSet<>();
        Set<String> words   = this.generateWordListFromKnownHashes();
        this.known.values().forEach(v -> endings.add(v.substring(v.lastIndexOf('.'))));
        
        System.out.format("Guessing by existing words: %s prefixes, %s words, %s endings, up to %s words%n", prefixes.size(), words.size(), endings.size(), maxWords);
        
        for (String prefix : prefixes)
        {
            guessByExistingWords(prefix, words, endings, maxWords);
        }
    }
    
    private void guessByExistingWords(String current, Set<String> words, Set<String> endings, int wordsLeft)
    {
        for (String word : words)
        {
            String path = current + word;
            for (String ending : endings)
            {
                this.check(path + ending);
            }
            
            if (wordsLeft <= 1 || word.isEmpty())
            {
                continue;
            }
            
            for (String separator : WORD_SEPARATORS)
            {
                guessByExistingWords(path + separator, words, endings, wordsLeft - 1);
            }
        }
    }
    
    private static final Set<String> WORD_SEPARATORS = Set.of("", "_", ".", "/", "-");
}
