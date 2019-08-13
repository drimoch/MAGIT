import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class UImain {

    private String m_currentRepository = "";
    private static String m_currentUserName = "Administrator";
    public MainEngine engine;


    public void run() throws IOException {
        engine= new MainEngine();
        Scanner scanner = new Scanner(System.in);
        String menu = String.format(
                "[1]  SET USER NAME\n" +
                        "[2]  LOAD REPOSITORY FROM XML\n" +
                        "[3]  SWITCH REPOSITORY\n" +
                        "[4]  LIST RECENT COMMIT\n" +
                        "[5]  SHOW WC STATUS\n" +
                        "[6]  COMMIT\n" +
                        "[7]  LIST ALL BRANCHES\n" +
                        "[8]  CREATE NEW BRANCH\n" +
                        "[9]  DELETE BRANCH\n" +
                        "[10] CHECKOUT\n" +
                        "[11] DISPLAY ACTIVE BRANCH\n" +
                        "[12] REBASE BRANCH\n" +
                        "[13] CHANGE DIRECTORY PATH\n" +
                        "[14] EXIT\n");
        System.out.println(menu);
        int userInput = tryParseint(scanner.next());

        while (userInput != 14) {

            if (userInput < 0 || userInput > 14)
                System.out.println("Invalid input, please enter a number between 0 and 12");
            else if ((userInput > 3 && userInput < 14 && validateCommand()) || userInput <= 3) {
                switch (userInput) {

                    case 1:
                        setUserName();
                        break;
                    case 2:
                        //loadRepo();
                        break;
                    case 3:
                        setCurrentRepository();
                        break;
                    case 4:
                        displayHeadDetails();
                        break;
                    case 5:
                        showWCstatus();
                        break;
                    case 6:
                        commit();
                        break;
                    case 7:
                        listAllBranches();
                        break;
                    case 8:
                        createBranch();//implemented bonus
                        break;
                    case 9:
                        deleteBranch();
                        break;
                    case 10:
                        checkOut();
                        break;
                    case 11:
                        displayActiveBranch();
                        break;
                    case 12:
                        resetHeadBranch();//bonus
                        break;
                    case 13:
                        changeDirPath();//bonus
                        break;
                }
            }
            else System.out.println("Please specify a valid directory path (command number 3)");

            userInput = tryParseint(scanner.next());

        }


    }




    public void setUserName(){
            Scanner scanner= new Scanner(System.in);
            System.out.println("Please enter your name");
            m_currentUserName= scanner.nextLine();
    }
    public void setCurrentRepository(){
            Scanner scan= new Scanner(System.in);
            System.out.println("Please enter the full repository path");
            String repo= scan.nextLine();
           if(FileUtils.getFile(repo+"\\.magit").exists())
            m_currentRepository=repo;
           else
               System.out.println("Directory does not exist");
           return;
    }
    public void displayActiveBranch() throws IOException {
        List<String> res=  engine.displayLatestCommitHistory(m_currentRepository);
        res.forEach(i-> System.out.println(String.format(i, " Name of creator:", " Date created", "Commit message" )));

    }
    public void  deleteBranch() {
        Scanner scanner= new Scanner(System.in);
        System.out.println("Plese enter a branch name you'd like to delete");
        String branch= scanner.nextLine();
        File branchFile= FileUtils.getFile(m_currentRepository+"\\.magit\\branches\\"+branch);
        String master=EngineUtils.readFileToString(m_currentRepository+"\\.magit\\branches\\HEAD");
        if (!branchFile.exists())
            System.out.println("No such branch exists");
        else if(master.equals(branch))
            System.out.println("Head branch cannot be deleted");
        else branchFile.delete();


    }

    public void validateCommit(CommitObj commitObject, Map<String, List<FolderItem>> mapOfdif) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String doNext = scanner.nextLine();
        while (!doNext.equalsIgnoreCase("y") && !doNext.equalsIgnoreCase("n")) {
            System.out.println("No such command, please enter y or n");
            doNext = scanner.nextLine();
        }
        if (doNext.equalsIgnoreCase("y")) {
            System.out.println("Give a short description of the new commit");
            commitObject.setCommitMessage(scanner.nextLine());
            commitObject.setUserName(m_currentUserName);
            engine.finalizeCommit(commitObject, mapOfdif, this.m_currentRepository);


        } else System.out.println("Commit canceled, all changes discarded");
        return;
    }

    public void commit() {
        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject = new CommitObj();
        try {

            if (!engine.checkForChanges(mapOfdif, commitObject, this.m_currentRepository)) {
                System.out.println("No changes detected, nothing to commit");
                return;
            } else {
                displayChanges(commitObject);
                System.out.println("Submit commit changes? press (y)es to commit (n)o to cancel operation ");
                validateCommit(commitObject, mapOfdif);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void showCurrentCommitDetails(){

    }
    public void showWCstatus() throws IOException {
        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject = new CommitObj();
        System.out.println("The current repository path is:\n"+
                m_currentRepository);
        System.out.println("Current user:"+m_currentUserName+"\n");
        engine.checkForChanges(mapOfdif,commitObject,m_currentRepository);
        displayChanges(commitObject);


    }
    public void saveOpenChanges() {

        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject = new CommitObj();
        try {

            if (engine.checkForChanges(mapOfdif, commitObject, this.m_currentRepository)) {
                System.out.println("System has detected the following unsaved changes: ");
                displayChanges(commitObject);
                System.out.println("Would you like to save these changes before performing the branch operation?\n" +
                        "Unsaved changes will be deleted permenantly! \n" + "press (y)es to save, (n)o to continue without saving");
                validateCommit(commitObject, mapOfdif);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public void checkOut() {

        String branch= getBranchName();
        if(!branch.equals(null)){
            saveOpenChanges();
            engine.switchHeadBranch(branch, m_currentRepository);
        }


    }
    public String getBranchName(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please specify the branch name you would like to switch to");
        String branchName = scanner.nextLine();
        while (!FileUtils.getFile(this.m_currentRepository + "\\.magit\\branches\\" + branchName).exists()) {
            System.out.format("There is no branch by the name %s. Specify a different branch or press ESC to cancel operation", branchName);
            branchName = scanner.nextLine();
        }
        if (branchName.equals(27)) {
            System.out.println("Operation canceled");
            return null;
        } else
        return branchName;
    }

    public String createBranch() throws IOException {

        Scanner scanner = new Scanner(System.in);
        String relativePath = this.m_currentRepository + "\\.magit\\branches\\";
        File head = new File(relativePath + "HEAD");
        File currentHead = new File(relativePath + FileUtils.readFileToString(head, StandardCharsets.UTF_8));

        System.out.println("Please enter the new branch name");
        String branchName = scanner.nextLine();
        while (FileUtils.getFile(relativePath + branchName).exists()) {
            System.out.println("Branch exists, please enter a different name");
            branchName = scanner.nextLine();
        }

        EngineUtils.createBranchFile(m_currentRepository, branchName);
        //BONUS
        System.out.println("Would you like to switch to the new branch? Enter (y)es to switch, (n)o otherwise ");
        String input=scanner.nextLine();
        while(!input.equalsIgnoreCase("y")&& !input.equalsIgnoreCase("n")){
            System.out.println("Please enter a valid input");
            input=scanner.nextLine();
        }
        if (input.equalsIgnoreCase("y")) {
            Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
            CommitObj commitObject = new CommitObj();
            if (!engine.checkForChanges(mapOfdif, commitObject, this.m_currentRepository))//if there are no changes
                engine.switchHeadBranch(branchName, m_currentRepository);
            else
                System.out.println("The system has detected unsaved changes. Please commit them before switching to a different branch");
        }


        return branchName;

    }
    public void resetHeadBranch() throws IOException {
        Scanner scanner= new Scanner(System.in);
        System.out.println("Enter Sha1");
        String Sha1= scanner.nextLine();
        if(EngineUtils.isACommit(Sha1,m_currentRepository)) {
            saveOpenChanges();
            String head=EngineUtils.readFileToString(m_currentRepository+"\\.magit\\branches\\HEAD");
//            String newSha1= EngineUtils.readFileToString(m_currentRepository+"\\.magit\\branches\\"+branch);
            EngineUtils.overWriteFileContent(m_currentRepository+"\\.magit\\branches\\"+head,Sha1);
            displayHeadDetails();
        }
    }


    public void displayChanges(CommitObj obj) {
        Map<String, String> deleted = obj.deleted, added = obj.added, changed = obj.changed;
        //TODO move logic to engine, send back just the message!
        String deletedMessage, addedMessage, changedMessage;
        deletedMessage = (deleted.isEmpty() ? "No files were deleted\n" : "Files deleted from directory:\n");
        changedMessage = (changed.isEmpty() ? "No files were changed\n" : "Files changed in directory:\n");
        addedMessage = (added.isEmpty() ? "No files were added\n" : "Files added to directory:\n");
        List<String> d = deleted.values().stream().collect(Collectors.toList());
        List<String> a = added.values().stream().collect(Collectors.toList());
        List<String> c = changed.values().stream().collect(Collectors.toList());


        System.out.println(deletedMessage + String.join("\n", d));
        System.out.println(changedMessage + String.join("\n", c));
        System.out.println(addedMessage + String.join("\n", a));

    }

    public void initRepository() {
        try {
            String repoName, rootDirPath;
            Scanner scanner = new Scanner(System.in);

            System.out.println("Enter Repository name: ");
            repoName = scanner.nextLine();
            System.out.println("Enter root directory path: ");
            rootDirPath = scanner.nextLine();
            engine.initRepository(rootDirPath, repoName);
        } catch (Exception e) {
        }
    }
    public int tryParseint(String num){
            try{
                int res=  Integer.parseInt(num);
                return res;
            }
            catch(Exception NumberFormatException){
                return -1;
        }
    }
    public boolean validateCommand(){
        return (FileUtils.getFile(m_currentRepository+"\\.magit").exists());
    }
    public void displayHeadDetails() throws IOException {

            List <String> lst= engine.displayLastCommitDetails(m_currentRepository);
            lst.forEach(i-> System.out.println(i+"\n"));
    }

    public void listAllBranches() throws IOException {
           List <String> res= engine.listAllBranches(m_currentRepository);
             res.forEach(i-> System.out.println(String.format(i," Root directory SHA1:", " Parent branch SHA1:", " Created by:", "Date submitted:", "Commit message:")));

    }
    public void changeDirPath(){
        String sourcePath, destPath;
        Scanner scanner= new Scanner(System.in);
        System.out.println(String.format("Please enter the file/directory source path (relative to %s)",m_currentRepository));
        sourcePath= scanner.nextLine();
        while(!FileUtils.getFile(m_currentRepository+"\\"+sourcePath).exists() && sourcePath.contains(".magit")&& !sourcePath.equals(27)){
            System.out.println("Invalid input. Please specify a valid input or press ESC to cancel ");
            sourcePath=scanner.nextLine();
        }
        if(sourcePath.equals(27))
            return;
        System.out.println(String.format("Please enter the file/directory destination path (relative to %s)" +
                "to rename a file/ directory " +
                "source path: example_folder/current_name " +
                "destination path: example_folder/new_name",m_currentRepository));
        destPath= scanner.nextLine();

        try
        {
            if(sourcePath.trim().length()>0 && destPath!=null && destPath.trim().length()>0 && !destPath.contains(".magit"))
            {
                if (FileUtils.getFile(m_currentRepository+ "\\" + sourcePath).isFile()) {
                    FileUtils.moveFile(
                            FileUtils.getFile(m_currentRepository + "\\" + sourcePath+ ".txt"),
                            FileUtils.getFile(m_currentRepository + "\\" + destPath+".txt"));
                }
                else {
                    FileUtils.moveDirectory(
                            FileUtils.getFile(m_currentRepository + "\\" + sourcePath),
                            FileUtils.getFile(m_currentRepository + "\\" + destPath));
                }
                System.out.println("Succesfully transfered " + sourcePath + " to " + destPath);
            }
            else {
                System.out.println("Invalid destination path");
            }
        }catch(FileNotFoundException ex)
        {
            System.out.println("given destination path does not exist");
        }catch(FileAlreadyExistsException e){
            System.out.println("A file/folder by this name already exists");
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }


}