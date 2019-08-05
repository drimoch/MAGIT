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
    private final String m_startMenuText;
    private final String m_mainMenuText;
    private String m_currentRepository="C:\\tester";
    private static String m_currentUserName = "Administrator";
    public MainEngine engine;

    public UImain() {
        engine = new MainEngine();
        m_startMenuText = String.format(
                "What would you like to do?\n" +
                        "1. Update user name\n" +
                        "2. Load repository from XML\n" +
                        "3. Exit ");
        m_mainMenuText = String.format("3.  Switch repository \n" +
                "1.  List Last commit content\n" +
                "2.  Show working copy's status\n" +
                "3.  Commit\n" +
                "4.  List all branches\n" +
                "5.  Create new branch\n" +
                "6.  Delete branch\n" +
                "7. Checkout\n" +
                "8. Show active branch's commit history\n" +
                "9. Exit");
    }


    public void run() throws IOException {
//        String userName;
//        System.out.println(String.format("Hello %s", engine.getUserName()));
//        Scanner scanner = new Scanner(System.in);
//        int userChoice = printMenu(m_startMenuText, m_numOfChoicesStartMenu);
//        while (userChoice != m_numOfChoicesStartMenu) {
//            switch (userChoice) {
//                case 1:
//                    System.out.println("Enter new user name: ");
//                    userName = scanner.nextLine();
//                    engine.setUserName(userName);
//                    userChoice = printMenu(m_startMenuText, m_numOfChoicesStartMenu);
//                    break;
//                case 2:
//                    //here we will write code that loads a repo
//                    //and at the end of it, the main menu will be displayed
//                    int mainMenuChoice = printMenu(m_mainMenuText, m_numOfChoicesMainMenu);
//                    while (mainMenuChoice >= 1 && mainMenuChoice <= m_numOfChoicesMainMenu) {
//                        switch (mainMenuChoice) {
//                            case 1:
//                                break;
//                            case 9:
//                                System.exit(0);
//                                break;
//                            default:
//                                if (mainMenuChoice != m_numOfChoicesMainMenu) {
//                                    mainMenuChoice = printMenu(m_mainMenuText, m_numOfChoicesMainMenu);
//                                }
//                        }
//                    }
//
//                case 3:
//                    System.exit(0);
//                    break;
//            }
//        }


       // initRepository();
       commit();
        createBranch();
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
            engine.finalizeCommit(commitObject,mapOfdif, this.m_currentRepository);


        } else System.out.println("Commit canceled, all changes discarded");
        return;
    }
    public void commit() {
        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject=new CommitObj();
        try {

            if (!engine.checkForChanges(mapOfdif, commitObject,this.m_currentRepository )) {
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

    public void saveOpenChanges(){

        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject=null;
        try {

            if (engine.checkForChanges(mapOfdif, commitObject, this.m_currentRepository )) {
                System.out.println("System has detected the following unsaved changes: ");
                displayChanges(commitObject);
                System.out.println("Would you like to save these changes before performing the branch operation?\n" +
                        "Unsaved changes will be deleted permenantly! \n" + "press (y)es to save, (n)o to continue without saving");
                validateCommit(commitObject,mapOfdif);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    public void checkOut(){

        Scanner scanner= new Scanner(System.in);
        System.out.println("Please specify the branch name you would like to switch to");
        String branchName = scanner.nextLine();
        while(!FileUtils.getFile(this.m_currentRepository+"\\.magit\\branches\\"+ branchName).exists()&& branchName.equals(27)) {
        System.out.format("There is no branch by the name %S",branchName, ". Specify a different branch or press ESC to cancel operation");
        }
        if (branchName.equals(27)){
        System.out.println("Operation canceled");
            return;
        }
        else
        saveOpenChanges();
        engine.switchHeadBranch(branchName, m_currentRepository);



    }
    public String createBranch() throws IOException {

        Scanner scanner= new Scanner(System.in);
        String relativePath= this.m_currentRepository+"\\.magit\\branches\\";
        File head= new File(relativePath+"HEAD");
        File currentHead= new File(relativePath+FileUtils.readFileToString(head, StandardCharsets.UTF_8));

        System.out.println("Please enter the new branch name");
        String branchName = scanner.nextLine();
        while(FileUtils.getFile(relativePath+branchName).exists()){
                System.out.println("Branch exists, please enter a different name");
                branchName= scanner.nextLine();
        }

        EngineUtils.createBranchFile(m_currentRepository, branchName);


    return branchName;

    }


    public void displayChanges(CommitObj obj) {
        Map<String, String> deleted= obj.deleted,  added= obj.added, changed= obj.changed;
        //TODO move logic to engine, send back just the message!
        String deletedMessage, addedMessage, changedMessage;
        deletedMessage = (deleted.isEmpty() ? "No files were deleted\n" : "Files deleted from directory:\n");
        changedMessage = (changed.isEmpty() ? "No files were changed\n" : "Files changed in directory:\n");
        addedMessage = (added.isEmpty() ? "No files were added\n" : "Files added to directory:\n");
        List <String>d= deleted.values().stream().collect(Collectors.toList());
        List <String>a= added.values().stream().collect(Collectors.toList());
        List <String>c= changed.values().stream().collect(Collectors.toList());


        System.out.println(deletedMessage + String.join("\n",d));
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

    public int printMenu(String i_menuText, int i_numOfOptions) {
        String userChoice;
        Scanner scanner = new Scanner(System.in);

        System.out.println(i_menuText);
        userChoice = scanner.nextLine();
        return inputValidation(userChoice, i_numOfOptions);


//        string numOfPlayersStr = Console.ReadLine();
//        while (!menuInputValidation(numOfPlayersStr, "1", "2"))
//        {
//            Console.WriteLine("Invalid input. Please type 1 or 2:");
//            numOfPlayersStr = Console.ReadLine();
//        }
    }


    private int inputValidation(String i_choice, int i_numOfChoices) {
        Scanner scanner = new Scanner(System.in);
        int userChoiceInt = 0;
        String message = String.format("Invalid input, enter a number between 1-%d", i_numOfChoices);

        while (userChoiceInt < 1 || userChoiceInt > i_numOfChoices) {
            while (!tryParseInt(i_choice)) {
                System.out.println(message);
                i_choice = scanner.nextLine();
            }
            userChoiceInt = Integer.parseInt(i_choice);

            if (userChoiceInt < 1 || userChoiceInt > i_numOfChoices) {
                System.out.println(message);
                i_choice = scanner.nextLine();
            }
        }
        return userChoiceInt;

    }

    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}