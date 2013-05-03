/* FSDirectory.java
 *
 * Copyright (c) 1997, 2000 Douglass R. Cutting.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package com.lucene.store;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.util.Hashtable;

import com.lucene.store.Directory;
import com.lucene.store.InputStream;
import com.lucene.store.OutputStream;

/**
  Straightforward implementation of Directory as a directory of files.
    @see Directory
    @author Doug Cutting
*/

final public class FSDirectory extends Directory {
  /** This cache of directories ensures that there is a unique Directory
   * instance per path, so that synchronization on the Directory can be used to
   * synchronize access between readers and writers.
   *
   * This should be a WeakHashMap, so that entries can be GC'd, but that would
   * require Java 1.2.  Instead we use refcounts...  */
  private static final Hashtable DIRECTORIES = new Hashtable();

  /** Returns the directory instance for the named location.
   * 
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   * 
   * @param path the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @returns the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(String path, boolean create)
      throws IOException {
    return getDirectory(new File(path), create);
  }

  /** Returns the directory instance for the named location.
   * 
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   * 
   * @param file the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @returns the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(File file, boolean create)
    throws IOException {
    file = new File(file.getCanonicalPath());
    FSDirectory dir;
    synchronized (DIRECTORIES) {
      dir = (FSDirectory)DIRECTORIES.get(file);
      if (dir == null) {
	dir = new FSDirectory(file, create);
	DIRECTORIES.put(file, dir);
      }
    }
    synchronized (dir) {
      dir.refCount++;
    }
    return dir;
  }

  private File directory = null;
  private int refCount;

  public FSDirectory(File path, boolean create) throws IOException {
    directory = path;
    if (!directory.exists() && create)
      directory.mkdir();
    if (!directory.isDirectory())
      throw new IOException(path + " not a directory");

    if (create) {				  // clear old files
      String[] files = directory.list();
      for (int i = 0; i < files.length; i++) {
	File file = new File(directory, files[i]);
	if (!file.delete())
	  throw new IOException("couldn't delete " + files[i]);
      }
    }

  }

  /** Returns an array of strings, one for each file in the directory. */
  public final String[] list() throws IOException {
    return directory.list();
  }
       
  /** Returns true iff a file with the given name exists. */
  public final boolean fileExists(String name) throws IOException {
    File file = new File(directory, name);
    return file.exists();
  }
       
  /** Returns the time the named file was last modified. */
  public final long fileModified(String name) throws IOException {
    File file = new File(directory, name);
    return file.lastModified();
  }
       
  /** Returns the time the named file was last modified. */
  public static final long fileModified(File directory, String name)
       throws IOException {
    File file = new File(directory, name);
    return file.lastModified();
  }

  /** Returns the length in bytes of a file in the directory. */
  public final long fileLength(String name) throws IOException {
    File file = new File(directory, name);
    return file.length();
  }

  /** Removes an existing file in the directory. */
  public final void deleteFile(String name) throws IOException {
    File file = new File(directory, name);
    if (!file.delete())
      throw new IOException("couldn't delete " + name);
  }

  /** Renames an existing file in the directory. */
  public final synchronized void renameFile(String from, String to)
      throws IOException {
    File old = new File(directory, from);
    File nu = new File(directory, to);

    /* This is not atomic.  If the program crashes between the call to
       delete() and the call to renameTo() then we're screwed, but I've
       been unable to figure out how else to do this... */

    if (nu.exists())
      if (!nu.delete())
	throw new IOException("couldn't delete " + to);

    if (!old.renameTo(nu))
      throw new IOException("couldn't rename " + from + " to " + to);
  }

  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
  public final OutputStream createFile(String name) throws IOException {
    return new FSOutputStream(new File(directory, name));
  }

  /** Returns a stream reading an existing file. */
  public final InputStream openFile(String name) throws IOException {
    return new FSInputStream(new File(directory, name));
  }

  /** Closes the store to future operations. */
  public final synchronized void close() throws IOException {
    if (--refCount <= 0) {
      synchronized (DIRECTORIES) {
	DIRECTORIES.remove(directory);
      }
    }
  }
}


final class FSInputStream extends InputStream {
  private class Descriptor extends RandomAccessFile {
    public long position;
    public Descriptor(File file, String mode) throws IOException {
      super(file, mode);
    }
  }

  Descriptor file = null;
  boolean isClone;

  public FSInputStream(File path) throws IOException {
    file = new Descriptor(path, "r");
    length = file.length();
  }

  /** InputStream methods */
  protected final void readInternal(byte[] b, int offset, int len)
       throws IOException {
    synchronized (file) {
      long position = getFilePointer();
      if (position != file.position) {
	file.seek(position);
	file.position = position;
      }
      int total = 0;
      do {
	int i = file.read(b, offset+total, len-total);
	if (i == -1)
	  throw new IOException("read past EOF");
	file.position += i;
	total += i;
      } while (total < len);
    }
  }

  public final void close() throws IOException {
    if (!isClone)
      file.close();
  }

  /** Random-access methods */
  protected final void seekInternal(long position) throws IOException {
  }

  protected final void finalize() throws IOException {
    close();					  // close the file 
  }

  public Object clone() {
    FSInputStream clone = (FSInputStream)super.clone();
    clone.isClone = true;
    return clone;
  }
}


final class FSOutputStream extends OutputStream {
  RandomAccessFile file = null;

  public FSOutputStream(File path) throws IOException {
    if (path.isFile())
      throw new IOException(path + " already exists");
    file = new RandomAccessFile(path, "rw");
  }

  /** output methods: */
  public final void flushBuffer(byte[] b, int size) throws IOException {
    file.write(b, 0, size);
  }
  public final void close() throws IOException {
    super.close();
    file.close();
  }

  /** Random-access methods */
  public final void seek(long pos) throws IOException {
    super.seek(pos);
    file.seek(pos);
  }
  public final long length() throws IOException {
    return file.length();
  }

  protected final void finalize() throws IOException {
    file.close();				  // close the file 
  }

}
