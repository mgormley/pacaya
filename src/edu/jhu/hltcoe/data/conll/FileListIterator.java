package edu.jhu.hltcoe.data.conll;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public abstract class FileListIterator<T> implements Iterator<T> {

    private Iterator<T> curIter;
    private int fileIdx = 0;
    private List<File> files;

    public FileListIterator(List<File> files) {
        this.files = files;
    }
    
    public abstract Iterator<T> getIteratorInstance(File file);

    private boolean nextFile() {
        do {
            if (fileIdx >= files.size()) {
                return false;
            }
            curIter = getIteratorInstance(files.get(fileIdx));
            fileIdx++;
        } while (curIter != null);         
        return true;
    }
    
    @Override
    public boolean hasNext() {
        while (curIter == null || !curIter.hasNext()) {
            if (!nextFile()) {
                return false;
            }
        }
        return curIter != null && curIter.hasNext();
    }

    @Override
    public T next() {
        hasNext();
        T item = curIter != null ? curIter.next() : null;
        return item;
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");        
    }
    
}