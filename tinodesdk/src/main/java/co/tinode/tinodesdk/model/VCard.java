package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

public class VCard implements Serializable {

    public final static String TYPE_HOME = "HOME";
    public final static String TYPE_WORK = "WORK";
    public final static String TYPE_MOBILE = "MOBILE";
    public final static String TYPE_PERSONAL = "PERSONAL";
    public final static String TYPE_BUSINESS = "BUSINESS";
    public final static String TYPE_OTHER = "OTHER";
    // Full name
    public String fn;
    public Name n;
    public String org;
    public String title;
    // List of phone numbers associated with the contact
    public Contact[] tel;
    // List of contact's email addresses
    public Contact[] email;
    public Contact[] impp;
    // Avatar photo. Java does not have a useful bitmap class, so keeping it as bits here.

    public Photo photo;

    public VCard() {
    }

    public VCard(String fullName, byte[] avatar) {
        this.fn = fullName;
        this.photo = new Photo(avatar);
    }

    private static String typeToString(ContactType tp) {
        if (tp == null) {
            return null;
        }

        switch (tp) {
            case HOME:
                return TYPE_HOME;
            case WORK:
                return TYPE_WORK;
            case MOBILE:
                return TYPE_MOBILE;
            case PERSONAL:
                return TYPE_PERSONAL;
            case BUSINESS:
                return TYPE_BUSINESS;
            default:
                return TYPE_OTHER;
        }
    }

    private static ContactType stringToType(String str) {
        if (str == null) {
            return null;
        }

        switch (str) {
            case TYPE_HOME:
                return ContactType.HOME;
            case TYPE_WORK:
                return ContactType.WORK;
            case TYPE_MOBILE:
                return ContactType.MOBILE;
            case TYPE_PERSONAL:
                return ContactType.PERSONAL;
            case TYPE_BUSINESS:
                return ContactType.BUSINESS;
            default:
                return ContactType.OTHER;
        }
    }

    @JsonIgnore
    public byte[] getPhotoBits() {
        return photo == null ? null : photo.data;
    }
    @JsonIgnore
    public void setPhotoBits(byte[] bits) {
        photo = new Photo(bits);
    }

    public void addPhone(String phone, ContactType type) {
        addPhone(phone, typeToString(type));
    }

    public void addPhone(String phone, String type) {
        tel = Contact.append(tel, new Contact(type, phone));
    }

    public void addEmail(String addr, String type) {
        email = Contact.append(email, new Contact(type, addr));
    }

    @JsonIgnore
    public String getPhoneByType(String type) {
        String phone = null;
        if (tel != null) {
            for (Contact tt : tel) {
                if (tt.type != null && tt.type.equals(type)) {
                    phone = tt.uri;
                    break;
                }
            }
        }
        return phone;
    }

    @JsonIgnore
    public String getPhoneByType(ContactType type) {
        return getPhoneByType(typeToString(type));
    }

    public enum ContactType {HOME, WORK, MOBILE, PERSONAL, BUSINESS, OTHER}

    public static <T extends VCard> T copy(T dst, VCard src) {
        dst.fn = src.fn;
        dst.n = src.n != null ? src.n.copy() : null;
        dst.org = src.org;
        dst.title = src.title;
        dst.tel = Contact.copyArray(src.tel);
        dst.email = Contact.copyArray(src.email);
        dst.impp = Contact.copyArray(src.impp);
        // Shallow copy of the photo
        dst.photo = src.photo != null ? src.photo.copy() : null;

        return dst;
    }

    public VCard copy() {
        return copy(new VCard(), this);
    }

    public static class Name implements Serializable {
        public String surname;
        public String given;
        public String additional;
        public String prefix;
        public String suffix;

        public Name copy() {
            Name dst = new Name();
            dst.surname = surname;
            dst.given = given;
            dst.additional = additional;
            dst.prefix = prefix;
            dst.suffix = suffix;
            return dst;
        }
    }

    public static class Contact implements Serializable, Comparable<Contact> {
        public String type;
        public String uri;

        private ContactType tp;

        public Contact(String type, String uri) {
            this.type = type;
            this.uri = uri;
            this.tp = stringToType(type);
        }

        @JsonIgnore
        public ContactType getType() {
            if (tp != null) {
                return tp;
            }
            return stringToType(type);
        }

        public Contact copy() {
            return new Contact(type, uri);
        }

        static Contact[] copyArray(Contact[] src){
            Contact[] dst = null;
            if (src != null) {
                dst = Arrays.copyOf(src, src.length);
                for (int i=0; i<src.length;i++) {
                    dst[i] = src[i].copy();
                }
            }
            return dst;
        }

        public static Contact[] append(Contact[] arr, Contact val) {
            int insertAt;
            if (arr == null) {
                arr = new Contact[1];
                arr[0] = val;
            } else if ((insertAt = Arrays.binarySearch(arr, val)) >=0) {
                if (!TYPE_OTHER.equals(val.type)) {
                    arr[insertAt].type = val.type;
                    arr[insertAt].tp = stringToType(val.type);
                }
            } else {
                arr = Arrays.copyOf(arr, arr.length + 1);
                arr[arr.length - 1] = val;
            }

            Arrays.sort(arr);

            return arr;
        }

        @Override
        public int compareTo(Contact c) {
            return uri.compareTo(c.uri);
        }

        @Override
        public String toString() {
            return type + ":" + uri;
        }
    }

    public static class Photo implements Serializable {
        public byte[] data;
        public String type;

        public Photo() {}

        public Photo(byte[] bits) {
            data = bits;
        }

        public Photo copy() {
            Photo ret = new Photo();
            ret.data = data;
            ret.type = type;
            return ret;
        }
    }
}

