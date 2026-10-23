package types.viewer;

import no.stelar7.cdragon.types.anm.ANMParser;
import no.stelar7.cdragon.types.anm.data.ANMFile;
import no.stelar7.cdragon.types.skl.data.ReadableBone;
import no.stelar7.cdragon.util.handlers.HashHandler;
import no.stelar7.cdragon.util.types.ByteArray;
import no.stelar7.cdragon.viewer.rendering.models.skeleton.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

import java.lang.Math;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline check of the viewer animation tracks, using synthetic animations for every parsed version
 */
public class TestAnimationTracks
{
    private static final int   JOINT_A = 0x12345678;
    private static final int   JOINT_B = 0x9ABCDEF0;
    private static final float EPSILON = 1e-3f;

    // 90 degrees around Y
    private static final Quaternionf ROT_90 = new Quaternionf().rotateY((float) Math.toRadians(90));
    private static final Quaternionf ROT_45 = new Quaternionf().rotateY((float) Math.toRadians(45));

    @Test
    public void testVersion4()
    {
        // palettes: vectors 0=(0,0,0) 1=(2,4,6) 2=(1,1,1) 3=(10,0,0), quaternions 0=identity 1=90 deg around Y
        ByteBuffer b = header("r3d2anmd", 4, 64 + 4 * 12 + 2 * 16 + 2 * 2 * 12);
        int positionOffset = 64;
        int rotationOffset = positionOffset + 4 * 12;
        int frameOffset    = rotationOffset + 2 * 16;

        b.putInt(0).putInt(0).putInt(0).putInt(0);
        b.putInt(2).putInt(2).putFloat(0.5f);
        b.putInt(0).putInt(0).putInt(0);
        b.putInt(positionOffset).putInt(rotationOffset).putInt(frameOffset);
        b.putInt(0).putInt(0).putInt(0);
        putVectors(b);
        putQuaternion(b, new Quaternionf());
        putQuaternion(b, ROT_90);

        // frame 0: A at vector 0 rotation 0, B at vector 3 rotation 1; frame 1: A at vector 1 rotation 1
        int[][] frames = {{JOINT_A, 0, 2, 0}, {JOINT_B, 3, 2, 1}, {JOINT_A, 1, 2, 1}, {JOINT_B, 3, 2, 1}};
        for (int[] f : frames)
        {
            b.putInt(f[0]).putShort((short) f[1]).putShort((short) f[2]).putShort((short) f[3]).putShort((short) 0);
        }

        checkTwoFrameAnimation(parse(b));
    }

    @Test
    public void testVersion5()
    {
        int positionOffset = 64;
        int rotationOffset = positionOffset + 4 * 12;
        int hashOffset     = rotationOffset + 2 * 6;
        int frameOffset    = hashOffset + 2 * 4;

        ByteBuffer b = header("r3d2anmd", 5, frameOffset + 2 * 2 * 6);
        b.putInt(0).putInt(0).putInt(0).putInt(0);
        b.putInt(2).putInt(2).putFloat(0.5f);
        b.putInt(hashOffset).putInt(0).putInt(0);
        b.putInt(positionOffset).putInt(rotationOffset).putInt(frameOffset);
        b.putInt(0).putInt(0).putInt(0);
        putVectors(b);
        putCompressedQuaternion(b, new Quaternionf());
        putCompressedQuaternion(b, ROT_90);
        b.putInt(JOINT_A).putInt(JOINT_B);

        // frames reference the joint by position in the hash table: (vector, scale, rotation) for A, then B
        int[][] frames = {{0, 2, 0}, {3, 2, 1}, {1, 2, 1}, {3, 2, 1}};
        for (int[] f : frames)
        {
            b.putShort((short) f[0]).putShort((short) f[1]).putShort((short) f[2]);
        }

        checkTwoFrameAnimation(parse(b));
    }

    @Test
    public void testVersion3()
    {
        ByteBuffer b = header("r3d2anmd", 3, 16 + (36 + 2 * 28));
        b.putInt(0).putInt(1).putInt(2).putInt(2); // 2 frames at 2 fps
        byte[] name = Arrays.copyOf("Root".getBytes(StandardCharsets.US_ASCII), 32);
        b.put(name).putInt(0);
        putQuaternion(b, new Quaternionf());
        b.putFloat(0).putFloat(0).putFloat(0);
        putQuaternion(b, ROT_90);
        b.putFloat(2).putFloat(4).putFloat(6);

        Animation anim = new Animation(parse(b), null);
        int       hash = (int) HashHandler.computeELFHash("Root");
        assertEquals(1f, anim.getDuration(), EPSILON);
        assertEquals(Set.of(hash), anim.getJointHashes());

        BoneData mid = anim.sample(hash, 0.25f);
        assertEquals("Root", mid.getName());
        assertVector(new Vector3f(1, 2, 3), mid.getPosition());
        assertRotation(ROT_45, mid.getRotation());
        assertVector(new Vector3f(1, 1, 1), mid.getScale());
    }

    @Test
    public void testCompressed()
    {
        int hashOffset  = 116;
        int entryOffset = hashOffset + 2 * 4;

        ByteBuffer b = header("r3d2canm", 3, entryOffset + 4 * 10);
        b.putInt(0).put("abcd".getBytes(StandardCharsets.US_ASCII)).putInt(0);
        b.putInt(2).putInt(4).putInt(0); // 2 joints, 4 keys, no jump caches
        b.putFloat(2f).putFloat(30f);
        for (int i = 0; i < 6; i++)
        {
            b.putInt(0);
        }
        putVector(b, new Vector3f(0, 0, 0));
        putVector(b, new Vector3f(4, 8, 12));
        putVector(b, new Vector3f(0, 0, 0));
        putVector(b, new Vector3f(2, 2, 2));
        b.putInt(entryOffset).putInt(0).putInt(hashOffset);
        b.putInt(JOINT_A).putInt(JOINT_B);

        // stored out of time order on purpose: A translation at the end, A translation at the start, A rotation at the start, B scale at the middle
        putKey(b, 65535, 1, 0, 65535, 65535, 65535);
        putKey(b, 0, 1, 0, 0, 0, 0);
        int[] identity = compressQuaternion(new Quaternionf());
        putKey(b, 0, 0, 0, identity[0], identity[1], identity[2]);
        putKey(b, 32768, 2, 1, 65535, 0, 32768);

        Animation anim = new Animation(parse(b), null);
        assertEquals(2f, anim.getDuration(), EPSILON);

        BoneData a = anim.sample(JOINT_A, 0.5f);
        assertVector(new Vector3f(1, 2, 3), a.getPosition());
        assertRotation(new Quaternionf(), a.getRotation());
        assertVector(new Vector3f(1, 1, 1), a.getScale());
        assertVector(new Vector3f(3, 6, 9), anim.sample(JOINT_A, 1.5f).getPosition());

        // only a scale key: no translation, identity rotation, and the scale holds before and after the key
        BoneData bData = anim.sample(JOINT_B, 0.1f);
        assertVector(new Vector3f(0, 0, 0), bData.getPosition());
        assertRotation(new Quaternionf(), bData.getRotation());
        assertVector(new Vector3f(2, 0, 1), bData.getScale());
    }

    @Test
    public void testSkeletonFilter()
    {
        ByteBuffer b = header("r3d2anmd", 4, 64 + 4 * 12 + 2 * 16 + 2 * 2 * 12);
        b.putInt(0).putInt(0).putInt(0).putInt(0);
        b.putInt(2).putInt(2).putFloat(0.5f);
        b.putInt(0).putInt(0).putInt(0);
        b.putInt(64).putInt(64 + 48).putInt(64 + 48 + 32);
        b.putInt(0).putInt(0).putInt(0);
        putVectors(b);
        putQuaternion(b, new Quaternionf());
        putQuaternion(b, ROT_90);
        for (int i = 0; i < 2; i++)
        {
            b.putInt(JOINT_A).putShort((short) 0).putShort((short) 2).putShort((short) 0).putShort((short) 0);
            b.putInt(JOINT_B).putShort((short) 3).putShort((short) 2).putShort((short) 1).putShort((short) 0);
        }

        ReadableBone bone = new ReadableBone();
        bone.setName("joint_b");
        // skeletons store the hash unsigned
        bone.setHash(Integer.toUnsignedLong(JOINT_B));

        Animation anim = new Animation(parse(b), new Skeleton(List.of(bone)));
        assertEquals(Set.of(JOINT_B), anim.getJointHashes());
        assertEquals("joint_b", anim.sample(0.3f).get(JOINT_B).getName());
        assertNull(anim.sample(JOINT_A, 0.3f));
    }

    private static void checkTwoFrameAnimation(ANMFile file)
    {
        Animation anim = new Animation(file, null);
        assertEquals(1f, anim.getDuration(), EPSILON);
        assertEquals(Set.of(JOINT_A, JOINT_B), anim.getJointHashes());

        BoneData start = anim.sample(JOINT_A, 0f);
        assertVector(new Vector3f(0, 0, 0), start.getPosition());
        assertRotation(new Quaternionf(), start.getRotation());
        assertVector(new Vector3f(1, 1, 1), start.getScale());

        BoneData mid = anim.sample(JOINT_A, 0.25f);
        assertVector(new Vector3f(1, 2, 3), mid.getPosition());
        assertRotation(ROT_45, mid.getRotation());

        // the last frame holds until the end, then time wraps around
        assertVector(new Vector3f(2, 4, 6), anim.sample(JOINT_A, 0.5f).getPosition());
        assertVector(new Vector3f(2, 4, 6), anim.sample(JOINT_A, 0.9f).getPosition());
        assertVector(new Vector3f(1, 2, 3), anim.sample(JOINT_A, 1.25f).getPosition());

        BoneData other = anim.sample(0.25f).get(JOINT_B);
        assertVector(new Vector3f(10, 0, 0), other.getPosition());
        assertRotation(ROT_90, other.getRotation());

        Matrix4f expected = new Matrix4f().translationRotateScale(new Vector3f(10, 0, 0), ROT_90, new Vector3f(1, 1, 1));
        assertTrue(expected.equals(other.toMatrix(), EPSILON), "bone matrix");
    }

    private static ANMFile parse(ByteBuffer b)
    {
        assertEquals(b.capacity(), b.position(), "synthetic animation size");
        return new ANMParser().parse(new ByteArray(b.array()));
    }

    private static ByteBuffer header(String magic, int version, int bodySize)
    {
        ByteBuffer b = ByteBuffer.allocate(12 + bodySize).order(ByteOrder.LITTLE_ENDIAN);
        b.put(magic.getBytes(StandardCharsets.US_ASCII)).putInt(version);
        return b;
    }

    private static void putVectors(ByteBuffer b)
    {
        putVector(b, new Vector3f(0, 0, 0));
        putVector(b, new Vector3f(2, 4, 6));
        putVector(b, new Vector3f(1, 1, 1));
        putVector(b, new Vector3f(10, 0, 0));
    }

    private static void putVector(ByteBuffer b, Vector3f v)
    {
        b.putFloat(v.x).putFloat(v.y).putFloat(v.z);
    }

    private static void putQuaternion(ByteBuffer b, Quaternionf q)
    {
        b.putFloat(q.x).putFloat(q.y).putFloat(q.z).putFloat(q.w);
    }

    private static void putCompressedQuaternion(ByteBuffer b, Quaternionf q)
    {
        for (int v : compressQuaternion(q))
        {
            b.putShort((short) v);
        }
    }

    private static void putKey(ByteBuffer b, int time, int type, int joint, int x, int y, int z)
    {
        b.putShort((short) time).putShort((short) (type << 14 | joint)).putShort((short) x).putShort((short) y).putShort((short) z);
    }

    /**
     * The inverse of ANMParser.decompressQuaternion: index of the largest component, then the other three in 15 bits each
     */
    private static int[] compressQuaternion(Quaternionf q)
    {
        float[] c   = {q.x, q.y, q.z, q.w};
        int     max = 0;
        for (int i = 1; i < 4; i++)
        {
            if (Math.abs(c[i]) > Math.abs(c[max]))
            {
                max = i;
            }
        }
        float sign = c[max] < 0 ? -1 : 1;

        long bits = (long) max << 45;
        int  shift = 30;
        for (int i = 0; i < 4; i++)
        {
            if (i == max)
            {
                continue;
            }
            long v = Math.round((c[i] * sign + 1 / Math.sqrt(2)) / Math.sqrt(2) * 32767);
            bits |= Math.max(0, Math.min(32767, v)) << shift;
            shift -= 15;
        }
        return new int[]{(int) (bits & 0xFFFF), (int) ((bits >>> 16) & 0xFFFF), (int) ((bits >>> 32) & 0xFFFF)};
    }

    private static void assertVector(Vector3f expected, Vector3f actual)
    {
        assertTrue(expected.equals(actual, EPSILON), "expected " + expected + " got " + actual);
    }

    private static void assertRotation(Quaternionf expected, Quaternionf actual)
    {
        // q and -q are the same rotation
        float dot = Math.abs(expected.dot(actual));
        assertEquals(1f, dot, EPSILON, "expected " + expected + " got " + actual);
    }
}
