package com.dimentor.util;

import com.dimentor.repository.FileRepository;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

;

public class FileUtil {
    public static String getHex(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toString()))) {
            return DigestUtils.md5Hex(bis);
        }
    }

    public static String getFilenameWithVersion(File file, List<MetaFile> metaFiles) throws IOException {
        if (file == null)
            throw new IllegalArgumentException("File in parameter is null");

        String clearFileName = VersionUtil.getClearFileName(file.getName());
        int version = VersionUtil.getVersion(file.getName());
        String hex = FileUtil.getHex(file);

        if (metaFiles != null) {
            for (MetaFile mf : metaFiles) {
                if (mf.getName().equals(clearFileName)) {
                    if (mf.getHex().equalsIgnoreCase(hex)) {
                        return null;
                        //file the same
                    } else {
                        version = Math.max(mf.getVersion(), version) + 1;
                        return VersionUtil.getFileNameWithVersion(clearFileName, version);
                        //return with new version
                    }
                }
            }
        }
        return file.getName();
        //haven't matches
    }

    public static ZipFile createRightZip(String clDir, String servDir) throws IOException {
        try (ZipFile zipFile = new ZipFile("zip.zip");
             Stream<Path> walk = Files.walk(Path.of(clDir), FileVisitOption.FOLLOW_LINKS);
        ) {
            Map<String, List<MetaFile>> map = new FileRepository().getStructureByUri(servDir);

            walk.forEach(o -> {
                File file = o.toFile();
                if (file.isFile()) {
                    Path dirRelativize = Path.of(clDir).getParent().relativize(file.toPath()).getParent();
                        List<MetaFile> metaFiles = map.get(dirRelativize.toString());
                    try {
                        String filenameWithVersion = FileUtil.getFilenameWithVersion(file, metaFiles);
                        if (filenameWithVersion != null) {
                            zipFile.addFile(file);
                            zipFile.renameFile(file.getName(),
                                    dirRelativize + "\\" + filenameWithVersion);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return zipFile;
        }
    }
}
