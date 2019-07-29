
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MainEngine {
    private String currentRepository= "C:\\Users\\David\\Documents\\TestRepo";




    //COMMIT RELATED FUNCTIONS

    public void commit() throws IOException {
        Map <String,List<String>> mapOfWC= Commit.scanWorkingCopy(currentRepository);
    }
    public void initRepository(String rootDirPath, String repoName) throws IOException {
        Repo.initRepo(rootDirPath, repoName);
    }

}
