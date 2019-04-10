package net.feheren_fekete.applist.launcher;

import android.view.ViewGroup;

import net.feheren_fekete.applist.applistpage.ApplistPageFragment;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.widgetpage.WidgetPageFragment;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class LauncherPagerAdapter extends FragmentStatePagerAdapter {

    private List<PageData> mPages = Collections.emptyList();
    private Map<Integer, WeakReference<Fragment>> mPageFragments = new ArrayMap<>();

    public LauncherPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setPages(List<PageData> pages) {
        mPages = pages;
        notifyDataSetChanged();
    }

    public List<PageData> getPages() {
        return mPages;
    }

    public PageData getPageData(int position) {
        return mPages.get(position);
    }

    public int getMainPagePosition() {
        for (int i = 0; i < mPages.size(); ++i) {
            PageData pageData = mPages.get(i);
            if (pageData.isMainPage()) {
                return i;
            }
        }
        return -1;
    }

    public int getPagePosition(long pageId) {
        for (int i = 0; i < mPages.size(); ++i) {
            PageData pageData = mPages.get(i);
            if (pageData.getId() == pageId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        PageData pageData = mPages.get(position);
        if (pageData.getType() == PageData.TYPE_APPLIST_PAGE) {
            fragment = ApplistPageFragment.newInstance(pageData.getId());
        } else {
            fragment = WidgetPageFragment.newInstance(pageData.getId());
        }
        mPageFragments.put(position, new WeakReference<>(fragment));

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

    @Nullable
    public Fragment getPageFragment(int position) {
        WeakReference<Fragment> fragmentWeakReference = mPageFragments.get(position);
        if (fragmentWeakReference == null) {
            return null;
        }
        return fragmentWeakReference.get();
    }

}
