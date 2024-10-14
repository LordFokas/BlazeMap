package com.eerussianguy.blazemap.engine.storage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.maps.TileResolution;

public class InternalStorage extends PublicStorage {
    private static final String PATTERN_MIP = PATTERN + " [%d]";
    private static final String PATTERN_OLD = "%s+%s";
    private static final Pattern PATTERN_OLD_REGEX = Pattern.compile("^([a-z0-9_]+)\\+([a-z0-9_]+)$");

    public InternalStorage(StorageType type, File dir, String child) {
        this(type, new File(dir, child));
    }

    public InternalStorage(StorageType type, File dir) {
        super(type, dir);
        dir.mkdirs();
    }

    public File getMipmapDir(ResourceLocation node, String file, TileResolution resolution) {
        Objects.requireNonNull(node);
        File d = new File(dir, String.format(PATTERN_MIP, node.getNamespace(), node.getPath(), resolution.pixelWidth));
        d.mkdirs();
        return new File(d, file);
    }

    /**
     * Port files from their v0.4 location to their v0.5 location
     */
    public void tryPortDimension(ResourceLocation node) throws IOException {
        Objects.requireNonNull(node);
        File newFile = getDir(node).getParentFile();

        if(!newFile.exists() ||
            (newFile.isDirectory() && newFile.list().length == 0)) {
            File oldFile = new File(dir.getParent(), String.format(PATTERN_OLD, node.getNamespace(), node.getPath()));
            oldFile.getParentFile().mkdirs();

            if(oldFile.exists()) {
                move(oldFile, newFile);

                File[] layers = newFile.listFiles();
                for(File layer : layers) {
                    portLayer(layer);
                }
            }
        }
    }

    public void portLayer(File oldFile) throws IOException {
        String oldFilename = oldFile.getName();
        Matcher matcher = PATTERN_OLD_REGEX.matcher(oldFilename);

        if(matcher.matches()) {
            File newFile = new File(dir, String.format(PATTERN_MIP, matcher.group(1), matcher.group(2), TileResolution.FULL.pixelWidth));
            newFile.mkdirs();

            move(oldFile, newFile);
        }
    }

    public PublicStorage addon() {
        return new PublicStorage(type, dir);
    }

    public PublicStorage addon(ResourceLocation node) {
        return new PublicStorage(childType(), getDir(node));
    }

    public InternalStorage internal(ResourceLocation node) {
        return new InternalStorage(childType(), getDir(node));
    }

    protected StorageType childType() {
        return switch(type) {
            case GLOBAL -> StorageType.SERVER;
            case SERVER -> StorageType.LEVEL;
            default -> StorageType.GENERIC;
        };
    }
}
