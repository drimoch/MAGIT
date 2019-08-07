import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class UImain {

    private final int m_numOfChoicesStartMenu = 3;
    private final int m_numOfChoicesMainMenu = 9;
   //private final String m_startMenuText;
    private String m_currentRepository = "";
    private static String m_currentUserName = "Administrator";
    public MainEngine engine;




        public void run () throws IOException {
        boolean validCommand= false;
       System.out.println(String.format("Hello %s", m_currentUserName));
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
                            "[11] DISPLAY ACTIVE BRANCH NAME\n" +
                            "[12] EXIT\n");
            System.out.println(menu);
            int userInput = tryParseint(scanner.next());

       while (userInput!=12) {

           if(userInput < 0 ||userInput>12 )
               System.out.println("Invalid input, please enter a number between 0 and 12");
           else if(userInput>2 && userInput<12 ){
              if( validateCommand())
           switch (userInput) {

               case 1:
                   setUserName();
               case 2:
                   //loadRepo();
               case 3:
                   setCurrentRepository();
               case 4:
                   displayHeadDetails();//check repo exists
               case 5:
                   showWCstatus();//check repo exists
               case 6:
                   commit();//check repo exists
               case 7:
                   listAllBranches();//check repo exists
               case 8:
                   createBranch();//check repo exists
               case 9:
                   deleteBranch();//check repo exists
               case 10:
                   checkOut();
               case 11:
                   displayActiveBranch();

               default:
                   System.out.println(menu);

           } else System.out.println("Please specify a valid directory path (command number 3)");
           }

           userInput=tryParseint(scanner.next());

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
    }
    public void displayActiveBranch(){


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
            commitObject.setUserName(engine.userName);
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

        Scanner scanner = new Scanner(System.in);
        System.out.println("Please specify the branch name you would like to switch to");
        String branchName = scanner.nextLine();
        while (!FileUtils.getFile(this.m_currentRepository + "\\.magit\\branches\\" + branchName).exists() && !branchName.equals(27)) {
            System.out.format("There is no branch by the name %s. Specify a different branch or press ESC to cancel operation", branchName);
            branchName = scanner.nextLine();
        }
        if (branchName.equals(27)) {
            System.out.println("Operation canceled");
            return;
        } else
            saveOpenChanges();
        engine.switchHeadBranch(branchName, m_currentRepository);


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


        return branchName;

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
        return (!FileUtils.getFile(m_currentRepository+"\\.magit").exists());
    }
    public void displayHeadDetails() throws IOException {

            List <String> lst= engine.displayLastCommitDetails(m_currentRepository);
            lst.forEach(i-> System.out.println(i+"\n"));
    }

    public void listAllBranches() throws IOException {
           List <String> res= engine.listAllBranches(m_currentRepository);
             res.forEach(i-> System.out.println(i+"\n"));

    }


}