package zitsp.revisiontrace.git;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import zitsp.privateutil.Charsets;

public class GitDiffTrace {
    
    private GitDiffTrace() {
    }

    public static List<DiffEntry> trimExtraExtensionFiles(List<DiffEntry> diffEntries, String extension) {
        if (extension == null || extension.equals("")) {
            return diffEntries;
        }
        List<DiffEntry> list = new ArrayList<>();
        diffEntries.forEach(e -> {
            if (e.getNewPath().endsWith(extension)) {
                list.add(e);
            }
        });
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list;
        }
    }

    
    public static boolean outputDiff(DiffEntry diffEntry, Path repoPath, OutputStream outStream) {
        return (diffEntry != null) ? outputDiff(Arrays.asList(diffEntry), repoPath, outStream) : false;
    }
    
    public static boolean outputDiff(List<DiffEntry> diffEntries, Path repoPath, OutputStream outStream) {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        return (diffEntries != null && !diffEntries.isEmpty()) ? outputDiff(diffEntries, repositoryDir, outStream) : false;
    }

    public static boolean outputDiff(List<DiffEntry> diffEntries, Optional<Path> gitDir, OutputStream outStream) {
        if (gitDir.isPresent() && diffEntries != null && !diffEntries.isEmpty()) {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir.get().toFile()).build();
                    DiffFormatter diffFormatter = new DiffFormatter(outStream)) {
                diffFormatter.setRepository(repository);
                diffFormatter.format(diffEntries);
                return true;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
    
    public static boolean outputDiff(DiffEntry diffEntry, Optional<Path> gitDir, OutputStream outStream) {
        if (gitDir.isPresent() && diffEntry != null) {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir.get().toFile()).build();
                    DiffFormatter diffFormatter = new DiffFormatter(outStream)) {
                diffFormatter.setRepository(repository);
                diffFormatter.format(diffEntry);
                return true;
            } catch (MissingObjectException exception) {
                Path newPath = Paths.get(gitDir.get().toAbsolutePath().getParent().toString(), diffEntry.getNewPath());
                try (BufferedReader reader = Files.newBufferedReader(newPath, Charsets.getDefault())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String addLine = new StringBuffer("+").append(line).toString();
                        outStream.write(addLine.getBytes());
                    }
                    outStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
    

    public static boolean outputDiffToFile(DiffEntry diffEntry, Path repoPath, Path outPath) {
        return (diffEntry != null) ? outputDiffToFile(Arrays.asList(diffEntry), repoPath, outPath) : false;
    }
    
    public static boolean outputDiffToFile(List<DiffEntry> diffEntries, Path repoPath, Path outPath) {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        return (diffEntries != null && !diffEntries.isEmpty()) ? outputDiffToFile(diffEntries, repositoryDir, outPath) : false;
    }

    public static boolean outputDiffToFile(List<DiffEntry> diffEntries, Optional<Path> gitPath, Path outPath) {
        Optional<Path> outFile = Optional.ofNullable(outPath.toAbsolutePath());
        if (outFile.isPresent() && Files.notExists(outFile.get())) {
            try {
                Files.createFile(outFile.get());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        if (gitPath.isPresent() && outFile.isPresent()) {
            try (OutputStream outStream = Files.newOutputStream(outFile.get())) {
                return outputDiff(diffEntries, gitPath, outStream);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    private static final String GITDIFFTRACE_TMPFILE_PREFIX = "GITDIFFTRACE-";
    private static final int BUFFER_SIZE = 512 * 1024;

    public static ArrayList<String> getDiffText(DiffEntry diffEntry, Path repoPath) {
        return (diffEntry != null) ? getDiffText(Arrays.asList(diffEntry), repoPath) : new ArrayList<>();
    }

    public static ArrayList<String> getDiffText(List<DiffEntry> diffEntries, Path repoPath) {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        return (diffEntries != null && !diffEntries.isEmpty()) ? getDiffText(diffEntries, repositoryDir) : new ArrayList<>();
    }

    public static ArrayList<String> getDiffText(List<DiffEntry> diffEntries, Optional<Path> gitDir) {
        ArrayList<String> doc = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try {
                Path tmpDiff = Files.createTempFile(GITDIFFTRACE_TMPFILE_PREFIX, null);
                try (OutputStream diffWriter = Files.newOutputStream(tmpDiff)) {
                    BufferedOutputStream buffWriter = new BufferedOutputStream(diffWriter, BUFFER_SIZE);
                    outputDiff(diffEntries, gitDir, buffWriter);
                    buffWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                doc.addAll(Files.readAllLines(tmpDiff, Charsets.getDefault()));
                Files.delete(tmpDiff);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
        return doc;
    }
}
