# Offline audio fixtures

Expected outputs for `offline.OggTest`. No game data: each file is the ww2ogg conversion of a synthetic WEM that the test
builds in code (`OggTest.buildWem` / `OggTest.buildTriadWem`: random packet bytes from a fixed seed, a hand-written stripped
setup header, ids from the bundled codebook libraries). The test converts the same WEM with `OGGParser` and compares byte for byte.

Generated with ww2ogg 0.24 by hcs (Windows build, sha1 `70add68629a6bdc1afa5ddfa288067146e585901`) by the overhaul harness
`AudioCheck.java`:

    ww2ogg <name>.wem -o <name>.ww2ogg.ogg --pcb <library> [flags]

| file                 | WEM built by                                          | `--pcb` library                    | flags                |
|----------------------|-------------------------------------------------------|------------------------------------|----------------------|
| external.ww2ogg.ogg  | `buildWem("external", false, aotuvId())`              | `packed_codebooks_aoTuV_603.bin`   |                      |
| vorbext.ww2ogg.ogg   | `buildWem("vorbext", false, standardOnlyId())`        | `packed_codebooks.bin`             |                      |
| fallback.ww2ogg.ogg  | `buildWem("external", false, standardOnlyId())`       | `packed_codebooks.bin`             |                      |
| inline.ww2ogg.ogg    | `buildWem("inline", false, aotuvId())`                | `packed_codebooks_aoTuV_603.bin`   | `--inline-codebooks` |
| full.ww2ogg.ogg      | `buildWem("full", false, aotuvId())`                  | `packed_codebooks_aoTuV_603.bin`   | `--full-setup`       |
| triad.ww2ogg.ogg     | `buildTriadWem()`                                     | `packed_codebooks_aoTuV_603.bin`   |                      |

The codebook libraries are the ones in `src/main/resources/codebook/`. If the WEM builders change, these files have to be
regenerated with ww2ogg.
