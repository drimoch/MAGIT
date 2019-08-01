import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommitObj {
    private String m_submitterName;
    String dateCreated;
    String commitMessage;
    String PreviousCommit;
    String CommitSHA1;
    public Map<String, String> added;
    public Map<String, String> changed;
    public Map<String, String> deleted;

    public String getUserName() {
        return m_submitterName;
    }

    public void setUserName(String i_userNAme) {
        m_submitterName = i_userNAme;
    }

    CommitObj(String o_PreviousCommit, String o_CommitSHA1) {
        PreviousCommit = o_PreviousCommit;
        CommitSHA1 = o_CommitSHA1;
        deleted = new HashMap<String, String>();
        changed = new HashMap<String, String>();
        added = new HashMap<String, String>();
        DateFormat dateFormat = new SimpleDateFormat("dd.mm.yyyy-hh:mm:ss:sss");
        Date date = new Date();
        dateCreated = dateFormat.format(date);
    }

    public void setCommitMessage(String message) {
        this.commitMessage = message;
    }


    @Override
    public String toString() {
        Field[] fields = this.getClass().getDeclaredFields();

        return m_submitterName + "," + dateCreated + "," + commitMessage + "," + PreviousCommit + "," + CommitSHA1;
    }

}
