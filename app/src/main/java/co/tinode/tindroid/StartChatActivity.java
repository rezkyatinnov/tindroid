package co.tinode.tindroid;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.view.View;

import com.google.android.material.tabs.TabLayout;

/**
 * View to display a single conversation
 */
public class StartChatActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = "StartChatActivity";

    private static final int COUNT_OF_TABS = 3;
    private static final int TAB_SEARCH = 0;
    private static final int TAB_NEW_GROUP = 1;
    private static final int TAB_BY_ID = 2;


    static {
        // Otherwise crash on pre-Lollipop (API < 21)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartChatActivity.this, ChatsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });
        }

        TabLayout tabLayout = findViewById(R.id.tabsContacts);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.tabPager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                final ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setTitle(R.string.action_new_chat);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == UiUtils.READ_EXTERNAL_STORAGE_PERMISSION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UiUtils.requestAvatar(getSupportFragmentManager().findFragmentById(R.id.contentFragment));
            }
        }
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {
        Fragment mSearch;
        Fragment mCreateGroup;
        Fragment mById;

        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_SEARCH:
                    if (mSearch == null) {
                        mSearch = new FindFragment();
                    }
                    return mSearch;
                case TAB_NEW_GROUP:
                    if (mCreateGroup == null) {
                        mCreateGroup = new CreateGroupFragment();
                    }
                    return mCreateGroup;
                case TAB_BY_ID:
                    if (mById == null) {
                        mById = new AddByIDFragment();
                    }
                    return mById;
                default:
                    throw new IllegalArgumentException("Invalid TAB position " + position);
            }
        }

        @Override
        public int getCount() {
            return COUNT_OF_TABS;
        }
    }
}
