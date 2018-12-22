package co.tinode.tindroid.account;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.util.SparseArray;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


/**
 * Constants and misc utils
 */
public class Utils {
    private static final String TAG = "Utils";

    // Account management constants
    public static final String TOKEN_TYPE = "co.tinode.token";
    public static final String ACCOUNT_TYPE = "co.tinode.account";
    public static final String SYNC_AUTHORITY = "com.android.contacts";
    public static final String TINODE_IM_PROTOCOL = "Tinode";

    // Constants for accessing shared preferences
    public static final String PREFS_HOST_NAME = "pref_hostName";
    public static final String PREFS_USE_TLS = "pref_useTLS";

    // Prefixes for various contacts
    public static final String TAG_LABEL_PHONE = "tel:";
    public static final String TAG_LABEL_EMAIL = "email:";
    public static final String TAG_LABEL_TINODE = "tinode:";

    public static final int FETCH_EMAIL = 0x1;
    public static final int FETCH_PHONE = 0x2;
    public static final int FETCH_IM = 0x4;

    /**
     * MIME-type used when storing a profile {@link ContactsContract.Data} entry.
     */
    public static final String MIME_PROFILE =
            "vnd.android.cursor.item/vnd.tinode.profile";
    public static final String DATA_PID = Data.DATA1;
    public static final String DATA_SUMMARY = Data.DATA2;
    public static final String DATA_DETAIL = Data.DATA3;

    public static Account createAccount(String accountName) {
        return new Account(accountName, ACCOUNT_TYPE);
    }

    /**
     * Read address book contacts from the Contacts content provider.
     *
     * @param resolver content resolver to use.
     * @param flags bit flags indicating types f contacts to fetch.
     * @return
     */
    public static SparseArray<ContactHolder> fetchContacts(ContentResolver resolver, int flags) {
        SparseArray<ContactHolder> map = new SparseArray<>();

        final String[] projection = {
                Data.CONTACT_ID,
                Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Im.PROTOCOL,
                ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL,
        };

        LinkedList<String> args = new LinkedList<>();
        if ((flags & FETCH_EMAIL) != 0) {
            args.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        }
        if ((flags & FETCH_PHONE) != 0) {
            args.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        }
        if ((flags & FETCH_IM) != 0) {
            args.add(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
        }
        if (args.size() == 0) {
            throw new IllegalArgumentException();
        }

        StringBuilder sel = new StringBuilder(Data.MIMETYPE);
        sel.append(" IN (");
        for (int i=0; i<args.size(); i++) {
            sel.append("?,");
        }
        // Strip final comma.
        sel.setLength(sel.length() - 1);
        sel.append(")");

        final String selection = sel.toString();

        final String[] selectionArgs = args.toArray(new String[]{});

        // Get contacts from the database.
        Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI, projection,
                selection, selectionArgs, null);
        if (cursor == null) {
            Log.d(TAG, "Failed to fetch contacts");
            return map;
        }

        final int contactIdIdx = cursor.getColumnIndex(Data.CONTACT_ID);
        final int mimeTypeIdx = cursor.getColumnIndex(Data.MIMETYPE);
        final int dataIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
        final int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE);
        final int imProtocolIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL);
        final int imProtocolNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL);

        final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        final String country = Locale.getDefault().getCountry();

        while (cursor.moveToNext()) {
            int type = cursor.getInt(typeIdx);
            int contact_id = cursor.getInt(contactIdIdx);
            String data = cursor.getString(dataIdx);
            String mimeType = cursor.getString(mimeTypeIdx);

            // Log.d(TAG, "Got id=" + contact_id + ", mime='" + mimeType +"', val='" + data + "'");

            ContactHolder holder = map.get(contact_id);
            if (holder == null) {
                holder = new ContactHolder();
                map.put(contact_id, holder);
            }

            switch (mimeType) {
                case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                    // This is an email
                    //Log.d(TAG, "Adding email '" + data + "' to contact=" + contact_id);
                    holder.putEmail(data);
                    break;
                case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                    int protocol = cursor.getInt(imProtocolIdx);
                    String protocolName = cursor.getString(imProtocolNameIdx);
                    // Log.d(TAG, "Possibly adding IM '" + data + "' to contact=" + contact_id);
                    if (protocol == ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM &&
                            protocolName.equals(TINODE_IM_PROTOCOL)) {
                        holder.putIm(data);
                        // Log.d(TAG, "Added IM '" + data + "' to contact=" + contact_id);
                    }
                    break;
                case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                    // This is a phone number. Use mobile phones only.
                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                        // Log.d(TAG, "Adding mobile phone '" + data + "' to contact=" + contact_id);
                        try {
                            // Normalize phone number format
                            data = phoneUtil.format(phoneUtil.parse(data, country),
                                    PhoneNumberUtil.PhoneNumberFormat.E164);
                            holder.putPhone(data);
                        } catch (NumberParseException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
        cursor.close();

        return map;
    }

    public static class ContactHolder {
        List<String> emails;
        List<String> phones;
        List<String> ims;

        public ContactHolder() {
            emails = null;
            phones = null;
            ims = null;
        }

        private static void Stringify(List<String> vals, String label, StringBuilder str) {
            if (vals != null && vals.size() > 0) {
                if (str.length() > 0) {
                    str.append(",");
                }

                for (String entry : vals) {
                    str.append(label);
                    str.append(entry);
                    str.append(",");
                }
                // Strip trailing comma.
                str.setLength(str.length() - 1);
            }
        }

        public void putEmail(String email) {
            if (emails == null) {
                emails = new LinkedList<>();
            }
            emails.add(email);
        }

        public void putPhone(String phone) {
            if (phones == null) {
                phones = new LinkedList<>();
            }
            phones.add(phone);
        }

        public void putIm(String im) {
            if (ims == null) {
                ims = new LinkedList<>();
            }
            ims.add(im);
        }

        public int getEmailCount() {
            return emails != null ? emails.size() : 0;
        }

        public int getPhoneCount() {
            return phones != null ? phones.size() : 0;
        }

        public int getImCount() {
            return ims != null ? ims.size() : 0;
        }

        public String getEmail() {
            return emails != null ? emails.get(0) : null;
        }

        public String getPhone() {
            return phones != null ? phones.get(0) : null;
        }

        public String getIm() {
            return ims != null ? ims.get(0) : null;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            Stringify(emails, TAG_LABEL_EMAIL, str);
            Stringify(phones, TAG_LABEL_PHONE, str);
            Stringify(ims, TAG_LABEL_TINODE, str);
            return str.toString();
        }

        public String bestContact() {
            if (ims != null) {
                return TAG_LABEL_TINODE + ims.get(0);
            }

            if (phones != null) {
                return TAG_LABEL_PHONE + phones.get(0);
            }

            if (emails != null) {
                return TAG_LABEL_EMAIL + emails.get(0);
            }

            return "";
        }
    }
}
