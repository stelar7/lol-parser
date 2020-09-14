package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.Pair;

import java.util.*;
import java.util.function.BiFunction;

public class BBQObjectInfo
{
    BBQAsset asset;
    long     pathId;
    int      dataOffset;
    int      size;
    int      typeId;
    int      classId;
    boolean  isDestroyed;
    short    unknown0;
    short    unknown1;
    
    public BBQObjectInfo(BBQAsset asset, BinaryReader buf)
    {
        this.asset = asset;
        load(buf);
    }
    
    public void load(BinaryReader buf)
    {
        this.pathId = this.readId(buf);
        this.dataOffset = buf.readInt() + this.asset.dataOffset;
        this.size = buf.readInt();
        if (this.asset.format < 17)
        {
            this.typeId = buf.readInt();
            this.classId = buf.readShort();
        } else
        {
            int type  = buf.readInt();
            int clazz = this.asset.tree.classIds.get(type);
            this.typeId = clazz;
            this.classId = clazz;
        }
        
        if (this.asset.format <= 10)
        {
            this.isDestroyed = buf.readShort() > 0;
        }
        
        if (this.asset.format >= 11 && this.asset.format <= 16)
        {
            this.unknown0 = buf.readShort();
        }
        
        if (this.asset.format >= 15 && this.asset.format <= 16)
        {
            this.unknown1 = buf.readByte();
        }
    }
    
    long readId(BinaryReader buf)
    {
        if (this.asset.longObjectIds)
        {
            return buf.readLong();
        }
        return this.asset.readId(buf);
    }
    
    private BBQTypeTree getTypeTree()
    {
        if (this.typeId < 0)
        {
            Map<Integer, BBQTypeTree> tree = this.asset.tree.typeTrees;
            if (tree.containsKey(this.typeId))
            {
                return tree.get(this.typeId);
            } else if (tree.containsKey(this.classId))
            {
                return tree.get(this.classId);
            }
            
            return BBQAssetTypeMetadata.fromFile("bbq/structs.dat").typeTrees.get(this.classId);
        }
        
        return this.asset.types.get(this.typeId);
    }
    
    public Object getType()
    {
        if (this.typeId > 0)
        {
            return UtilHandler.getBBQClassData().get(String.valueOf(this.typeId));
        } else if (!this.asset.types.containsKey(this.typeId))
        {
            String typename = "(null)";
            BBQObjectInfo script = ((Map<String, BBQObjectInfo>) this.read()).get("m_Script");
            if (script != null)
            {
                BBQObjectInfo type = ((Map<String, BBQObjectInfo>) script.read()).get("m_ClassName");
                System.out.println();
            } else if(this.asset.tree.typeTrees.containsKey(this.typeId)) {
                typename = this.asset.tree.typeTrees.get(this.typeId).type;
            } else {
                typename = String.valueOf(this.typeId);
            }
            this.asset.typeNames.put(this.typeId, typename);
        }
        
        return this.asset.types.get(this.typeId).type;
    }
    
    public Object read()
    {
        this.asset.buf.seek(this.asset.bufferOffset + this.dataOffset);
        byte[] data = this.asset.buf.readBytes(this.size);
        return readValue(getTypeTree(), new RandomAccessReader(data));
    }
    
    protected Object readValue(BBQTypeTree type, BinaryReader buf)
    {
        boolean     align        = false;
        int         expectedSize = type.size;
        int         pos          = buf.pos();
        String      t            = type.type;
        BBQTypeTree firstChild   = type.children.size() > 0 ? type.children.get(0) : new BBQTypeTree(this.asset.format);
        
        Object  result      = null;
        boolean shouldAlign = false;
        
        if (t.equals("bool"))
        {
            result = buf.readBoolean();
        } else if (t.equals("SInt8"))
        {
            result = buf.readByte();
        } else if (t.equals("UInt8"))
        {
            result = buf.readByte();
        } else if (t.equals("SInt16"))
        {
            result = buf.readShort();
        } else if (t.equals("UInt16"))
        {
            result = buf.readShort();
        } else if (t.equals("SInt64"))
        {
            result = buf.readLong();
        } else if (t.equals("UInt64"))
        {
            result = buf.readLong();
        } else if (t.equals("SInt32"))
        {
            result = buf.readInt();
        } else if (t.equals("UInt32"))
        {
            result = buf.readInt();
        } else if (t.equals("unsigned int"))
        {
            result = buf.readInt();
        } else if (t.equals("int"))
        {
            result = buf.readInt();
        } else if (t.equals("float"))
        {
            buf.align();
            result = buf.readFloat();
        } else if (t.equals("double"))
        {
            buf.align();
            result = buf.readDouble();
        } else if (t.equals("string"))
        {
            int size = type.size;
            if (size == -1)
            {
                size = buf.readInt();
            }
            result = buf.readString(size);
            shouldAlign = type.children.get(0).shouldAlign();
        } else
        {
            if (type.isArray)
            {
                firstChild = type;
            }
            
            if (t.startsWith("PPtr<"))
            {
                result = new BBQObjectPointer(type, this.asset, buf);
            } else if (firstChild != null && firstChild.isArray)
            {
                align = firstChild.shouldAlign();
                size = buf.readInt();
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
                    throw new UnsupportedOperationException("Pair type has too many children!");
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
                for (BBQTypeTree child : type.children)
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
        }
        
        int after = buf.pos();
        int size  = after - pos;
        if (expectedSize > 0 && size < expectedSize)
        {
            throw new RuntimeException("Expected to read " + expectedSize + " but only read " + size + " for type " + type.name);
        }
        
        if (align || type.shouldAlign())
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
