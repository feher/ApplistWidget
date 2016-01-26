package net.feheren_fekete.applistwidget.model;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PageData {
    private String mName;
    private List<SectionData> mSections;

    public PageData(String name, List<SectionData> sections) {
        mName = name;
        mSections = sections;
    }

    public String getName() {
        return mName;
    }

    public List<SectionData> getSections() {
        return mSections;
    }

    public void setSections(List<SectionData> sections) {
        mSections = sections;
    }

    public boolean hasSection(String sectionName) {
        return getSection(sectionName) != null;
    }

    public @Nullable SectionData getSection(String sectionName) {
        for (SectionData section : mSections) {
            if (section.getName().equals(sectionName)) {
                return section;
            }
        }
        return null;
    }

    public boolean hasApp(AppData app) {
        for (SectionData section : mSections) {
            if (section.hasApp(app)) {
                return true;
            }
        }
        return false;
    }

    public void addSection(SectionData section) {
        // Always add to the beginning of the list.
        mSections.add(0, section);
    }

    public void removeSection(String sectionName) {
        List<SectionData> remainingSections = new ArrayList<>();
        for (SectionData section : mSections) {
            if (!section.getName().equals(sectionName)) {
                remainingSections.add(section);
            }
        }
        mSections = remainingSections;
    }

}
