/*
 *    Copyright 2020 Paul Hagedorn (Panzer1119)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
