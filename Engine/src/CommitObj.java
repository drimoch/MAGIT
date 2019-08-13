import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommitObj {
    public Map<String, String> added;
    public Map<String, String> changed;
    public Map<String, String> deleted;
    String rootDirSha1;
    String PreviousCommit;
    private String m_submitterName;
    String dateCreated;
    String commitMessage;

    public Map<String, String> added;
    public Map<String, String> changed;
    public Map<String, String> deleted;

    public String getUserName() {
        return m_submitterName;
    }

    public void setUserName(String i_userNAme) {
        m_submitterName = i_userNAme;
    }


    CommitObj() {

        deleted = new HashMap<>();
        changed = new HashMap<>();
        added = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("dd.mm.yyyy-hh:mm:ss:sss");
        Date date = new Date();
        dateCreated = dateFormat.format(date);
    }
    public void setPreviousCommit(String commitSHA1){
        PreviousCommit=commitSHA1;
    }
    public void setCommitSHA1(String commitSHA1){
        rootDirSha1=commitSHA1;
    }


    CommitObj(String i_PreviousCommitSha, String i_rootSHA1, String i_dateCreated, String i_commitMessage, String i_submitterName) {
        PreviousCommit = i_PreviousCommitSha;
        rootDirSha1 = i_rootSHA1;
        deleted = new HashMap<>();
        changed = new HashMap<>();
        added = new HashMap<>();

        dateCreated = i_dateCreated;
        commitMessage = i_commitMessage;
        m_submitterName = i_submitterName;
    }

    public String getUserName() {
        return m_submitterName;
    }

    public void setUserName(String i_userNAme) {
        m_submitterName = i_userNAme;
    }


    public void setCommitMessage(String message) {
        this.commitMessage = message;
    }

    @Override
    public String toString() {
        return rootDirSha1 + "\n" + PreviousCommit +"\n" + m_submitterName + "\n" + dateCreated + "\n" + commitMessage+ "\ncommit" ;


    }

}
