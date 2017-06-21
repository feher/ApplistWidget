package net.feheren_fekete.applist.launcher;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.model.LauncherModel;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.launcherpage.model.WidgetModel;
import net.feheren_fekete.applist.utils.ScreenshotUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Callable;

import bolts.Task;

public class PageEditorFragment extends Fragment implements PageEditorAdapter.Listener {

    public static final class DoneEvent {}

    private LauncherModel mLauncherModel;
    private WidgetModel mWidgetModel;
    private PageEditorAdapter mAdapter;
    private ScreenshotUtils mScreenshotUtils;

    private class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
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
                    ((PageEditorAdapter.PageViewHolder) viewHolder).screenshot.animate()
                            .scaleX(0.9f).scaleY(0.9f).setDuration(150).start();
                }
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            ((PageEditorAdapter.PageViewHolder) viewHolder).screenshot.animate()
                    .scaleX(1.0f).scaleY(1.0f).setDuration(150).start();

            // This is needed to re-draw (re-bind) all the items in the RecyclerView.
            // We want to update the page numbers of every item.
            mAdapter.notifyDataSetChanged();
        }
    }

    public PageEditorFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLauncherModel = LauncherModel.getInstance();
        mWidgetModel = WidgetModel.getInstance();
        mScreenshotUtils = new ScreenshotUtils();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.launcher_page_editor_fragment, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.launcher_page_editor_page_list);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        mAdapter = new PageEditorAdapter(mScreenshotUtils, this);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback());
        touchHelper.attachToRecyclerView(recyclerView);

        ImageView addPageButton = (ImageView) view.findViewById(R.id.launcher_page_editor_add_page);
        addPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewPage();
            }
        });

        ImageView doneButton = (ImageView) view.findViewById(R.id.launcher_page_editor_done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doneWithEditing();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        mAdapter.setPages(mLauncherModel.getPages());
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPageTapped(final int position) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setItems(R.array.launcher_page_editor_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                setMainPage(position);
                                break;
                            case 1:
                                removePage(position);
                                break;
                        }
                    }
                })
                .setCancelable(true)
                .create();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        alertDialog.show();
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
    }

    private void addNewPage() {
        Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mLauncherModel.addPage(new PageData(System.currentTimeMillis(), PageData.TYPE_LAUNCHER_PAGE, false));
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
        if (pageData.getType() != PageData.TYPE_APPLIST_PAGE) {
            Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mScreenshotUtils.deleteScreenshot(screenshotPath);
                    mWidgetModel.deleteWidgetsOfPage(pageId);
                    mLauncherModel.removePage(position);
                    return null;
                }
            });
        } else {
            Toast.makeText(
                    getContext(),
                    R.string.launcher_page_editor_cannot_remove_apps_page,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void doneWithEditing() {
        EventBus.getDefault().post(new DoneEvent());
    }

}
