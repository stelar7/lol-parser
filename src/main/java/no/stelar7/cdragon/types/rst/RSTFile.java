package no.stelar7.cdragon.types.rst;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.*;

public final class RSTFile
{
    private final String            magic;
    private final int               major;
    private final int               minor;
    private final String            config;
    private final int               hashBits;
    private final Map<Long, String> entries;
    
    /**
     * @param minor    the "has config" flag of version 2 files, 0 otherwise
     * @param config   the font config of version 2 files, or null
     * @param hashBits the number of bits of each entry used for the hash, see {@link #getHashBits()}
     * @param entries  hash to text (an unmodifiable view of the map is kept)
     */
    public RSTFile(String magic, int major, int minor, String config, int hashBits, Map<Long, String> entries)
    {
        this.magic = magic;
        this.major = major;
        this.minor = minor;
        this.config = config;
        this.hashBits = hashBits;
        this.entries = Collections.unmodifiableMap(entries);
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public int getMajor()
    {
        return major;
    }
    
    public int getMinor()
    {
        return minor;
    }
    
    public String getConfig()
    {
        return config;
    }
    
    public Map<Long, String> getEntries()
    {
        return entries;
    }
    
    /**
     * The number of bits of each entry used for the hash (40 for version 2/3, 39 or 38 for version 4/5), the rest is the string offset
     */
    public int getHashBits()
    {
        return hashBits;
    }
    
    /**
     * Looks up a key. Returns the key itself if it is not in the file.
     * <p>
     * Files from patch 14.15 and newer hash the lowercased key with xxh3_64, older files with xxhash64 (see CDTB rstfile.py).
     * The file does not say which one it uses, so xxh3 is tried first, then xxhash64.
     */
    public String getFromHash(String key)
    {
        String lowerCased = key.toLowerCase(Locale.ENGLISH);

        String value = getFromHash(HashHandler.computeXXH3AsLong(lowerCased), null);
        if (value == null)
        {
            value = getFromHash(HashHandler.computeXXHash64AsLong(lowerCased), null);
        }
        return value == null ? key : value;
    }
    
    /**
     * Looks up an already hashed key, the hash is masked to the hash bits used by this file
     */
    public String getFromHash(long hash, String defaultValue)
    {
        return entries.getOrDefault(hash & ((1L << hashBits) - 1), defaultValue);
    }
    
    /**
     * Returns the entries keyed by their name, sorted by key.
     * The hashes in keyNames may be full 64 bit hashes (eg. {@link RSTKeyNames#XXH3_URL}), they are masked to the hash bits of this file.
     * Entries without a name are keyed as "{hex hash}".
     *
     * @param keyNames hash to key name, see {@link RSTKeyNames}
     */
    public Map<String, String> getNamedEntries(Map<Long, String> keyNames)
    {
        long              mask   = (1L << hashBits) - 1;
        Map<Long, String> masked = new HashMap<>(keyNames.size() * 2);
        keyNames.forEach((hash, name) -> masked.putIfAbsent(hash & mask, name));

        int                 hexLength = (hashBits + 3) / 4;
        Map<String, String> named     = new TreeMap<>();
        entries.forEach((hash, text) -> {
            String name = masked.get(hash);
            if (name == null)
            {
                name = String.format("{%0" + hexLength + "x}", hash);
            }
            named.put(name, text);
        });
        return named;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RSTFile rstFile = (RSTFile) o;
        return major == rstFile.major &&
               minor == rstFile.minor &&
               Objects.equals(magic, rstFile.magic) &&
               Objects.equals(config, rstFile.config) &&
               Objects.equals(entries, rstFile.entries);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(magic, major, minor, config, entries);
    }
    
    @Override
    public String toString()
    {
        return "RSTFile{" +
               "magic='" + magic + '\'' +
               ", major=" + major +
               ", minor=" + minor +
               ", config='" + config + '\'' +
               ", entries=" + entries +
               '}';
    }
}
