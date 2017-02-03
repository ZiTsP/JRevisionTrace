package zitsp.revisiontrace.svn;

import java.io.File;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnDiffTrace {
    
    private SvnDiffTrace() {
    }

    public static boolean getDiff(SvnSessionInfo info,long oldRevNum, long newRevNum, OutputStream output) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        svnOperationFactory.setAuthenticationManager(info.getAuthenticate().get());
        try {
            SvnTarget root = SvnTarget.fromURL(info.getUrl().get());
            SVNRevision oldRev = SVNRevision.create(oldRevNum);
            SVNRevision newRev = SVNRevision.create(newRevNum);
            SvnTarget target = SvnTarget.fromURL(info.getUrl().get());
            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            final SvnDiff diff = svnOperationFactory.createDiff();
            diffGenerator.setRepositoryRoot(root);
            diff.setSource(target, oldRev, newRev);
            diff.setDiffGenerator(diffGenerator);
            diff.setUseGitDiffFormat(true);
            diff.setOutput(output);
            diff.run();
            return true;
        } catch (SVNException e) {
            e.printStackTrace();
            return false;
        } finally {
            svnOperationFactory.dispose();
        }
    }
    
    public static boolean getFileDiff(SvnSessionInfo info, String filePath ,long oldRevNum, long newRevNum, OutputStream output) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        svnOperationFactory.setAuthenticationManager(info.getAuthenticate().get());
        try {
            SvnTarget root = SvnTarget.fromURL(info.getUrl().get());
            SVNRevision oldRev = SVNRevision.create(oldRevNum);
            SVNRevision newRev = SVNRevision.create(newRevNum);
            SVNURL fileUrl = info.getUrl().get().appendPath(filePath, true);
            SvnTarget target = SvnTarget.fromURL(fileUrl);
            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            final SvnDiff diff = svnOperationFactory.createDiff();
            diffGenerator.setRepositoryRoot(root);
            diff.setSource(target, oldRev, newRev);
            diff.setDiffGenerator(diffGenerator);
            diff.setUseGitDiffFormat(true);
            diff.setOutput(output);
            diff.run();
            return true;
        } catch (SVNException e) {
            e.printStackTrace();
            return false;
        } finally {
            svnOperationFactory.dispose();
        }
    }

//    public static void main(String[] args) {
//        String url = "http://192.168.56.101/repos/sample";
//        String url2 = "http://192.168.56.101/repos/sample/trunk/src/OpenView.java";
//        String name = "apache";
//        String password = "svn";
//        SvnSessionInfo info = new SvnSessionInfo(url, name, password);
//        getFileDiff(info, "trunk/src/HelloWorld.java", 5, 7, System.out);
//    }
}