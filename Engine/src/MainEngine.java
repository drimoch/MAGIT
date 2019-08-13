import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import jaxbClasses.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class MainEngine {
    public static final String m_relativePathToObjDir = ".magit\\objects";
    private static String m_currentRepository = "C:\\tester";
    private static String m_currentUserName = "Administrator";
    public String userName;
    private Map<String, CommitObj> m_repoCommitObjects;
    private Map<String, MagitSingleCommit> m_repoCommits;
    private Map<String, MagitBlob> m_repoBlobs;
    private Map<String, MagitSingleFolder> m_repoFolders;

    public static String scanWorkingCopy(String currentRepository1, Map<String, List<FolderItem>> foldersMap) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        walk(dir, foldersMap, filesList);
        String rootSha1 = calculateFileSHA1(filesList);
        foldersMap.put(rootSha1, filesList);
        return rootSha1;

    }


    public static String calculateFileSHA1(List<FolderItem> folderContent) {
        String res = "";
        for (FolderItem fItem : folderContent) {
            res = res + fItem.getSha1();
        }
        return DigestUtils.sha1Hex(res);
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

    public static String getUserName() {
        return m_currentUserName;
    }

    public static void setUserName(String i_userName) {
        m_currentUserName = i_userName;
    }

    public static String getCurrentRepository() {
        return m_currentRepository;
    }

    public static void setCurrentRepository(String i_currentRepository) {
        m_currentRepository = i_currentRepository;
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

    public static void compareWCtoCommit(Map<String, List<FolderItem>> WCmap,
                                         Map<String, List<FolderItem>> LastCommitMap,
                                         String currentWCKey,
                                         String currentCommitKey,
                                         String path,
                                         Map<String, String> deletedList, Map<String, String> addedList, Map<String, String> changedList) {
        FolderItemEquator itemsEquator = new FolderItemEquator();
        if (currentCommitKey.equals(currentWCKey))
            return;
        else {
            List<FolderItem> currentCommitFolder = LastCommitMap.get(currentCommitKey);
            List<FolderItem> currentWCFolder = WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
                deleted.stream().
                        forEach(o -> mapLeavesOfPathTree(LastCommitMap, o, path, deletedList));
            }
            //added files = wcmap-commitmap
            if (!WCmap.isEmpty()) {
                List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                added.stream().
                        forEach(o -> mapLeavesOfPathTree(WCmap, o, path, addedList));
            }
            //we remain with the common files. go through them
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                for (FolderItem item : changed) {
                    if (item.getType().equals("folder")) {
                        Optional<FolderItem> alteredCopy = WCmap.get(currentWCKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals("folder")).findFirst();
                        compareWCtoCommit(WCmap,
                                LastCommitMap,
                                alteredCopy.get().getSha1(),
                                item.getSha1(),
                                path + "\\" + item.getItemName(),
                                deletedList, addedList, changedList);

                    } else changedList.put(item.getSha1(), path + "\\" + item.getItemName());
                }
            }
        }

    }

    public static void mapLeavesOfPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, Map<String, String> leaves) {
        if (item.getType().equals("file"))
            leaves.put(item.getSha1(), path + "\\" + item.getItemName());

        else {
            mapOfPath.get(item.getSha1()).stream().forEach(i -> mapLeavesOfPathTree(mapOfPath, i, path + "\\" + item.getItemName(), leaves));
        }

    }


    public Map<String, List<FolderItem>> loadRepoFromXML(MagitRepository i_repoToParse) throws RepositoryAlreadyExistException, IOException, FolderIsNotEmptyException, RepoXmlNotValidException {
        String repoLocation = i_repoToParse.getLocation();
        File repoLocationFile = FileUtils.getFile(repoLocation);
        Map<String, List<FolderItem>> firstCommitMap = new HashMap<>();
        if (EngineUtils.isRepoLocationValid(repoLocationFile)) {
            m_repoBlobs = EngineUtils.createRepoBlobsMap(i_repoToParse.getMagitBlobs());
            m_repoFolders = new HashMap<>();
            EngineUtils.createRepoFoldersMaps(i_repoToParse.getMagitFolders(), m_repoFolders);
            m_repoCommits = EngineUtils.createRepoCommitMap(i_repoToParse.getMagitCommits());

            List<MagitSingleCommit> firstCommitList = i_repoToParse.getMagitCommits().getMagitSingleCommit().stream()
                    .filter(magitSingleCommit -> magitSingleCommit.getPrecedingCommits() == null)
                    .collect(Collectors.toList());
            if (firstCommitList.size() > 1) {
                throw new RepoXmlNotValidException("more than one commit with no preceding commit");
            }
            List<FolderItem> rootItems = new LinkedList<>();
            MagitSingleCommit firstCommit = firstCommitList.get(0);
            m_repoCommitObjects=new HashMap<>();
            magitCommitToMap(firstCommitMap, rootItems, firstCommit);

            //loop on all commit->produce map->send map to mapToObjects function->create commitObj and put it in the m_repoCommitObjects
         //   i_repoToParse.getMagitCommits().getMagitSingleCommit().stream().filter(magitSingleCommit -> magitSingleCommit.getPrecedingCommits().getPrecedingCommit().)
System.out.println();
        }
        return firstCommitMap;
    }

    private void magitCommitToMap(Map<String, List<FolderItem>> firstCommitMap, List<FolderItem> rootItems, MagitSingleCommit i_magitSingleCommit) throws RepoXmlNotValidException {
        String rootFolderID = i_magitSingleCommit.getRootFolder().getId();
        if (isRootDirValid(rootFolderID)) {
            magitSingleCommitToMap(rootFolderID, firstCommitMap, rootItems);
        }
        Collections.sort(rootItems, FolderItem::compareTo);
        FolderItem currFolder = new FolderItem(m_repoFolders.get(rootFolderID), rootItems);
        firstCommitMap.put(currFolder.getSha1(), rootItems);
        CommitObj firstCommitObj = new CommitObj(null, currFolder.getSha1(), i_magitSingleCommit.getDateOfCreation(), i_magitSingleCommit.getMessage(), i_magitSingleCommit.getAuthor());
        if (m_repoCommitObjects.containsKey(i_magitSingleCommit.getId())) {
            throw new RepoXmlNotValidException(String.format("Illegal XML: two commits or more have the same ID\ncommit id:{}  ", i_magitSingleCommit.getId()));
        }
        m_repoCommitObjects.put(i_magitSingleCommit.getId(), firstCommitObj);
    }

    public void magitSingleCommitToMap(String i_parentFolderID, Map<String, List<FolderItem>> i_result, List<FolderItem> i_parentFolderItems) throws RepoXmlNotValidException {

        MagitSingleFolder rootFolder = m_repoFolders.get(i_parentFolderID);
        List<Item> folderItems = rootFolder.getItems().getItem();
        for (Item folderItem : folderItems) {
            String itemType = folderItem.getType();
            String itemID = folderItem.getId();
            if (itemType.equals("blob")) {
                if (!m_repoBlobs.containsKey(itemID)) {

                    throw new RepoXmlNotValidException(
                            String.format("Folder contain blob with id that does not exist\nfolder id:{}", i_parentFolderID));
                }
                FolderItem currFile = new FolderItem(m_repoBlobs.get(itemID));
                i_parentFolderItems.add(currFile);

            } else if (itemType.equals("folder")) {
                if (itemID.equals(i_parentFolderID)) {
                    throw new RepoXmlNotValidException(
                            String.format("Illegal XML: Folder contains a sub folder with the same id\n folder id:{}", i_parentFolderID));
                }
                if (!m_repoFolders.containsKey(itemID)) {
                    throw new RepoXmlNotValidException(
                            String.format("Folder contain folder with id that does not exist\nfolder id:{}", i_parentFolderID));
                }
                List<FolderItem> currFolderItems = new LinkedList<>();
                magitSingleCommitToMap(itemID, i_result, currFolderItems);
                Collections.sort(currFolderItems, FolderItem::compareTo);
                FolderItem currFolder = new FolderItem(m_repoFolders.get(itemID), currFolderItems);
                i_parentFolderItems.add(currFolder);
                i_result.put(currFolder.getSha1(), currFolderItems);

            }
        }

    }

    private boolean isRootDirValid(String i_rootFolderID) throws RepoXmlNotValidException {
        MagitSingleFolder folderToCheck = m_repoFolders.get(i_rootFolderID);
        if (folderToCheck == null) {
            throw new RepoXmlNotValidException("some commit includes a directory that does not exist");
        }
        if (!folderToCheck.isIsRoot()) {
            throw new RepoXmlNotValidException("some commit main folder is not a root folder");
        }
        return true;
    }

    public CommitObj commit(Map<String, List<FolderItem>> mapOfdif) throws IOException {
        CommitObj commit = null;
        Map<String, List<FolderItem>> mapOfLatestCommit = new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC = new HashMap<>();
        String latestCommitSha1;

        latestCommitSha1 = EngineUtils.getLastCommitSha(m_currentRepository);
        String WCSha1 = scanWorkingCopy(m_currentRepository, mapOfWC);
        if (!WCSha1.equals(latestCommitSha1)) {
            commit = new CommitObj(latestCommitSha1, WCSha1);
            if (!latestCommitSha1.equals(""))
                mapOfLatestCommit = createLatestCommitMap(latestCommitSha1);
            compareWCtoCommit(mapOfWC, mapOfLatestCommit, WCSha1, latestCommitSha1, m_currentRepository, commit.deleted, commit.added, commit.changed);

        }
        for (String key : mapOfWC.keySet()) {
            if (!mapOfLatestCommit.containsKey(key)) {
                mapOfdif.put(key, mapOfWC.get(key));
            }
        }
        return commit;


    }

    public void initRepository(String rootDirPath, String repoName) throws IOException {
        initRepo(rootDirPath, repoName);
    }

    public Map<String, List<FolderItem>> createLatestCommitMap(String i_rootDirSha1) throws IOException {
        Map<String, List<FolderItem>> result = new HashMap<String, List<FolderItem>>();
        createCommitMapRec(i_rootDirSha1, result);
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

    public void finalizeCommit(CommitObj obj, Map<String, List<FolderItem>> mapOfdif) throws IOException {
        //zip files and create the commit file. change head branch pointer)
        String targetPath = m_currentRepository + "\\.magit\\objects\\";
        obj.changed.forEach((key, string) -> EngineUtils.ZipFile(key, string, targetPath));
        obj.added.forEach((key, string) -> EngineUtils.ZipFile(key, string, targetPath));
        foldersToFile(mapOfdif, targetPath);

        String newCommitContent = obj.toString();
        String newCommitSha1 = DigestUtils.sha1Hex(newCommitContent);

        EngineUtils.StringToZipFile(newCommitContent, targetPath, newCommitSha1);
        EngineUtils.overWriteFileContent(m_currentRepository + "\\.magit\\branches\\master", newCommitSha1);
    }

    public void foldersToFile(Map<String, List<FolderItem>> mapOfdif, String targetPath) {

        mapOfdif.forEach((key, item) -> {
            try {

                EngineUtils.StringToZipFile(EngineUtils.listToString(item, "\n"), targetPath, key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }
    //checkout related functions:
    // map the commit and parse it into WC

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
