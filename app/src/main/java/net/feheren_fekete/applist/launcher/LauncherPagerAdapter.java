package net.feheren_fekete.applist.launcher;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.ArrayMap;
import android.view.ViewGroup;

import net.feheren_fekete.applist.applistpage.ApplistFragment;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.launcherpage.LauncherPageFragment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LauncherPagerAdapter extends FragmentStatePagerAdapter {

    private List<PageData> mPages = Collections.emptyList();
    private Map<Integer, Fragment> mPageFragments = new ArrayMap<>();

    public LauncherPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setPages(List<PageData> pages) {
        mPages = pages;
        notifyDataSetChanged();
    }

    public int getMainPagePosition() {
        for (int i = 0; i < mPages.size(); ++i) {
            PageData pageData = mPages.get(i);
            if (pageData.isMainPage()) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        PageData pageData = mPages.get(position);
        if (pageData.getType() == PageData.TYPE_APPLIST_PAGE) {
            fragment = new ApplistFragment();
        } else {
            fragment = LauncherPageFragment.newInstance(position);
        }
        mPageFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageFragments.remove(position);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    public Fragment getPageFragment(int position) {
        return mPageFragments.get(position);
    }

}
