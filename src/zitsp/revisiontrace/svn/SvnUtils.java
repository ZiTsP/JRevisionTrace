package zitsp.revisiontrace.svn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNStatus;

public final class SvnUtils {

    private SvnUtils() {
    }
    

    public static final Path SVN_LOCAL_DIR_PATH = Paths.get(".svn");
    
    public static final boolean isSvnRootDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            try {
                return Files.list(path).anyMatch(e -> (Files.isDirectory(e) && e.getFileName().equals(SVN_LOCAL_DIR_PATH)));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
    
    public static final Optional<Path> getSvnDir(Path path) {
        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            if (path.getFileName().equals(SVN_LOCAL_DIR_PATH)) {
                return Optional.of(path.getParent());
            } else {
                return (isSvnRootDir(path)) ? Optional.of(path) : Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    public static final Optional<SVNURL> getRemoteUrl(Path localPath) {
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
    
    public static final long getLatestRevision(SvnSessionInfo info) {
        SVNRepository repository = null;
        try {
            repository = SVNRepositoryFactory.create(info.getUrl().get());
            repository.setAuthenticationManager(info.getAuthenticate().get());
            return repository.getLatestRevision();
        } catch (SVNException e) {
            return -1L;
        } finally {
            repository.closeSession();
        }
    }

    public static List<String> extractSpecificType(List<String> diffEntries, List<String> specificExtensions) {
        List<String> newList = new ArrayList<>();
        diffEntries.parallelStream().forEach(entry -> {
            specificExtensions.forEach(extension -> {
                if (entry.endsWith(extension)) {
                    newList.add(entry);
                }
            });
        });
        return newList;
    }
}
