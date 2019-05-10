package net.feheren_fekete.applist.launcher.pageeditor;

import android.Manifest;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.signature.ObjectKey;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.ScreenshotUtils;
import net.feheren_fekete.applist.launcher.repository.database.LauncherPageData;
import net.feheren_fekete.applist.utils.ScreenUtils;
import net.feheren_fekete.applist.utils.glide.GlideApp;

import java.io.File;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import static org.koin.java.KoinJavaComponent.get;

public class PageEditorAdapter extends RecyclerView.Adapter<PageEditorAdapter.PageViewHolder> {

    private ScreenUtils mScreenUtils = get(ScreenUtils.class);
    private ScreenshotUtils mScreenshotUtils = get(ScreenshotUtils.class);

    private Listener mListener;
    private List<LauncherPageData> mPages = Collections.emptyList();
    private @Nullable Drawable mWallpaper;
    private float mPagePreviewSizeMultiplier;

    private boolean mShowMovePageIndicator = true;
    private boolean mShowMainPageIndicator = true;

    public interface Listener {
        void onHomeTapped(int position);
        void onRemoveTapped(int position);
        void onPageMoverTouched(int position, RecyclerView.ViewHolder viewHolder);
        void onPageTapped(int position, RecyclerView.ViewHolder viewHolder);
    }

    public class PageViewHolder extends RecyclerView.ViewHolder {
        public ViewGroup layout;
        public ImageView wallpaper;
        public ImageView screenshot;
        public ImageView homeIcon;
        public ImageView removeIcon;
        public TextView pageNumber;
        public PageViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.launcher_page_editor_item_layout);
            wallpaper = itemView.findViewById(R.id.launcher_page_editor_item_wallpaper);
            screenshot = itemView.findViewById(R.id.launcher_page_editor_item_screenshot);
            homeIcon = itemView.findViewById(R.id.launcher_page_editor_item_home_icon);
            removeIcon = itemView.findViewById(R.id.launcher_page_editor_item_remove_icon);
            pageNumber = itemView.findViewById(R.id.launcher_page_editor_item_page_number);

            // REF: 2017_07_10_set_page_editor_root_layout
            final Point screenSize = mScreenUtils.getScreenSize(layout.getContext());
            ViewGroup.LayoutParams rootLayoutParams = layout.getLayoutParams();
            rootLayoutParams.width = Math.round(screenSize.x * mPagePreviewSizeMultiplier);
            rootLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            layout.setLayoutParams(rootLayoutParams);

            // We add an extra multiplier to have margins between the pages.
            final float marginMultiplier = 0.9f;

            // REF: 2017_07_10_set_page_editor_wallpaper_layout
            RelativeLayout.LayoutParams imageLayoutParams = (RelativeLayout.LayoutParams) wallpaper.getLayoutParams();
            imageLayoutParams.width = Math.round(screenSize.x * mPagePreviewSizeMultiplier * marginMultiplier);
            imageLayoutParams.height = Math.round(screenSize.y * mPagePreviewSizeMultiplier * marginMultiplier);
            wallpaper.setLayoutParams(imageLayoutParams);

            // REF: 2017_07_10_set_page_editor_screenshot_layout
            imageLayoutParams = (RelativeLayout.LayoutParams) screenshot.getLayoutParams();
            imageLayoutParams.width = Math.round(screenSize.x * mPagePreviewSizeMultiplier * marginMultiplier);
            imageLayoutParams.height = Math.round(screenSize.y * mPagePreviewSizeMultiplier * marginMultiplier);
            screenshot.setLayoutParams(imageLayoutParams);

            homeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mShowMainPageIndicator) {
                        mListener.onHomeTapped(getAdapterPosition());
                    }
                }
            });
            removeIcon.setOnClickListener(v -> mListener.onRemoveTapped(getAdapterPosition()));
            pageNumber.setOnTouchListener((v, event) -> {
                if (mShowMovePageIndicator) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mListener.onPageMoverTouched(
                                getAdapterPosition(), PageViewHolder.this);
                    }
                }
                return false;
            });
            screenshot.setOnClickListener(view -> mListener.onPageTapped(
                    getAdapterPosition(), PageViewHolder.this));
        }
    }

    public PageEditorAdapter(float pagePreviewSizeMultiplier, Listener listener) {
        mPagePreviewSizeMultiplier = pagePreviewSizeMultiplier;
        mListener = listener;
    }

    public void showMainPageIndicator(boolean show) {
        mShowMainPageIndicator = show;
        notifyDataSetChanged();
    }

    public void showMovePageIndicator(boolean show) {
        mShowMovePageIndicator = show;
        notifyDataSetChanged();
    }

    public void setPages(List<LauncherPageData> pages) {
        mPages = pages;
        notifyDataSetChanged();
    }

    public LauncherPageData getItem(int position) {
        return mPages.get(position);
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mPages, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mPages, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public long getItemId(int position) {
        return mPages.get(position).getId();
    }

    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.launcher_page_editor_item, parent, false);
        return new PageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        LauncherPageData pageData = mPages.get(position);

        if (mShowMainPageIndicator) {
            holder.homeIcon.setVisibility(View.VISIBLE);
            if (pageData.isMainPage()) {
                holder.homeIcon.setBackgroundResource(R.drawable.page_editor_button_left_corner_selected);
            } else {
                holder.homeIcon.setBackgroundResource(R.drawable.page_editor_button_left_corner);
            }
        } else {
            holder.homeIcon.setVisibility(View.GONE);
        }

        holder.removeIcon.setVisibility(
                (pageData.getType() != LauncherPageData.TYPE_APPLIST_PAGE) ? View.VISIBLE : View.GONE);

        String screenshotPath = mScreenshotUtils.createScreenshotPath(holder.screenshot.getContext(), pageData.getId());
        File file = new File(screenshotPath);
        if (file.exists()) {
            GlideApp.with(holder.screenshot.getContext())
                    .load(file)
                    .signature(new ObjectKey(String.valueOf(file.lastModified())))
                    .into(holder.screenshot);
        } else {
            holder.screenshot.setImageDrawable(null);
        }
        if (mWallpaper == null) {
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(holder.wallpaper.getContext());
            WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
            if (wallpaperInfo != null) {
                // User has a live wallpaper. We don't know how to capture the live wallpaper,
                // so we just use a constant color bitmap as a placeholder for now.
                final Bitmap bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bmp);
                canvas.drawColor(0xbb5d1e66);
                mWallpaper = new BitmapDrawable(holder.wallpaper.getResources(), bmp);
            } else if (ContextCompat.checkSelfPermission(
                    holder.screenshot.getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mWallpaper = wallpaperManager.getDrawable();
            }
        }
        holder.wallpaper.setImageDrawable(mWallpaper);

        holder.pageNumber.setText(String.valueOf(position + 1));
        // REF: 2017_07_06_set_pagenumber_drawable
        if (mShowMovePageIndicator) {
            holder.pageNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_move, 0);
        } else {
            holder.pageNumber.setCompoundDrawables(null, null, null, null);
        }
    }

    @Override
    public int getItemCount() {
        return mPages.size();
    }

}
