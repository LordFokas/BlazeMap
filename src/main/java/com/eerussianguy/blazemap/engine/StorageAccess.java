package com.eerussianguy.blazemap.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.IStorageAccess;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class StorageAccess implements IStorageAccess {
    protected final File dir;

    public StorageAccess(File dir) {
        this.dir = dir;
    }

    @Override
    public boolean exists(ResourceLocation node) {
        return getFile(node).exists();
    }

    @Override
    public boolean exists(ResourceLocation node, String child) {
        return getFile(node, child).exists();
    }

    @Override
    public MinecraftStreams.Input read(ResourceLocation node) throws IOException {
        return new MinecraftStreams.Input(new FileInputStream(getFile(node)));
    }

    @Override
    public MinecraftStreams.Input read(ResourceLocation node, String child) throws IOException {
        return new MinecraftStreams.Input(new FileInputStream(getFile(node, child)));
    }

    @Override
    public MinecraftStreams.Output write(ResourceLocation node) throws IOException {
        return new MinecraftStreams.Output(new FileOutputStream(getFile(node)));
    }

    @Override
    public MinecraftStreams.Output write(ResourceLocation node, String child) throws IOException {
        return new MinecraftStreams.Output(new FileOutputStream(getFile(node, child)));
    }

    @Override
    public void move(ResourceLocation source, ResourceLocation destination) throws IOException {
        move(getFile(source), getFile(destination));
    }

    @Override
    public void move(ResourceLocation node, String source, String destination) throws IOException {
        move(getFile(node, source), getFile(node, destination));
    }

    protected void move(File source, File destination) throws IOException {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    protected File getFile(ResourceLocation node) {
        Objects.requireNonNull(node);
        File mod = new File(dir, node.getNamespace());
        File file = new File(mod, node.getPath());
        file.getParentFile().mkdirs();
        return file;
    }

    protected File getFile(ResourceLocation node, String child) {
        Objects.requireNonNull(node);
        File dir = getFile(node);
        dir.mkdirs();
        return new File(dir, child);
    }

    public static class Internal extends StorageAccess {
        private static final String PATTERN = "[%s] %s";
        private static final String PATTERN_MIP = "[%s] %s [%d]";
        private static final String PATTERN_OLD = "%s+%s";
        private static final Pattern PATTERN_OLD_REGEX = Pattern.compile("^([a-z0-9_]+)\\+([a-z0-9_]+)$");

        public Internal(File dir, String child) {
            this(new File(dir, child));
        }

        public Internal(File dir) {
            super(dir);
            dir.mkdirs();
        }

        @Override
        public File getFile(ResourceLocation node) {
            Objects.requireNonNull(node);
            File file = new File(dir, String.format(PATTERN, node.getNamespace(), node.getPath()));
            file.getParentFile().mkdirs();
            return file;
        }

        public File getMipmap(ResourceLocation node, String file, TileResolution resolution) {
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
            File newFile = getFile(node).getParentFile();

            if ( !newFile.exists() ||
                (newFile.isDirectory() && newFile.list().length == 0))
            {
                File oldFile = new File(dir.getParent(), String.format(PATTERN_OLD, node.getNamespace(), node.getPath()));
                oldFile.getParentFile().mkdirs();

                if (oldFile.exists()) {
                    move(oldFile, newFile);

                    File[] layers = newFile.listFiles();
                    for (File layer : layers) {
                        portLayer(layer);
                    }
                }
            }
        }

        public void portLayer(File oldFile) throws IOException {
            String oldFilename = oldFile.getName();
            Matcher matcher = PATTERN_OLD_REGEX.matcher(oldFilename);

            if (matcher.matches()) {
                File newFile = new File(dir, String.format(PATTERN_MIP, matcher.group(1), matcher.group(2), TileResolution.FULL.pixelWidth));
                newFile.mkdirs();

                move(oldFile, newFile);
            }
        }

        public StorageAccess addon() {
            return new StorageAccess(dir);
        }

        public StorageAccess addon(ResourceLocation node) {
            return new StorageAccess(getFile(node));
        }

        public StorageAccess.Internal internal(ResourceLocation node) {
            return new Internal(getFile(node));
        }
    }
}