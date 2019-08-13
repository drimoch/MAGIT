import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import jaxbClasses.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class EngineUtils {
    private static String m_relativeBranchPath = "\\.magit\\branches";

    public static List<FolderItem> parseToFolderList(String i_filePath) throws IOException {

        List<FolderItem> result = new ArrayList<>();
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
        InputStream stream;
        List<String> fileLines = new ArrayList<>();

        ZipFile zipFile = new ZipFile(i_filePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();
        stream = zipFile.getInputStream(entry);
        reader = new BufferedReader(new InputStreamReader(stream));

        while ((currentLine = reader.readLine()) != null) {
            fileLines.add(currentLine);
        }

        return fileLines;
    }

    public static String getLastCommitSha(String i_currentRepository) throws IOException {
        String branchName, commitFileSha1;
        List<String> res = new LinkedList<>();
        File headFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + "\\HEAD");
        branchName = FileUtils.readFileToString(headFile, StandardCharsets.UTF_8);
        File branchFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + "\\" + branchName);
        commitFileSha1 = FileUtils.readFileToString(branchFile, StandardCharsets.UTF_8);

        if (commitFileSha1.equals(""))
            return "";
        res = getZippedFileLines(i_currentRepository + "\\.magit\\objects\\" + commitFileSha1 + ".zip");
        return res.get(0);
    }

    public static void StringToZipFile(String content, String targetPath, String fileName) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(content);
        File f = new File(targetPath + fileName + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(fileName);
        out.putNextEntry(e);
        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
    }

    public static void overWriteFileContent(String path, String CommitSHA1) {
        try {
            FileWriter fw = new FileWriter(path, false);
            fw.write(CommitSHA1);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void ZipFile(String zipFileName, String sourceFile, String zipTarget) {

        try {
            FileOutputStream fos = new FileOutputStream(zipTarget + zipFileName + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFile);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", sourceFile);
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex);
        }

    }

    public static String listToString(List<?> list, String delimiter) {
        List<String> res = new LinkedList<>();
        list.forEach(i -> res.add(i.toString()));
        String temp = String.join(delimiter, res);
        return temp;
    }

    public static void loadBranchesFromXML(MagitRepository i_repo) {

        Stream<MagitSingleFolder> magitFoldersStream = i_repo.getMagitFolders().getMagitSingleFolder().stream();
        MagitSingleFolder[] rootFolders = (MagitSingleFolder[]) magitFoldersStream.filter(magitFolders -> magitFolders.isIsRoot()).toArray();
//TODO:follow logic of createLatestCommitMap - your current mission - create object files for all repo objects
        i_repo.getMagitBranches().getHead();//in the end
    }

    public static boolean isRepoLocationValid(File i_repoLocation) throws IOException, RepositoryAlreadyExistException, FolderIsNotEmptyException, RepoXmlNotValidException {
        if (!i_repoLocation.isDirectory()) {
            throw new RepoXmlNotValidException("location given is a file location, not directory");
        }
        if (i_repoLocation.list().length > 0)//true-> folder has files/sub directories
        {
            Stream<Path> rootDir = Files.walk(Paths.get(i_repoLocation.getAbsolutePath()));
            int magitFolder = (int) rootDir.filter(Files::isDirectory).filter(path -> path.getFileName().toString().equals(".magit")).limit(1).count();
            if (magitFolder == 1) {
                throw new RepositoryAlreadyExistException(i_repoLocation.getAbsolutePath());
            } else {
                throw new FolderIsNotEmptyException(i_repoLocation.getAbsolutePath());
            }
        }
        return true;
    }

    public static Map<String, MagitBlob> createRepoBlobsMap(MagitBlobs i_magitBlobs) throws RepoXmlNotValidException {
        Map<String, MagitBlob> result = new HashMap<>();
        String currBlobID;
        for (MagitBlob blob : i_magitBlobs.getMagitBlob()) {
            currBlobID = blob.getId();
            if (result.containsKey(currBlobID)) {
                throw new RepoXmlNotValidException(String.format("Xml has two or more blobs with the same id \nid: {}", currBlobID));
            } else {
                result.put(currBlobID, blob);
            }
        }
        return result;

    }

    public static void createRepoFoldersMaps(MagitFolders i_magitFolders, Map<String, MagitSingleFolder> i_Folders) throws RepoXmlNotValidException {
        String currFolderID;
        for (MagitSingleFolder folder : i_magitFolders.getMagitSingleFolder()) {
            currFolderID = folder.getId();
            if (i_Folders.containsKey(currFolderID)) {
                throw new RepoXmlNotValidException(String.format("Xml has two or more folders with the same id \nid: {}", currFolderID));
            } else {
                i_Folders.put(currFolderID, folder);
            }
        }
    }

    public static Map<String, MagitSingleCommit> createRepoCommitMap(MagitCommits i_magitCommits) throws RepoXmlNotValidException {
        Map<String, MagitSingleCommit> result = new HashMap<>();
        String currCommitID;
        for (MagitSingleCommit commit : i_magitCommits.getMagitSingleCommit()) {
            currCommitID = commit.getId();
            if (result.containsKey(currCommitID)) {
                throw new RepoXmlNotValidException(String.format("Xml has two or more commits with the same id \nid: {}", currCommitID));
            } else {
                result.put(currCommitID, commit);
            }
        }
        return result;
    }


}


