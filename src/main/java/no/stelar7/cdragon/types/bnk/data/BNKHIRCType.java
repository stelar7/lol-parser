package no.stelar7.cdragon.types.bnk.data;

/**
 * HIRC object types (AkBank::AKBKHircType).
 * <p>
 * The ids above 0x0F were reassigned over time, so the type depends on the bank version (wwiser get_hirc_dispatch):
 * <ul>
 *     <li>v72 and older: 0x10 feedback bus, 0x11 feedback node, 0x12 fx shareset, 0x13 fx custom, 0x14 aux bus</li>
 *     <li>v73 - v126: as above, plus 0x15 LFO modulator, 0x16 envelope modulator, 0x17 audio device</li>
 *     <li>v127 and newer: 0x10 fx shareset, 0x11 fx custom, 0x12 aux bus, 0x13 LFO modulator, 0x14 envelope modulator,
 *     0x15 audio device, 0x16 time modulator, 0x17 sidechain mix</li>
 * </ul>
 */
public enum BNKHIRCType
{
    SETTING(1, 1),
    SFX_VOICE(2, 2),
    EVENT_ACTION(3, 3),
    EVENT(4, 4),
    RANDOM_SEQUENCE(5, 5),
    SWITCH_CONTAINER(6, 6),
    ACTOR_MIXER(7, 7),
    AUDIO_BUS(8, 8),
    BLEND_CONTAINER(9, 9),
    MUSIC_SEGMENT(10, 10),
    MUSIC_TRACK(11, 11),
    MUSIC_SWITCH_CONTAINER(12, 12),
    MUSIC_PLAYLIST_CONTAINER(13, 13),
    ATTENUATION(14, 14),
    DIALOGUE_EVENT(15, 15),
    /**
     * Feedback bus, removed in v127
     */
    MOTION_BUS(16, -1),
    /**
     * Feedback node, removed in v127
     */
    MOTION_FX(17, -1),
    /**
     * Fx shareset
     */
    EFFECT(18, 16),
    FX_CUSTOM(19, 17),
    AUX_BUS(20, 18),
    LFO_MODULATOR(21, 19),
    ENVELOPE_MODULATOR(22, 20),
    AUDIO_DEVICE(23, 21),
    TIME_MODULATOR(-1, 22),
    SIDECHAIN_MIX(-1, 23),
    UNKNOWN(-1, -1);

    private final int legacyValue;
    private final int modernValue;

    BNKHIRCType(int legacyValue, int modernValue)
    {
        this.legacyValue = legacyValue;
        this.modernValue = modernValue;
    }

    /**
     * @return the type with this value in a bank of this version, or UNKNOWN
     */
    public static BNKHIRCType fromValue(int v, int bankVersion)
    {
        for (BNKHIRCType type : values())
        {
            if (type != UNKNOWN && type.getValue(bankVersion) == v)
            {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * @return the id of this type in a bank of this version, or -1 if the type doesn't exist in that version
     */
    public int getValue(int bankVersion)
    {
        if (bankVersion <= 72 && legacyValue > 20)
        {
            return -1;
        }
        return bankVersion <= 126 ? legacyValue : modernValue;
    }
}
