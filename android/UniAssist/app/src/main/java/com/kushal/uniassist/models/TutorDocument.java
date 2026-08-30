package com.kushal.uniassist.models;

public class TutorDocument {
    private int id;
    private String doc_type;
    private String file_path;
    private String uploaded_at;
    private String file_url;

    public TutorDocument(int id, String doc_type, String file_path, String uploaded_at, String file_url) {
        this.id = id;
        this.doc_type = doc_type;
        this.file_path = file_path;
        this.uploaded_at = uploaded_at;
        this.file_url = file_url;
    }

    public int getId() { return id; }
    public String getDocType() { return doc_type; }
    public String getFilePath() { return file_path; }
    public String getUploadedAt() { return uploaded_at; }
    public String getFileUrl() { return file_url; }
}
