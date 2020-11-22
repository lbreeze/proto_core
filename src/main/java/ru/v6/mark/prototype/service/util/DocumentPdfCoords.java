package ru.v6.mark.prototype.service.util;

public class DocumentPdfCoords {

    private int llx;//lower left x of text area
    private int lly;//lower left y of text area
    private int urx;//upper right x of text area
    private int ury;//upper right y of text area
    private String description;


    public DocumentPdfCoords(int llx, int lly, int urx, int ury, String description) {
        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;
        this.description = description;
    }


    public int getLlx() {
        return llx;
    }

    public void setLlx(int llx) {
        this.llx = llx;
    }

    public int getLly() {
        return lly;
    }

    public void setLly(int lly) {
        this.lly = lly;
    }

    public int getUrx() {
        return urx;
    }

    public void setUrx(int urx) {
        this.urx = urx;
    }

    public int getUry() {
        return ury;
    }

    public void setUry(int ury) {
        this.ury = ury;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
