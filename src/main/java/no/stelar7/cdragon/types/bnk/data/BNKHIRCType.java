package no.stelar7.cdragon.types.bnk.data;

import java.util.stream.Stream;

public enum BNKHIRCType
{
    SETTING(1),
    SFX_VOICE(2),
    EVENT_ACTION(3),
    EVENT(4),
    RANDOM_SEQUENCE(5),
    SWITCH_CONTAINER(6),
    ACTOR_MIXER(7),
    AUDIO_BUS(8),
    BLEND_CONTAINER(9),
    MUSIC_SEGMENT(10),
    MUSIC_TRACK(11),
    MUSIC_SWITCH_CONTAINER(12),
    MUSIC_PLAYLIST_CONTAINER(13),
    ATTENUATION(14),
    DIALOGUE_EVENT(15),
    MOTION_BUS(16),
    MOTION_FX(17),
    EFFECT(18),
    AUX_BUS(20),;
    
    int value;
    
    BNKHIRCType(int type)
    {
        this.value = type;
    }
    
    public static BNKHIRCType fromByte(byte b)
    {
        return Stream.of(values()).filter(i -> i.value == b).findFirst().get();
    }
}
