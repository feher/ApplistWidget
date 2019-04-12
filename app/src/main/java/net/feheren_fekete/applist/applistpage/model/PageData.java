package net.feheren_fekete.applist.applistpage.model;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

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

    public boolean hasSectionWithName(String sectionName) {
        for (SectionData section : mSections) {
            if (section.getName().equals(sectionName)) {
                return true;
            }
        }
        return false;
    }

    public @Nullable SectionData getSection(long sectionId) {
        for (SectionData section : mSections) {
            if (section.getId() == sectionId) {
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

    public boolean renameSection(long sectionId, String newSectionName) {
        SectionData section = getSection(sectionId);
        if (section != null) {
            section.setName(newSectionName);
            return true;
        }
        return false;
    }

    public boolean hasStartable(StartableData startableData) {
        for (SectionData section : mSections) {
            if (section.hasStartable(startableData)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public SectionData getSectionOfStartable(long startableId) {
        for (SectionData section : mSections) {
            if (section.hasStartable(startableId)) {
                return section;
            }
        }
        return null;
    }

    public boolean removeStartable(long startableId) {
        for (SectionData section : mSections) {
            if (section.removeStartable(startableId)) {
                return true;
            }
        }
        return false;
    }

    public void addSection(SectionData section) {
        // Always add to the beginning of the list.
        mSections.add(0, section);
    }

    public void removeSection(long sectionId) {
        List<SectionData> remainingSections = new ArrayList<>();
        for (SectionData section : mSections) {
            if (section.getId() != sectionId) {
                remainingSections.add(section);
            }
        }
        mSections = remainingSections;
    }

}
