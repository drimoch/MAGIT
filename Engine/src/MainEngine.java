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
    private String currentRepository = "C:\\Users\\David\\Documents\\TestRepo";

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
        FolderItem currentFolderItem;
        for (final File f : dir.listFiles()) {
            if (!f.getName().endsWith(".magit")) {
                path = Paths.get(f.getPath());
                attr = Files.readAttributes(path, BasicFileAttributes.class);

                if (f.isDirectory()) {
                    List<FolderItem> subFiles = new LinkedList<FolderItem>();
                    walk(f, foldersMap, subFiles);
                    Collections.sort(subFiles, FolderItem::compareTo);
                    String key = calculateFileSHA1(subFiles);
                    foldersMap.put(key, subFiles);

                    currentFolderItem = new FolderItem(key, f.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                    parentFolder.add(currentFolderItem);


                }

                if (f.isFile()) {
                    fileContent = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                    currentFolderItem = new FolderItem(DigestUtils.sha1Hex(fileContent), f.getName(), "user name", attr.lastModifiedTime().toString(),"file");
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


    //COMMIT RELATED FUNCTIONS

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

    public void commit() throws IOException {
        Map<String, List<FolderItem>> mapOfWC = scanWorkingCopy(currentRepository);
    }

    public void initRepository(String rootDirPath, String repoName) throws IOException {
        initRepo(rootDirPath, repoName);
    }

//    public Map<String, List<String>> createLatestCommitMap(String rootDirSha) {
//        List
//    }

}
