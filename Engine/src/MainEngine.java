import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class MainEngine {
    public static final String m_relativePathToObjDir = ".magit\\objects";
    private static String m_currentRepository = "C:\\Users\\David\\Documents\\TestRepo";


    public static Map<String, List<FolderItem>> scanWorkingCopy(String currentRepository1) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        Map<String, List<FolderItem>> foldersMap = new HashMap<String, List<FolderItem>>();
        walk(dir, foldersMap, filesList);
        foldersMap.put(calculateFileSHA1(filesList), filesList);
        return foldersMap;

    }

    //TODO: Handle exceptions in walk!
    public static void walk(File dir, Map<String, List<FolderItem>> foldersMap, List<FolderItem> parentFolder) throws IOException {
        String fileContent;
        Path path;
        BasicFileAttributes attr;
        List<FolderItem> subFiles;
        FolderItem currentFolderItem;
        for (final File folderItem : dir.listFiles()) {
            if (!folderItem.getName().endsWith(".magit")) {
                path = Paths.get(folderItem.getPath());
                attr = Files.readAttributes(path, BasicFileAttributes.class);

                if (folderItem.isDirectory()) {
                    subFiles = new LinkedList<FolderItem>();
                    walk(folderItem, foldersMap, subFiles);
                    Collections.sort(subFiles, FolderItem::compareTo);
                    String key = calculateFileSHA1(subFiles);
                    foldersMap.put(key, subFiles);

                    currentFolderItem = new FolderItem(key, folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                    parentFolder.add(currentFolderItem);


                }

                if (folderItem.isFile()) {
                    fileContent = FileUtils.readFileToString(folderItem, StandardCharsets.UTF_8);
                    currentFolderItem = new FolderItem(DigestUtils.sha1Hex(fileContent), folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "file");
                    parentFolder.add(currentFolderItem);


                }

            }
        }
        return;

    }

    public static String calculateFileSHA1(List<FolderItem> folderContent) {
        String res = "";
        for (FolderItem fItem : folderContent) {
            res = res + fItem.getSha1();
        }
        return DigestUtils.sha1Hex(res);
    }

    public static void createCommit(Map<String, List<String>> sha1Map, String path) {
        //get master commit fro
    }

    public static void initRepo(String path, String name) throws IOException {
        //take care of  exceptions when using this
        if (path != null && name != null && name != "" && path != "") {
            String rootDirPath = path + "\\" + name;
            File rootDirFileObj = FileUtils.getFile(rootDirPath);
            if (!rootDirFileObj.exists()) {
                FileUtils.forceMkdir(rootDirFileObj);
                Path objectsDirPath = java.nio.file.Paths.get(rootDirPath + "\\" + ".magit\\objects");
                Files.createDirectories(objectsDirPath);
                File branchesFileObj = FileUtils.getFile(rootDirPath + "\\" + ".magit\\branches");
                FileUtils.forceMkdir(branchesFileObj);

                String headContent = "master";
                String branchesPath = branchesFileObj.getAbsolutePath();
                File headFile = new File(branchesPath + "\\HEAD");
                File masterFile = new File(branchesPath + "\\master");
                FileUtils.touch(masterFile);
                FileUtils.writeStringToFile(headFile, headContent);

                System.out.println("Create " + rootDirPath + " success. ");
            } else {
                System.out.println(rootDirPath + " exist. ");
            }
        }
    }


    //COMMIT RELATED FUNCTIONS

    public static String getCurrentRepository() {
        return m_currentRepository;
    }

    public static void setCurrentRepository(String i_currentRepository) {
        m_currentRepository = i_currentRepository;
    }

    public void commit() throws IOException {
        Map<String, List<FolderItem>> mapOfWC = scanWorkingCopy(m_currentRepository);
    }

    public void initRepository(String rootDirPath, String repoName) throws IOException {
        initRepo(rootDirPath, repoName);
    }

    public Map<String, List<FolderItem>> createLatestCommitMap(String i_rootDirSha) throws IOException {
        Map<String, List<FolderItem>> result = new HashMap<String, List<FolderItem>>();
        createCommitMapRec(i_rootDirSha, result);
        return result;
    }

    private void createCommitMapRec(String i_rootDirSha, Map<String, List<FolderItem>> i_commitMap) throws IOException {
        List<FolderItem> rootDir = EngineUtils.parseToFolderList(m_currentRepository + "\\" + m_relativePathToObjDir + "\\" + i_rootDirSha + ".zip");
        i_commitMap.put(i_rootDirSha, rootDir);
        for (FolderItem item : rootDir) {
            if (item.getType().equals("folder")) {
                createCommitMapRec(item.getSha1(), i_commitMap);
            }
        }
    }

}
