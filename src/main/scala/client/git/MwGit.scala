package client.git

import java.io.File
import java.nio.file.Files
import java.util.{Date, TimeZone}

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{Constants, PersonIdent, Repository}
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.{CanonicalTreeParser, TreeWalk}

import scala.collection.JavaConverters._


class MwGit(repository: Repository) {

  def addFile(name: String, content: String, author: String, date: Date): Unit = {
    val file = new File(repository.getDirectory.getParent, name)

    Files.write(file.toPath, content.getBytes)

    new Git(repository).add().addFilepattern(name).call()

    val ident = new PersonIdent(author, "",
      date, TimeZone.getTimeZone("UTC"))

    new Git(repository).commit()
      .setMessage("Added " + name)
      .setCommitter(ident)
      .call()
  }

  def fileHistory(name: String): Unit = {

//    val lastCommitId = repository.resolve(Constants.HEAD)
//
//    val revWalk = new RevWalk(repository)
//    val commit = revWalk.parseCommit(lastCommitId)
//
//    val tree = new TreeWalk(repository)
//    tree.addTree(commit.getTree)
//    commit.getParents foreach {
//      parent => tree.addTree(parent.getTree)
//    }
//    tree.setFilter(TreeFilter.ANY_DIFF)

    val logCommand = new Git(repository).log
      .add(repository.resolve(Constants.HEAD))
      .addPath(name)

    val commits = logCommand.call().asScala

    commits.map(fileContent(_, name))


  }

  def fileContent(commit: RevCommit, name: String): String = {
    val treeWalk = TreeWalk.forPath(repository, name, commit.getTree)

    var bytes: Array[Byte] = Array.empty

    if (treeWalk != null) {
      treeWalk.setRecursive(true)
      val canonicalTreeParser = treeWalk.getTree(0, classOf[CanonicalTreeParser])

      while (!canonicalTreeParser.eof()) {
        if (canonicalTreeParser.getEntryPathString == name) {
          val objectLoader = repository.open(canonicalTreeParser.getEntryObjectId)
          bytes = objectLoader.getBytes
        }
        canonicalTreeParser.next(1)
      }
    }

    new String(bytes)
  }

  def  close(): Unit = {
    repository.close()
  }

}

object MwGit {

  def create(directory: String): MwGit = {
    val repository = FileRepositoryBuilder.create(new File(directory, ".git"))
    repository.create()

    new MwGit(repository)
  }

  def createTemp(directory: String): MwGit = {
    val localPath = Files.createTempDirectory(directory)
    create(localPath.toString)
  }

  def open(directory: String): MwGit = {
    val builder = new FileRepositoryBuilder()
    val repository = builder.setGitDir(new File(directory))
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build()

    new MwGit(repository)
  }


}
