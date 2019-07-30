import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EngineUtils {
    private static String m_relativeBranchPath = "\\.magit\\branches";

    public static List<FolderItem> parseToFolderList(String i_filePath) throws IOException {

        List<FolderItem> result = new ArrayList<FolderItem>();
        String[] itemDetails;
        List<String> folderItems = getZippedFileLines(i_filePath);
        for (String line : folderItems) {
            itemDetails = line.split(",");
            result.add(new FolderItem(itemDetails));
        }
        return result;
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

    public static String getLastCommitSha(String i_currentRepository) throws IOException {
        String branchName, rootDirSha;
        String[] branchFilelines;
        File headFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + "\\HEAD");
        branchName = FileUtils.readFileToString(headFile, StandardCharsets.UTF_8);
        File branchFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + branchName);
        rootDirSha = FileUtils.readFileToString(headFile, StandardCharsets.UTF_8);
        return rootDirSha;
    }
}
