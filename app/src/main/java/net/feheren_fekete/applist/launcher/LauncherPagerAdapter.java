package net.feheren_fekete.applist.launcher;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.ArrayMap;
import android.view.ViewGroup;

import net.feheren_fekete.applist.applistpage.ApplistFragment;
import net.feheren_fekete.applist.launcherpage.LauncherPageFragment;

import java.util.Map;

public class LauncherPagerAdapter extends FragmentStatePagerAdapter {

    private Map<Integer, Fragment> mPageFragments = new ArrayMap<>();

    public LauncherPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public int getMainPagePosition() {
        return 1;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment;
        if (position == 1) {
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
        return 3;
    }

    public Fragment getPageFragment(int position) {
        return mPageFragments.get(position);
    }

}
