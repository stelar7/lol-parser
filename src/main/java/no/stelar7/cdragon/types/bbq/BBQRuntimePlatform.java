package no.stelar7.cdragon.types.bbq;

import java.util.Arrays;

public enum BBQRuntimePlatform
{
    OSX_EDITOR(0),
    OSX_PLAYER(1),
    WINDOWS_PLAYER(2),
    OSX_WEB_PLAYER(3),
    OSX_DASHBOARD_PLAYER(4),
    WINDOWS_WEB_PLAYER(5),
    WINDOWS_EDITOR(7),
    IPHONE_PLAYER(8),
    PS3(9),
    XBOX360(10),
    ANDROID(11),
    NACL(12),
    LINUX_PLAYER(13),
    FLASH_PLAYER(15),
    WEBGLP_LAYER(17),
    METRO_PLAYER_X86(18),
    WSA_PLAYER_X86(18),
    METRO_PLAYER_X64(19),
    WSA_PLAYER_X64(19),
    METRO_PLAYER_ARM(20),
    WSA_PLAYER_ARM(20),
    WP8_PLAYER(21),
    BB10_PLAYER(22),
    BLACKBERRY_PLAYER(22),
    TIZEN_PLAYER(23),
    PSP2(24),
    PS4(25),
    PSM(26),
    PSM_PLAYER(26),
    XBOXONE(27),
    SAMSUNGTV_PLAYER(28);
    
    private int flag;
    
    BBQRuntimePlatform(int flag)
    {
        this.flag = flag;
    }
    
    public static BBQRuntimePlatform from(int flag)
    {
        return Arrays.stream(values()).filter(v -> v.flag == flag).findFirst().get();
    }
    
}
