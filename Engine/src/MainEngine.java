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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainEngine {
    private String currentRepository = "C:\\Users\\David\\Documents\\TestRepo";

    public static void testerFunction(String WCpath, String CommitPath){
        try {
            String WCsha1=null, commitsha1=null;
            // Map<String, List<FolderItem>> foldersMap = new HashMap<String, List<FolderItem>>();

            Map<String, List<FolderItem>>  WCmap= new HashMap<>();
            WCsha1= scanWorkingCopy(WCpath,WCmap);
            Map<String, List<FolderItem>>  commitmap= new HashMap<>();
            commitsha1= scanWorkingCopy(CommitPath,commitmap);
            List<String> deletedList= new LinkedList<>();
            List<String> addedList= new LinkedList<>();
            List<String> changedList= new LinkedList<>();
            compareWCtoCommit(WCmap,
                   commitmap,
                  WCsha1,
                  commitsha1,
                     WCpath,
                     deletedList,  addedList, changedList);
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();



        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static String scanWorkingCopy(String currentRepository1,  Map<String, List<FolderItem>> foldersMap) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
       // Map<String, List<FolderItem>> foldersMap = new HashMap<String, List<FolderItem>>();
        walk(dir, foldersMap, filesList);
        String rootSha1=calculateFileSHA1(filesList);
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
        for (final File f : dir.listFiles()) {
            if (!f.getName().endsWith(".magit")) {
                path = Paths.get(f.getPath());
                attr = Files.readAttributes(path, BasicFileAttributes.class);

                if (f.isDirectory()) {
                    subFiles = new LinkedList<FolderItem>();
                    walk(f, foldersMap, subFiles);
                    Collections.sort(subFiles, FolderItem::compareTo);
                    String key = calculateFileSHA1(subFiles);
                    foldersMap.put(key, subFiles);

                    currentFolderItem = new FolderItem(key, f.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                    parentFolder.add(currentFolderItem);


                }

                if (f.isFile()) {
                    fileContent = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                    currentFolderItem = new FolderItem(DigestUtils.sha1Hex(fileContent), f.getName(), "user name", attr.lastModifiedTime().toString(), "file");
                    parentFolder.add(currentFolderItem);
                    foldersMap.put(currentFolderItem.getSha1(), null);


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
            List<FolderItem>  currentCommitFolder=LastCommitMap.get(currentCommitKey);
            List<FolderItem>  currentWCFolder= WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
                    deleted.stream().
                            forEach(o -> mapLeavesofPathTree(LastCommitMap, o,path,deletedList));

            //added files = wcmap-commitmap
            List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                    added.stream().
                            forEach(o -> mapLeavesofPathTree(WCmap, o,path,addedList));
            //we remain with the common files. go through them
            List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(LastCommitMap.get(currentCommitKey), WCmap.get(currentWCKey),itemsEquator);
            for (FolderItem item : changed) {
                if (item.getType().equals("folder")) {
                    Optional<FolderItem> alteredCopy = WCmap.get(currentWCKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals ("folder")).findFirst();
                    compareWCtoCommit(WCmap,
                            LastCommitMap,
                            alteredCopy.get().getSha1(),
                            item.getSha1(),
                            path + "\\" + item.getItemName(),
                            deletedList, addedList, changedList);

                }
                else changedList.add(path+"\\"+item.getItemName());
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


    public static void mapLeavesofPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, List <String> leaves) {
        if(item.getType().equals("file"))
            leaves.add(path+"\\"+item.getItemName());

        else{
            mapOfPath.get(item.getSha1()).stream().forEach(i->mapLeavesofPathTree(mapOfPath,i, path+"\\"+item.getItemName(),leaves));
        }

    }

    public void initRepository(String rootDirPath, String repoName) throws IOException {
        initRepo(rootDirPath, repoName);
    }

//    public Map<String, List<String>> createLatestCommitMap(String rootDirSha) {
//        List
//    }

}
