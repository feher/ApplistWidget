package net.feheren_fekete.applist.launcher;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import net.feheren_fekete.applist.ApplistsFragment;
import net.feheren_fekete.applist.launcherpage.LauncherPageFragment;

public class LauncherPagerAdapter extends FragmentStatePagerAdapter {

    public LauncherPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return new ApplistsFragment();
        } else {
            return new LauncherPageFragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
