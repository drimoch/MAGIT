import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UImain {


    public MainEngine engine;

    public UImain() {
        engine = new MainEngine();

    }


    public void run() throws IOException {

       // initRepository();
        commit();
    }

    public void commit() {


        Map<String, List<FolderItem>> mapOfdif = new HashMap<>();
        CommitObj commitObject;
        String doNext, WCsha1;
        Scanner scanner = new Scanner(System.in);
        try {

            commitObject = engine.commit(mapOfdif);
            if (commitObject==null) {
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
                    engine.finalizeCommit(commitObject,mapOfdif);


                } else System.out.println("Commit canceled");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();


    }


    public void displayChanges(Map<String, String> deleted, Map<String, String> added, Map<String, String> changed) {
        //TODO move logic to engine, send back just the message!
        String deletedMessage, addedMessage, changedMessage;
        deletedMessage = (deleted.isEmpty() ? "No files were deleted" : "Files deleted from directory:\n");
        changedMessage = (changed.isEmpty() ? "No files were changed" : "Files changed in directory:\n");
        addedMessage = (deleted.isEmpty() ? "No files were added" : "Files added to directory:\n");
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

}