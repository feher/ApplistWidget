package net.feheren_fekete.applist.model;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PageData extends BaseData {
    private String mName;
    private List<SectionData> mSections;

    public PageData(long id, String name, List<SectionData> sections) {
        super(id);
        mName = name;
        mSections = sections;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
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

    public @Nullable SectionData getSectionByRemovable(boolean removable) {
        for (SectionData section : mSections) {
            if (section.isRemovable() == removable) {
                return section;
            }
        }
        return null;
    }

    public boolean renameSection(String oldSectionName, String newSectionName) {
        SectionData section = getSection(oldSectionName);
        if (section != null) {
            section.setName(newSectionName);
            return true;
        }
        return false;
    }

    public boolean hasApp(AppData app) {
        for (SectionData section : mSections) {
            if (section.hasApp(app)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeApp(AppData app) {
        for (SectionData section : mSections) {
            if (section.removeApp(app)) {
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
