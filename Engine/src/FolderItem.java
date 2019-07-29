import java.nio.file.attribute.FileTime;

public class FolderItem implements Comparable {
    private String m_sha1;
    private String m_itemName;
    private String m_userName;
    private String m_lastModified;
    private String m_type;

    public FolderItem(String i_sha1, String i_itemName, String i_userName, String i_lastModified, String i_typr) {
        String m_sha1 = i_sha1;
        String m_itemName = i_itemName;
        String m_userName = i_userName;
        String m_lastModified = i_lastModified;
        String m_type = i_typr;
    }

    public String getItemName() {
        return m_itemName;
    }

    public String getSha1() {
        return m_sha1;
    }

    @Override
    public int compareTo(Object i_FolderItemToCompare) {
        FolderItem folderItemToCompare = (FolderItem) i_FolderItemToCompare;

        return m_itemName.compareTo(folderItemToCompare.getItemName());
    }

}
