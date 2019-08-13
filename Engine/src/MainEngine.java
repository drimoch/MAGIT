import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import jaxbClasses.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.collections4.CollectionUtils;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


public class MainEngine {
    public static final String m_relativePathToObjDir = ".magit\\objects";
    private static String m_currentRepository = "C:\\tester";
    private static String m_currentUserName = "Administrator";
    public String userName;
    private Map<String, CommitObj> m_repoCommitObjects;
    private Map<String, MagitSingleCommit> m_repoCommits;
    private Map<String, MagitBlob> m_repoBlobs;
    private Map<String, MagitSingleFolder> m_repoFolders;


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
                    if(folderItem.list().length==0)
                        folderItem.delete();
                    else {
                        subFiles = new LinkedList<FolderItem>();
                        walk(folderItem, foldersMap, subFiles);
                        Collections.sort(subFiles, FolderItem::compareTo);
                        String key = calculateFileSHA1(subFiles);
                        foldersMap.put(key, subFiles);

                        currentFolderItem = new FolderItem(key, folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                        parentFolder.add(currentFolderItem);
                    }

                }

                if (folderItem.isFile()) {
                    fileContent = EngineUtils.readFileToString(folderItem.getPath());
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
                Files.write(Paths.get(headFile.getPath()), headContent.getBytes());

                System.out.println("Create " + rootDirPath + " success. ");
            } else {
                System.out.println(rootDirPath + " exist. ");
            }
        }
    }


    //COMMIT RELATED FUNCTIONS


public boolean checkForChanges(Map<String, List<FolderItem>> mapOfdif, CommitObj commit, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> mapOfLatestCommit= new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC=new HashMap<>();
        String latestCommitRoot;
        String latestCommitSha1 =EngineUtils.readFileToString( currentRepo+"\\.magit\\branches\\"+  EngineUtils.readFileToString(currentRepo + "\\.magit\\branches\\HEAD"));

        latestCommitRoot= EngineUtils.getLastCommitRoot(currentRepo);
        String WCSha1= scanWorkingCopy(currentRepo, mapOfWC);
        if(!WCSha1.equals(latestCommitRoot)){
            commit.setCommitSHA1(WCSha1);
                commit.setPreviousCommit(latestCommitSha1);
                if(!latestCommitSha1.equals(""))
                mapOfLatestCommit= createLatestCommitMap(latestCommitRoot,currentRepo);
                compareWCtoCommit(mapOfWC, mapOfLatestCommit,WCSha1,latestCommitRoot,currentRepo,commit.deleted,commit.added,commit.changed);

            for (String key : mapOfWC.keySet()) {
                if (!mapOfLatestCommit.containsKey(key)) {
                    mapOfdif.put(key, mapOfWC.get(key));
                }
            }
            return true;
        }
        else
        return false;

    }

    

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
                        forEach(o -> mapLeavesOfPathTree(LastCommitMap, o, path, deletedList));
            }
            //added files = wcmap-commitmap
            if(!WCmap.isEmpty()) {
                List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                added.stream().
                        forEach(o -> mapLeavesOfPathTree(WCmap, o, path, addedList));
            }
            //we remain with the common files. go through them and compare
            if(!LastCommitMap.isEmpty()) {
                List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(WCmap.get(currentWCKey),LastCommitMap.get(currentCommitKey), itemsEquator);
                for (FolderItem item : changed) {
                    Optional<FolderItem> alteredCopy = LastCommitMap.get(currentCommitKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals(item.getType())).findFirst();
                    if (item.getType().equals("folder")) {
                        compareWCtoCommit(WCmap,
                                LastCommitMap,
                                item.getSha1(),
                                alteredCopy.get().getSha1(),
                                path + "\\" + item.getItemName(),
                                deletedList, addedList, changedList);

                    } else if(!alteredCopy.get().getSha1().equals(item.getSha1()))
                        changedList.put(item.getSha1(), path + "\\" + item.getItemName());
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
            
          (firstCommitMap, rootItems, firstCommit);

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

 public List<String> displayLastCommitDetails(String currRepo) throws IOException {
        Map<String, List<FolderItem>> result;
        String rootSha1=EngineUtils.getLastCommitRoot(currRepo);
        result= createLatestCommitMap(rootSha1, currRepo);
        List<String> objects= new LinkedList<>();
        stringifyRepo(result,currRepo,rootSha1,objects);
        return objects;


    }
     public void stringifyRepo( Map<String, List<FolderItem>> map, String repo, String root, List<String> objects){
        List <FolderItem> lst= map.get(root);
        for(FolderItem i: lst){
            objects.add(i.getDetails()+"\n " +
                    "path:"+ repo+"\\"+i.getItemName());
            if(i.getType().equals("folder"))
                stringifyRepo(map, repo+"\\"+i.getItemName(), i.getSha1(),objects);
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

  

    public void initRepository(String rootDirPath, String repoName) throws IOException {
        initRepo(rootDirPath, repoName);
    }

public Map<String, List<FolderItem>> createLatestCommitMap(String i_rootDirSha, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> result = new HashMap<String, List<FolderItem>>();
        createCommitMapRec(i_rootDirSha, result, currentRepo);
        return result;
    }

    private void createCommitMapRec(String i_rootDirSha, Map<String, List<FolderItem>> i_commitMap, String currentRepo) throws IOException {
        List<FolderItem> rootDir = EngineUtils.parseToFolderList(currentRepo+ "\\" + m_relativePathToObjDir + "\\" + i_rootDirSha + ".zip");
        i_commitMap.put(i_rootDirSha, rootDir);
        for (FolderItem item : rootDir) {
            if (item.getType().equals("folder")) {
                createCommitMapRec(item.getSha1(), i_commitMap, currentRepo);
            }
        }
    }

    public List<String> listAllBranches(String currentRepo) throws IOException {
        File branches= FileUtils.getFile(currentRepo+ "\\.magit\\branches");
        String sha1;
        List <String> branchesList= new LinkedList<>();
        for(File i: branches.listFiles())
        {
            if(!i.getPath().equals(currentRepo+ "\\.magit\\branches\\HEAD")) {
                sha1= EngineUtils.readFileToString(i.getPath());
                branchesList.add(i.getName()+"\n %s"+
                        EngineUtils.listToString(EngineUtils.getZippedFileLines(currentRepo+"\\.magit\\objects\\"+sha1+".zip")," %s"));

            }

        }
        return branchesList;
    }

    public void mapActiveBranch(){

    }

   public void finalizeCommit(CommitObj obj, Map<String, List<FolderItem>> mapOfdif, String currentRepo) throws IOException {
        String targetPath=currentRepo+"\\.magit\\objects\\";
        obj.changed.forEach((key,string)->EngineUtils.ZipFile(key,string,targetPath));
        obj.added.forEach((key,string)->EngineUtils.ZipFile(key,string,targetPath));
        foldersToFile(mapOfdif, targetPath);

        String newCommitContent=obj.toString();
        String newCommitSha1=DigestUtils.sha1Hex(newCommitContent);

        EngineUtils.StringToZipFile(newCommitContent, targetPath, newCommitSha1 );
        String currentHead=EngineUtils.readFileToString(currentRepo+"\\.magit\\branches\\HEAD");
        EngineUtils.overWriteFileContent(currentRepo+"\\.magit\\branches\\"+currentHead, newCommitSha1);
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
 public List<String> displayLatestCommitHistory(String currRepo) throws IOException {
        String curCommit=EngineUtils.readFileToString( currRepo+"\\.magit\\branches\\"+  EngineUtils.readFileToString(currRepo + "\\.magit\\branches\\HEAD"));
        List <String> res= new LinkedList<>();
        List <String> commitContent;
        while(!curCommit.equals("")){
            commitContent=EngineUtils.getZippedFileLines(currRepo+"\\.magit\\objects\\"+ curCommit+".zip");
            res.add("Commit SHA1:" + curCommit+ "%s "+ EngineUtils.listToString(commitContent.subList(2,5)," %s "));
            curCommit=commitContent.get(1);
        }
        return res;

    }
    public void switchHeadBranch(String branchName, String currentRepository){

        String branchFile= currentRepository+"\\.magit\\branches\\"+ branchName;

        try {
            String skip=currentRepository+"\\.magit";
            String Commitsha1= EngineUtils.readFileToString(branchFile), rootsha1;
            for(File i: FileUtils.getFile(currentRepository).listFiles()){
                if(!i.getPath().contains(skip))
                    FileUtils.deleteQuietly(i);
            }

            EngineUtils.overWriteFileContent(currentRepository+"\\.magit\\branches\\HEAD", branchName);
            rootsha1= EngineUtils.getLastCommitRoot(currentRepository);
            Map<String, List<FolderItem>> mapOfCommit= createLatestCommitMap(rootsha1,currentRepository);
            parseMapToWC(mapOfCommit,rootsha1,currentRepository+"\\.magit\\objects\\",currentRepository);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public void parseMapToWC(Map <String,List< FolderItem>> dirMap, String dirRootSHA1, String sourcePath, String destPath){
        String newDestPath;
        List <FolderItem> items= dirMap.get(dirRootSHA1);
       for( FolderItem i: items){
           if (i.getType().equals("file")){

                EngineUtils.extractFile(sourcePath+i.getSha1()+".zip", i.getSha1(),destPath+"\\"+i.getItemName());
           }
           else{
               File folder = new File(destPath+"\\"+i.getItemName());
               folder.mkdir();
               parseMapToWC(dirMap, i.getSha1(),sourcePath, destPath+"\\"+i.getItemName());
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
