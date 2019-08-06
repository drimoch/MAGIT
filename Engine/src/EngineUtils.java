import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class EngineUtils{
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
        List<String> res;
        File headFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + "\\HEAD");
        branchName = FileUtils.readFileToString(headFile, StandardCharsets.UTF_8);
        File branchFile = FileUtils.getFile(i_currentRepository + m_relativeBranchPath + "\\" + branchName);
        commitFileSha1 = FileUtils.readFileToString(branchFile, StandardCharsets.UTF_8);

        if (commitFileSha1.equals(""))
            return "";
        res = getZippedFileLines(i_currentRepository + "\\.magit\\objects\\" + commitFileSha1 + ".zip");
        String result= res.get(0);
        return result;
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

    public static void overWriteFileContent(String path, String Content) {
        try {
            FileWriter fw = new FileWriter(path, false);
            fw.write(Content);
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


    public static void extractFile(String zipPath, String entry, String destPath)  {
        try {
            File f= new File(destPath);
            String content= String.join("",getZippedFileLines(zipPath));
            FileUtils.writeStringToFile(f,content);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createBranchFile(String destPath,String branchName) throws IOException {

        File newBranch= new File(destPath+ "\\.magit\\branches\\"+branchName);
        String head=FileUtils.readFileToString(FileUtils.getFile(destPath+ "\\.magit\\branches\\HEAD"));
        String mainBranch=  FileUtils.readFileToString(FileUtils.getFile(destPath+ "\\.magit\\branches\\" +head));
        FileUtils.writeStringToFile(newBranch,mainBranch);

    }

}


