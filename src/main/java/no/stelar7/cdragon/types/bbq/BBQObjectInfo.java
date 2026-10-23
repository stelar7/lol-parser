package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.Pair;

import java.util.*;
import java.util.function.BiFunction;

public class BBQObjectInfo
{
    private static volatile Map<String, String> classNames;

    BBQAsset    asset;
    long        pathId;
    long        dataOffset;
    int         size;
    /**
     * The class id (for MonoBehaviours with a script: -2 - script index)
     */
    int         typeId;
    int         classId;
    /**
     * Index into the type list of the file (format 16+), -1 for older files
     */
    int         typeIndex = -1;
    boolean     isDestroyed;
    short       unknown0;
    short       unknown1;
    BBQTypeTree typeTree;

    public BBQObjectInfo(BBQAsset asset, BinaryReader buf)
    {
        this.asset = asset;
        load(buf);
    }

    public void load(BinaryReader buf)
    {
        this.pathId = this.readId(buf);

        long start = (this.asset.format >= 22) ? buf.readLong() : Integer.toUnsignedLong(buf.readInt());
        this.dataOffset = start + this.asset.dataOffset;
        this.size = buf.readInt();

        BBQAssetTypeMetadata types = this.asset.tree;
        if (this.asset.format < 16)
        {
            this.typeId = buf.readInt();
            this.classId = Short.toUnsignedInt(buf.readShort());
            this.typeTree = types.typeTrees.get(this.typeId);
        } else
        {
            this.typeIndex = buf.readInt();
            if (this.typeIndex < 0 || this.typeIndex >= types.classIds.size())
            {
                throw new ParseException("Object " + this.pathId + " has an invalid type index " + this.typeIndex, buf.pos() - 4);
            }

            int clazz = types.classIds.get(this.typeIndex);
            this.typeId = clazz;
            this.classId = clazz < 0 ? 114 : clazz;
            this.typeTree = types.typeList.get(this.typeIndex);
        }

        if (this.asset.format < 11)
        {
            this.isDestroyed = buf.readShort() != 0;
        }

        if (this.asset.format >= 11 && this.asset.format < 17)
        {
            this.unknown0 = buf.readShort();
        }

        if (this.asset.format == 15 || this.asset.format == 16)
        {
            this.unknown1 = buf.readByte();
        }

        if (this.typeTree == null && !types.hasTypeTree)
        {
            // stripped file, try the type trees bundled with the library
            this.typeTree = BBQAssetTypeMetadata.getDefault().typeTrees.get(this.classId);
        }
    }

    long readId(BinaryReader buf)
    {
        if (this.asset.longObjectIds)
        {
            return buf.readLong();
        }

        if (this.asset.format >= 14)
        {
            buf.align();
        }

        return this.asset.readId(buf);
    }

    /**
     * The type tree used to read this object, or null if neither the file nor the bundled defaults have one
     */
    public BBQTypeTree getTypeTree()
    {
        return this.typeTree;
    }

    private static Map<String, String> getClassNames()
    {
        if (classNames == null)
        {
            classNames = UtilHandler.getBBQClassData();
        }
        return classNames;
    }

    public Object getType()
    {
        if (this.classId > 0)
        {
            String name = getClassNames().get(String.valueOf(this.classId));
            if (name != null)
            {
                return name;
            }
        }

        if (this.typeTree != null)
        {
            return this.typeTree.type;
        }

        return String.valueOf(this.typeId);
    }

    /**
     * Returns the raw bytes of this object
     */
    public byte[] readBytes()
    {
        this.asset.buf.seek(this.asset.bufferOffset + this.dataOffset);
        return this.asset.buf.readBytes(this.size);
    }

    public Object read()
    {
        BBQTypeTree tree = getTypeTree();
        if (tree == null)
        {
            throw new ParseException("No type tree for class " + this.classId + " (bundle has no type tree, and the class is not in the bundled defaults); use readBytes() instead", -1);
        }

        return readValue(tree, new RandomAccessReader(readBytes(), this.asset.endian));
    }
    
    protected Object readValue(BBQTypeTree type, BinaryReader buf)
    {
        boolean     shouldAlign  = false;
        int         expectedSize = type.size;
        int         pos          = (int) buf.pos();
        String      t            = type.type;
        BBQTypeTree firstChild   = !type.children.isEmpty() ? type.children.get(0) : new BBQTypeTree(this.asset.format);
        
        Object result = null;
        
        switch (t)
        {
            case "bool":
                result = buf.readBoolean();
                break;
            case "SInt8":
                result = buf.readByte();
                break;
            case "UInt8":
                result = buf.readByte();
                break;
            case "SInt16":
                result = buf.readShort();
                break;
            case "UInt16":
                result = buf.readShort();
                break;
            case "SInt64":
                result = buf.readLong();
                break;
            case "UInt64":
                result = buf.readLong();
                break;
            case "SInt32":
                result = buf.readInt();
                break;
            case "UInt32":
                result = buf.readInt();
                break;
            case "unsigned int":
                result = buf.readInt();
                break;
            case "int":
                result = buf.readInt();
                break;
            // values are only aligned after a node that has the align flag (0x4000) set, never before
            case "float":
                result = buf.readFloat();
                break;
            case "double":
                result = buf.readDouble();
                break;
            case "string":
                int stringSize = type.size;
                if (stringSize == -1)
                {
                    stringSize = buf.readInt();
                }
                result = buf.readString(stringSize);
                shouldAlign = type.children.get(0).shouldAlign();
                break;
            default:
                if (type.isArray)
                {
                    firstChild = type;
                }
                
                if (t.startsWith("PPtr<"))
                {
                    result = new BBQObjectPointer(type, this.asset, buf);
                } else if (firstChild != null && firstChild.isArray)
                {
                    shouldAlign = firstChild.shouldAlign();
                    int         size      = buf.readInt();
                    BBQTypeTree arrayType = firstChild.children.get(1);
                    if (arrayType.type.equals("char") || arrayType.type.equals("UInt8"))
                    {
                        result = buf.readBytes(size);
                    } else
                    {
                        result = new ArrayList<>();
                        for (int i = 0; i < size; i++)
                        {
                            ((ArrayList<Object>) result).add(readValue(arrayType, buf));
                        }
                    }
                } else if (t.equals("pair"))
                {
                    if (type.children.size() != 2)
                    {
                        throw new ParseException("Pair type " + type.name + " has " + type.children.size() + " children, expected 2", pos);
                    }
                    
                    Object first  = readValue(type.children.get(0), buf);
                    Object second = readValue(type.children.get(1), buf);
                    result = new Pair<>(first, second);
                } else if (t.startsWith("ExposedReference"))
                {
                    BiFunction<BBQTypeTree, BinaryReader, Object> readValueExposed = (internalType, internalBuf) -> {
                        if (internalType.name.equals("exposedName"))
                        {
                            internalBuf.readInt();
                            return "";
                        }
                        
                        return readValue(internalType, internalBuf);
                    };
                    
                    Map<String, Object> dataStore = new HashMap<>();
                    for (BBQTypeTree child : type.children)
                    {
                        dataStore.put(child.name, readValueExposed.apply(child, buf));
                    }
                    
                    result = loadObject(type, dataStore);
                } else
                {
                    Map<String, Object> dataStore = new HashMap<>();
                    List<BBQTypeTree>   children  = type.children;
                    for (BBQTypeTree child : children)
                    {
                        dataStore.put(child.name, readValue(child, buf));
                    }
                    
                    result = loadObject(type, dataStore);
                    if (t.equals("StreamedResource"))
                    {
                        //((BBQObjectInfo)result).asset = resolveStreamingAsset(result.source);
                    } else if (t.equals("StreamingInfo"))
                    {
                        //((BBQObjectInfo)result).asset = resolveStreamingAsset(result.path);
                    }
                }
                break;
        }
        
        int after = (int) buf.pos();
        int size  = after - pos;
        if (expectedSize > 0 && size < expectedSize)
        {
            throw new ParseException("Expected to read " + expectedSize + " bytes but only read " + size + " for " + type.type + " " + type.name, pos);
        }
        
        if (shouldAlign || type.shouldAlign())
        {
            buf.align();
        }
        
        return result;
    }
    
    private Object loadObject(BBQTypeTree type, Map<String, Object> dataStore)
    {
        /*
        String field = type.name;
        if(UnityEngine.class.getDeclaredField(field)) {
            return createObject(dataStore);
        }
        */
        return dataStore;
    }
}
