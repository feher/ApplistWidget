package net.feheren_fekete.applist.launcher.pageeditor;

import android.Manifest;
import android.appwidget.AppWidgetHost;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.feheren_fekete.applist.MainActivity;
import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.applistpage.ApplistDialogs;
import net.feheren_fekete.applist.launcher.LauncherStateManager;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.widgetpage.model.WidgetModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Callable;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import bolts.Task;

import static org.koin.java.KoinJavaComponent.get;

public class PageEditorFragment extends Fragment {

    private static final String TAG = PageEditorFragment.class.getSimpleName();

    private static final String FRAGMENT_ARG_REQUEST_DATA = PageEditorFragment.class.getSimpleName() + ".FRAGMENT_ARG_REQUEST_DATA";
    private static final String FRAGMENT_ARG_USE_AS_PAGE_PICKER = PageEditorFragment.class.getSimpleName() + ".FRAGMENT_ARG_USE_AS_PAGE_PICKER";
    private static final String FRAGMENT_ARG_ADD_PADDING = PageEditorFragment.class.getSimpleName() + ".FRAGMENT_ARG_ADD_PADDING";

    private static final int PERMISSIONS_REQUEST_READ_WALLPAPER = 1234;

    public static final class DoneEvent {}
    public static final class PageTappedEvent {
        public final Bundle requestData;
        public final PageData pageData;
        public PageTappedEvent(Bundle requestData, PageData pageData) {
            this.requestData = requestData;
            this.pageData = pageData;
        }
    }

    // TODO: Inject these singletons
    private LauncherStateManager mLauncherStateManager = get(LauncherStateManager.class);
    private LauncherModel mLauncherModel = LauncherModel.getInstance();
    private WidgetModel mWidgetModel = WidgetModel.getInstance();
    private ScreenshotUtils mScreenshotUtils = get(ScreenshotUtils.class);
    private ScreenUtils mScreenUtils = get(ScreenUtils.class);

    private RecyclerView mRecyclerView;
    private PageEditorAdapter mAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private int mDragScrollThreshold;
    private int mMaxDragScroll;

    private Bundle mRequestData;

    private class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private int itemAction;

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Nothing.
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            boolean result = mAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mLauncherModel.setPages(mAdapter.getItems());
                    return null;
                }
            });
            return result;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (viewHolder != null) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    ((PageEditorAdapter.PageViewHolder) viewHolder).layout.animate()
                            .scaleX(0.9f).scaleY(0.9f).setDuration(150).start();
                    itemAction = actionState;
                }
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (itemAction == ItemTouchHelper.ACTION_STATE_DRAG) {
                ((PageEditorAdapter.PageViewHolder) viewHolder).layout.animate()
                        .scaleX(1.0f).scaleY(1.0f).setDuration(150).start();

                // This is needed to re-draw (re-bind) all the items in the RecyclerView.
                // We want to update the page numbers of every item.
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            int scrollX = 0;
            final int absOutOfBounds = Math.abs(viewSizeOutOfBounds);
            if (absOutOfBounds > mDragScrollThreshold) {
                final int direction = (int) Math.signum(viewSizeOutOfBounds);
                scrollX = direction * mMaxDragScroll;
                if (scrollX == 0) {
                    scrollX = viewSizeOutOfBounds > 0 ? 1 : -1;
                }
            }
            return scrollX;
        }
    }

    public static PageEditorFragment newInstance(boolean addPadding, boolean useAsPagePicker, Bundle requestData) {
        PageEditorFragment fragment = new PageEditorFragment();
        Bundle args = new Bundle();
        args.putBoolean(FRAGMENT_ARG_ADD_PADDING, addPadding);
        args.putBoolean(FRAGMENT_ARG_USE_AS_PAGE_PICKER, useAsPagePicker);
        args.putBundle(FRAGMENT_ARG_REQUEST_DATA, requestData);
        fragment.setArguments(args);
        return fragment;
    }

    public PageEditorFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_page_editor_fragment, container, false);

        if (getArguments().getBoolean(FRAGMENT_ARG_ADD_PADDING)) {
            // REF: 2017_06_22_12_00_transparent_status_bar_top_padding
            final int topPadding = mScreenUtils.getStatusBarHeight(getContext());
            // REF: 2017_06_22_12_00_transparent_navigation_bar_bottom_padding
            final int bottomPadding = mScreenUtils.hasNavigationBar(getContext()) ? mScreenUtils.getNavigationBarHeight(getContext()) : 0;
            view.findViewById(R.id.launcher_page_editor_fragment_layout).setPadding(0, topPadding, 0, bottomPadding);
        }

        mRecyclerView = view.findViewById(R.id.launcher_page_editor_page_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));

        final boolean useAsPagePicker = getArguments().getBoolean(FRAGMENT_ARG_USE_AS_PAGE_PICKER);

        final float pagePreviewSizeMultiplier = useAsPagePicker ? 0.5f : 0.7f;
        mAdapter = new PageEditorAdapter(pagePreviewSizeMultiplier, mPageEditorAdapterListener);
        mAdapter.showMainPageIndicator(!useAsPagePicker);
        mAdapter.showMovePageIndicator(!useAsPagePicker);
        mRecyclerView.setAdapter(mAdapter);

        mMaxDragScroll = Math.round(mScreenUtils.dpToPx(getContext(), 3));
        mDragScrollThreshold = Math.round(mScreenUtils.dpToPx(getContext(), 100));
        mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback());
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        if (!useAsPagePicker) {
            SnapHelper helper = new PagerSnapHelper();
            helper.attachToRecyclerView(mRecyclerView);
            final Point screenSize = mScreenUtils.getScreenSize(getContext());
            final int padding = Math.round((screenSize.x * (pagePreviewSizeMultiplier / 2)) / 2);
            mRecyclerView.setPadding(padding, 0, padding, 0);
        }

        View addPageButton = view.findViewById(R.id.launcher_page_editor_add_page);
        addPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewPage();
            }
        });

        Button doneButton = view.findViewById(R.id.launcher_page_editor_done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doneWithEditing();
            }
        });
        if (useAsPagePicker) {
            doneButton.setText(R.string.launcher_page_editor_cancel);
            doneButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_close, 0, 0);
        } else {
            doneButton.setText(R.string.launcher_page_editor_done);
            doneButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_done, 0, 0);
        }

        mRequestData = getArguments().getBundle(FRAGMENT_ARG_REQUEST_DATA);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureReadWallpaperPermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScreenshotUtils.cancelScheduledScreenshot();
        EventBus.getDefault().register(this);
        mAdapter.setPages(mLauncherModel.getPages());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_WALLPAPER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Update the adapter to show the current wallpaper in the items' backgrounds.
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadedEvent(LauncherModel.DataLoadedEvent event) {
        mAdapter.setPages(mLauncherModel.getPages());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPagesChangedEvent(LauncherModel.PagesChangedEvent event) {
        mAdapter.setPages(mLauncherModel.getPages());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageAddedEvent(LauncherModel.PageAddedEvent event) {
        mAdapter.addPage(event.pageData);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemPosition(event.pageData));
    }

    private void ensureReadWallpaperPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ApplistDialogs.messageDialog(
                        getActivity(),
                        getContext().getString(R.string.launcher_page_editor_permission_title),
                        getContext().getString(R.string.launcher_page_editor_permission_message),
                        new Runnable() {
                            @Override
                            public void run() {
                                ActivityCompat.requestPermissions(
                                        getActivity(),
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        PERMISSIONS_REQUEST_READ_WALLPAPER);
                            }

                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                // Nothing
                            }
                        });
            } else {
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_WALLPAPER);
            }
        }
    }

    private PageEditorAdapter.Listener mPageEditorAdapterListener = new PageEditorAdapter.Listener() {
        @Override
        public void onHomeTapped(int position) {
            setMainPage(position);
        }

        @Override
        public void onRemoveTapped(int position) {
            removePage(position);
        }

        @Override
        public void onPageMoverTouched(int position, RecyclerView.ViewHolder viewHolder) {
            mItemTouchHelper.startDrag(viewHolder);
        }

        @Override
        public void onPageTapped(int position, RecyclerView.ViewHolder viewHolder) {
            handlePageTapped(position);
        }
    };

    private void addNewPage() {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mLauncherModel.addPage(new PageData(System.currentTimeMillis(), PageData.TYPE_WIDGET_PAGE, false));
                return null;
            }
        });
    }

    private void setMainPage(final int position) {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mLauncherModel.setMainPage(position);
                return null;
            }
        });
    }

    private void removePage(final int position) {
        final PageData pageData = mAdapter.getItem(position);
        final long pageId = pageData.getId();
        final String screenshotPath = mScreenshotUtils.createScreenshotPath(getContext(), pageId);
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.launcher_page_editor_remove_dialog_title)
                .setMessage(R.string.launcher_page_editor_remove_dialog_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final AppWidgetHost appWidgetHost = ((MainActivity)getActivity()).getAppWidgetHost();
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mScreenshotUtils.deleteScreenshot(screenshotPath);
                                mWidgetModel.deleteWidgetsOfPage(pageId, appWidgetHost);
                                mLauncherModel.removePage(position);
                                mLauncherStateManager.clearPageVisible(pageId);
                                return null;
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Nothing.
                    }
                })
                .setCancelable(true)
                .create();
        alertDialog.show();
    }

    private void handlePageTapped(int position) {
        final PageData pageData = mAdapter.getItem(position);
        EventBus.getDefault().post(new PageTappedEvent(mRequestData, pageData));
    }

    private void doneWithEditing() {
        EventBus.getDefault().post(new DoneEvent());
    }

}
