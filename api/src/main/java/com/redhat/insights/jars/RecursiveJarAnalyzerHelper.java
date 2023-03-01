/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Helper to analyze JAR files (and EAR / WAR) in a recursive fashion. */
public class RecursiveJarAnalyzerHelper {

  private final InsightsLogger logger;

  public RecursiveJarAnalyzerHelper(InsightsLogger logger) {
    this.logger = logger;
  }

  public final List<JarInfo> listDeploymentContent(
      final JarAnalyzer analyzer,
      final Path tempDir,
      final String parentName,
      final Path deployment)
      throws IOException, URISyntaxException {
    final List<JarInfo> jarInfos = new ArrayList<>();
    if (isArchive(deployment)) {
      Path target = createTempDirectory(tempDir, "unarchive");
      unzip(deployment, target);
      Files.walkFileTree(
          target,
          new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              if (Files.isRegularFile(file) && isArchive(file)) {
                try {
                  String path = parentName + '/' + formatPath(target.relativize(file));
                  Optional<JarInfo> info = analyzer.process(file.toUri().toURL());
                  if (info.isPresent()) {
                    JarInfo jarInfo = info.get();
                    logger.debug("Adding the info for " + jarInfo);
                    jarInfo.attributes().put("path", path);
                    jarInfos.add(jarInfo);
                  }
                  if (listingRequired(file)) {
                    jarInfos.addAll(listDeploymentContent(analyzer, tempDir, path, file));
                  }
                } catch (URISyntaxException ex) {
                  logger.error("Error reading jar", ex);
                }
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              return FileVisitResult.CONTINUE;
            }

            private String formatPath(Path path) {
              return path.toString().replace(File.separatorChar, '/');
            }
          });
    }
    return jarInfos;
  }

  public static final boolean listingRequired(Path zip) throws IOException {
    try (final ZipFile zipFile = new ZipFile(zip.toFile())) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          if (isArchive(zipFile.getInputStream(entry))) {
            return true;
          }
        }
      }
    } catch (IOException e) {
      // ignore, if we cannot set it, world will not end
    }
    return false;
  }

  /**
   * Create a temporary directory with the same attributes as its parent directory.
   *
   * @param dir the path to directory in which to create the directory
   * @param prefix the prefix string to be used in generating the directory's name; may be {@code
   *     null}
   * @return the path to the newly created directory that did not exist before this method was
   *     invoked
   * @throws IOException
   */
  public static Path createTempDirectory(Path dir, String prefix) throws IOException {
    try {
      return Files.createTempDirectory(dir, prefix);
    } catch (UnsupportedOperationException ex) {
    }
    return Files.createTempDirectory(dir, prefix);
  }

  /**
   * Test if the target path is an archive.
   *
   * @param path path to the file.
   * @return true if the path points to a zip file - false otherwise.
   * @throws IOException
   */
  public static final boolean isArchive(Path path) throws IOException {
    if (Files.exists(path) && Files.isRegularFile(path)) {
      try (ZipFile zip = new ZipFile(path.toFile())) {
        return true;
      } catch (ZipException e) {
        return false;
      }
    }
    return false;
  }

  /**
   * Test if the target path is an archive.
   *
   * @param in stream that reads the target path
   * @return true if the path points to a zip file - false otherwise.
   * @throws IOException
   */
  public static final boolean isArchive(InputStream in) throws IOException {
    if (in != null) {
      try (ZipInputStream zip = new ZipInputStream(in)) {
        return zip.getNextEntry() != null;
      } catch (ZipException e) {
        return false;
      }
    }
    return false;
  }

  /**
   * Unzip a file to a target directory.
   *
   * @param zip the path to the zip file.
   * @param target the path to the target directory into which the zip file will be unzipped.
   * @throws IOException
   */
  public void unzip(Path zip, Path target) throws IOException {
    try (final ZipFile zipFile = new ZipFile(zip.toFile())) {
      unzip(zipFile, target);
    }
  }

  private void unzip(final ZipFile zip, final Path targetDir) throws IOException {
    final Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      final ZipEntry entry = entries.nextElement();
      final String name = entry.getName();
      final Path current = resolveSecurely(targetDir, name);
      if (entry.isDirectory()) {
        if (!Files.exists(current)) {
          Files.createDirectories(current);
        }
      } else {
        if (Files.notExists(current.getParent())) {
          Files.createDirectories(current.getParent());
        }
        try (final InputStream eis = zip.getInputStream(entry)) {
          Files.copy(eis, current);
        }
      }
      try {
        Files.getFileAttributeView(current, BasicFileAttributeView.class)
            .setTimes(
                entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
      } catch (IOException e) {
        // ignore, if we cannot set it, world will not end
      }
    }
  }

  /**
   * Resolve a path from the rootPath checking that it doesn't go out of the rootPath.
   *
   * @param rootPath the starting point for resolution.
   * @param path the path we want to resolve.
   * @return the resolved path.
   * @throws IOException if the resolved path is out of the rootPath or if the resolution failed.
   */
  public final Path resolveSecurely(Path rootPath, String path) throws IOException {
    Path resolvedPath;
    if (path == null || path.isEmpty()) {
      resolvedPath = rootPath.normalize();
    } else {
      String relativePath = removeSuperflousSlashes(path);
      resolvedPath = rootPath.resolve(relativePath).normalize();
    }
    if (!resolvedPath.startsWith(rootPath)) {
      throw new IOException(
          String.format("Access denied to the content at %s in the deployment", resolvedPath));
    }
    return resolvedPath;
  }

  private String removeSuperflousSlashes(String path) {
    if (path.startsWith("/")) {
      return removeSuperflousSlashes(path.substring(1));
    }
    return path;
  }

  /**
   * Delete a path recursively, not throwing Exception if it fails or if the path is null.
   *
   * @param path a Path pointing to a file or a directory that may not exists anymore.
   */
  public void deleteSilentlyRecursively(final Path path) {
    if (path != null) {
      try {
        deleteRecursively(path);
      } catch (IOException ioex) {
        logger.debug(String.format("Error deleting file %s", path), ioex);
      }
    }
  }

  /**
   * Delete a path recursively.
   *
   * @param path a Path pointing to a file or a directory that may not exists anymore.
   * @throws IOException
   */
  public void deleteRecursively(final Path path) throws IOException {
    logger.debug(String.format("Deleting %s recursively", path));
    if (Files.exists(path)) {
      Files.walkFileTree(
          path,
          new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
              logger.debug(String.format("Error deleting file %s", path), exc);
              throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }
}
