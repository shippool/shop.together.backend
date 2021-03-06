package io.interface21.shop2gether;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.ameba.http.AbstractBase;

import java.io.Serializable;
import java.util.List;

/**
 * An ItemVO is the View Object that represents an Item.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextNoteVO.class, name = "tnote")
})
public abstract class ItemVO extends AbstractBase implements Serializable {

    private String persistentKey;
    public boolean shareable;
    public long version;
    public long lastModified;

    @JsonIgnore
    public List<UserGroupVO> sharedWith;

    public String getPersistentKey() {
        return persistentKey;
    }

    public List<UserGroupVO> getSharedWith() {
        return sharedWith;
    }

    @Override
    public String toString() {
        return "ItemVO{" +
                "persistentKey=" + persistentKey +
                ", shareable=" + shareable +
                ", sharedWith=" + sharedWith +
                "} " + super.toString();
    }
}
