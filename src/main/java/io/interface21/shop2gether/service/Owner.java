package io.interface21.shop2gether.service;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import io.interface21.shop2gether.Coordinate;
import io.interface21.shop2gether.ItemVO;
import io.interface21.shop2gether.OwnerVO;
import io.interface21.shop2gether.VerificationVO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.ameba.exception.NotFoundException;
import org.ameba.integration.jpa.ApplicationEntity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import static io.interface21.shop2gether.service.Owner.COLUMN_ACTIVE;
import static io.interface21.shop2gether.service.Owner.COLUMN_EMAIL;
import static io.interface21.shop2gether.service.Owner.COLUMN_USERNAME;

/**
 * An Owner is the actual Owner of {@link Item Items} and is the user of the system.
 *
 * @author <a href="mailto:scherrer@openwms.org">Heiko Scherrer</a>
 */
@Getter
@ToString(exclude = "password")
@EqualsAndHashCode
@AllArgsConstructor
@Table(name = Owner.TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_UNAME_ACTIVE", columnNames = {COLUMN_USERNAME, COLUMN_ACTIVE}),
                @UniqueConstraint(name = "UC_EMAIL_ACTIVE", columnNames = {COLUMN_EMAIL, COLUMN_ACTIVE})
        })
@Entity
public class Owner extends ApplicationEntity {

    public static final String TABLE_NAME = "T_OWNER";
    public static final String COLUMN_USERNAME = "C_USERNAME";
    public static final String COLUMN_PASSWORD = "C_PASSWORD";
    public static final String COLUMN_PHONE = "C_PHONE";
    public static final String COLUMN_EMAIL = "C_EMAIL";
    public static final String COLUMN_ACTIVE = "C_ACTIVE";

    @NotNull
    @Column(name = COLUMN_USERNAME, nullable = false)
    private String username;
    @Column(name = COLUMN_PASSWORD)
    private String password;
    @Column(name = COLUMN_PHONE)
    private String phonenumber;
    @Column(name = COLUMN_EMAIL)
    private String email;
    @Column(name = COLUMN_ACTIVE)
    private boolean active = true;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "longitude", column = @Column(name = "C_HOME_LONG")),
            @AttributeOverride(name = "latitude", column = @Column(name = "C_HOME_LATI")),
            @AttributeOverride(name = "longitudeDelta", column = @Column(name = "C_HOME_LONG_D")),
            @AttributeOverride(name = "latitudeDelta", column = @Column(name = "C_HOME_LATI_D"))
    })
    private Coordinate home;

    /**
     * Homeposition internally used for querying.
     */
    @Column(name = "C_HOME_POS", length = 2048)
    private Point homePosition;
    @Column(name = "C_CODE")
    private String verificationCode;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "C_CODE_SENT")
    private Date verificationCodeSent;

    @OrderBy("lastModifiedDt desc")
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(name = "T_OWNER_ITEM", joinColumns = {@JoinColumn(name = "C_OWNER_PK")}, inverseJoinColumns = @JoinColumn(name = "C_ITEM_PK"))
    private Set<Item> items = new HashSet<>();

    private LinkedList<Coordinate> interestedArea = new LinkedList<>();

    /**
     * Dear JPA ...
     */
    protected Owner() {
    }

    public Owner(String username) {
        this.username = username;
    }


    public Owner(String username, String email, Coordinate homePosition) {
        this.username = username;
        this.email = email;
        this.home = homePosition;
    }

    private Owner(Builder builder) {
        this.username = builder.username;
        this.phonenumber = builder.phonenumber;
        this.email = builder.email;
        this.home = builder.home;
        this.verificationCode = builder.verificationCode;
        if (this.verificationCode != null) {
            this.verificationCodeSent = new Date();
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Set<Item> getItems() {
        return items;
    }

    public Optional<Item> getItem(Long persistentKey) {
        return items.stream().filter(i -> i.getpKey().equals(persistentKey)).findFirst();
    }

    public boolean addItem(Item item) {
        return items.add(item);
    }

    public void updateItem(Item toSave) {
        items.stream().filter(i -> i.getpKey().equals(toSave.getpKey())).findFirst().orElseThrow(NotFoundException::new).copyFrom(toSave);
    }

    void verificationSent(VerificationVO verification) {
        this.verificationCode = verification.getCode();
        this.phonenumber = verification.getPhonenumber();
        this.verificationCodeSent = new Date();
    }

    @PrePersist
    @PreUpdate
    protected void onPostConstruct() {
        if (home != null) {
            this.homePosition = new GeometryFactory().createPoint(new com.vividsolutions.jts.geom.Coordinate(home.getLongitude(), home.getLatitude()));
        }
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    /**
     * Checks if the verification code has not expired and matches the sent one.
     *
     * @param verification Holds the code to compare
     * @return This
     * @throws IllegalArgumentException if doesn't match or expired
     */
    Owner throwIfInvalid(VerificationVO verification) {
        if (!verification.codeEquals(this.verificationCode)) {
            throw new IllegalArgumentException("Verificationcode does not match the previously sent one");
        }
        if (verification.hasExpired(this.verificationCodeSent)) {
            throw new IllegalArgumentException("Verificationcode expired");
        }
        return this;
    }

    public <T extends ItemVO> Owner copyFrom(OwnerVO<T> toSave) {
        this.username = toSave.username;
        this.phonenumber = toSave.phonenumber;
        this.home = toSave.home;
        return this;
    }


    /**
     * {@code Owner} builder static inner class.
     */
    public static final class Builder {
        private String username;
        private String phonenumber;
        private String email;
        private Coordinate home;
        private String verificationCode;

        private Builder() {
        }

        /**
         * Sets the {@code username} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code username} to set
         * @return a reference to this Builder
         */
        public Builder withUsername(String val) {
            this.username = val;
            return this;
        }

        /**
         * Sets the {@code phonenumber} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code phonenumber} to set
         * @return a reference to this Builder
         */
        public Builder withPhonenumber(String val) {
            this.phonenumber = val;
            return this;
        }

        /**
         * Sets the {@code email} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code email} to set
         * @return a reference to this Builder
         */
        public Builder withEmail(String val) {
            this.email = val;
            return this;
        }

        /**
         * Sets the {@code home} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code home} to set
         * @return a reference to this Builder
         */
        public Builder withHome(Coordinate val) {
            this.home = val;
            return this;
        }

        /**
         * Sets the {@code verificationCode} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code verificationCode} to set
         * @return a reference to this Builder
         */
        public Builder withVerificationCode(String val) {
            this.verificationCode = val;
            return this;
        }

        /**
         * Returns a {@code Owner} built from the parameters previously set.
         *
         * @return a {@code Owner} built with parameters of this {@code Owner.Builder}
         */
        public Owner build() {
            return new Owner(this);
        }
    }
}
