package zitsp.revisiontrace.git;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

public class WorkDirDiff {

    private WorkDirDiff() {
    }

    public static List<DiffEntry> getWorkDirDiffEntries(Path repoPath) {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        List<DiffEntry> entries = new ArrayList<>();
        repositoryDir.ifPresent(repo -> {
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

    public static List<DiffEntry> getStagedDiffEntries(Path repoPath) {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        List<DiffEntry> entries = new ArrayList<>();
        repositoryDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build();
                    TreeWalk treeWalk = new TreeWalk(repository);
                    RevWalk revWalk = new RevWalk(repository);
                    DiffFormatter diffFormatter = new DiffFormatter(null)) {
//                try (ObjectReader reader = repository.newObjectReader()) {
                    DirCache cache = repository.readDirCache();
//                    try (TreeWalk treeWalk = new TreeWalk(repository); RevWalk revWalk = new RevWalk(repository)) {
                        treeWalk.setRecursive(true);
                        RevTree headTree = revWalk.parseTree(repository.resolve(Constants.HEAD));
                        treeWalk.addTree(headTree);
                        treeWalk.addTree(new DirCacheIterator(cache));
                        diffFormatter.setRepository(repository);
                      AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
                      DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
                        entries.addAll(diffFormatter.scan(treeIterator, dirCacheIterator));
//                        while (treeWalk.next()) {
//                            AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
//                            DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
//                            if (treeIterator != null && dirCacheIterator != null) {
////                                try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
//                                    diffFormatter.setRepository(repository);
//                                    entries.addAll(diffFormatter.scan(treeIterator, dirCacheIterator));
//                                    treeIterator.reset();
//                                    dirCacheIterator.reset();
//                                    break;
////                                }
//                            }
////                        }
////                    }
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
//        repositoryDir.ifPresent(repo -> {
//            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build();
//                    TreeWalk treeWalk = new TreeWalk(repository);
//                    RevWalk revWalk = new RevWalk(repository);
//                    DiffFormatter diffFormatter = new DiffFormatter(null)) {
////                try (ObjectReader reader = repository.newObjectReader()) {
//                    DirCache cache = repository.readDirCache();
////                    try (TreeWalk treeWalk = new TreeWalk(repository); RevWalk revWalk = new RevWalk(repository)) {
//                        treeWalk.setRecursive(true);
//                        RevTree headTree = revWalk.parseTree(repository.resolve(Constants.HEAD));
//                        treeWalk.addTree(headTree);
//                        treeWalk.addTree(new DirCacheIterator(cache));
//                        while (treeWalk.next()) {
//                            AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
//                            DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
//                            if (treeIterator != null && dirCacheIterator != null) {
////                                try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
//                                    diffFormatter.setRepository(repository);
//                                    entries.addAll(diffFormatter.scan(treeIterator, dirCacheIterator));
//                                    treeIterator.reset();
//                                    dirCacheIterator.reset();
//                                    break;
////                                }
//                            }
////                        }
////                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
        return entries;
    }
//    public static List<DiffEntry> getUnstagedDiffEntries(Path repoPath) {
//        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
//        List<DiffEntry> entries = new ArrayList<>();
//        repositoryDir.ifPresent(repo -> {
//            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
//                try (ObjectReader reader = repository.newObjectReader()) {
//                    DirCache cache = repository.readDirCache();
//                    try (TreeWalk treeWalk = new TreeWalk(repository); RevWalk revWalk = new RevWalk(repository)) {
//                        treeWalk.setRecursive(true);
//                        RevTree headTree = revWalk.parseTree(repository.resolve(Constants.HEAD));
//                        treeWalk.addTree(headTree);
//                        treeWalk.addTree(new DirCacheIterator(cache));
//                        FileTreeIterator workTreeIter = new FileTreeIterator(repository);
//                        treeWalk.addTree(workTreeIter);
//                        while (treeWalk.next()) {
//    //                        AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
//                            DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
//                            WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) treeWalk.getTree(2,WorkingTreeIterator.class);
//                            if (dirCacheIterator != null && workingTreeIterator != null) {
//                                try (DiffFormatter diffFormatter = new DiffFormatter(null)) {
//                                    diffFormatter.setRepository(repository);
//                                    entries.addAll(diffFormatter.scan(dirCacheIterator, workingTreeIterator));
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        return entries;
//    }
    
    public static List<DiffEntry> getUnstagedDiffEntries(Path repoPath)   {
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
        List<DiffEntry> entries = new ArrayList<>();
        repositoryDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build();
                    TreeWalk treeWalk = new TreeWalk(repository);
                    RevWalk revWalk = new RevWalk(repository);
                    DiffFormatter diffFormatter = new DiffFormatter(null)) {
                DirCache cache = repository.readDirCache();
                treeWalk.setRecursive(true);
                RevTree headTree = revWalk.parseTree(repository.resolve(Constants.HEAD));
                treeWalk.addTree(headTree);
                treeWalk.addTree(new DirCacheIterator(cache));
                FileTreeIterator workTreeIter = new FileTreeIterator(repository);
                treeWalk.addTree(workTreeIter);
                diffFormatter.setRepository(repository);
                DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
                WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) treeWalk.getTree(2,WorkingTreeIterator.class);
                entries.addAll(diffFormatter.scan(dirCacheIterator, workingTreeIterator));
//                while (treeWalk.next()) {
//                    DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
//                    WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) treeWalk.getTree(2,WorkingTreeIterator.class);
//                    if (dirCacheIterator != null && workingTreeIterator != null) {
//                        diffFormatter.setRepository(repository);
//                        entries.addAll(diffFormatter.scan(dirCacheIterator, workingTreeIterator));
//                        break;
//                    }
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return entries;
    }
    
    public static void main(String[] args) {
        Path path = Paths.get("C:/Users/zitsp/workspace/Git/JMetricsCounters");
        GitLogTrace.getWaitingEntries(path).forEach(System.out::println);
        getWorkDirDiffEntries(path).forEach(System.out::println);
    }

}
