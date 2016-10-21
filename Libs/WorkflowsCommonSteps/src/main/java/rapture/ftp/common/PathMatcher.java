package rapture.ftp.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableSet;

class PathMatcher implements FileVisitor<Path> {

    String pattern = null;
    Path root = null;
    List<String> results;
    int depth;

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public PathMatcher(String pattern) {
        this.pattern = pattern;
        File file = new File(pattern);
        depth = 0;
        while (!file.exists()) {
            depth++;
            file = file.getParentFile();
        }
        root = file.toPath();
        results = new ArrayList<>();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        FileVisitResult ret = FileVisitResult.CONTINUE;
        return ret;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String path = file.toFile().getAbsolutePath();
        if (path.matches(pattern)) results.add(path);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    public Path getPath() {
        return root;
    }

    public List<String> getResults() throws IOException {
        Files.walkFileTree(root, ImmutableSet.of(FileVisitOption.FOLLOW_LINKS), depth, this);
        return results;
    }
}
