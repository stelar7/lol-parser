package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

@Data
public class ReleasemanifestDataContentFile
{
    private int    nameIndex;
    private int    version;
    private String hash;
    private int    flags;
    private int    size;
    private int    compressedSize;
    private int    unknown;
    private short  type;
    private short  padding;
    
    // Flags:
    // 0x01 :  Managedfiles dir (?)
    // 0x02 :  Archived/Filearchives dir (?)
    // 0x04 :  (?) #
    // 0x10 :  Compressed
    // lol_air_client: all 0
    // lol_air_client_config_euw: all 0
    // lol_launcher: all & 4
    // lol_game_client: all & 4
}
