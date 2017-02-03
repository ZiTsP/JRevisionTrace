package zitsp.revisiontrace.git;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitSessionInfo {

    protected final Optional<Path> root;
    protected final Optional<Path> gitDir;
    
    public GitSessionInfo(Path path) {
        gitDir = GitUtils.getGitDir(path);
        root = Optional.ofNullable(gitDir.orElse(null).toAbsolutePath().getParent());
    }
    
    public Optional<Path> getRepositoryRoot() {
        return this.root;
    }
    
    public Optional<Path> getGitDir() {
        return this.gitDir;
    }
    
    public ArrayList<RevCommit> getAllComits() {
        return GitLogTrace.getAllComits(this.gitDir);
    }
    
    public List<Ref> getAllTags() {
        return GitLogTrace.getAllTags(this.gitDir);
    }
    
    public List<DiffEntry> getHeadEntries() {
        return GitLogTrace.getHeadEntries(this.gitDir);
    }

    public List<DiffEntry> getWaitingEntries() {
        return GitLogTrace.getWaitingEntries(this.gitDir);
    }

    public boolean outPutDiff(DiffEntry entry, OutputStream outStream) {
        return (entry != null) ? GitDiffTrace.outputDiff(Arrays.asList(entry), this.gitDir, outStream) : false;
    }
    
    public boolean outPutDiff(List<DiffEntry> list, OutputStream outStream) {
        return GitDiffTrace.outputDiff(list, this.gitDir, outStream);
    }

    public Optional<RevCommit> getHeadCommit() {
        return GitUtils.getHeadCommit(this.gitDir.get());
    }

    /*
     * Extensions for RemoteRepository
     * 
    private Optional<String> url = Optional.empty();
    private Optional<String> auther = Optional.empty();
    private Optional<UsernamePasswordCredentialsProvider> authenticate = Optional.empty();

    public void addUrl(String url) {
        this.url = Optional.ofNullable(url);
    }
    public String getUrl() {
        return this.url.orElse("");
    }
    public void addAuthenticate(String auther, String pass) {
        this.auther = Optional.ofNullable(auther);
        this.authenticate = Optional.of(new UsernamePasswordCredentialsProvider(auther,pass));
    }

    public String getAuthName() {
        return this.auther.orElse("");
    }
    
    public Optional<UsernamePasswordCredentialsProvider> getAuthenticate() {
        return this.authenticate;
    }
    
    public class Clone {
        public Clone(Path localPath, String url) {
            
        }
    }
    */
}
