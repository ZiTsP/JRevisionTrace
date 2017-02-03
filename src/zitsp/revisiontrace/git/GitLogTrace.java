package zitsp.revisiontrace.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

public class GitLogTrace {
    
    private GitLogTrace() {
    }

    public static final ArrayList<RevCommit> getAllComits(Path path) {
        Optional<Path> gitDir = GitUtils.getGitDir(path);
        return getAllComits(gitDir);
    }
    
    public static final ArrayList<RevCommit> getAllComits(Optional<Path> gitDir) {
        ArrayList<RevCommit> commits = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
                try (Git git = new Git(repository)) {
                    Iterable<RevCommit> allCommits = null;
                    try {
                        allCommits = git.log().all().call();
                    } catch (NullPointerException e) {
                        allCommits = git.log().call();
                    }
                    allCommits.forEach(e -> commits.add(e));
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return commits;
    }


    public static final List<Ref> getAllTags(Path path) {
        Optional<Path> gitDir = GitUtils.getGitDir(path);
        return getAllTags(gitDir);
    }
    
    public static final List<Ref> getAllTags(Optional<Path> gitDir) {
        List<Ref> tags = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
                try (Git git = new Git(repository)) {
                    tags.addAll(git.tagList().call());
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return tags;
    }


    public static final List<DiffEntry> getHeadEntries(Path repoPath) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getHeadEntries(gitDir);
    }
    
    public static final List<DiffEntry> getHeadEntries(Optional<Path> gitDir) {
        return getDiffEntries(gitDir, GitUtils.getHeadCommit(gitDir.orElse(null)).get());
    }

    
    public static final List<DiffEntry> getWaitingEntries(Path repoPath) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getWaitingEntries(gitDir);
    }
    
    public static final List<DiffEntry> getWaitingEntries(Optional<Path> gitDir) {
        List<DiffEntry> entries = new ArrayList<>();
        gitDir.ifPresent(repo -> {
             entries.addAll(WorkDirDiff.getStagedDiffEntries(repo));
             List<DiffEntry> list = WorkDirDiff.getUnstagedDiffEntries(repo);
             list.forEach(e -> {
                 if (!entries.contains(e)) {
                     entries.add(e);
                 }
             });
        });
        return entries;
    }
    
    
    public static final List<DiffEntry> getStagedDiffEntries(Path repoPath) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getStagedDiffEntries(gitDir);
    }
    
    public static final List<DiffEntry> getStagedDiffEntries(Optional<Path> gitDir) {
        List<DiffEntry> entries = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build();
                    TreeWalk treeWalk = new TreeWalk(repository);
                    RevWalk revWalk = new RevWalk(repository);
                    DiffFormatter diffFormatter = new DiffFormatter(null)) {
                DirCache cache = repository.readDirCache();
                treeWalk.setRecursive(true);
                treeWalk.addTree(revWalk.parseTree(repository.resolve(Constants.HEAD)));
                AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
                DirCacheIterator dirCacheIterator = new DirCacheIterator(cache);
                diffFormatter.setRepository(repository);
                entries.addAll(diffFormatter.scan(treeIterator, dirCacheIterator));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return entries;
    }
    

    public static final List<DiffEntry> getUnstagedDiffEntries(Path repoPath) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getUnstagedDiffEntries(gitDir);
    }
    
    public static final List<DiffEntry> getUnstagedDiffEntries(Optional<Path> gitDir) {
        List<DiffEntry> entries = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build();
                    DiffFormatter diffFormatter = new DiffFormatter(null)) {
                DirCache cache = repository.readDirCache();
                DirCacheIterator dirCacheIterator = new DirCacheIterator(cache);
                WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) new FileTreeIterator(repository);
                diffFormatter.setRepository(repository);
                entries.addAll(diffFormatter.scan(dirCacheIterator, workingTreeIterator));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return entries;
    }

    public static final List<DiffEntry> getDiffEntries(Path repoPath, RevCommit commit) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getDiffEntries(gitDir, commit);
    }
    
    public static final List<DiffEntry> getDiffEntries(Optional<Path> gitDir, RevCommit commit) {
        List<DiffEntry> diffEntries = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
                List<RevTree> fromTrees = new ArrayList<>();
                Arrays.stream(commit.getParents()).forEach(parent -> {
                    try (RevWalk revWalk = new RevWalk(repository)) {
                        ObjectId id = repository.resolve(parent.name());
                        RevCommit oldCommit = revWalk.parseCommit(id);
                        RevTree fromTree = oldCommit.getTree();
                        fromTrees.add(fromTree);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                RevTree toTree = commit.getTree();
                fromTrees.forEach(fromTree -> {
                    try (ObjectReader reader = repository.newObjectReader()) {
                        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                        oldTreeIter.reset(reader, fromTree);
                        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                        newTreeIter.reset(reader, toTree);
                        try (Git git = new Git(repository)) {
                            List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
                            diffEntries.addAll(diffs);
                        } catch (GitAPIException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return diffEntries;
    }


    public static final List<DiffEntry> getDiffEntries(Path repoPath, RevCommit oldCommit, RevCommit newCommit) {
        Optional<Path> gitDir = GitUtils.getGitDir(repoPath);
        return getDiffEntries(gitDir, oldCommit, newCommit);
    }

    public static final List<DiffEntry> getDiffEntries(Optional<Path> gitDir, RevCommit oldCommit, RevCommit newCommit) {
        List<DiffEntry> diffEntries = new ArrayList<>();
        gitDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
                RevTree toTree = oldCommit.getTree();
                RevTree fromTree = oldCommit.getTree();
                try (ObjectReader reader = repository.newObjectReader(); Git git = new Git(repository)) {
                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, fromTree);
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, toTree);
                    List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
                    diffEntries.addAll(diffs);
                } catch (GitAPIException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return diffEntries;
    }


}
