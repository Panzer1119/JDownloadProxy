package de.codemakers.jdownloadproxy.download;

public enum DownloadStatus {
    
    QUEUED(false, false),
    CHECKING(false, true),
    DOWNLOADING(false, true),
    FINISHED(true, false),
    ERRORED(true, false),
    UNKNOWN(true, false);
    
    private final boolean done;
    private final boolean locked;
    
    DownloadStatus(boolean done, boolean locked) {
        this.done = done;
        this.locked = locked;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
}
