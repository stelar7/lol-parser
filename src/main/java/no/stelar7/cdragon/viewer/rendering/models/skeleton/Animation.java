package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import no.stelar7.cdragon.types.anm.data.*;
import no.stelar7.cdragon.types.anm.data.versioned.*;
import no.stelar7.cdragon.types.skl.data.ReadableBone;
import no.stelar7.cdragon.util.handlers.HashHandler;
import org.joml.*;

import java.lang.Math;
import java.util.*;

/**
 * Per joint key frame tracks built from an animation, which can be sampled at any time.
 * <p>
 * Supports every parsed animation version:
 * <ul>
 * <li>v3: per bone frames, identified by bone name (hashed with the ELF hash, like the skeleton)</li>
 * <li>v4 / v5: per frame lists of (joint hash, palette indices); one frame every frameDuration seconds</li>
 * <li>compressed (r3d2canm): separate rotation/translation/scale keys per joint, at arbitrary times</li>
 * </ul>
 * Values are interpolated linearly (slerp for rotations) between keys. Time wraps around at {@link #getDuration()}.
 * Joints without a key for a channel use the identity for it (no translation, no rotation, scale 1).
 */
public class Animation
{
    private static final Vector3f    ZERO     = new Vector3f();
    private static final Vector3f    ONE      = new Vector3f(1, 1, 1);
    private static final Quaternionf IDENTITY = new Quaternionf();

    private final Map<Integer, Track> tracks = new LinkedHashMap<>();
    private final Map<Integer, String> names  = new HashMap<>();
    private       float               duration;

    /**
     * @param animation the animation
     * @param skeleton  if not null, only joints that are in this skeleton get a track (and the tracks get the joint names)
     */
    public Animation(ANMFile animation, Skeleton skeleton)
    {
        if (animation.getVersion1() != null)
        {
            buildCompressed(animation.getVersion1());
        } else if (animation.getVersion3() != null)
        {
            buildVersion3(animation.getVersion3());
        } else if (animation.getVersion4() != null)
        {
            ANMDataVersion4 v4 = animation.getVersion4();
            buildFrames(v4.getFrames(), v4.getFrameCount(), v4.getFrameDuration(), v4.getPositions(), v4.getRotations());
        } else if (animation.getVersion5() != null)
        {
            ANMDataVersion5 v5 = animation.getVersion5();
            buildFrames(v5.getFrames(), v5.getFrameCount(), v5.getFrameDelay(), v5.getPositions(), v5.getQuaternions());
        }

        if (skeleton != null && skeleton.bones != null)
        {
            Map<Integer, String> skeletonNames = new HashMap<>();
            for (ReadableBone bone : skeleton.bones)
            {
                skeletonNames.put((int) bone.getHash(), bone.getName());
            }

            tracks.keySet().retainAll(skeletonNames.keySet());
            names.putAll(skeletonNames);
        }

        tracks.values().forEach(Track::sort);
    }

    private Track track(int hash)
    {
        return tracks.computeIfAbsent(hash, k -> new Track());
    }

    private void buildCompressed(ANMDataVersion1 data)
    {
        duration = data.getAnimationLength();
        for (ANMEntry entry : data.getEntries())
        {
            Track track = track(entry.getJointHash());
            switch (entry.getDataType())
            {
                case ANMEntry.TYPE_ROTATION -> track.rotations.add(new Key<>(entry.getTime(), entry.getRotation()));
                case ANMEntry.TYPE_TRANSLATION -> track.translations.add(new Key<>(entry.getTime(), entry.getVector()));
                case ANMEntry.TYPE_SCALE -> track.scales.add(new Key<>(entry.getTime(), entry.getVector()));
                default ->
                {
                    // the parser rejects other types
                }
            }
        }
    }

    private void buildVersion3(ANMDataVersion3 data)
    {
        float frameDuration = data.getFps() > 0 ? 1f / data.getFps() : 0;
        duration = data.getFrameCount() * frameDuration;

        for (ANMBone bone : data.getBones())
        {
            int hash = (int) HashHandler.computeELFHash(bone.getName());
            names.put(hash, bone.getName());

            Track track = track(hash);
            for (int i = 0; i < bone.getFrames().size(); i++)
            {
                ANMBoneFrame frame = bone.getFrames().get(i);
                float        time  = i * frameDuration;
                track.rotations.add(new Key<>(time, frame.getRotation()));
                track.translations.add(new Key<>(time, frame.getTranslation()));
            }
        }
    }

    /**
     * v4 and v5: every frame has an entry per joint, with the real joint hash and indices into the vector and quaternion palettes
     */
    private void buildFrames(Map<Integer, List<ANMFrame>> frames, int frameCount, float frameDuration, List<? extends Vector3f> vectors, List<Quaternionf> quaternions)
    {
        duration = frameCount * frameDuration;

        List<Integer> frameIndices = new ArrayList<>(frames.keySet());
        Collections.sort(frameIndices);
        for (int index : frameIndices)
        {
            float time = index * frameDuration;
            for (ANMFrame frame : frames.get(index))
            {
                Track track = track(frame.getBoneHash());
                track.translations.add(new Key<>(time, paletteValue(vectors, frame.getPositionId())));
                track.scales.add(new Key<>(time, paletteValue(vectors, frame.getScaleId())));
                track.rotations.add(new Key<>(time, paletteValue(quaternions, frame.getRotationId())));
            }
        }
    }

    private static <T> T paletteValue(List<? extends T> palette, int index)
    {
        if (index < 0 || index >= palette.size())
        {
            throw new IllegalArgumentException("Animation palette index " + index + " is out of range (" + palette.size() + ")");
        }
        return palette.get(index);
    }

    /**
     * The length of the animation in seconds
     */
    public float getDuration()
    {
        return duration;
    }

    /**
     * The joint hashes that have a track
     */
    public Set<Integer> getJointHashes()
    {
        return Collections.unmodifiableSet(tracks.keySet());
    }

    /**
     * Samples one joint, or returns null if the animation has no track for it
     */
    public BoneData sample(int jointHash, float time)
    {
        Track track = tracks.get(jointHash);
        if (track == null)
        {
            return null;
        }

        float t = wrap(time);
        return new BoneData(jointHash,
                            names.get(jointHash),
                            sampleVector(track.translations, t, ZERO),
                            sampleRotation(track.rotations, t),
                            sampleVector(track.scales, t, ONE));
    }

    /**
     * Samples every joint, by joint hash
     */
    public Map<Integer, BoneData> sample(float time)
    {
        Map<Integer, BoneData> result = new LinkedHashMap<>();
        for (int hash : tracks.keySet())
        {
            result.put(hash, sample(hash, time));
        }
        return result;
    }

    /**
     * Kept for compatibility, this is the same as {@link #sample(float)}
     */
    public Map<Integer, BoneData> getClosestFrame(float time)
    {
        return sample(time);
    }

    private float wrap(float time)
    {
        if (duration <= 0)
        {
            return Math.max(0, time);
        }
        float t = time % duration;
        return t < 0 ? t + duration : t;
    }

    /**
     * Index of the last key at or before time, or -1 if the first key is after time
     */
    private static <T> int floorKey(List<Key<T>> keys, float time)
    {
        int lo = 0;
        int hi = keys.size() - 1;
        int found = -1;
        while (lo <= hi)
        {
            int mid = (lo + hi) >>> 1;
            if (keys.get(mid).time <= time)
            {
                found = mid;
                lo = mid + 1;
            } else
            {
                hi = mid - 1;
            }
        }
        return found;
    }

    private static Vector3f sampleVector(List<Key<Vector3f>> keys, float time, Vector3f fallback)
    {
        if (keys.isEmpty())
        {
            return new Vector3f(fallback);
        }

        int index = floorKey(keys, time);
        if (index < 0)
        {
            return new Vector3f(keys.get(0).value);
        }
        if (index == keys.size() - 1)
        {
            return new Vector3f(keys.get(index).value);
        }

        Key<Vector3f> a = keys.get(index);
        Key<Vector3f> b = keys.get(index + 1);
        return new Vector3f(a.value).lerp(b.value, alpha(a.time, b.time, time));
    }

    private static Quaternionf sampleRotation(List<Key<Quaternionf>> keys, float time)
    {
        if (keys.isEmpty())
        {
            return new Quaternionf(IDENTITY);
        }

        int index = floorKey(keys, time);
        if (index < 0)
        {
            return new Quaternionf(keys.get(0).value);
        }
        if (index == keys.size() - 1)
        {
            return new Quaternionf(keys.get(index).value);
        }

        Key<Quaternionf> a = keys.get(index);
        Key<Quaternionf> b = keys.get(index + 1);
        return new Quaternionf(a.value).slerp(b.value, alpha(a.time, b.time, time)).normalize();
    }

    private static float alpha(float start, float end, float time)
    {
        float span = end - start;
        return span <= 0 ? 0 : Math.min(1, Math.max(0, (time - start) / span));
    }

    private record Key<T>(float time, T value) {}

    private static final class Track
    {
        final List<Key<Vector3f>>    translations = new ArrayList<>();
        final List<Key<Quaternionf>> rotations    = new ArrayList<>();
        final List<Key<Vector3f>>    scales       = new ArrayList<>();

        void sort()
        {
            // stable, so keys with the same time keep their file order
            Comparator<Key<?>> byTime = Comparator.comparingDouble(Key::time);
            translations.sort(byTime);
            rotations.sort(byTime);
            scales.sort(byTime);
        }
    }
}
