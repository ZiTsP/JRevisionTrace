package zitsp.revisiontrace.common;

import java.nio.file.Path;
import java.util.Optional;

import zitsp.revisiontrace.git.GitUtils;
import zitsp.revisiontrace.svn.SvnUtils;

public class RepositoryCheck {
    
    private RepositoryCheck() {
    }
    
    public static Optional<REPOSITORY_TYPE> getType(Path path) {
        if (GitUtils.isGitRootDir(path)) {
            return Optional.of(REPOSITORY_TYPE.GIT);
        } else if (SvnUtils.isSvnRootDir(path)) {
            return Optional.of(REPOSITORY_TYPE.SVN);
        }
        return Optional.empty();
    }

}
