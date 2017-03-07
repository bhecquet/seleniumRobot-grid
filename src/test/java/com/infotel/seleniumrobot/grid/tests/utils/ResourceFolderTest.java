package com.infotel.seleniumrobot.grid.tests.utils;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.zeroturnaround.zip.ZipUtil;

import com.google.common.io.Files;
import com.infotel.seleniumrobot.grid.utils.ResourceFolder;

/**
 * @author Alexey Nikolaenko alexey@tcherezov.com
 *         Date: 08/10/2015
 */
public class ResourceFolderTest {

    @Test
    public void shouldZipLocalResources() {
        ResourceFolder uploadFolder = new ResourceFolder("upload");
        File file = uploadFolder.toZip();

        File tempDir = Files.createTempDir();
        ZipUtil.unpack(file, tempDir);

        verifyFilesInZip(tempDir,
                "upload",
                "upload/first.txt",
                "upload/directory",
                "upload/directory/second.txt",
                "upload/directory/dir",
                "upload/directory/dir/third.txt");
    }

    @Test
    public void shouldZipExternalResources_FlatFolderCase() {
        ResourceFolder uploadFolder = new ResourceFolder("flat");
        File file = uploadFolder.toZip();

        File tempDir = Files.createTempDir();
        ZipUtil.unpack(file, tempDir);

        verifyFilesInZip(tempDir,
                "flat",
                "flat/flat1.txt",
                "flat/flat2.txt",
                "flat/flat3.txt");
    }

    @Test
    public void shouldZipExternalResources_HierarchicalFolderCase() {
        ResourceFolder uploadFolder = new ResourceFolder("hierarchy");
        File file = uploadFolder.toZip();

        File tempDir = Files.createTempDir();
        ZipUtil.unpack(file, tempDir);

        verifyFilesInZip(tempDir,
                "hierarchy/level0.txt",
                "hierarchy/level1",
                "hierarchy/level1/level1.txt",
                "hierarchy/level1/level2",
                "hierarchy/level1/level2/level2.txt",
                "hierarchy/level1.1/level1.1.txt",
                "hierarchy/level1.1/level2.2",
                "hierarchy/level1.1/level2.2/level2.2.txt");
    }

    private void verifyFilesInZip(File dir, String... paths) {
        for (String path : paths) {
            Assert.assertTrue(new File(dir, path).exists(), String.format("File %s not exists in dir: %s", path, dir.getAbsolutePath()));
        }
    }
}
