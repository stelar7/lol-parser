package no.stelar7.cdragon.interfaces;

import java.nio.file.Path;

@FunctionalInterface
public interface Extractable
{
    void extract(Path output);
}
