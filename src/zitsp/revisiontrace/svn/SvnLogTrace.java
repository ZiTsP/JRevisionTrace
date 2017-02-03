package zitsp.revisiontrace.svn;

import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnLog;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnLogTrace {
    
    private SvnLogTrace() {
    }
    
    public static Long getLatestRevision(SvnSessionInfo info) {
        return SvnUtils.getLatestRevision(info);
    }
    
    public static List<String> getDiffEntries(SvnSessionInfo info, long oldRevNum, long newRevNum) {
        SVNRevision oldRev = SVNRevision.create(oldRevNum);
        SVNRevision newRev = SVNRevision.create(newRevNum);
        return getDiffEntries(info, oldRev, newRev);
    }
    
    public static List<String> getDiffEntries(SvnSessionInfo info, long newRevNum) {
        return (newRevNum > 1) ? getDiffEntries(info, newRevNum - 1, newRevNum) : new ArrayList<>();
    }

    public static List<String> getLastCommitedEntries(SvnSessionInfo info) {
        SVNRevision headHatRev = SVNRevision.create(SvnUtils.getLatestRevision(info) - 1);
        return getDiffEntries(info, headHatRev, SVNRevision.HEAD);
    }
    
    public static List<String> getCacheEntries(SvnSessionInfo info) {
        return getDiffEntries(info, SVNRevision.HEAD, SVNRevision.WORKING);
    }

    private static List<String> getDiffEntries(SvnSessionInfo info, SVNRevision oldRev, SVNRevision newRev) {
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        List<String> files = new ArrayList<>();
        try {
            svnOperationFactory.setAuthenticationManager(info.getAuthenticate().orElse(null));
            SvnTarget root = SvnTarget.fromURL(info.getUrl().get());
            SvnLog log = svnOperationFactory.createLog();
            log.setSingleTarget(root);
            log.addRange(SvnRevisionRange.create(oldRev, newRev));
            log.setUseMergeHistory(true);
            log.setDiscoverChangedPaths(true);
            SVNLogEntry entry = log.run();
            entry.getChangedPaths().entrySet().stream().parallel()
                .map(e -> e.getValue().getPath()).filter(e -> SvnLogTrace.hasExtension(e))
                .forEach(e -> {
                    StringBuffer tmp = new StringBuffer(info.getUrlString()).append(e);
                    files.add(tmp.toString());
                });
        } catch (SVNException e) {
            e.printStackTrace();
        } finally {
            svnOperationFactory.dispose();
        }
        return files;
    }
    
    private static boolean hasExtension(String path) {
        int index = path.lastIndexOf(".") - path.lastIndexOf("/");
        return (index <= 0) ? false : true;
    }
}
