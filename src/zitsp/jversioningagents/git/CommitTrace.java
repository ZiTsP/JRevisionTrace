package codelamp.pluginlamp.internal;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.MalformedInputException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import codelamp.pio.Charsets;
import codelamp.pio.Language;

public final class CommitTrace {

	private CommitTrace() {
	}

	public static final String GIT_DIR = ".git";
	private static final Path getGitDir(Path path) {
		if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
			return null;
		}
		Path tmpProject = null;
		try (DirectoryStream<Path> fileList = Files.newDirectoryStream(path, GIT_DIR)) {
			if (fileList != null) {
			    for (Path file : fileList) {
			    	tmpProject = file;
			    }
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return tmpProject;
	}
	private static final Path getGitDir(String path) {
		return getGitDir(Paths.get(path));
	}

	public static Path setGitDir(String path) {
		if (path == null || !Files.exists(Paths.get(path))) {
			return null;
		} else if (Paths.get(path).getFileName().toString().equals(GIT_DIR) && Files.isDirectory(Paths.get(path))) {
			return Paths.get(path);
		} else {
			Path repositoryDir = getGitDir(path);
			if (repositoryDir == null) {
				return null;
			}
			return repositoryDir;
		}
	}

	public static List<RevCommit> getAllComit(String path) {
		Path repositoryDir = setGitDir(path);
		if (repositoryDir == null) {
			return null;
		}
		List<RevCommit> commits = new ArrayList<>();
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryDir.toFile()).build()) {
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
		if (commits == null || commits.isEmpty()) {
			return null;
		} else {
			return commits;
		}
	}

	public static List<Ref> getAllTag(String path) {
		Path repositoryDir = setGitDir(path);
		if (repositoryDir == null) {
			return null;
		}
		List<Ref> tags = new ArrayList<>();
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryDir.toFile()).build()) {
			try (Git git = new Git(repository)) {
				tags = git.tagList().call();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (tags == null || tags.isEmpty()) {
			return null;
		} else {
			return tags;
		}
	}

	public static RevTag getRevTag(Ref tag, Repository repository) {
		RevWalk walk = new RevWalk(repository);
			try {
				System.out.println("O "+tag.getObjectId()+tag.getName() + " C " + tag.getPeeledObjectId());
				RevTag rtag = walk.parseTag(tag.getObjectId().toObjectId());
				System.out.println("N "+tag.getObjectId().toObjectId());
				System.out.println(rtag.getTagName() + rtag.toString());
				walk.close();
				return rtag;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			walk.close();
			return null;
	}

	private static final String ID_COMMIT = "commit ";
	private static final String ID_ANYOBJECT = "AnyObjectId[";
	public static String limitIdLength(AnyObjectId objectId, int idLength) {
		String id = objectId.toString();
		if (id == null) {
			return "";
		} else if (id.startsWith(ID_ANYOBJECT)) {
			return id.substring(ID_ANYOBJECT.length(), ID_ANYOBJECT.length() + idLength);
		} else if (id.startsWith(ID_COMMIT)) {
			return id.substring(ID_COMMIT.length(), ID_COMMIT.length() + idLength);
		} else {
			return "";
		}
	}
	public static String limitIdLength(AnyObjectId objectId) {
		return limitIdLength(objectId, 9);
	}
	public static String getTagNameFromRef(Ref tag) {
		String name = tag.getName();
		if (name == null || name.equals("")) {
			return "";
		}
		int index = name.lastIndexOf("/");
		if (index > 0) {
			return name.substring(index + 1);
		} else {
			return name;
		}
	}


	public static List<DiffEntry> getDiffEntry(RevCommit commit, String repositoryPath) {
		Path repositoryDir = setGitDir(repositoryPath);
		if (repositoryDir == null) {
			return null;
		}
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryDir.toFile()).build()) {
			List<DiffEntry> diffEntries = new ArrayList<>();
			Arrays.stream(commit.getParents()).forEach(entry -> {
				RevCommit oldCommit = null;
				try (RevWalk revWalk = new RevWalk(repository)) {
					ObjectId id = repository.resolve(entry.name());
					oldCommit = revWalk.parseCommit(id);
		        } catch (IOException e) {
					e.printStackTrace();
				}
				RevTree fromTree = oldCommit.getTree();
				RevTree toTree = commit.getTree();
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
				} catch (IncorrectObjectTypeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    });
			if (diffEntries == null || diffEntries.isEmpty()) {
				return null;
			} else {
				return diffEntries;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static List<DiffEntry> trimExtraExtensionFiles(List<DiffEntry> diffEntries, String extension) {
		if (extension == null || extension.equals("")) {
			return diffEntries;
		}
		List<DiffEntry> list = new ArrayList<>();
		diffEntries.forEach(e -> {
			if (e.getNewPath().endsWith(extension)) {
				list.add(e);
			}
		});
		if (list == null || list.isEmpty()) {
			return null;
		} else {
			return list;
		}
	}
	public static boolean outputDiff(List<DiffEntry> diffEntries, String repositoryPath, OutputStream outStream) {
		Path repositoryDir = setGitDir(repositoryPath);
		if (repositoryDir == null) {
			return false;
		}
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryDir.toFile()).build()) {
			try (DiffFormatter diffFormatter = new DiffFormatter(outStream)) {
				diffFormatter.setRepository(repository);
				diffFormatter.format(diffEntries);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return true;
	}
	public static boolean outputDiff(DiffEntry diffEntry, Path repositoryPath, OutputStream outStream) {
		if (repositoryPath == null) {
			throw new NullPointerException();
		}
		System.out.println("classified4");
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryPath.toFile()).build()) {
			try (DiffFormatter diffFormatter = new DiffFormatter(outStream)) {
				diffFormatter.setRepository(repository);
				diffFormatter.format(diffEntry);
				System.out.println("classified4");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		System.out.println("classified4");
		return true;
	}

	public static boolean outputDiffToText(List<DiffEntry> diffEntries, String repositoryPath, String path) {
		if (path == null || path.equals("") || diffEntries == null || diffEntries.isEmpty()) {
			return false;
		}
		Path outPut = Paths.get(path).toAbsolutePath();
		if (!Files.exists(outPut)) {
			try {
				Files.createFile(outPut);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		boolean success = false;
		try (OutputStream outStream = Files.newOutputStream(outPut)) {
			success = outputDiff(diffEntries, repositoryPath, outStream);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return success;
	}

	public static RevCommit getHeadCommit(Path repositoryPath) {
		return getCommit(repositoryPath, Constants.HEAD);
	}

	public static RevCommit getHeadCaretICommit(Path repositoryPath) {
		return getCommit(repositoryPath, "HEAD^");
	}

	public static RevCommit getCommit(Path repositoryPath, String resolveWord) {
		RevCommit commit = null;
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryPath.toFile()).build()) {
	        try (RevWalk revWalk = new RevWalk(repository)) {
				ObjectId id = repository.resolve(resolveWord);
	        	commit = revWalk.parseCommit(id);
	        } catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return commit;
	}

	private static final String DIFFVIEW_TMPFILE_PREFIX = "PLUGINLAMP_DIFFVIEW";
	private static final int BUFFER_SIZE = 512 * 1024;

	public static ArrayList<String> getDiffText(DiffEntry diffEntry, Path repositoryPath) {
		ArrayList<String> doc = new ArrayList<>();
		try {
			Path tmpDiff = Files.createTempFile(DIFFVIEW_TMPFILE_PREFIX, null);
			try (OutputStream diffWriter = Files.newOutputStream(tmpDiff)) {
				BufferedOutputStream buffWriter = new BufferedOutputStream(diffWriter, BUFFER_SIZE);
				CommitTrace.outputDiff(diffEntry, repositoryPath, buffWriter);
				buffWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try (BufferedReader diffReader = Files.newBufferedReader(tmpDiff, Charsets.getDefault())) {
				while (diffReader.ready()) {
					String text;
					if ((text = diffReader.readLine()) == null) {
						break;
					}
					doc.add(text);
				}
			} catch (MalformedInputException exception) {
				doc.clear();
				Files.lines(tmpDiff, Charsets.UTF8).forEach(e -> doc.add(e));
			} catch (IOException exception) {
				exception.printStackTrace();
			}
			Files.delete(tmpDiff);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return doc;
	}
	public static List<String> getDiffText(RevCommit commit, Path repositoryPath, Language language) {
		ArrayList<String> doc = new ArrayList<>();
		List<DiffEntry> diffEntry = CommitTrace.getDiffEntry(commit, repositoryPath, language);
		try {
			Path tmpDiff = Files.createTempFile(DIFFVIEW_TMPFILE_PREFIX, null);
			try (OutputStream diffWriter = Files.newOutputStream(tmpDiff)) {
				BufferedOutputStream buffWriter = new BufferedOutputStream(diffWriter, BUFFER_SIZE);
				CommitTrace.outputDiff(diffEntry, repositoryPath.toString(), buffWriter);
				buffWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try (BufferedReader diffReader = Files.newBufferedReader(tmpDiff, Charsets.getDefault())) {
				for (;;) {
					String text;
					if ((text = diffReader.readLine()) == null) {
						break;
					}
					doc.add(text);
				}
			} catch (MalformedInputException exception) {
				doc.clear();
				Files.lines(tmpDiff, Charsets.UTF8).forEach(e -> doc.add(e));
			} catch (IOException exception) {
				exception.printStackTrace();
			}
			Files.delete(tmpDiff);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return doc;
	}
	private static List<DiffEntry> getDiffEntry(RevCommit commit, Path repositoryPath) {
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repositoryPath.toFile()).build()) {
			List<DiffEntry> diffEntries = new ArrayList<>();
			Arrays.stream(commit.getParents()).forEach(entry -> {
				RevCommit oldCommit = null;
				try (RevWalk revWalk = new RevWalk(repository)) {
					ObjectId id = repository.resolve(entry.name());
					oldCommit = revWalk.parseCommit(id);
		        } catch (IOException e) {
					e.printStackTrace();
				}
				RevTree fromTree = oldCommit.getTree();
				RevTree toTree = commit.getTree();
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
				} catch (IncorrectObjectTypeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    });
			if (diffEntries == null || diffEntries.isEmpty()) {
				return null;
			} else {
				return diffEntries;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private static List<DiffEntry> getDiffEntry(RevCommit commit, Path repositoryPath, Language language) {
		List<DiffEntry> entries = getDiffEntry(commit, repositoryPath);
		if (entries == null || entries.isEmpty()) {
			return null;
		} else if (language == null || language.equals(Language.EMPTY)) {
			return entries;
		}
		List<DiffEntry> tmpList = new ArrayList<>();
		for (String extension : language.getExtension()) {
			entries.forEach(e -> {
				if (e.getNewPath().endsWith(extension)) {
					tmpList.add(e);
				}
			});
		}
		if (tmpList == null || tmpList.isEmpty()) {
			return null;
		} else {
			return tmpList;
		}
	}
}
