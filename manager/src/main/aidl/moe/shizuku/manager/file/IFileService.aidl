package moe.shizuku.manager.file;

interface IFileService {
    void destroy();

    boolean exists(String path);

    boolean isDirectory(String path);

    List<String> listFiles(String path);

    String getDetailedFilesJson(String path);

    byte[] readFile(String path);

    boolean writeFile(String path, in byte[] data);

    boolean copy(String srcPath, String destPath);

    boolean move(String srcPath, String destPath);

    boolean delete(String path);

    boolean mkdirs(String path);

    long getFileSize(String path);
}
