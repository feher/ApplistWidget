package net.feheren_fekete.applist;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import net.feheren_fekete.applist.model.DataModel;
import net.feheren_fekete.applist.utils.RunnableWithArg;

import java.util.ArrayList;
import java.util.List;

public class ApplistPagerAdapter2 extends FragmentStatePagerAdapter {

    private static final String TAG = ApplistPagerAdapter2.class.getSimpleName();

    private List<String> mPageNames;
    private DataModel mDataModel;
    private IconCache mIconCache;
    private ApplistFragment mCurrentPageFragment;
    private SparseArray<ApplistFragment> mPageFragments;

    public ApplistPagerAdapter2(FragmentManager manager,
                                DataModel dataModel,
                                IconCache iconCache) {
        super(manager);

        mPageNames = new ArrayList<>();
        mDataModel = dataModel;
        mIconCache = iconCache;
        mCurrentPageFragment = null;
        mPageFragments = new SparseArray<>();
    }

    @Override
    public ApplistFragment getItem(int position) {
        return ApplistFragment.newInstance(mPageNames.get(position), mDataModel, mIconCache);
    }

    @Override
    public int getItemPosition(Object object) {
        // FIXME: Fix this.
        return POSITION_NONE;
//        ApplistFragment fragment = (ApplistFragment) object;
//        int position = mPageNames.indexOf(fragment.getPageName());
//        Log.d(TAG, "ZIZI GET ITEM POS " + fragment.getPageName() + " at " + position);
//        if (position >= 0) {
//            return position;
//        } else {
//            return POSITION_NONE;
//        }
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
        mCurrentPageFragment = (ApplistFragment) object;
        super.setPrimaryItem(container, position, object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object o = super.instantiateItem(container, position);
        ApplistFragment fragment = (ApplistFragment) o;
        Log.d(TAG, "ZIZI INSTANTIATE FRAG " + fragment.getPageName());
        mPageFragments.put(position, fragment);
        return o;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Log.d(TAG, "ZIZI DESTROY FRAG " + mPageFragments.get(position).getPageName());
        mPageFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public void clearPageNames() {
        mPageNames.clear();
    }

    public void setPageNames(List<String> pageNames) {
        mPageNames = pageNames;
    }

    public List<String> getPageNames() {
        return mPageNames;
    }

    public ApplistFragment getCurrentPageFragment() {
        return mCurrentPageFragment;
    }

    public void forEachPageFragment(RunnableWithArg<ApplistFragment> runnable) {
        for (int i = 0; i < mPageFragments.size(); ++i) {
            ApplistFragment fragment = mPageFragments.valueAt(i);
            runnable.run(fragment);
        }
    }

}
