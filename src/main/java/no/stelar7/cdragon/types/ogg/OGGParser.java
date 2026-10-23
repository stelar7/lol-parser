package no.stelar7.cdragon.types.ogg;

import no.stelar7.cdragon.types.ogg.data.*;
import no.stelar7.cdragon.types.wem.data.WEMData;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.util.*;

/**
 * Converts Wwise RIFF/RIFX Vorbis (.wem) into a standard Ogg Vorbis stream.
 * <p>
 * Port of ww2ogg's Wwise_RIFF_Vorbis::generate_ogg (wwriff.cpp)
 */
public class OGGParser
{
    /**
     * How the codebooks and setup packet are stored in the wem.
     * ww2ogg leaves this as a command line choice (--inline-codebooks / --full-setup); here it is detected.
     */
    public enum SetupType
    {
        /**
         * Codebooks are referenced by id into the packed aoTuV 6.03 codebook library, setup header is stripped
         * (ww2ogg --pcb packed_codebooks_aoTuV_603.bin, vgmstream WWV_AOTUV603_CODEBOOKS; Wwise 2012+)
         */
        EXTERNAL_CODEBOOKS(false, false),
        /**
         * Codebooks are referenced by id into the packed "standard" codebook library, setup header is stripped
         * (ww2ogg default packed_codebooks.bin, vgmstream WWV_EXTERNAL_CODEBOOKS; old Wwise v48-v56)
         */
        EXTERNAL_STANDARD_CODEBOOKS(false, false),
        /**
         * Stripped codebooks are stored in the setup packet, setup header is stripped (--inline-codebooks)
         */
        INLINE_CODEBOOKS(true, false),
        /**
         * Standard vorbis codebooks and setup header are stored in the setup packet (--full-setup)
         */
        FULL_SETUP(true, true);

        private final boolean inlineCodebooks;
        private final boolean fullSetup;

        SetupType(boolean inlineCodebooks, boolean fullSetup)
        {
            this.inlineCodebooks = inlineCodebooks;
            this.fullSetup = fullSetup;
        }

        /**
         * @return the library external codebook ids refer to, or null if the codebooks are inline
         */
        public CodebookLibrary getCodebookLibrary()
        {
            return switch (this)
            {
                case EXTERNAL_CODEBOOKS -> CodebookLibrary.getDefault();
                case EXTERNAL_STANDARD_CODEBOOKS -> CodebookLibrary.getStandard();
                default -> null;
            };
        }

        public boolean isInlineCodebooks()
        {
            return inlineCodebooks;
        }

        public boolean isFullSetup()
        {
            return fullSetup;
        }
    }

    // an external (stripped) setup packet is ~0x100 bytes max, anything much bigger is probably inline codebooks (from vgmstream)
    private static final int INLINE_SETUP_SIZE_HINT = 0x200;

    /**
     * Converts the wem, detecting how its setup packet is stored
     */
    public OGGStream parse(WEMData wem)
    {
        RandomAccessReader bitStream = createReader(wem);

        if (wem.isHeaderTriadPresent())
        {
            OGGStream ogg = new OGGStream();
            generateOGGHeaderTriad(ogg, bitStream, wem);
            generateAudioPages(ogg, bitStream, wem, null, 0);
            return ogg;
        }

        ParseException failure = null;
        for (SetupType type : detectSetupTypes(bitStream, wem))
        {
            OGGStream ogg = new OGGStream();
            SetupResult setup;
            try
            {
                setup = generateOGGHeader(ogg, bitStream, wem, type);
            } catch (ParseException e)
            {
                if (failure == null)
                {
                    failure = e;
                } else
                {
                    failure.addSuppressed(e);
                }
                continue;
            }

            generateAudioPages(ogg, bitStream, wem, setup.modeBlockFlag, setup.modeBits);
            return ogg;
        }

        throw failure;
    }

    /**
     * Converts the wem, using the given setup type
     */
    public OGGStream parse(WEMData wem, SetupType type)
    {
        RandomAccessReader bitStream = createReader(wem);
        OGGStream          ogg       = new OGGStream();

        if (wem.isHeaderTriadPresent())
        {
            generateOGGHeaderTriad(ogg, bitStream, wem);
            generateAudioPages(ogg, bitStream, wem, null, 0);
        } else
        {
            SetupResult setup = generateOGGHeader(ogg, bitStream, wem, type);
            generateAudioPages(ogg, bitStream, wem, setup.modeBlockFlag, setup.modeBits);
        }

        return ogg;
    }

    private RandomAccessReader createReader(WEMData wem)
    {
        RandomAccessReader reader = new RandomAccessReader(wem.getDataBytes(), wem.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        if ((long) wem.getDataChunkOffset() + wem.getDataChunkSize() > reader.size())
        {
            throw new ParseException("Data chunk truncated", wem.getDataChunkOffset());
        }
        return reader;
    }

    /**
     * The setup types to try, most likely first.
     * Follows vgmstream's detection (wwise.c): a setup packet starting with a "BCV" codebook sync is a full setup,
     * a suspiciously big setup packet holds inline codebooks, anything else uses an external codebook library.
     * <p>
     * The external library depends on the Wwise version (vgmstream's setup_version_config):
     * a separate vorb chunk (v48-v53, ~2010-2011) uses the standard codebooks; vorbis info inside the fmt chunk (v56+)
     * can be either and isn't detectable, so like vgmstream for .wem the aoTuV codebooks are tried first, then the standard ones.
     * A wrong library is rejected by the codebook size checks and the exact setup packet size check.
     */
    private List<SetupType> detectSetupTypes(RandomAccessReader reader, WEMData wem)
    {
        boolean   oldVorbChunk = wem.getVorbChunkSize() != -1;
        SetupType external     = oldVorbChunk ? SetupType.EXTERNAL_STANDARD_CODEBOOKS : SetupType.EXTERNAL_CODEBOOKS;
        SetupType otherExt     = oldVorbChunk ? SetupType.EXTERNAL_CODEBOOKS : SetupType.EXTERNAL_STANDARD_CODEBOOKS;

        try
        {
            OGGPacket setup   = new OGGPacket(reader, wem.getDataChunkOffset() + wem.getSetupPacketOffset(), wem.isNoGranule());
            int       payload = setup.getOffset();

            if (setup.getSize() >= 4 && payload + 4 <= reader.size())
            {
                reader.seek(payload + 1);
                byte[] sync = reader.readBytes(3);
                if (sync[0] == 'B' && sync[1] == 'C' && sync[2] == 'V')
                {
                    return List.of(SetupType.FULL_SETUP);
                }
            }

            if (setup.getSize() > INLINE_SETUP_SIZE_HINT)
            {
                return List.of(SetupType.INLINE_CODEBOOKS, external, otherExt, SetupType.FULL_SETUP);
            }
        } catch (ParseException e)
        {
            // let the real parse report the problem
        }

        return List.of(external, otherExt, SetupType.INLINE_CODEBOOKS, SetupType.FULL_SETUP);
    }

    private void generateAudioPages(OGGStream ogg, RandomAccessReader bitStream, WEMData wem, boolean[] modeBlockFlag, int modeBits)
    {
        boolean previousBlockFlag = false;
        long    dataEnd           = (long) wem.getDataChunkOffset() + wem.getDataChunkSize();
        byte[]  bytes             = wem.getDataBytes();

        int offset = wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset();
        while (offset < dataEnd)
        {
            int size;
            int granule;
            int packetHeaderSize;
            int packetPayloadOffset;
            int nextOffset;

            if (wem.isOldPacketHeaders())
            {
                packetHeaderSize = 8;
                if (offset + packetHeaderSize > dataEnd)
                {
                    throw new ParseException("Page header truncated", offset);
                }

                OGGPacket8 audioPacket = new OGGPacket8(bitStream, offset);
                size = audioPacket.getSize();
                packetPayloadOffset = audioPacket.getOffset();
                granule = audioPacket.getGranule();
                nextOffset = audioPacket.nextOffset();
            } else
            {
                packetHeaderSize = wem.isNoGranule() ? 2 : 6;
                if (offset + packetHeaderSize > dataEnd)
                {
                    throw new ParseException("Page header truncated", offset);
                }

                OGGPacket audioPacket = new OGGPacket(bitStream, offset, wem.isNoGranule());
                size = audioPacket.getSize();
                packetPayloadOffset = audioPacket.getOffset();
                granule = audioPacket.getGranule();
                nextOffset = audioPacket.nextOffset();
            }

            offset = packetPayloadOffset;

            // the first byte is always read, even for an empty packet
            if ((long) offset + Math.max(size, 1) > bytes.length)
            {
                throw new ParseException("File truncated", offset);
            }

            // HACK: don't know what to do here (from ww2ogg)
            ogg.setGranule((granule == 0xFFFFFFFF) ? 1 : granule);

            int firstByte = bytes[offset] & 0xFF;
            if (wem.isModPackets())
            {
                if (modeBlockFlag == null)
                {
                    throw new ParseException("Didn't load modeBlockFlag", offset);
                }

                // OUT: 1 bit packet type (0 == audio)
                ogg.writeBit(0);

                // IN/OUT: N bit mode number (max 6 bits), LSB first in the first byte
                int modeNumber = firstByte & ((1 << modeBits) - 1);
                // IN: remaining bits of the first byte
                int remainder = firstByte >>> modeBits;

                if (modeNumber >= modeBlockFlag.length)
                {
                    throw new ParseException("Invalid mode number " + modeNumber, offset);
                }

                ogg.bitWrite(modeNumber, modeBits);

                if (modeBlockFlag[modeNumber])
                {
                    // long window, peek at next frame
                    boolean nextBlockFlag = false;

                    if (nextOffset + packetHeaderSize <= dataEnd)
                    {
                        // mod_packets always goes with 6-byte headers
                        OGGPacket nextPacket = new OGGPacket(bitStream, nextOffset, wem.isNoGranule());
                        if (nextPacket.getSize() > 0 && nextPacket.getOffset() < bytes.length)
                        {
                            int nextModeNumber = bytes[nextPacket.getOffset()] & ((1 << modeBits) - 1);
                            if (nextModeNumber >= modeBlockFlag.length)
                            {
                                throw new ParseException("Invalid mode number " + nextModeNumber, nextPacket.getOffset());
                            }
                            nextBlockFlag = modeBlockFlag[nextModeNumber];
                        }
                    }

                    // OUT: previous window type bit
                    ogg.writeBit(previousBlockFlag ? 1 : 0);
                    // OUT: next window type bit
                    ogg.writeBit(nextBlockFlag ? 1 : 0);
                }

                previousBlockFlag = modeBlockFlag[modeNumber];

                // OUT: remaining bits of the first (input) byte
                ogg.bitWrite(remainder, 8 - modeBits);
            } else
            {
                // nothing unusual for first byte
                ogg.bitWrite(firstByte, Byte.SIZE);
            }

            // remainder of packet
            if (size > 1)
            {
                ogg.writeBytes(bytes, offset + 1, size - 1);
            }

            offset = nextOffset;
            ogg.flushPage(false, offset == dataEnd);
        }

        if (offset > dataEnd)
        {
            throw new ParseException("Page truncated", offset);
        }
    }

    // port of generate_ogg_header_with_triad
    private void generateOGGHeaderTriad(OGGStream ogg, RandomAccessReader bitStream, WEMData wem)
    {
        int offset = wem.getDataChunkOffset() + wem.getSetupPacketOffset();

        // copy information packet
        offset = copyTriadPacket(ogg, bitStream, offset, 1, "information");

        // copy comment packet
        offset = copyTriadPacket(ogg, bitStream, offset, 3, "comment");

        // copy setup packet
        OGGPacket8 setup = new OGGPacket8(bitStream, offset);
        if (setup.getGranule() != 0)
        {
            throw new ParseException("Setup packet granule != 0", offset);
        }

        bitStream.seek(setup.getOffset());
        int startBits = bitStream.getTotalBitsRead();

        try
        {
            int type = bitStream.readBits(8);
            if (type != 5)
            {
                throw new ParseException("Wrong type for setup packet", setup.getOffset());
            }
            ogg.bitWrite(type, Byte.SIZE);

            // 'vorbis'
            for (int i = 0; i < 6; i++)
            {
                ogg.bitWrite(bitStream.readBits(8), Byte.SIZE);
            }

            int codebookCountLess1 = bitStream.readBits(8);
            ogg.bitWrite(codebookCountLess1, Byte.SIZE);
            int codebookCount = codebookCountLess1 + 1;

            for (int i = 0; i < codebookCount; i++)
            {
                CodebookLibrary.copy(bitStream, ogg);
            }

            copyRemainingBits(ogg, bitStream, startBits, setup.getSize());
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Setup packet truncated", bitStream.pos(), e);
        }

        ogg.flushPage(false, false);
        offset = setup.nextOffset();

        if (offset != wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset())
        {
            throw new ParseException("First audio packet doesn't follow setup packet", offset);
        }
    }

    private int copyTriadPacket(OGGStream ogg, RandomAccessReader bitStream, int offset, int expectedType, String name)
    {
        OGGPacket8 packet = new OGGPacket8(bitStream, offset);
        if (packet.getGranule() != 0)
        {
            throw new ParseException(name + " packet granule != 0", offset);
        }

        if (packet.getSize() < 1 || (long) packet.getOffset() + packet.getSize() > bitStream.size())
        {
            throw new ParseException(name + " packet truncated", offset);
        }

        bitStream.seek(packet.getOffset());
        int type = bitStream.readU8();
        if (type != expectedType)
        {
            throw new ParseException("Wrong type for " + name + " packet", packet.getOffset());
        }
        ogg.bitWrite(type, Byte.SIZE);

        // the type byte is part of the packet
        byte[] rest = bitStream.readBytes(packet.getSize() - 1);
        ogg.writeBytes(rest, 0, rest.length);

        // each header packet on its own page
        ogg.flushPage(false, false);
        return packet.nextOffset();
    }

    private void copyRemainingBits(OGGStream ogg, RandomAccessReader bitStream, int startBits, long packetSize)
    {
        while (bitStream.getTotalBitsRead() - startBits < packetSize * 8)
        {
            ogg.writeBit(bitStream.readBits(1));
        }
    }

    private void generateIdentificationHeader(OGGStream ogg, WEMData wem)
    {
        ogg.writeVorbisHeader(1);

        // version
        ogg.bitWrite(0, 32);
        ogg.bitWrite(wem.getChannelCount(), 8);
        ogg.bitWrite(wem.getSampleRate(), 32);
        // bitrate max
        ogg.bitWrite(0, 32);
        // bitrate nominal
        ogg.bitWrite(wem.getBytesPerSecond() * 8, 32);
        // bitrate min
        ogg.bitWrite(0, 32);
        ogg.bitWrite(wem.getBlockSize0Pow(), 4);
        ogg.bitWrite(wem.getBlockSize1Pow(), 4);
        // framing
        ogg.bitWrite(1, 1);

        // identification packet on its own page
        ogg.flushPage(false, false);
    }

    private void generateCommentHeader(OGGStream ogg, WEMData wem)
    {
        ogg.writeVorbisHeader(3);

        String vendor = "converted from Audiokinetic Wwise by ww2ogg 0.24";

        ogg.bitWriteLengthAndString(vendor);

        if (wem.getLoopCount() == 0)
        {
            // no user comments
            ogg.bitWrite(0, 32);
        } else
        {
            // two comments, loop start and end
            ogg.bitWrite(2, 32);

            ogg.bitWriteLengthAndString("LoopStart=" + Integer.toUnsignedString(wem.getLoopStart()));
            ogg.bitWriteLengthAndString("LoopEnd=" + Integer.toUnsignedString(wem.getLoopEnd()));
        }

        // framing
        ogg.bitWrite(1, 1);

        ogg.flushPage(false, false);
    }

    private static final class SetupResult
    {
        private final boolean[] modeBlockFlag;
        private final int       modeBits;

        private SetupResult(boolean[] modeBlockFlag, int modeBits)
        {
            this.modeBlockFlag = modeBlockFlag;
            this.modeBits = modeBits;
        }
    }

    private SetupResult generateSetupHeader(OGGStream ogg, WEMData wem, RandomAccessReader bitStream, SetupType type)
    {
        boolean   inlineCodebook = type.isInlineCodebooks();
        boolean   fullSetup      = type.isFullSetup();
        boolean[] modeBlockFlag  = null;
        int       modeBits       = 0;

        ogg.writeVorbisHeader(5);

        int       setupOffset = wem.getDataChunkOffset() + wem.getSetupPacketOffset();
        OGGPacket setupPacket = new OGGPacket(bitStream, setupOffset, wem.isNoGranule());

        if (setupPacket.getGranule() != 0)
        {
            throw new ParseException("Setup packet granule != 0", setupOffset);
        }

        bitStream.seek(setupPacket.getOffset());
        int startBits = bitStream.getTotalBitsRead();

        try
        {
            int codebookCountLess1 = bitStream.readBits(8);
            int codebookCount      = codebookCountLess1 + 1;
            ogg.bitWrite(codebookCountLess1, 8);

            CodebookLibrary.CodebookInfo[] codebooks = new CodebookLibrary.CodebookInfo[codebookCount];
            if (inlineCodebook)
            {
                for (int i = 0; i < codebookCount; i++)
                {
                    if (fullSetup)
                    {
                        codebooks[i] = CodebookLibrary.copy(bitStream, ogg);
                    } else
                    {
                        codebooks[i] = CodebookLibrary.rebuild(bitStream, 0, ogg);
                    }
                }
            } else
            {
                CodebookLibrary codebook = type.getCodebookLibrary();

                for (int i = 0; i < codebookCount; i++)
                {
                    int codebookId = bitStream.readBits(10);
                    try
                    {
                        codebooks[i] = codebook.rebuild(codebookId, ogg);
                    } catch (ParseException e)
                    {
                        //         B         C         V
                        //    4    2    4    3    5    6
                        // 0100 0010 0100 0011 0101 0110
                        // \_______|____ ___|/
                        //              X
                        //            11 0100 0010
                        if (codebookId == 0x342)
                        {
                            int codebookIdentifier = bitStream.readBits(14);
                            if (codebookIdentifier == 0x1590)
                            {
                                // starts with BCV, probably full setup
                                throw new ParseException("Invalid codebook id 0x342, probably a full setup", bitStream.pos(), e);
                            }
                        }

                        throw e;
                    }
                }
            }

            // Time Domain transforms (placeholder)
            ogg.bitWrite(0, 6);
            ogg.bitWrite(0, 16);

            if (fullSetup)
            {
                copyRemainingBits(ogg, bitStream, startBits, setupPacket.getSize());
            } else
            {
                int floorCountLess1 = bitStream.readBits(6);
                int floorCount      = floorCountLess1 + 1;
                ogg.bitWrite(floorCountLess1, 6);

                for (int i = 0; i < floorCount; i++)
                {
                    rebuildFloor(ogg, bitStream, codebookCount);
                }

                int residueCountLess1 = bitStream.readBits(6);
                int residueCount      = residueCountLess1 + 1;
                ogg.bitWrite(residueCountLess1, 6);

                for (int i = 0; i < residueCount; i++)
                {
                    rebuildResidue(ogg, bitStream, codebooks, !inlineCodebook);
                }

                int mappingCountLess1 = bitStream.readBits(6);
                int mappingCount      = mappingCountLess1 + 1;
                ogg.bitWrite(mappingCountLess1, 6);

                for (int i = 0; i < mappingCount; i++)
                {
                    rebuildMapping(ogg, bitStream, wem, floorCount, residueCount);
                }

                int modeCountLess1 = bitStream.readBits(6);
                int modeCount      = modeCountLess1 + 1;
                ogg.bitWrite(modeCountLess1, 6);

                modeBlockFlag = new boolean[modeCount];
                modeBits = CodebookLibrary.ilog(modeCount - 1);

                for (int i = 0; i < modeCount; i++)
                {
                    int blockFlag = bitStream.readBits(1);
                    ogg.writeBit(blockFlag);

                    modeBlockFlag[i] = (blockFlag != 0);

                    // only 0 valid for windowtype and transformtype
                    ogg.bitWrite(0, 16);
                    ogg.bitWrite(0, 16);

                    int mapping = bitStream.readBits(8);
                    ogg.bitWrite(mapping, 8);
                    if (mapping >= mappingCount)
                    {
                        throw new ParseException("Invalid mode mapping", bitStream);
                    }
                }

                // framing
                ogg.bitWrite(1, 1);
            }
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Setup packet truncated", bitStream.pos(), e);
        }

        ogg.flushPage(false, false);

        int bitsRead = bitStream.getTotalBitsRead() - startBits;
        if ((bitsRead + 7) / 8 != setupPacket.getSize())
        {
            throw new ParseException("Didn't read exactly the setup packet (" + ((bitsRead + 7) / 8) + " of " + setupPacket.getSize() + " bytes)", setupOffset);
        }

        if (setupPacket.nextOffset() != wem.getDataChunkOffset() + wem.getFirstAudioPacketOffset())
        {
            throw new ParseException("First audio packet doesn't follow setup packet", setupPacket.nextOffset());
        }

        return new SetupResult(modeBlockFlag, modeBits);
    }

    private void rebuildFloor(OGGStream ogg, RandomAccessReader bitStream, int codebookCount)
    {
        // always floor type 1
        ogg.bitWrite(1, 16);

        int floor1Partitions = bitStream.readBits(5);
        ogg.bitWrite(floor1Partitions, 5);

        int[] floor1PartitionClassList = new int[floor1Partitions];

        int maxClass = 0;
        for (int j = 0; j < floor1Partitions; j++)
        {
            int floor1PartitionClass = bitStream.readBits(4);
            ogg.bitWrite(floor1PartitionClass, 4);

            floor1PartitionClassList[j] = floor1PartitionClass;
            maxClass = Math.max(maxClass, floor1PartitionClass);
        }

        int[] floor1ClassDimensionList = new int[maxClass + 1];

        for (int j = 0; j <= maxClass; j++)
        {
            int classDimensionsLess1 = bitStream.readBits(3);
            ogg.bitWrite(classDimensionsLess1, 3);
            floor1ClassDimensionList[j] = classDimensionsLess1 + 1;

            int classSubclasses = bitStream.readBits(2);
            ogg.bitWrite(classSubclasses, 2);

            if (classSubclasses != 0)
            {
                int masterbook = bitStream.readBits(8);
                ogg.bitWrite(masterbook, 8);

                if (masterbook >= codebookCount)
                {
                    throw new ParseException("Invalid floor1 masterbook", bitStream);
                }
            }

            for (int k = 0; k < (1 << classSubclasses); k++)
            {
                int subclassBookPlus1 = bitStream.readBits(8);
                ogg.bitWrite(subclassBookPlus1, 8);

                int subclassBook = subclassBookPlus1 - 1;
                if (subclassBook >= 0 && subclassBook >= codebookCount)
                {
                    throw new ParseException("Invalid floor1 subclass book", bitStream);
                }
            }
        }

        int floorMultiplierLess1 = bitStream.readBits(2);
        ogg.bitWrite(floorMultiplierLess1, 2);

        int rangeBits = bitStream.readBits(4);
        ogg.bitWrite(rangeBits, 4);

        for (int j = 0; j < floor1Partitions; j++)
        {
            int currentClass = floor1PartitionClassList[j];
            for (int k = 0; k < floor1ClassDimensionList[currentClass]; k++)
            {
                ogg.bitWrite(bitStream.readBits(rangeBits), rangeBits);
            }
        }
    }

    /**
     * @param validateBooks check the books like libvorbis' res0_unpack does; an external codebook id is valid in every library,
     *                      so this is what rejects the wrong library (vgmstream relies on libvorbis for this)
     */
    private void rebuildResidue(OGGStream ogg, RandomAccessReader bitStream, CodebookLibrary.CodebookInfo[] codebooks, boolean validateBooks)
    {
        int codebookCount = codebooks.length;

        int residueType = bitStream.readBits(2);
        ogg.bitWrite(residueType, 16);

        if (residueType > 2)
        {
            throw new ParseException("Invalid residue type", bitStream);
        }

        int residueBegin                = bitStream.readBits(24);
        int residueEnd                  = bitStream.readBits(24);
        int residuePartitionSizeLess1   = bitStream.readBits(24);
        int residueClassificationsLess1 = bitStream.readBits(6);
        int residueClassbook            = bitStream.readBits(8);

        int residueClassifications = residueClassificationsLess1 + 1;

        ogg.bitWrite(residueBegin, 24);
        ogg.bitWrite(residueEnd, 24);
        ogg.bitWrite(residuePartitionSizeLess1, 24);
        ogg.bitWrite(residueClassificationsLess1, 6);
        ogg.bitWrite(residueClassbook, 8);

        if (residueClassbook >= codebookCount)
        {
            throw new ParseException("Invalid residue classbook", bitStream);
        }

        if (validateBooks)
        {
            // the classbook must be able to express every partition combination it is used for
            CodebookLibrary.CodebookInfo classbook = codebooks[residueClassbook];
            long                         partvals  = 1;
            for (int d = 0; d < classbook.dimensions(); d++)
            {
                partvals *= residueClassifications;
                if (partvals > classbook.entries())
                {
                    break;
                }
            }
            if (classbook.dimensions() < 1 || partvals > classbook.entries())
            {
                throw new ParseException("Residue classbook " + residueClassbook + " can't express the partitions, wrong codebook library?", bitStream);
            }
        }

        int[] residueCascade = new int[residueClassifications];
        for (int j = 0; j < residueClassifications; j++)
        {
            int highBits = 0;

            int lowBits = bitStream.readBits(3);
            ogg.bitWrite(lowBits, 3);

            int bitFlag = bitStream.readBits(1);
            ogg.writeBit(bitFlag);

            if (bitFlag == 1)
            {
                highBits = bitStream.readBits(5);
                ogg.bitWrite(highBits, 5);
            }

            residueCascade[j] = (highBits * 8) + lowBits;
        }

        for (int j = 0; j < residueClassifications; j++)
        {
            for (int k = 0; k < 8; k++)
            {
                if ((residueCascade[j] & (1 << k)) != 0)
                {
                    int residueBook = bitStream.readBits(8);
                    ogg.bitWrite(residueBook, 8);

                    if (residueBook >= codebookCount)
                    {
                        throw new ParseException("Invalid residue book", bitStream);
                    }

                    if (validateBooks && codebooks[residueBook].lookupType() == 0)
                    {
                        throw new ParseException("Residue book " + residueBook + " has no value mapping, wrong codebook library?", bitStream);
                    }
                }
            }
        }
    }

    private void rebuildMapping(OGGStream ogg, RandomAccessReader bitStream, WEMData wem, int floorCount, int residueCount)
    {
        // always mapping type 0, the only one
        ogg.bitWrite(0, 16);

        int submapsFlag = bitStream.readBits(1);
        ogg.writeBit(submapsFlag);

        int submaps = 1;
        if (submapsFlag == 1)
        {
            int submapsLess1 = bitStream.readBits(4);
            ogg.bitWrite(submapsLess1, 4);
            submaps = submapsLess1 + 1;
        }

        int squarePolarFlag = bitStream.readBits(1);
        ogg.writeBit(squarePolarFlag);

        int channels = wem.getChannelCount();
        if (squarePolarFlag == 1)
        {
            int couplingStepsLess1 = bitStream.readBits(8);
            ogg.bitWrite(couplingStepsLess1, 8);
            int couplingSteps = couplingStepsLess1 + 1;

            int iLogChannels = CodebookLibrary.ilog(channels - 1);
            for (int j = 0; j < couplingSteps; j++)
            {
                int magnitude = bitStream.readBits(iLogChannels);
                int angle     = bitStream.readBits(iLogChannels);

                ogg.bitWrite(magnitude, iLogChannels);
                ogg.bitWrite(angle, iLogChannels);

                if (angle == magnitude || magnitude >= channels || angle >= channels)
                {
                    throw new ParseException("Invalid coupling", bitStream);
                }
            }
        }

        // a rare reserved field not removed by Ak!
        int mappingReserved = bitStream.readBits(2);
        ogg.bitWrite(mappingReserved, 2);

        if (mappingReserved != 0)
        {
            throw new ParseException("Mapping reserved field nonzero", bitStream);
        }

        if (submaps > 1)
        {
            for (int j = 0; j < channels; j++)
            {
                int mappingMux = bitStream.readBits(4);
                ogg.bitWrite(mappingMux, 4);

                if (mappingMux >= submaps)
                {
                    throw new ParseException("mapping_mux >= submaps", bitStream);
                }
            }
        }

        for (int j = 0; j < submaps; j++)
        {
            // Another! Unused time domain transform configuration placeholder!
            int timeConfig = bitStream.readBits(8);
            ogg.bitWrite(timeConfig, 8);

            int floorNumber = bitStream.readBits(8);
            ogg.bitWrite(floorNumber, 8);
            if (floorNumber >= floorCount)
            {
                throw new ParseException("Invalid floor mapping", bitStream);
            }

            int residueNumber = bitStream.readBits(8);
            ogg.bitWrite(residueNumber, 8);
            if (residueNumber >= residueCount)
            {
                throw new ParseException("Invalid residue mapping", bitStream);
            }
        }
    }

    private SetupResult generateOGGHeader(OGGStream ogg, RandomAccessReader bitStream, WEMData wem, SetupType type)
    {
        generateIdentificationHeader(ogg, wem);
        generateCommentHeader(ogg, wem);
        return generateSetupHeader(ogg, wem, bitStream, type);
    }
}
