package net.feheren_fekete.applistwidget.viewmodel;


public class SectionItem extends BaseItem {
    private String mName;

    public SectionItem(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
