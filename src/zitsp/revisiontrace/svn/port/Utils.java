package zitsp.revisiontrace.svn.port;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;

public final class Utils {
    
    private Utils() {
    }

    protected static final Path SVN_LOCAL_DIR_PATH = Paths.get(".svn");
    
    protected static final boolean isSvnRootDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            try {
                return Files.list(path).anyMatch(e -> (Files.isDirectory(e) && e.getFileName().equals(SVN_LOCAL_DIR_PATH)));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
    
    protected static final Optional<Path> getSvnDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            if (path.getFileName().equals(SVN_LOCAL_DIR_PATH)) {
                return Optional.of(path.getParent());
            } else {
                return (isSvnRootDir(path)) ? Optional.of(path) : Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    protected static final Optional<SVNURL> getRemoteUrl(Path localPath) {
        Optional<Path> path = getSvnDir(localPath);
        if(path.isPresent()) {
            SVNStatus status;
            try {
                status = SVNClientManager.newInstance().getStatusClient().doStatus(localPath.toFile(), false);
                return Optional.of(status.getRemoteURL());
            } catch (SVNException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    protected static final OptionalLong getLatestRevision(SvnPortal info) {
        SVNRepository repository = null;
        try {
            repository = SVNRepositoryFactory.create(info.getUrl().get());
            if (info.getAuthenticate().isPresent()) {
                repository.setAuthenticationManager(info.getAuthenticate().get());
            }
            return OptionalLong.of(repository.getLatestRevision());
        } catch (SVNException e) {
            return OptionalLong.empty();
        } finally {
            repository.closeSession();
        }
    }

    protected static List<SvnDiffEntry> extractSpecificType(List<SvnDiffEntry> diffEntries, List<String> specificExtensions) {
        List<SvnDiffEntry> newList = new ArrayList<>();
        diffEntries.parallelStream().forEach(entry -> {
            specificExtensions.forEach(extension -> {
                if (entry.getFilePath().endsWith(extension)) {
                    newList.add(entry);
                }
            });
        });
        return newList;
    }
}
