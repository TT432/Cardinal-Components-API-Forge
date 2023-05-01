package io.github.tt432.ccaforge.entrypointes;

import net.minecraftforge.forgespi.locating.IModFile;

/**
 * @param entrypoint entrypoint of the container
 * @param mod        which mod hold the container
 * @author DustW
 */
public record EntrypointContainer<T>(T entrypoint, IModFile mod) {
}
