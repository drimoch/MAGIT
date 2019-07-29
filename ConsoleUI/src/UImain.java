import java.io.IOException;
import java.util.Scanner;

public class UImain {


    public MainEngine engine;

    public UImain() {
        engine = new MainEngine();

    }


    public void run() throws IOException {
        initRepository();
        engine.commit();
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