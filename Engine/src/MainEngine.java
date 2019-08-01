import com.sun.deploy.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainEngine {
    public static final String m_relativePathToObjDir = ".magit\\objects";
    private static String m_currentRepository = "C:\\Users\\David\\Documents\\TestRepo";
    private static String m_currentUserName = "Administrator";
    public String userName;



    public static String scanWorkingCopy(String currentRepository1,  Map<String, List<FolderItem>> foldersMap) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        //foldersMap = new HashMap<String, List<FolderItem>>();
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

    public CommitObj commit(Map<String, List<FolderItem>> mapOfdif) throws IOException {
        CommitObj commit=null;
        Map<String, List<FolderItem>> mapOfLatestCommit= new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC=new HashMap<>();
        String latestCommitSha1;

        latestCommitSha1= EngineUtils.getLastCommitSha(m_currentRepository);
        String WCSha1= scanWorkingCopy(m_currentRepository, mapOfWC);
        if(!WCSha1.equals(latestCommitSha1)){
                commit= new CommitObj(latestCommitSha1, WCSha1);
                if(!latestCommitSha1.equals(""))
                mapOfLatestCommit= createLatestCommitMap(latestCommitSha1);
                compareWCtoCommit(mapOfWC, mapOfLatestCommit,WCSha1,latestCommitSha1,m_currentRepository,commit.deleted,commit.added,commit.changed);

        }
        for (String key : mapOfWC.keySet()) {
            if (!mapOfLatestCommit.containsKey(key)) {
                mapOfdif.put(key, mapOfWC.get(key));
            }
        }
        return commit;


    }

    //
    public static void compareWCtoCommit(Map<String, List<FolderItem>> WCmap,
                                         Map<String, List<FolderItem>> LastCommitMap,
                                         String currentWCKey,
                                         String currentCommitKey,
                                         String path,
                                         Map<String,String> deletedList, Map<String,String> addedList, Map<String,String> changedList) {
        FolderItemEquator itemsEquator = new FolderItemEquator();
        if (currentCommitKey.equals(currentWCKey))
            return;
        else {
            List<FolderItem> currentCommitFolder = LastCommitMap.get(currentCommitKey);
            List<FolderItem> currentWCFolder = WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            if(!LastCommitMap.isEmpty()) {
                List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
                deleted.stream().
                        forEach(o -> mapLeavesofPathTree(LastCommitMap, o, path, deletedList));
            }
            //added files = wcmap-commitmap
            if(!WCmap.isEmpty()) {
                List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                added.stream().
                        forEach(o -> mapLeavesofPathTree(WCmap, o, path, addedList));
            }
            //we remain with the common files. go through them
            if(!LastCommitMap.isEmpty()) {
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



    public static class FolderItemEquator implements Equator<FolderItem> {
        @Override
        public boolean equate(FolderItem t1, FolderItem t2) {
            return (t1.getItemName().equals(t2.getItemName()) && t1.getType().equals( t2.getType()));
        }

        @Override
        public int hash(FolderItem folderItem) {
            return (folderItem.getItemName()+ folderItem.getType()).hashCode();
        }

    }


    public static void mapLeavesOfPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, Map <String,String> leaves) {
        if(item.getType().equals("file"))
            leaves.put(item.getSha1(), path+"\\"+item.getItemName());

        else {
            mapOfPath.get(item.getSha1()).stream().forEach(i -> mapLeavesOfPathTree(mapOfPath, i, path + "\\" + item.getItemName(), leaves));
        }

    }

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

    public void finalizeCommit(CommitObj obj, Map<String, List<FolderItem>> mapOfdif) throws IOException {
        //zip files and create the commit file. change head branch pointer)
        String targetPath=m_currentRepository+"\\.magit\\objects\\";
        obj.changed.forEach((key,string)->EngineUtils.ZipFile(key,string,targetPath));
        obj.added.forEach((key,string)->EngineUtils.ZipFile(key,string,targetPath));
        foldersToFile(mapOfdif, targetPath);
        StringToZipFile(obj.toString(), targetPath, obj.CommitSHA1 );
        EngineUtils.overWriteFileContent(m_currentRepository+"\\.magit\\master", obj.CommitSHA1);
    }
    public void foldersToFile(Map<String, List<FolderItem>> mapOfdif ,String targetPath){

       mapOfdif.forEach((key, item)-> {
           try {

               StringToZipFile( EngineUtils.listToString(item,"\n"), targetPath, key);
           } catch (IOException e) {
               e.printStackTrace();
           }
       });

    }
    public void StringToZipFile(String content, String targetPath, String fileName ) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(content);
        File f = new File(targetPath+fileName+".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(fileName);
        out.putNextEntry(e);
        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
    }
}
