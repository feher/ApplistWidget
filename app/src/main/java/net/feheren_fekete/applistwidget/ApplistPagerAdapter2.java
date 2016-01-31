package net.feheren_fekete.applistwidget;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import net.feheren_fekete.applistwidget.model.DataModel;

import java.util.ArrayList;
import java.util.List;

public class ApplistPagerAdapter2 extends FragmentStatePagerAdapter {
    private List<String> mPageNames;
    private DataModel mDataModel;
    private Fragment mCurrentPageFragment;

    public ApplistPagerAdapter2(FragmentManager manager, DataModel dataModel) {
        super(manager);

        mPageNames = new ArrayList<>();
        mDataModel = dataModel;
        mCurrentPageFragment = null;
    }

    @Override
    public Fragment getItem(int position) {
        return ApplistFragment.newInstance(mPageNames.get(position), mDataModel);
    }

    @Override
    public int getCount() {
        return mPageNames.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mPageNames.get(position);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        mCurrentPageFragment = (Fragment) object;
        super.setPrimaryItem(container, position, object);
    }

    public void clearPageNames() {
        mPageNames.clear();
    }

    public void setPageNames(List<String> pageNames) {
        mPageNames = pageNames;
    }

    public Fragment getCurrentPageFragment() {
        return mCurrentPageFragment;
    }

}
