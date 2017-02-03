package zitsp.revisiontrace.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public final class GitUtils {
    
    private GitUtils() {
    }

    public static final Path GIT_DIR_PATH = Paths.get(".git");
    
    public static final boolean isGitRootDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            try {
                return Files.list(path).anyMatch(e -> (Files.isDirectory(path) && e.getFileName().equals(GIT_DIR_PATH)));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    public static final Optional<Path> getGitDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            if (path.getFileName().equals(GIT_DIR_PATH)) {
                return Optional.of(path);
            } else {
                try {
                    return Files.list(path)
                            .filter(e -> (Files.isDirectory(e) && e.getFileName().equals(GIT_DIR_PATH)))
                            .findFirst();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }
    
    public static final Optional<Path> getRootGitDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            if (path.getFileName().equals(GIT_DIR_PATH)) {
                return Optional.of(path.getParent());
            } else {
                return (isGitRootDir(path) ? Optional.of(path) : Optional.empty());
            }
        }
        return Optional.empty();
    }

    public static Optional<RevCommit> getHeadCommit(Path repositoryPath) {
        return getRevCommit(repositoryPath, Constants.HEAD);
    }

    public static Optional<RevCommit> getHeadCaretICommit(Path repositoryPath) {
        return getRevCommit(repositoryPath, "HEAD^");
    }

    public static Optional<RevCommit> getRevCommit(Path repositoryPath, String resolveWord) {
        Optional<Path> gitDir = GitUtils.getGitDir(repositoryPath);
        return getRevCommit(gitDir, resolveWord);
    }
    
    public static Optional<RevCommit> getRevCommit(Optional<Path> gitDir, String resolveWord) {
        RevCommit commit = null;
        if (gitDir.isPresent()) {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir.get().toFile()).build();
                RevWalk revWalk = new RevWalk(repository)) {
                ObjectId id = repository.resolve(resolveWord);
                commit = revWalk.parseCommit(id);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return Optional.ofNullable(commit);
    }

    private static final String ID_COMMIT = "commit ";
    private static final String ID_ANYOBJECT = "AnyObjectId[";
    private static final int DEFAULT_ID_LENGTH = 9;
    
    public static Optional<String> limitIdLength(AnyObjectId objectId, int idLength) {
        String id = objectId.toString();
        if (id == null) {
            return Optional.empty();
        } else if (id.startsWith(ID_ANYOBJECT)) {
            return Optional.of(id.substring(ID_ANYOBJECT.length(), ID_ANYOBJECT.length() + idLength));
        } else if (id.startsWith(ID_COMMIT)) {
            return Optional.of(id.substring(ID_COMMIT.length(), ID_COMMIT.length() + idLength));
        } else {
            return Optional.empty();
        }
    }
    public static Optional<String> limitIdLength(AnyObjectId objectId) {
        return limitIdLength(objectId, DEFAULT_ID_LENGTH);
    }
    
    public static Optional<String> getTagNameFromRef(Ref tag) {
        Optional<String> name = Optional.ofNullable(tag.getName());
        if (name.isPresent()) {
            int index = name.get().lastIndexOf("/");
            return (index > 0) ? Optional.of(name.get().substring(index + 1)) : Optional.empty();
        }
        return Optional.empty();
    }

}
