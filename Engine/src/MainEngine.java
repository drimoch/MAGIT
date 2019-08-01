import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.collections4.CollectionUtils;

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


    public static void testerFunction(String WCpath, String CommitPath) {
        try {
            String WCsha1 = null, commitsha1 = null;
            // Map<String, List<FolderItem>> foldersMap = new HashMap<String, List<FolderItem>>();

            Map<String, List<FolderItem>> WCmap = new HashMap<>();
            WCsha1 = scanWorkingCopy(WCpath, WCmap);
            Map<String, List<FolderItem>> commitmap = new HashMap<>();
            commitsha1 = scanWorkingCopy(CommitPath, commitmap);
            List<String> deletedList = new LinkedList<>();
            List<String> addedList = new LinkedList<>();
            List<String> changedList = new LinkedList<>();
            compareWCtoCommit(WCmap,
                    commitmap,
                    WCsha1,
                    commitsha1,
                    WCpath,
                    deletedList, addedList, changedList);
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static String scanWorkingCopy(String currentRepository1, Map<String, List<FolderItem>> foldersMap) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        foldersMap = new HashMap<String, List<FolderItem>>();
        walk(dir, foldersMap, filesList);
        String rootSha1 = calculateFileSHA1(filesList);
        foldersMap.put(rootSha1, filesList);
        return rootSha1;

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

    //
    public static void compareWCtoCommit(Map<String, List<FolderItem>> WCmap,
                                         Map<String, List<FolderItem>> LastCommitMap,
                                         String currentWCKey,
                                         String currentCommitKey,
                                         String path,
                                         List<String> deletedList, List<String> addedList, List<String> changedList) {
        FolderItemEquator itemsEquator = new FolderItemEquator();
        if (currentCommitKey.equals(currentWCKey))
            return;
        else {
            List<FolderItem> currentCommitFolder = LastCommitMap.get(currentCommitKey);
            List<FolderItem> currentWCFolder = WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
            deleted.stream().
                    forEach(o -> mapLeavesofPathTree(LastCommitMap, o, path, deletedList));

            //added files = wcmap-commitmap
            List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
            added.stream().
                    forEach(o -> mapLeavesofPathTree(WCmap, o, path, addedList));
            //we remain with the common files. go through them
            List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(LastCommitMap.get(currentCommitKey), WCmap.get(currentWCKey), itemsEquator);
            for (FolderItem item : changed) {
                if (item.getType().equals("folder")) {
                    Optional<FolderItem> alteredCopy = WCmap.get(currentWCKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals("folder")).findFirst();
                    compareWCtoCommit(WCmap,
                            LastCommitMap,
                            alteredCopy.get().getSha1(),
                            item.getSha1(),
                            path + "\\" + item.getItemName(),
                            deletedList, addedList, changedList);

                } else changedList.add(path + "\\" + item.getItemName());
            }
        }

    }

    public static void mapLeavesofPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, List<String> leaves) {
        if (item.getType().equals("file"))
            leaves.add(path + "\\" + item.getItemName());

        else {
            mapOfPath.get(item.getSha1()).stream().forEach(i -> mapLeavesofPathTree(mapOfPath, i, path + "\\" + item.getItemName(), leaves));
        }

    }

    public void commit() throws IOException {
        Map<String, List<FolderItem>> mapOfWC;
        String latestCommitSha = EngineUtils.getLastCommitSha(m_currentRepository);
        if (latestCommitSha.isEmpty() || latestCommitSha == null) {//first commit

        } else {//not first commit

        }
        //    mapOfWC = scanWorkingCopy(m_currentRepository);
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

    public static class FolderItemEquator implements Equator<FolderItem> {
        @Override
        public boolean equate(FolderItem t1, FolderItem t2) {
            return (t1.getItemName().equals(t2.getItemName()) && t1.getType().equals(t2.getType()));
        }

        @Override
        public int hash(FolderItem folderItem) {
            return (folderItem.getItemName() + folderItem.getType()).hashCode();
        }

    }

}
