import java.io.IOException;

public class Program {
    public static void main(String[] args) {

        UImain consoleUI = new UImain();
        try {
            consoleUI.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
