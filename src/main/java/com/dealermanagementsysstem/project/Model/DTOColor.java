package com.dealermanagementsysstem.project.Model;

public class DTOColor {
    private int ColorID;
    private String ColorName;
    private int VersionID;

    public DTOColor() {
    }

    public DTOColor(int colorID, String colorName, int versionID) {
        ColorID = colorID;
        ColorName = colorName;
        VersionID = versionID;
    }

    public int getColorID() {
        return ColorID;
    }

    public void setColorID(int colorID) {
        ColorID = colorID;
    }

    public String getColorName() {
        return ColorName;
    }

    public void setColorName(String colorName) {
        ColorName = colorName;
    }

    public int getVersionID() {
        return VersionID;
    }

    public void setVersionID(int versionID) {
        VersionID = versionID;
    }
}
