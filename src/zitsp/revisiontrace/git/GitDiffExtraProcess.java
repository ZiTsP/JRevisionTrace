package zitsp.revisiontrace.git;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

import zitsp.revisiontrace.common.LinesExtraction;

public class GitDiffExtraProcess extends LinesExtraction {

    private GitDiffExtraProcess() {
    }
    
    public static List<DiffEntry> extractSpecificType(List<DiffEntry> diffEntries, List<String> specificExtensions) {
        List<DiffEntry> newList = new ArrayList<>();
        diffEntries.parallelStream().forEach(entry -> {
            specificExtensions.forEach(extension -> {
                if (entry.getNewPath().endsWith(extension)) {
                    newList.add(entry);
                }
            });
        });
        return newList;
    }
    
}
