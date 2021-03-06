package org.wordpress.android.ui;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AddQuickPressShortcutActivity extends ListActivity {
    static final int ADD_ACCOUNT_REQUEST = 0;

    public String[] blogNames;
    public int[] siteIds;
    public String[] accountUsers;
    public String[] blavatars;
    public List<String> accountNames = new ArrayList<>();

    @Inject SiteStore mSiteStore;
    @Inject FluxCImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.add_quickpress_shortcut);
        setTitle(getResources().getText(R.string.quickpress_window_title));

        displayAccounts();
    }

    private void displayAccounts() {
        List<SiteModel> sites = mSiteStore.getVisibleSites();

        ListView listView = (ListView) findViewById(android.R.id.list);

        ImageView iv = new ImageView(this);
        iv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_divider));
        listView.addFooterView(iv);
        listView.setVerticalFadingEdgeEnabled(false);
        listView.setVerticalScrollBarEnabled(true);

        if (sites.size() > 0) {
            ScrollView sv = new ScrollView(this);
            sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            LinearLayout layout = new LinearLayout(this);
            layout.setPadding(10, 10, 10, 0);
            layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

            layout.setOrientation(LinearLayout.VERTICAL);

            blogNames = new String[sites.size()];
            siteIds = new int[sites.size()];
            accountUsers = new String[sites.size()];
            blavatars = new String[sites.size()];
            for (int i = 0; i < sites.size(); i++) {
                SiteModel site = sites.get(i);
                blogNames[i] = SiteUtils.getSiteNameOrHomeURL(site);
                accountUsers[i] = site.getUsername();
                siteIds[i] = site.getId();
                if (site.getUrl() != null) {
                    blavatars[i] = SiteUtils.getSiteIconUrl(site, 60);
                } else {
                    blavatars[i] = "";
                }
                accountNames.add(i, blogNames[i]);
            }

            setListAdapter(new HomeListAdapter());

            listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
                    AddQuickPressShortcutActivity.this.buildDialog(position);
                }
            });

            if (sites.size() == 1) {
                AddQuickPressShortcutActivity.this.buildDialog(0);
            }

        } else {
            // no account, load new account view
            Intent i = new Intent(AddQuickPressShortcutActivity.this, SignInActivity.class);
            startActivityForResult(i, ADD_ACCOUNT_REQUEST);
        }
    }

    private void buildDialog(final int position) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddQuickPressShortcutActivity.this);
        dialogBuilder.setTitle(R.string.quickpress_add_alert_title);

        final EditText quickPressShortcutName = new EditText(AddQuickPressShortcutActivity.this);
        quickPressShortcutName.setText("QP " + StringEscapeUtils.unescapeHtml4(accountNames.get(position)));
        dialogBuilder.setView(quickPressShortcutName);

        dialogBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(quickPressShortcutName.getText())) {
                    ToastUtils.showToast(AddQuickPressShortcutActivity.this, R.string.quickpress_add_error,
                            ToastUtils.Duration.LONG);
                } else {
                    Intent shortcutIntent = new Intent(getApplicationContext(), EditPostActivity.class);
                    shortcutIntent.setAction(Intent.ACTION_MAIN);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_QUICKPRESS_BLOG_ID, siteIds[position]);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_IS_QUICKPRESS, true);

                    Intent addIntent = new Intent();
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, quickPressShortcutName.getText().toString());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext
                            (AddQuickPressShortcutActivity.this, R.mipmap.app_icon));

                    WordPress.wpDB.addQuickPressShortcut(siteIds[position], quickPressShortcutName.getText().toString());

                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    AddQuickPressShortcutActivity.this.sendBroadcast(addIntent);
                    AddQuickPressShortcutActivity.this.finish();
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            // just let the dialog close
            public void onClick(DialogInterface dialog, int which) {}
        });

        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    if (mSiteStore.getVisibleSitesCount() > 0) {
                        displayAccounts();
                        break;
                    }
                }
                finish();
                break;
        }
    }

    protected class HomeListAdapter extends BaseAdapter {
        public HomeListAdapter() {
        }

        public int getCount() {
            return mSiteStore.getVisibleSitesCount();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout view = (RelativeLayout) convertView;
            if (view == null) {
                LayoutInflater inflater = getLayoutInflater();
                view = (RelativeLayout)inflater.inflate(R.layout.home_row, parent, false);
            }
            String username = accountUsers[position];
            view.setId(Integer.valueOf(siteIds[position]));

            TextView blogName = (TextView)view.findViewById(R.id.blogName);
            TextView blogUsername = (TextView)view.findViewById(R.id.blogUser);
            NetworkImageView blavatar = (NetworkImageView)view.findViewById(R.id.blavatar);

            blogName.setText(
                    StringEscapeUtils.unescapeHtml4(blogNames[position]));
            blogUsername.setText(
                    StringEscapeUtils.unescapeHtml4(username));
            blavatar.setErrorImageResId(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp);
            blavatar.setImageUrl(blavatars[position], mImageLoader);

            return view;

        }

    }
}
