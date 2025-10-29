package CompositeDesign.FileSystem;

public class File implements FileSystemIntf{
    String fileName;

    public File(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void ls() {
        System.out.println("File Name: "+fileName);
    }
}
