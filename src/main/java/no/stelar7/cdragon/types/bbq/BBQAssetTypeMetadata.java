package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * The type section of a serialized file.
 * See SerializedType in https://github.com/K0lb3/UnityPy/blob/master/UnityPy/files/SerializedFile.py
 */
public class BBQAssetTypeMetadata
{
    private static final String DEFAULT_STRUCTS = "bbq/structs.dat";

    private static volatile BBQAssetTypeMetadata defaultTypes;

    List<Integer>             classIds    = new ArrayList<>();
    /**
     * The type tree of each type, in file order (null where the file has no type tree)
     */
    List<BBQTypeTree>         typeList    = new ArrayList<>();
    Map<Integer, BBQTypeTree> typeTrees   = new HashMap<>();
    Map<Integer, byte[]>      hashes      = new HashMap<>();
    List<BBQTypeTree>         refTypes    = new ArrayList<>();
    List<String>              refClasses  = new ArrayList<>();
    Map<String, BBQTypeTree>  sharedSubtrees = new LinkedHashMap<>();
    boolean                   hasTypeTree = true;
    BBQAsset                  asset;
    String                    generatorVersion;
    BBQRuntimePlatform        targetPlatform;

    private int       format;
    private ByteOrder endian;

    public BBQAssetTypeMetadata(BBQAsset asset)
    {
        this.asset = asset;
        if (asset != null)
        {
            this.format = asset.format;
            this.endian = asset.endian;
        }
    }

    /**
     * Loads the type trees bundled with this library (UnityPack's structs.dat, Unity 5.5, serialized format 15, little-endian).
     * Used for stripped files that do not contain their own type trees.
     * The result is cached.
     */
    public static BBQAssetTypeMetadata getDefault()
    {
        if (defaultTypes == null)
        {
            synchronized (BBQAssetTypeMetadata.class)
            {
                if (defaultTypes == null)
                {
                    byte[] data = UtilHandler.readInternalAsBytes(DEFAULT_STRUCTS);
                    if (data == null)
                    {
                        throw new ParseException("Unable to load the default type trees from " + DEFAULT_STRUCTS, -1);
                    }

                    BBQAssetTypeMetadata types = new BBQAssetTypeMetadata(null);
                    types.format = 15;
                    types.endian = ByteOrder.LITTLE_ENDIAN;
                    types.load(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
                    defaultTypes = types;
                }
            }
        }

        return defaultTypes;
    }

    /**
     * Only the bundled structs.dat is supported
     */
    public static BBQAssetTypeMetadata fromFile(String filename)
    {
        if (DEFAULT_STRUCTS.equals(filename))
        {
            return getDefault();
        }

        throw new ParseException("External type trees are not supported (" + filename + ")", -1);
    }

    public void load(BinaryReader buf)
    {
        if (format >= 7)
        {
            this.generatorVersion = buf.readString();
        }

        if (format >= 8)
        {
            this.targetPlatform = BBQRuntimePlatform.from(buf.readInt());
        }

        if (format >= 13)
        {
            this.hasTypeTree = buf.readBoolean();
        }

        int typeCount = buf.readInt();
        for (int i = 0; i < typeCount; i++)
        {
            readType(buf, false);
        }
    }

    /**
     * Reads the referenced types (format 20+)
     */
    public void loadRefTypes(BinaryReader buf)
    {
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
        {
            readType(buf, true);
        }
    }

    /**
     * Reads the shared sub trees (format 26+) and resolves the nodes (type flag 0x20) of the types and ref types that use them:
     * such a node gets the children of the root of the shared tree its ref hash points at.
     * See SerializedFile._resolve_shared_subtrees in https://github.com/K0lb3/UnityPy/blob/master/UnityPy/files/SerializedFile.py
     */
    public void loadSharedSubtrees(BinaryReader buf)
    {
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
        {
            long        pos  = buf.pos();
            BBQTypeTree tree = new BBQTypeTree(Math.max(format, 23));
            tree.load(buf, endian);
            if (tree.getHash() == null || tree.getHash().length != 16)
            {
                throw new ParseException("Truncated shared type sub tree", pos);
            }
            sharedSubtrees.put(HexFormat.of().formatHex(tree.getHash()), tree);
        }

        Map<String, List<BBQTypeTree>> resolved = new HashMap<>();
        for (BBQTypeTree tree : typeList)
        {
            if (tree != null)
            {
                resolveSubtrees(tree, tree.getRefHashes(), resolved, new HashSet<>());
            }
        }
        for (BBQTypeTree tree : refTypes)
        {
            if (tree != null)
            {
                resolveSubtrees(tree, tree.getRefHashes(), resolved, new HashSet<>());
            }
        }
    }

    private void resolveSubtrees(BBQTypeTree node, List<byte[]> hashes, Map<String, List<BBQTypeTree>> resolved, Set<String> resolving)
    {
        // a node that uses a shared sub tree has no children of its own, so this only walks the nodes of this tree
        for (BBQTypeTree child : node.children)
        {
            resolveSubtrees(child, hashes, resolved, resolving);
        }

        if (!node.usesSharedSubtree())
        {
            return;
        }

        long index = node.getRefTypeHash();
        if (index < 0 || index >= hashes.size())
        {
            throw new ParseException("Type tree node \"" + node.getName() + "\" references shared sub tree " + index + ", but the tree only has " + hashes.size() + " ref hashes", -1);
        }

        String hash = HexFormat.of().formatHex(hashes.get((int) index));
        if (!resolved.containsKey(hash))
        {
            BBQTypeTree shared = sharedSubtrees.get(hash);
            if (shared == null)
            {
                throw new ParseException("Missing shared type sub tree " + hash, -1);
            }

            if (!resolving.add(hash))
            {
                throw new ParseException("Shared type sub tree " + hash + " references itself", -1);
            }

            if (shared.isEmpty())
            {
                resolved.put(hash, new ArrayList<>());
            } else
            {
                resolveSubtrees(shared, shared.getRefHashes(), resolved, resolving);
                resolved.put(hash, shared.children);
            }
            resolving.remove(hash);
        }

        node.children = resolved.get(hash);
    }

    /**
     * The shared sub trees (format 26+), by hex hash
     */
    public Map<String, BBQTypeTree> getSharedSubtrees()
    {
        return sharedSubtrees;
    }

    private void readType(BinaryReader buf, boolean isRefType)
    {
        int classId         = buf.readInt();
        int scriptTypeIndex = -1;

        if (format >= 16)
        {
            boolean isStrippedType = buf.readBoolean();
        }

        if (format >= 17)
        {
            scriptTypeIndex = buf.readShort();
        }

        byte[] hash = null;
        if (format >= 13)
        {
            boolean hasScriptId = (isRefType && scriptTypeIndex >= 0) || (format < 16 && classId < 0) || (format >= 16 && classId == 114);
            if (hasScriptId)
            {
                byte[] scriptId = buf.readBytes(16);
                byte[] oldHash  = buf.readBytes(16);
                hash = new byte[32];
                System.arraycopy(scriptId, 0, hash, 0, 16);
                System.arraycopy(oldHash, 0, hash, 16, 16);
            } else
            {
                hash = buf.readBytes(16);
            }
        }

        // MonoBehaviours with a script get their own id, so they do not overwrite each other
        int key = classId;
        if (format >= 17 && classId == 114)
        {
            key = scriptTypeIndex >= 0 ? -2 - scriptTypeIndex : -1;
        }

        BBQTypeTree tree = null;
        if (hasTypeTree)
        {
            tree = new BBQTypeTree(format);
            tree.load(buf, endian);
        }

        if (format >= 21)
        {
            if (isRefType)
            {
                String className    = buf.readString();
                String namespace    = buf.readString();
                String assemblyName = buf.readString();
                refClasses.add(namespace.isEmpty() ? className : namespace + "." + className);
            } else
            {
                int dependencies = buf.readInt();
                buf.seek(buf.pos() + 4L * dependencies);
            }
        }

        if (isRefType)
        {
            refTypes.add(tree);
            return;
        }

        classIds.add(key);
        typeList.add(tree);
        if (hash != null)
        {
            this.hashes.put(key, hash);
        }
        if (tree != null)
        {
            typeTrees.put(key, tree);
        }
    }

    public List<Integer> getClassIds()
    {
        return classIds;
    }

    public Map<Integer, BBQTypeTree> getTypeTrees()
    {
        return typeTrees;
    }

    public boolean hasTypeTree()
    {
        return hasTypeTree;
    }

    public String getGeneratorVersion()
    {
        return generatorVersion;
    }

    public BBQRuntimePlatform getTargetPlatform()
    {
        return targetPlatform;
    }
}
