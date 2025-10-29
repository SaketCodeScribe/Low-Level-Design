package CompositeDesign.FileSystem;

import java.util.ArrayList;
import java.util.List;

public class Directory implements FileSystemIntf{
    String fileName;
    List<FileSystemIntf> directories;

    public Directory(String fileName) {
        this.fileName = fileName;
        directories = new ArrayList<>();
    }

    @Override
    public void ls() {
        System.out.println("Directory Name: "+fileName);
        for(FileSystemIntf directory:directories)
            directory.ls();
    }

    public void add(FileSystemIntf object){
        directories.add(object);
    }

}
