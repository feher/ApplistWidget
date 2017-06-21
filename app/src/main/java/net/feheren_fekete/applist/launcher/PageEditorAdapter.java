package net.feheren_fekete.applist.launcher;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.signature.ObjectKey;

import net.feheren_fekete.applist.R;
import net.feheren_fekete.applist.launcher.model.PageData;
import net.feheren_fekete.applist.utils.ScreenshotUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class PageEditorAdapter extends RecyclerView.Adapter<PageEditorAdapter.PageViewHolder> {

    private ScreenshotUtils mScreenshotUtils;
    private Listener mListener;
    private List<PageData> mPages = Collections.emptyList();

    public interface Listener {
        void onPageTapped(int position);
    }

    public class PageViewHolder extends RecyclerView.ViewHolder {
        public ImageView screenshot;
        public ImageView homeIcon;
        public TextView pageNumber;
        public PageViewHolder(View itemView) {
            super(itemView);
            screenshot = (ImageView) itemView.findViewById(R.id.launcher_page_editor_item_screenshot);
            homeIcon = (ImageView) itemView.findViewById(R.id.launcher_page_editor_item_home_icon);
            pageNumber = (TextView) itemView.findViewById(R.id.launcher_page_editor_item_page_number);
            itemView.findViewById(R.id.launcher_page_editor_item_layout).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onPageTapped(getAdapterPosition());
                }
            });
        }
    }

    public PageEditorAdapter(ScreenshotUtils screenshotUtils, Listener listener) {
        mScreenshotUtils = screenshotUtils;
        mListener = listener;
    }

    public void setPages(List<PageData> pages) {
        mPages = pages;
        notifyDataSetChanged();
    }

    public void addPage(PageData page) {
        mPages.add(page);
        notifyItemInserted(mPages.size() - 1);
    }

    public PageData getItem(int position) {
        return mPages.get(position);
    }

    public List<PageData> getItems() {
        return mPages;
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
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.launcher_page_editor_item, parent, false);
        return new PageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        PageData pageData = mPages.get(position);
        holder.homeIcon.setVisibility(pageData.isMainPage() ? View.VISIBLE : View.GONE);
        String screenshotPath = mScreenshotUtils.createScreenshotPath(holder.screenshot.getContext(), pageData.getId());
        File file = new File(screenshotPath);
        if (file.exists()) {
            GlideApp.with(holder.screenshot.getContext())
                    .load(file)
                    .signature(new ObjectKey(String.valueOf(file.lastModified())))
                    .into(holder.screenshot);
        }
        holder.pageNumber.setText(String.valueOf(position + 1));
    }

    @Override
    public int getItemCount() {
        return mPages.size();
    }

}
