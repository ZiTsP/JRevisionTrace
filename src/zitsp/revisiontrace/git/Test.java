package zitsp.revisiontrace.git;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
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

public class Test {

    static void testcode(Path repoPath){
        System.out.println(repoPath);
        Optional<Path> repositoryDir = GitUtils.getGitDir(repoPath);
//        repositoryDir = Optional.of(Paths.get("C:/Users/zitsp/workspace/Git/JMetricsCounters/"));
        
        System.out.println(repositoryDir.orElse(Paths.get("HOGE")));
        repositoryDir.ifPresent(repo -> {
            try (Repository repository = new FileRepositoryBuilder().setGitDir(repo.toFile()).build()) {
//                try (Git git = new Git(repository)) {
//                    List<DiffEntry> diffs = git.diff().call();
//                    System.out.println(diffs.isEmpty());
//                } catch (GitAPIException e) {
//                    e.printStackTrace();
//                }


                try (ObjectReader reader = repository.newObjectReader()) {

                    RevTree fromTree = GitUtils.getHeadCaretICommit(repo).get().getTree();
                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, fromTree);
                    RevTree toTree = GitUtils.getHeadCommit(repo).get().getTree();

                    try (RevWalk revWalk = new RevWalk(repository)) {
                        toTree = revWalk.parseTree(toTree.getId());
                    }
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, toTree.getId());
                    AbstractTreeIterator workbeforeTree = null;
                    try {
                        workbeforeTree = prepareTreeParser(repository, Constants.HEAD);
                    } catch (Exception e3) {
                        // TODO Auto-generated catch block
                        e3.printStackTrace();
                    }
                    FileTreeIterator workTreeIter = new FileTreeIterator(repository);

                    RevTree tree = new RevWalk(repository).parseTree(repository.resolve(Constants.HEAD));
                    TreeWalk treeWalk = new TreeWalk(repository);
                    Throwable localThrowable2 = null;
                        treeWalk.setRecursive(true);
                        treeWalk.addTree(tree);
                        DirCache dc = repository.readDirCache();
                        treeWalk.addTree(new DirCacheIterator(dc));
                        FileTreeIterator workTreeIter2 = new FileTreeIterator(repository);
                        treeWalk.addTree(workTreeIter2);
                        
//                            treeWalk.addTree(new EmptyTreeIterator());
//                        treeWalk.addTree(new DirCacheIterator(dc));
                        System.out.println(treeWalk.getTreeCount());
//                        AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);
//
                        treeWalk.next();
                        DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
                        WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) treeWalk.getTree(2,WorkingTreeIterator.class);
                        TreeWalk newTreeWalk = new TreeWalk(repository);
                        RevWalk walk = new RevWalk(repository);
                        while (treeWalk.next()) {
                            AbstractTreeIterator treeIterator2 = treeWalk.getTree(0, AbstractTreeIterator.class);
                            DirCacheIterator dirCacheIterator2 = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);
                            WorkingTreeIterator workingTreeIterator2 = (WorkingTreeIterator) treeWalk.getTree(2,WorkingTreeIterator.class);
//                            if (treeIterator2 != null) {
                            if (workingTreeIterator2 != null) {
                                try (DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
                                    diffFormatter.setRepository(repository);
                                    List<DiffEntry> diffs = diffFormatter.scan(treeIterator2, dirCacheIterator2);
                                    treeIterator2.reset();
                                    treeIterator2.first();
                                    dirCacheIterator2.reset();
                                    dirCacheIterator2.first();
//                                    List<DiffEntry> diffs = diffFormatter.scan(dirCacheIterator2, workingTreeIterator2);
                                    
                                    diffs.stream().forEachOrdered(e -> {
                                        System.out.println(e + " ::: " +e.getNewId());
                                    });
                                }
                            }
                            if (treeIterator2 != null) {
                                System.out.println(">   " + treeWalk.getPathString()+treeIterator2.getEntryObjectId());
                            }
                            if (dirCacheIterator2 != null) {
                                System.out.println(">>  " + treeWalk.getPathString()+dirCacheIterator2.getEntryObjectId());
                            }
                            if (workingTreeIterator2 != null && dirCacheIterator2 != null && (workingTreeIterator2.isModified(dirCacheIterator2.getDirCacheEntry(), true,
                                    treeWalk.getObjectReader()))) {
                                System.out.println(">>> " + treeWalk.getPathString() + workingTreeIterator2.getEntryObjectId());
                                try (DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
                                    diffFormatter.setRepository(repository);
//                                    RevTree tree1 = walk.parseTree(treeIterator2.getEntryObjectId());
//                                    RevTree tree2 = walk.parseTree( workingTreeIterator2.getEntryObjectId());
                                    List<DiffEntry> diffs = diffFormatter.scan(dirCacheIterator2, workingTreeIterator2);

                                    treeIterator2.reset();
                                    treeIterator2.first();
                                    dirCacheIterator2.reset();
                                    dirCacheIterator2.reset();
                                    workingTreeIterator2.reset();
                                    workingTreeIterator2.first();
                                    diffs.stream().forEachOrdered(e -> {
                                        System.out.println(e + " :: " +e.getNewId());
                                    });
                                }
                            }
                        }
                        
                    try (DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
                        diffFormatter.setRepository(repository);
                        List<DiffEntry> diffs = diffFormatter.scan(dirCacheIterator, workingTreeIterator);
                        diffs = diffFormatter.scan(fromTree, toTree);
                        diffs.stream().forEachOrdered(e -> {
                            System.out.println(e + "MODIF&&&" +e.getNewId());
                        });
                        System.out.println(" ");
                        List<DiffEntry> diffs2 = diffFormatter.scan(workbeforeTree,workTreeIter);
                        diffs2.stream().forEachOrdered(e -> {
                            System.out.println(e + " " +e.getNewId());
                        });
                        System.out.println("XXXX");
//                        try (Git git = new Git(repository)) {
//                            List<DiffEntry> diffs3 = git.diff().setOldTree(treeIterator).setNewTree(dirCacheIterator).call();
//                            System.out.println("Tree & Cache :"+diffs3.isEmpty());
//                            diffs3.stream().forEach(System.out::println);
//                            diffs3 = git.diff().setOldTree(dirCacheIterator).setNewTree(workingTreeIterator).call();
//                            System.out.println("Cache & Work :"+diffs3.isEmpty());
//                            diffs3.stream().forEach(System.out::println);
//                            diffs3 = git.diff().setOldTree(treeIterator).setNewTree(workingTreeIterator).call();
//                            System.out.println("Tree & Work :"+diffs3.isEmpty());
//                            diffs3.stream().forEach(System.out::println);
////                            git.diff().setCached(true).setOldTree(newTreeIter).setOutputStream(System.out).call();
//                        } catch (Exception e1) {
//                            e1.printStackTrace();
//                        }
                        // indexdiffでRevisionと現在の差分が取れる
                        IndexDiff index = new IndexDiff(repository, Constants.HEAD, workTreeIter2);
                        
                        index.diff();
                        index.getModified().forEach(e -> {
                            System.out.println(e);
                        });
                        
                        
//                    }
//                    try (Git git = new Git(repository)) {
//                        List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
//                        diffEntries.addAll(diffs);
//                    } catch (GitAPIException e) {
//                        e.printStackTrace();
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
//                System.out.println(GitUtils.getHeadCommit(repo));
//                File wt = repository.getWorkTree();
//                DirCache cache = repository.readDirCache();
//                for (int j = 0 ; j < cache.getEntryCount() ; j++) {
//                    System.out.println(cache.getEntry(j));
//                }
//                System.out.println(cache.getEntryCount());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
    }
    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws Exception {
        Ref head = repository.getRef(ref);
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head.getObjectId());
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        ObjectReader oldReader = repository.newObjectReader();
        try {
            oldTreeParser.reset(oldReader, tree.getId());
        } finally {
            oldReader.close();
        }
        return oldTreeParser;
    }
    
    public static void main(String[] args) {
        // For FPF Java Version
//        int max = 1 << 12;
//        int array[][] = new int[max][max];
//        int array2[][] = new int[max][max];
//        System.out.println(max);
//        System.out.println(Integer.toHexString(max));
//        System.out.println(array.length);
//        System.out.println(array[max-1][max-1]);
        Path path = Paths.get("C:/Users/zitsp/workspace/Git/JPrivateUtils");
        int PIPEDOUTPUTSTREAM_BUFFER_SIZE = 512 * 10;
        GitLogTrace.getWaitingEntries(path).forEach(diffentry -> {
//        List<DiffEntry> diffentry = GitLogTrace.getWaitingEntries(path);

            try {
                PipedOutputStream pout = new PipedOutputStream();
                OutputStream out = new BufferedOutputStream(pout, PIPEDOUTPUTSTREAM_BUFFER_SIZE);

                PipedInputStream pin = new PipedInputStream(pout);
                BufferedReader  in = new BufferedReader(new InputStreamReader(pin));

//                GitLogTrace.getWaitingEntries(path).forEach(diffentry -> {
                    System.out.println("$$$$$$$$$$$$$$$$$$$$$" + diffentry);
                GitDiffTrace.outputDiff(diffentry, path, out);
                System.out.println("###################" + diffentry);
//                });
                out.write(0);
                out.flush();
                out.close();
                System.out.println("out close");
                pout.close();
                System.out.println("pout close");
                String line;
                while((line = in.readLine()) != null) {
                    System.out.println(line);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
//        testcode(path);
    }

}
