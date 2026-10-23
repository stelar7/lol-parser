package no.stelar7.cdragon.types.sco;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.scb.data.*;
import no.stelar7.cdragon.util.readers.*;
import no.stelar7.cdragon.util.types.math.*;

import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Text static mesh (.sco) reader, the text version of .scb. Produces the same data classes as SCBParser.
 * <p>
 * Layout reference: LeagueToolkit Core/Mesh/StaticMesh.cs (ReadAscii) and StaticMeshFace.cs.
 * <pre>
 * [ObjectBegin]
 * Name= name
 * CentralPoint= x y z
 * PivotPoint= x y z        (optional)
 * VertexColors= 0|1        (optional)
 * Verts= N
 * x y z                    (N lines)
 * r g b a                  (N lines, only when VertexColors= 1)
 * Faces= M
 * 3 i0 i1 i2 material u0 u1 u2 v0 v1 v2   (M lines)
 * [ObjectEnd]
 * </pre>
 * The face uvs are stored in the same order as in .scb (all u's, then all v's).
 */
public class SCOParser implements Parseable<SCBFile>
{
    public static final String MAGIC = "[ObjectBegin]";

    /**
     * True if the first non-whitespace text (after an optional UTF-8 BOM) is "[ObjectBegin]"
     */
    @Override
    public boolean canParse(byte[] data)
    {
        if (data == null)
        {
            return false;
        }

        int start = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF)
        {
            start = 3;
        }
        while (start < data.length && (data[start] == ' ' || data[start] == '\t' || data[start] == '\r' || data[start] == '\n'))
        {
            start++;
        }

        if (data.length - start < MAGIC.length())
        {
            return false;
        }
        for (int i = 0; i < MAGIC.length(); i++)
        {
            if (data[start + i] != MAGIC.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public SCBFile parse(RandomAccessReader raf)
    {
        String   text  = new String(raf.readRemaining(), StandardCharsets.UTF_8);
        String[] lines = text.split("\r?\n");

        int line = 0;
        while (line < lines.length && lines[line].isBlank())
        {
            line++;
        }

        if (line >= lines.length || !lines[line].strip().replace("﻿", "").equals(MAGIC))
        {
            throw new ParseException("Invalid sco header", 0);
        }
        line++;

        String         filename        = "";
        Vector3f       center          = null;
        Vector3f       pivot           = null;
        boolean        hasVertexColors = false;
        int            vertexCount     = 0;
        int            faceCount       = 0;
        List<Vector3f> vertices        = new ArrayList<>();
        List<Vector4b> vertexColors    = new ArrayList<>();
        List<SCBFace>  faces           = new ArrayList<>();
        boolean        ended           = false;

        while (line < lines.length && !ended)
        {
            String current = lines[line++].strip();
            if (current.isEmpty())
            {
                continue;
            }

            String[] parts = current.split("\\s+");
            switch (parts[0])
            {
                case "Name=" -> filename = current.substring(parts[0].length()).strip();
                case "CentralPoint=" -> center = parseVector(parts, 1, line);
                case "PivotPoint=" -> pivot = parseVector(parts, 1, line);
                case "VertexColors=" -> hasVertexColors = parseInt(parts, 1, line) != 0;
                case "Verts=" ->
                {
                    int count = parseInt(parts, 1, line);
                    checkCount(count, lines.length - line, line);
                    vertexCount = count;
                    for (int i = 0; i < count; i++)
                    {
                        vertices.add(parseVector(lines[line++].strip().split("\\s+"), 0, line));
                    }

                    if (hasVertexColors)
                    {
                        checkCount(count, lines.length - line, line);
                        for (int i = 0; i < count; i++)
                        {
                            String[] c     = lines[line++].strip().split("\\s+");
                            Vector4b color = new Vector4b();
                            color.setX((byte) parseInt(c, 0, line));
                            color.setY((byte) parseInt(c, 1, line));
                            color.setZ((byte) parseInt(c, 2, line));
                            color.setW((byte) parseInt(c, 3, line));
                            vertexColors.add(color);
                        }
                    }
                }
                case "Faces=" ->
                {
                    int count = parseInt(parts, 1, line);
                    checkCount(count, lines.length - line, line);
                    faceCount = count;
                    for (int i = 0; i < count; i++)
                    {
                        faces.add(parseFace(lines[line++].strip().split("\\s+"), line));
                    }
                }
                case "[ObjectEnd]" -> ended = true;
                default ->
                {
                    // unknown keyword, ignore it
                }
            }
        }

        if (center == null)
        {
            throw new ParseException("sco is missing CentralPoint=", -1);
        }
        if (pivot == null)
        {
            pivot = center;
        }

        SCBHeader  header  = new SCBHeader(MAGIC, (short) 0, (short) 0, filename);
        SCBContent content = new SCBContent(vertexCount, faceCount, 0, null, null, center, pivot, hasVertexColors, vertexColors, vertices, faces, new ArrayList<>());
        return new SCBFile(header, content);
    }

    private SCBFace parseFace(String[] parts, int line)
    {
        // 3 i0 i1 i2 material u0 u1 u2 v0 v1 v2 (the material name may contain spaces)
        if (parts.length < 11)
        {
            throw new ParseException("Invalid sco face on line " + line, -1);
        }

        Vector3i indices = new Vector3i();
        indices.x = parseInt(parts, 1, line);
        indices.y = parseInt(parts, 2, line);
        indices.z = parseInt(parts, 3, line);

        int uvStart = parts.length - 6;

        String material = String.join(" ", java.util.Arrays.copyOfRange(parts, 4, uvStart));
        return new SCBFace(indices, material, parseVector(parts, uvStart, line), parseVector(parts, uvStart + 3, line));
    }

    private void checkCount(int count, int available, int line)
    {
        if (count < 0 || count > available)
        {
            throw new ParseException("Invalid sco element count " + count + " on line " + line, -1);
        }
    }

    private Vector3f parseVector(String[] parts, int start, int line)
    {
        try
        {
            return new Vector3f(Float.parseFloat(parts[start]), Float.parseFloat(parts[start + 1]), Float.parseFloat(parts[start + 2]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
        {
            throw new ParseException("Invalid sco vector on line " + line, -1, e);
        }
    }

    private int parseInt(String[] parts, int index, int line)
    {
        try
        {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
        {
            throw new ParseException("Invalid sco number on line " + line, -1, e);
        }
    }
}
