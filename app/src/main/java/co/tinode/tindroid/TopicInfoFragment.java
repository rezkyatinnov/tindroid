package co.tinode.tindroid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;

import co.tinode.tindroid.account.ContactsManager;
import co.tinode.tindroid.db.StoredSubscription;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tindroid.widgets.LetterTileDrawable;
import co.tinode.tindroid.widgets.RoundImageDrawable;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.NotConnectedException;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.Acs;
import co.tinode.tinodesdk.model.PrivateType;
import co.tinode.tinodesdk.model.ServerMessage;
import co.tinode.tinodesdk.model.Subscription;

import static android.app.Activity.RESULT_OK;

/**
 * Topic Info fragment: p2p or a group topic.
 */
public class TopicInfoFragment extends Fragment {

    private static final String TAG = "TopicInfoFragment";

    private static final int ACTION_REMOVE = 4;
    private static final int ACTION_BAN = 5;

    private ComTopic<VxCard> mTopic;
    private MembersAdapter mAdapter;

    private PromisedReply.FailureListener<ServerMessage> mFailureListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topic_info, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mAdapter = new MembersAdapter();
        mFailureListener = new UiUtils.ToastFailureListener(activity);

        RecyclerView rv = view.findViewById(R.id.groupMembers);
        rv.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.VERTICAL, false));
        rv.addItemDecoration(new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL));
        rv.setAdapter(mAdapter);
        rv.setNestedScrollingEnabled(false);

        // Set up listeners

        view.findViewById(R.id.uploadAvatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.requestAvatar(TopicInfoFragment.this);
            }
        });

        final Switch muted = view.findViewById(R.id.switchMuted);
        muted.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                try {
                    mTopic.updateMuted(isChecked);
                } catch (NotConnectedException ignored) {
                    muted.setChecked(!isChecked);
                    Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    muted.setChecked(!isChecked);
                }
            }
        });

        view.findViewById(R.id.permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.showEditPermissions(activity, mTopic, mTopic.getAccessMode().getWant(), null,
                        UiUtils.ACTION_UPDATE_SELF_SUB, "O");
            }
        });

        view.findViewById(R.id.buttonLeaveGroup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTopic.isOwner()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(R.string.owner_cannot_unsubscribe)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(true)
                            .setNegativeButton(android.R.string.ok, null)
                            .show();
                } else {
                    try {
                        mTopic.delete().thenApply(null, mFailureListener);
                    } catch (NotConnectedException ignored) {
                        Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {
                        Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        view.findViewById(R.id.buttonAddMembers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MessageActivity) activity).showFragment(MessageActivity.FRAGMENT_EDIT_MEMBERS,
                        true, null);
            }
        });

        view.findViewById(R.id.authPermissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.showEditPermissions(activity, mTopic, mTopic.getAuthAcsStr(), null,
                        UiUtils.ACTION_UPDATE_AUTH, "O");
            }
        });

        view.findViewById(R.id.anonPermissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.showEditPermissions(activity, mTopic, mTopic.getAnonAcsStr(), null,
                        UiUtils.ACTION_UPDATE_ANON, "O");
            }
        });

        view.findViewById(R.id.userOne).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.showEditPermissions(activity, mTopic,
                        mTopic.getAccessMode().getWant(), null,
                        UiUtils.ACTION_UPDATE_SELF_SUB, "ASDO");
            }
        });

        view.findViewById(R.id.userTwo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.showEditPermissions(activity, mTopic,
                        mTopic.getSubscription(mTopic.getName()).acs.getGiven(),
                        mTopic.getName(),
                        UiUtils.ACTION_UPDATE_SUB, "ASDO");
            }
        });
    }

        @Override
    @SuppressWarnings("unchecked")
    // onResume sets up the form with values and views which do not change + sets up listeners.
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();
        final Bundle bundle = getArguments();

        if (activity == null || bundle == null) {
            return;
        }

        String name = bundle.getString("topic");
        mTopic = (ComTopic<VxCard>) Cache.getTinode().getTopic(name);

        final TextView title = activity.findViewById(R.id.topicTitle);
        final TextView subtitle = activity.findViewById(R.id.topicSubtitle);
        final TextView address = activity.findViewById(R.id.topicAddress);

        final View groupMembers = activity.findViewById(R.id.groupMembersWrapper);
        final View defaultPermissions = activity.findViewById(R.id.defaultPermissionsWrapper);
        final View uploadAvatarButton = activity.findViewById(R.id.uploadAvatar);

        // Launch edit dialog when title or subtitle is clicked.
        final View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTopicText();
            }
        };
        if (mTopic.isOwner()) {
            title.setOnClickListener(l);
        }
        subtitle.setOnClickListener(l);

        address.setText(mTopic.getName());

        if (mTopic.isGrpType()) {
            // Group topic

            uploadAvatarButton.setVisibility(mTopic.isManager() ? View.VISIBLE : View.GONE);

            groupMembers.setVisibility(View.VISIBLE);

            activity.findViewById(R.id.singleUserPermissions).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.p2pPermissions).setVisibility(View.GONE);

            Button button = activity.findViewById(R.id.buttonLeaveGroup);
            if (mTopic.isOwner()) {
                button.setEnabled(false);
                button.setAlpha(0.5f);
            } else {
                button.setEnabled(true);
                button.setAlpha(1f);
            }

            button = activity.findViewById(R.id.buttonAddMembers);
            if (!mTopic.isSharer() && !mTopic.isManager()) {
                // FIXME: allow sharers to add members but not remove.
                // Disable and gray out "invite members" button because only admins can
                // invite group members.
                button.setEnabled(false);
                button.setAlpha(0.5f);
            } else {
                button.setEnabled(true);
                button.setAlpha(1f);
            }

            defaultPermissions.setVisibility(mTopic.isManager() ? View.VISIBLE : View.GONE);

        } else {
            // P2P topic
            uploadAvatarButton.setVisibility(View.GONE);

            groupMembers.setVisibility(View.GONE);

            activity.findViewById(R.id.singleUserPermissions).setVisibility(View.GONE);
            activity.findViewById(R.id.p2pPermissions).setVisibility(View.VISIBLE);

            VxCard two = mTopic.getPub();
            ((TextView) activity.findViewById(R.id.userTwoLabel)).setText(two != null && two.fn != null ?
                    two.fn : mTopic.getName());

            defaultPermissions.setVisibility(View.GONE);
        }

        notifyContentChanged();
        notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // Dialog for editing pub.fn and priv
    private void showEditTopicText() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        VxCard pub = mTopic.getPub();
        final String title = pub == null ? null : pub.fn;
        final PrivateType priv = mTopic.getPriv();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View editor = View.inflate(builder.getContext(), R.layout.dialog_edit_group, null);
        builder.setView(editor).setTitle(R.string.edit_topic);

        final EditText titleEditor = editor.findViewById(R.id.editTitle);
        final EditText subtitleEditor = editor.findViewById(R.id.editPrivate);
        if (mTopic.isOwner()) {
            if (!TextUtils.isEmpty(title)) {
                titleEditor.setText(title);
            }
        } else {
            editor.findViewById(R.id.editTitleWrapper).setVisibility(View.GONE);
        }

        if (priv != null && !TextUtils.isEmpty(priv.getComment())) {
            subtitleEditor.setText(priv.getComment());
        }

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTitle = null;
                if (mTopic.isOwner()) {
                    newTitle = titleEditor.getText().toString();
                }
                String newPriv = subtitleEditor.getText().toString();
                UiUtils.updateTitle(activity, mTopic, newTitle, newPriv);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    // Confirmation dialog "Do you really want to do X?"
    private void showConfirmationDialog(final String topicTitle,
                                        final String title, final String uid, int message_id, final int what) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(activity);
        confirmBuilder.setNegativeButton(android.R.string.no, null);
        String message = activity.getString(message_id, title, topicTitle);
        confirmBuilder.setMessage(message);

        confirmBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    switch (what) {
                        case ACTION_REMOVE:
                            mTopic.eject(uid, false).thenApply(null, mFailureListener);
                            break;
                        case ACTION_BAN:
                            mTopic.eject(uid, true).thenApply(null, mFailureListener);
                            break;
                    }
                } catch (NotConnectedException ignored) {
                    Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                    Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        confirmBuilder.show();
    }

    // Dialog-menu with actions for individual subscribers, like "send message", "change permissions", "ban", etc.
    private void showMemberAction(final String topicTitle, final String title, final String uid, final String mode) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (Cache.getTinode().isMe(uid)) {
            return;
        }

        final String titleFixed = TextUtils.isEmpty(title) ?
                activity.getString(R.string.placeholder_contact_title) :
                title;
        final String topicTitleFixed = TextUtils.isEmpty(topicTitle) ?
                activity.getString(R.string.placeholder_topic_title) :
                topicTitle;

        AlertDialog.Builder actionBuilder = new AlertDialog.Builder(activity);
        final LinearLayout actions = (LinearLayout) View.inflate(activity, R.layout.dialog_member_actions, null);
        actionBuilder
                .setTitle(TextUtils.isEmpty(title) ?
                        activity.getString(R.string.placeholder_contact_title) :
                        title)
                .setView(actions)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog dialog = actionBuilder.create();
        View.OnClickListener ocl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent;
                    switch (v.getId()) {
                        case R.id.buttonViewProfile:
                            String lookupKey = ContactsManager.getLookupKey(activity.getContentResolver(), uid);
                            intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey));
                            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                                startActivity(intent);
                            }
                            break;
                        case R.id.buttonSendMessage:
                            intent = new Intent(activity, MessageActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            intent.putExtra("topic", uid);
                            startActivity(intent);
                            break;
                        case R.id.buttonPermissions:
                            UiUtils.showEditPermissions(activity, mTopic, mode, uid,
                                    UiUtils.ACTION_UPDATE_SUB, "O");
                            break;
                        case R.id.buttonMakeOwner:
                            mTopic.updateMode(uid, "+O").thenApply(null, mFailureListener);
                            break;
                        case R.id.buttonRemove: {
                            showConfirmationDialog(topicTitleFixed, titleFixed, uid,
                                    R.string.confirm_member_removal, ACTION_REMOVE);
                            break;
                        }
                        case R.id.buttonBlock: {
                            showConfirmationDialog(topicTitleFixed, titleFixed, uid,
                                    R.string.confirm_member_ban, ACTION_BAN);
                            break;
                        }
                    }
                } catch (NotConnectedException ignored) {
                    Toast.makeText(activity, R.string.no_connection, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                    Toast.makeText(activity, R.string.action_failed, Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            }
        };
        actions.findViewById(R.id.buttonViewProfile).setOnClickListener(ocl);
        actions.findViewById(R.id.buttonSendMessage).setOnClickListener(ocl);
        if (mTopic.isOwner()) {
            actions.findViewById(R.id.buttonMakeOwner).setOnClickListener(ocl);
        } else {
            actions.findViewById(R.id.buttonMakeOwner).setVisibility(View.GONE);
        }
        if (mTopic.isAdmin() || mTopic.isOwner()) {
            actions.findViewById(R.id.buttonPermissions).setOnClickListener(ocl);
            actions.findViewById(R.id.buttonRemove).setOnClickListener(ocl);
            actions.findViewById(R.id.buttonBlock).setOnClickListener(ocl);
        } else {
            actions.findViewById(R.id.buttonPermissions).setVisibility(View.GONE);
            actions.findViewById(R.id.buttonRemove).setVisibility(View.GONE);
            actions.findViewById(R.id.buttonBlock).setVisibility(View.GONE);
        }
        dialog.show();
    }

    void notifyDataSetChanged() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mTopic.isGrpType()) {
            mAdapter.resetContent();
        } else {
            ((TextView)activity.findViewById(R.id.userOne))
                    .setText(mTopic.getAccessMode().getWant());
            ((TextView)activity.findViewById(R.id.userTwo))
                    .setText(mTopic.getSubscription(mTopic.getName()).acs.getGiven());
        }
    }

    // Called when topic description is changed.
    private void notifyContentChanged() {

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final AppCompatImageView avatar = activity.findViewById(R.id.imageAvatar);
        final TextView title = activity.findViewById(R.id.topicTitle);
        final TextView subtitle = activity.findViewById(R.id.topicSubtitle);

        VxCard pub = mTopic.getPub();
        if (pub != null) {
            if (!TextUtils.isEmpty(pub.fn)) {
                title.setText(pub.fn);
                title.setTypeface(null, Typeface.NORMAL);
                title.setTextIsSelectable(true);
            } else {
                title.setText(R.string.placeholder_contact_title);
                title.setTypeface(null, Typeface.ITALIC);
                title.setTextIsSelectable(false);
            }
            final Bitmap bmp = pub.getBitmap();
            if (bmp != null) {
                avatar.setImageDrawable(new RoundImageDrawable(getResources(), bmp));
            } else {
                avatar.setImageDrawable(
                        new LetterTileDrawable(requireContext())
                                .setIsCircular(true)
                                .setContactTypeAndColor(
                                        mTopic.getTopicType() == Topic.TopicType.P2P ?
                                            LetterTileDrawable.TYPE_PERSON :
                                                LetterTileDrawable.TYPE_GROUP)
                                .setLetterAndColor(pub.fn, mTopic.getName()));
            }
        } else {
            avatar.setImageDrawable(
                    new LetterTileDrawable(requireContext()).setIsCircular(true));
        }

        PrivateType priv = mTopic.getPriv();
        if (priv != null && !TextUtils.isEmpty(priv.getComment())) {
            subtitle.setText(priv.getComment());
            subtitle.setTypeface(null, Typeface.NORMAL);
            subtitle.setTextIsSelectable(true);
        } else {
            subtitle.setText(R.string.placeholder_private);
            subtitle.setTypeface(null, Typeface.ITALIC);
            subtitle.setTextIsSelectable(false);
        }

        ((Switch) activity.findViewById(R.id.switchMuted)).setChecked(mTopic.isMuted());
        Acs acs = mTopic.getAccessMode();
        ((TextView) activity.findViewById(R.id.permissions)).setText(acs == null ? "" : acs.getMode());

        ((TextView) activity.findViewById(R.id.authPermissions)).setText(mTopic.getAuthAcsStr());
        ((TextView) activity.findViewById(R.id.anonPermissions)).setText(mTopic.getAnonAcsStr());
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Execution has to be delayed because the topic is not yet subscribed:
        // The image selection activity was on top while MessageActivity was paused. It just got
        // unpaused and did not have time to re-subscribe.
        if (requestCode == UiUtils.SELECT_PICTURE && resultCode == RESULT_OK) {
            final MessageActivity activity = (MessageActivity) getActivity();
            if (activity != null) {
                activity.submitForExecution(new Runnable() {
                    @Override
                    public void run() {
                        UiUtils.updateAvatar(activity, mTopic, data);
                    }
                });
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // inflater.inflate(R.menu.menu_topic_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView extraInfo;
        LinearLayout statusContainer;
        TextView[] status;
        ImageButton more;
        AppCompatImageView icon;


        MemberViewHolder(View item) {
            super(item);

            name = item.findViewById(android.R.id.text1);
            extraInfo = item.findViewById(android.R.id.text2);
            statusContainer = item.findViewById(R.id.statusContainer);
            status = new TextView[statusContainer.getChildCount()];
            for (int i = 0; i < status.length; i++) {
                status[i] = (TextView) statusContainer.getChildAt(i);
            }
            more = item.findViewById(R.id.optionsMenu);
            icon = item.findViewById(android.R.id.icon);
        }
    }

    private class MembersAdapter extends RecyclerView.Adapter<MemberViewHolder> {

        private Subscription<VxCard,PrivateType>[] mItems;
        private int mItemCount;

        @SuppressWarnings("unchecked")
        MembersAdapter() {
            mItems = (Subscription<VxCard,PrivateType>[]) new Subscription[8];
            mItemCount = 0;
        }

        /**
         * Must be run on UI thread
         */
        void resetContent() {
            if (mTopic != null) {
                Collection<Subscription<VxCard,PrivateType>> c = mTopic.getSubscriptions();
                if (c != null) {
                    mItemCount = c.size();
                    mItems = c.toArray(mItems);
                } else {
                    mItemCount = 0;
                }

                notifyDataSetChanged();
            }
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }

        @Override
        public long getItemId(int i) {
            return StoredSubscription.getId(mItems[i]);
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_member, parent, false);
            return new MemberViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final MemberViewHolder holder, int position) {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            final Subscription<VxCard,PrivateType> sub = mItems[position];
            final StoredSubscription ss = (StoredSubscription) sub.getLocal();
            final boolean isMe = Cache.getTinode().isMe(sub.user);

            Bitmap bmp = null;
            String title = isMe ? activity.getString(R.string.current_user) : null;
            if (sub.pub != null) {
                if (title == null) {
                    title = !TextUtils.isEmpty(sub.pub.fn) ? sub.pub.fn :
                            activity.getString(R.string.placeholder_contact_title);
                }
                bmp = sub.pub.getBitmap();
            } else {
                Log.w(TAG, "Pub is null for " + sub.user);
            }
            holder.name.setText(title);
            holder.extraInfo.setText(sub.acs != null ? sub.acs.getMode() : "");

            int i = 0;
            UiUtils.AccessModeLabel[] labels = UiUtils.accessModeLabels(sub.acs, ss.status);
            if (labels != null) {
                for (UiUtils.AccessModeLabel l : labels) {
                    holder.status[i].setText(l.nameId);
                    holder.status[i].setTextColor(l.color);
                    ((GradientDrawable) holder.status[i].getBackground()).setStroke(2, l.color);
                    holder.status[i++].setVisibility(View.VISIBLE);
                }
            }
            for (; i < holder.status.length; i++) {
                holder.status[i].setVisibility(View.GONE);
            }

            holder.icon.setImageDrawable(UiUtils.avatarDrawable(activity, bmp,
                    sub.pub != null ? sub.pub.fn : null, sub.user));

            final View.OnClickListener action = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    final Subscription<VxCard,PrivateType> sub = mItems[position];
                    VxCard pub = mTopic.getPub();
                    showMemberAction(pub != null ? pub.fn : null, holder.name.getText().toString(), sub.user,
                            sub.acs.getGiven());
                }
            };

            holder.itemView.setOnClickListener(action);
            if (isMe) {
                holder.more.setVisibility(View.INVISIBLE);
            } else {
                holder.more.setVisibility(View.VISIBLE);
                holder.more.setOnClickListener(action);
            }
        }
    }
}
