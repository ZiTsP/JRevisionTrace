/*** Eclipse Class Decompiler plugin, copyright (c) 2012 Chao Chen (cnfree2000@hotmail.com) ***/
package zitsp.revisiontrace.git;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.SkipWorkTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class IndexDiff {
    private static final int TREE = 0;
    private static final int INDEX = 1;
    private static final int WORKDIR = 2;
    private final Repository repository;
    private final RevTree tree;

    public static abstract interface WorkingTreeIteratorFactory {
        public abstract WorkingTreeIterator getWorkingTreeIterator(Repository paramRepository);
    }

    public static enum StageState {
        BOTH_DELETED(1), ADDED_BY_US(2), DELETED_BY_THEM(3), ADDED_BY_THEM(4), DELETED_BY_US(5), BOTH_ADDED(
                6), BOTH_MODIFIED(7);

        private final int stageMask;

        private StageState(int stageMask) {
            this.stageMask = stageMask;
        }

        int getStageMask() {
            return this.stageMask;
        }

        public boolean hasBase() {
            return (this.stageMask & 0x1) != 0;
        }

        public boolean hasOurs() {
            return (this.stageMask & 0x2) != 0;
        }

        public boolean hasTheirs() {
            return (this.stageMask & 0x4) != 0;
        }

        static StageState fromMask(int stageMask) {
            switch (stageMask) {
            case 1:
                return BOTH_DELETED;
            case 2:
                return ADDED_BY_US;
            case 3:
                return DELETED_BY_THEM;
            case 4:
                return ADDED_BY_THEM;
            case 5:
                return DELETED_BY_US;
            case 6:
                return BOTH_ADDED;
            case 7:
                return BOTH_MODIFIED;
            }
            return null;
        }
    }

    private static final class ProgressReportingFilter extends TreeFilter {
        private final ProgressMonitor monitor;
        private int count = 0;
        private int stepSize;
        private final int total;

        private ProgressReportingFilter(ProgressMonitor monitor, int total) {
            this.monitor = monitor;
            this.total = total;
            this.stepSize = (total / 100);
            if (this.stepSize == 0) {
                this.stepSize = 1000;
            }
        }

        public boolean shouldBeRecursive() {
            return false;
        }

        public boolean include(TreeWalk walker)
                throws MissingObjectException, IncorrectObjectTypeException, IOException {
            this.count += 1;
            if (this.count % this.stepSize == 0) {
                if (this.count <= this.total) {
                    this.monitor.update(this.stepSize);
                }
                if (this.monitor.isCancelled()) {
                    throw StopWalkException.INSTANCE;
                }
            }
            return true;
        }

        public TreeFilter clone() {
            throw new IllegalStateException("Do not clone this kind of filter: " + getClass().getName());
        }
    }

    private TreeFilter filter = null;
    private final WorkingTreeIterator initialWorkingTreeIterator;
    private Set<String> added = new HashSet();
    private Set<String> changed = new HashSet();
    private Set<String> removed = new HashSet();
    private Set<String> missing = new HashSet();
    private Set<String> modified = new HashSet();
    private Set<String> untracked = new HashSet();
    private Map<String, StageState> conflicts = new HashMap();
    private Set<String> ignored;
    private Set<String> assumeUnchanged;
    private DirCache dirCache;
    private IndexDiffFilter indexDiffFilter;
    private Map<String, IndexDiff> submoduleIndexDiffs = new HashMap();
    private SubmoduleWalk.IgnoreSubmoduleMode ignoreSubmoduleMode = null;
    private Map<FileMode, Set<String>> fileModes = new HashMap();

    public IndexDiff(Repository repository, String revstr, WorkingTreeIterator workingTreeIterator) throws IOException {
        this(repository, repository.resolve(revstr), workingTreeIterator);
    }

    public IndexDiff(Repository repository, ObjectId objectId, WorkingTreeIterator workingTreeIterator)
            throws IOException {
        this.repository = repository;
        if (objectId != null) {
            this.tree = new RevWalk(repository).parseTree(objectId);
        } else {
            this.tree = null;
        }
        this.initialWorkingTreeIterator = workingTreeIterator;
    }

    public void setIgnoreSubmoduleMode(SubmoduleWalk.IgnoreSubmoduleMode mode) {
        this.ignoreSubmoduleMode = mode;
    }

    private WorkingTreeIteratorFactory wTreeIt = new WorkingTreeIteratorFactory() {
        public WorkingTreeIterator getWorkingTreeIterator(Repository repo) {
            return new FileTreeIterator(repo);
        }
    };

    public void setWorkingTreeItFactory(WorkingTreeIteratorFactory wTreeIt) {
        this.wTreeIt = wTreeIt;
    }

    public void setFilter(TreeFilter filter) {
        this.filter = filter;
    }

    public boolean diff() throws IOException {
        return diff(null, 0, 0, "");
    }

    public boolean diff(ProgressMonitor monitor, int estWorkTreeSize, int estIndexSize, String title)
            throws IOException {
        this.dirCache = this.repository.readDirCache();

        TreeWalk treeWalk = new TreeWalk(this.repository);
        Throwable localThrowable2 = null;
        try {
            treeWalk.setRecursive(true);
            if (this.tree != null) {
                treeWalk.addTree(this.tree);
            } else {
                treeWalk.addTree(new EmptyTreeIterator());
            }
            treeWalk.addTree(new DirCacheIterator(this.dirCache));
            treeWalk.addTree(this.initialWorkingTreeIterator);
            Collection<TreeFilter> filters = new ArrayList(4);
            if (monitor != null) {
                if (estIndexSize == 0) {
                    estIndexSize = this.dirCache.getEntryCount();
                }
                int total = Math.max(estIndexSize * 10 / 9, estWorkTreeSize * 10 / 9);

                monitor.beginTask(title, total);
//                filters.add(new ProgressReportingFilter(monitor, total, null));
            }
            if (this.filter != null) {
                filters.add(this.filter);
            }
            filters.add(new SkipWorkTreeFilter(1));
            this.indexDiffFilter = new IndexDiffFilter(1, 2);
            filters.add(this.indexDiffFilter);
            treeWalk.setFilter(AndTreeFilter.create(filters));
            this.fileModes.clear();
            while (treeWalk.next()) {
                AbstractTreeIterator treeIterator = treeWalk.getTree(0, AbstractTreeIterator.class);

                DirCacheIterator dirCacheIterator = (DirCacheIterator) treeWalk.getTree(1, DirCacheIterator.class);

                WorkingTreeIterator workingTreeIterator = (WorkingTreeIterator) treeWalk.getTree(2,
                        WorkingTreeIterator.class);
                if (dirCacheIterator != null) {
                    DirCacheEntry dirCacheEntry = dirCacheIterator.getDirCacheEntry();
                    if (dirCacheEntry != null) {
                        int stage = dirCacheEntry.getStage();
                        if (stage > 0) {
                            String path = treeWalk.getPathString();
                            addConflict(path, stage);
                            continue;
                        }
                    }
                }
                if (treeIterator != null) {
                    if (dirCacheIterator != null) {
                        if ((!treeIterator.idEqual(dirCacheIterator))
                                || (treeIterator.getEntryRawMode() != dirCacheIterator.getEntryRawMode())) {
                            if ((!isEntryGitLink(treeIterator)) || (!isEntryGitLink(dirCacheIterator))
                                    || (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL)) {
                                this.changed.add(treeWalk.getPathString());
                            }
                        }
                    } else {
                        if ((!isEntryGitLink(treeIterator))
                                || (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL)) {
                            this.removed.add(treeWalk.getPathString());
                        }
                        if (workingTreeIterator != null) {
                            this.untracked.add(treeWalk.getPathString());
                        }
                    }
                } else if (dirCacheIterator != null) {
                    if ((!isEntryGitLink(dirCacheIterator))
                            || (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL)) {
                        this.added.add(treeWalk.getPathString());
                    }
                } else if ((workingTreeIterator != null) && (!workingTreeIterator.isEntryIgnored())) {
                    this.untracked.add(treeWalk.getPathString());
                }
                if (dirCacheIterator != null) {
                    if (workingTreeIterator == null) {
                        if ((!isEntryGitLink(dirCacheIterator))
                                || (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL)) {
                            this.missing.add(treeWalk.getPathString());
                        }
                    } else if (workingTreeIterator.isModified(dirCacheIterator.getDirCacheEntry(), true,
                            treeWalk.getObjectReader())) {
                        if ((!isEntryGitLink(dirCacheIterator)) || (!isEntryGitLink(workingTreeIterator))
                                || ((this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL)
                                        && (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.DIRTY))) {
                            this.modified.add(treeWalk.getPathString());
                        }
                    }
                }
                for (int i = 0; i < treeWalk.getTreeCount(); i++) {
                    Set<String> values = (Set) this.fileModes.get(treeWalk.getFileMode(i));
                    String path = treeWalk.getPathString();
                    if (path != null) {
                        if (values == null) {
                            values = new HashSet();
                        }
                        values.add(path);
                        this.fileModes.put(treeWalk.getFileMode(i), values);
                    }
                }
            }
        } catch (Throwable localThrowable1) {
            localThrowable2 = localThrowable1;
            throw localThrowable1;
        } finally {
            if (treeWalk != null) {
                if (localThrowable2 != null) {
                    try {
                        treeWalk.close();
                    } catch (Throwable x2) {
                        localThrowable2.addSuppressed(x2);
                    }
                } else {
                    treeWalk.close();
                }
            }
        }
        if (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.ALL) {
            SubmoduleWalk.IgnoreSubmoduleMode localIgnoreSubmoduleMode = this.ignoreSubmoduleMode;
            SubmoduleWalk smw = SubmoduleWalk.forIndex(this.repository);
            while (smw.next()) {
                try {
                    if (localIgnoreSubmoduleMode == null) {
                        localIgnoreSubmoduleMode = smw.getModulesIgnore();
                    }
                    if (SubmoduleWalk.IgnoreSubmoduleMode.ALL.equals(localIgnoreSubmoduleMode)) {
                        continue;
                    }
                } catch (ConfigInvalidException e) {
                    IOException e1 = new IOException(MessageFormat.format(JGitText.get().invalidIgnoreParamSubmodule,
                            new Object[] { smw.getPath() }));

                    e1.initCause(e);
                    throw e1;
                }
                Repository subRepo = smw.getRepository();
                if (subRepo != null) {
                    try {
                        ObjectId subHead = subRepo.resolve("HEAD");
                        if ((subHead != null) && (!subHead.equals(smw.getObjectId()))) {
                            this.modified.add(smw.getPath());
                        } else if (this.ignoreSubmoduleMode != SubmoduleWalk.IgnoreSubmoduleMode.DIRTY) {
                            IndexDiff smid = (IndexDiff) this.submoduleIndexDiffs.get(smw.getPath());
                            if (smid == null) {
                                smid = new IndexDiff(subRepo, smw.getObjectId(),
                                        this.wTreeIt.getWorkingTreeIterator(subRepo));

                                this.submoduleIndexDiffs.put(smw.getPath(), smid);
                            }
                            if (smid.diff()) {
                                if ((this.ignoreSubmoduleMode == SubmoduleWalk.IgnoreSubmoduleMode.UNTRACKED)
                                        && (smid.getAdded().isEmpty()) && (smid.getChanged().isEmpty())
                                        && (smid.getConflicting().isEmpty()) && (smid.getMissing().isEmpty())
                                        && (smid.getModified().isEmpty()) && (smid.getRemoved().isEmpty())) {
                                    subRepo.close();
                                    continue;
                                }
                                this.modified.add(smw.getPath());
                            }
                        }
                    } finally {
                        subRepo.close();
                    }
                }
            }
        }
        if (monitor != null) {
            monitor.endTask();
        }
        this.ignored = this.indexDiffFilter.getIgnoredPaths();
        if ((this.added.isEmpty()) && (this.changed.isEmpty()) && (this.removed.isEmpty()) && (this.missing.isEmpty())
                && (this.modified.isEmpty()) && (this.untracked.isEmpty())) {
            return false;
        }
        return true;
    }

    private boolean isEntryGitLink(AbstractTreeIterator ti) {
        return (ti != null) && (ti.getEntryRawMode() == FileMode.GITLINK.getBits());
    }

    private void addConflict(String path, int stage) {
        StageState existingStageStates = (StageState) this.conflicts.get(path);
        byte stageMask = 0;
        if (existingStageStates != null) {
            stageMask = (byte) (stageMask | existingStageStates.getStageMask());
        }
        int shifts = stage - 1;
        stageMask = (byte) (stageMask | 1 << shifts);
        StageState stageState = StageState.fromMask(stageMask);
        this.conflicts.put(path, stageState);
    }

    public Set<String> getAdded() {
        return this.added;
    }

    public Set<String> getChanged() {
        return this.changed;
    }

    public Set<String> getRemoved() {
        return this.removed;
    }

    public Set<String> getMissing() {
        return this.missing;
    }

    public Set<String> getModified() {
        return this.modified;
    }

    public Set<String> getUntracked() {
        return this.untracked;
    }

    public Set<String> getConflicting() {
        return this.conflicts.keySet();
    }

    public Map<String, StageState> getConflictingStageStates() {
        return this.conflicts;
    }

    public Set<String> getIgnoredNotInIndex() {
        return this.ignored;
    }

    public Set<String> getAssumeUnchanged() {
        if (this.assumeUnchanged == null) {
            HashSet<String> unchanged = new HashSet();
            for (int i = 0; i < this.dirCache.getEntryCount(); i++) {
                if (this.dirCache.getEntry(i).isAssumeValid()) {
                    unchanged.add(this.dirCache.getEntry(i).getPathString());
                }
            }
            this.assumeUnchanged = unchanged;
        }
        return this.assumeUnchanged;
    }

    public Set<String> getUntrackedFolders() {
        return this.indexDiffFilter == null ? Collections.emptySet()
                : new HashSet(this.indexDiffFilter.getUntrackedFolders());
    }

    public FileMode getIndexMode(String path) {
        DirCacheEntry entry = this.dirCache.getEntry(path);
        return entry != null ? entry.getFileMode() : FileMode.MISSING;
    }

    public Set<String> getPathsWithIndexMode(FileMode mode) {
        Set<String> paths = (Set) this.fileModes.get(mode);
        if (paths == null) {
            paths = new HashSet();
        }
        return paths;
    }
}
