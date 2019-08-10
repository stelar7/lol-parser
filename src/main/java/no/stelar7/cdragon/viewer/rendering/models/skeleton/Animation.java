package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import no.stelar7.cdragon.types.anm.data.ANMFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Animation
{
    public TreeMap<Float, Map<Integer, BoneData>> animationData = new TreeMap<>();
    
    public Animation(ANMFile animation, Skeleton skeleton)
    {
        AtomicReference<Float> time = new AtomicReference<>((float) 0);
        
        animation.getVersion5().getFrames().forEach((frame, frameData) -> {
            frameData.forEach(data -> {
                animationData.putIfAbsent(time.get(), new HashMap<>());
                BoneData boneFrame = BoneData.fromFrame(animation, data, skeleton);
                animationData.get(time.get()).put(boneFrame.hash, boneFrame);
            });
            
            time.updateAndGet(v -> v + animation.getVersion5().getFrameDelay());
        });
    }
}
