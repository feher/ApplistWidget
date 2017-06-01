package net.feheren_fekete.applist.model;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class WidgetModel {

    public static final class DataLoadedEvent {}
    public static class WidgetEvent {
        public final WidgetData widgetData;
        private WidgetEvent(WidgetData widgetData) {
            this.widgetData = widgetData;
        }
    }
    public static final class WidgetAddedEvent extends WidgetEvent {
        private WidgetAddedEvent(WidgetData widgetData) {
            super(widgetData);
        }
    }
    public static final class WidgetDeletedEvent extends WidgetEvent {
        private WidgetDeletedEvent(WidgetData widgetData) {
            super(widgetData);
        }
    }

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

    public void addWidget(WidgetData widgetData) {
        synchronized (this) {
            mWidgets.add(widgetData);
            EventBus.getDefault().post(new WidgetAddedEvent(widgetData));
        }
    }

    public void deleteWidget(WidgetData widgetData) {
        synchronized (this) {
            for (WidgetData widget : mWidgets) {
                if (widget.getProviderPackage().equals(widgetData.getProviderPackage())
                        && widget.getProviderClass().equals(widgetData.getProviderClass())) {
                    mWidgets.remove(widget);
                    EventBus.getDefault().post(new WidgetDeletedEvent(widgetData));
                    return;
                }
            }
        }
    }

}
