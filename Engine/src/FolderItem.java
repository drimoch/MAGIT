import com.sun.org.apache.xpath.internal.operations.Equals;

import java.nio.file.attribute.FileTime;

public class FolderItem implements Comparable {
    private String m_sha1;
    private String m_itemName;
    private String m_userName;
    private String m_lastModified;
    private String m_type;

    public FolderItem(String i_sha1, String i_itemName, String i_userName, String i_lastModified, String i_typr) {
        m_sha1 = i_sha1;
        m_itemName = i_itemName;
        m_userName = i_userName;
        m_lastModified = i_lastModified;
        m_type = i_typr;
    }

    public FolderItem(String[] i_itemDetails) {
        m_sha1 = i_itemDetails[0];
        m_itemName = i_itemDetails[1];
        m_userName = i_itemDetails[2];
        m_lastModified = i_itemDetails[3];
        m_type = i_itemDetails[4];
    }

    public String getItemName() {
        return m_itemName;
    }

    public String getSha1() {
        return m_sha1;
    }
    public String getType(){
        return m_type;
    }


    @Override
    public int compareTo(Object i_FolderItemToCompare) {
        FolderItem folderItemToCompare = (FolderItem) i_FolderItemToCompare;

        return m_itemName.compareTo(folderItemToCompare.getItemName());
    }


}
