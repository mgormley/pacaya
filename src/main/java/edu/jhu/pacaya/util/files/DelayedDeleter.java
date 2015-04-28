package edu.jhu.pacaya.util.files;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;

public class DelayedDeleter implements Serializable {

    private static final long serialVersionUID = -8740421458704363087L;

    private int numItemsToDelay;
    private LinkedList<File> queue;
    
    public DelayedDeleter(int numItemsToDelay) {
        this.numItemsToDelay = numItemsToDelay;
        queue = new LinkedList<File>();
    }
    
    public void delayedDelete(File file) {
        queue.addLast(file);
        if (queue.size() > numItemsToDelay) {
            File head = queue.remove();
            Files.deleteRecursively(head);
        }
    }

}
