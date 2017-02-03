package zitsp.revisiontrace.svn;

import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnSessionInfo {

    private final Optional<String> url;
    private final Optional<String> auther;
    private final Optional<SVNURL> svnUrl;
    private final Optional<ISVNAuthenticationManager> authenticate;
    
    public SvnSessionInfo(String url, String name, String pwd) {
        SVNURL tmp;
        try {
            tmp = SVNURL.parseURIEncoded(url);
        } catch (SVNException e) {
            tmp = null;
            e.printStackTrace();
        }
        this.url = Optional.ofNullable(url);
        this.svnUrl = Optional.ofNullable(tmp);
        this.auther = Optional.ofNullable(name);
        this.authenticate = Optional.ofNullable(SVNWCUtil.createDefaultAuthenticationManager(name, pwd.toCharArray()));
    }
    
    public Optional<SVNURL> getUrl() {
        return this.svnUrl;
    }
    
    public String getUrlString() {
        return this.url.orElse("");
    }
    
    public Optional<ISVNAuthenticationManager> getAuthenticate() {
        return this.authenticate;
    }
    
    public String getAuthName() {
        return this.auther.orElse("");
    }

    public List<String> getLastCommitedEntries() {
        return SvnLogTrace.getLastCommitedEntries(this);
    }

    public List<String> getCacheEntries() {
        return SvnLogTrace.getCacheEntries(this);
    }

    public boolean outPutDiff(String filePath, long revNum, OutputStream outStream) {
        return (revNum > 1) ? SvnDiffTrace.getFileDiff(this, filePath, revNum -1 , revNum, outStream) : false;
    }
}
