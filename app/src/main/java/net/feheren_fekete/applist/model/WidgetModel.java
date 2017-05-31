package net.feheren_fekete.applist.model;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class WidgetModel {

    public static final class DataLoadedEvent {}

    private List<WidgetData> mWidgets = new ArrayList<>();

    public void loadData() {
        synchronized (this) {
            // TODO
            EventBus.getDefault().postSticky(new DataLoadedEvent());
        }
    }

    public List<WidgetData> getWidgets() {
        synchronized (this) {
            // Return a copy to avoid messing up the data by the caller.
            List<WidgetData> result = new ArrayList<>();
            for (WidgetData widgetData : mWidgets) {
                result.add(new WidgetData(widgetData));
            }
            return result;
        }
    }

}
