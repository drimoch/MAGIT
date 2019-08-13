import Exceptions.RepositoryAlreadyExistException;
import jaxbClasses.MagitRepository;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.Collectors;

public class UImain {

    private final int m_numOfChoicesStartMenu = 3;
    private final int m_numOfChoicesMainMenu = 9;
    private final String m_startMenuText;
    private final String m_mainMenuText;
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


        // initRepository();
        // commit();
        loadXML();

    }

    public void showMenu() {
        String userName;
        System.out.println(String.format("Hello %s", engine.getUserName()));
        Scanner scanner = new Scanner(System.in);
        int userChoice = printMenu(m_startMenuText, m_numOfChoicesStartMenu);
        while (userChoice != m_numOfChoicesStartMenu) {
            switch (userChoice) {
                case 1:
                    System.out.println("Enter new user name: ");
                    userName = scanner.nextLine();
                    engine.setUserName(userName);
                    userChoice = printMenu(m_startMenuText, m_numOfChoicesStartMenu);
                    break;
                case 2:
                    //here we will write code that loads a repo
                    //and at the end of it, the main menu will be displayed
                    int mainMenuChoice = printMenu(m_mainMenuText, m_numOfChoicesMainMenu);
                    while (mainMenuChoice >= 1 && mainMenuChoice <= m_numOfChoicesMainMenu) {
                        switch (mainMenuChoice) {
                            case 1:
                                break;
                            case 9:
                                System.exit(0);
                                break;
                            default:
                                if (mainMenuChoice != m_numOfChoicesMainMenu) {
                                    mainMenuChoice = printMenu(m_mainMenuText, m_numOfChoicesMainMenu);
                                }
                        }
                    }

                case 3:
                    System.exit(0);
                    break;
            }
        }
    }

    public void commit() {


        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject;
        String doNext, WCsha1;
        Scanner scanner = new Scanner(System.in);
        try {

            commitObject = engine.commit(mapOfdif);
            if (commitObject == null) {
                System.out.println("No changes detected, nothing to commit");
                return;
            } else {
                displayChanges(commitObject.deleted, commitObject.added, commitObject.changed);
                System.out.println("Commit changes? press (y)es to commit (n)o to cancel operation ");
                doNext = scanner.nextLine();
                while (!doNext.equalsIgnoreCase("y") && !doNext.equalsIgnoreCase("n")) {
                    System.out.println("No such command, please enter y or n");
                    doNext = scanner.nextLine();
                }
                if (doNext.equalsIgnoreCase("y")) {
                    System.out.println("Give a short description of the current commit");
                    commitObject.setCommitMessage(scanner.nextLine());
                    commitObject.setUserName(engine.userName);
                    engine.finalizeCommit(commitObject, mapOfdif);


                } else System.out.println("Commit canceled");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();


    }

    public void loadXML() {
        try {
            MagitRepository m = JAXBHandler.loadXML("C:\\Users\\David\\Downloads\\ex1-small.xml");

            engine.loadRepoFromXML(m);
        } catch (NoSuchFileException e) {
            // here we'll notify the user that location tag in XML does not exist
        } catch (RepositoryAlreadyExistException e) {
            //here we'll tell the user there is already a repository in that location, what does he wants to do?
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void displayChanges
            (Map<String, String> deleted, Map<String, String> added, Map<String, String> changed) {
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