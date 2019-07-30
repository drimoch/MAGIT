import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EngineUtils {
    public static List<FolderItem> parseToFolderList(String i_filePath) throws IOException {


//        String destination = "some/destination/folder";
//
//            ZipFile zipFile = new ZipFile(i_filePath);
//            zipFile.extractAll(destination);
        List<FolderItem> result = new ArrayList<FolderItem>();
        String[] itemDetails;
        List<String> folderitems = getZippedFileLines(i_filePath);
        for (String line : folderitems) {
            itemDetails = line.split(",");

            result.add(new FolderItem(itemDetails));

        }
        File rootDir = FileUtils.getFile(i_filePath);
        String rootDirContent;
        rootDirContent = FileUtils.readFileToString(rootDir, StandardCharsets.UTF_8);
        return null;
    }

    public static List<String> getZippedFileLines(String i_filePath) throws IOException {
        BufferedReader reader;
        String currentLine;
        List<String> fileLines = new ArrayList<>();
        ZipFile zipFile = new ZipFile(i_filePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();
        InputStream stream = zipFile.getInputStream(entry);
        reader = new BufferedReader(new InputStreamReader(stream));

        while ((currentLine = reader.readLine()) != null) {
            fileLines.add(currentLine);
        }
        return fileLines;
    }
}
