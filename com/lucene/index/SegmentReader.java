/* SegmentReader.java
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
package com.lucene.index;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import com.lucene.util.BitVector;
import com.lucene.store.Directory;
import com.lucene.store.InputStream;
import com.lucene.document.Document;

public final class SegmentReader extends IndexReader {
  Directory directory;
  private boolean closeDirectory = false;
  private String segment;

  FieldInfos fieldInfos;
  private FieldsReader fieldsReader;

  TermInfosReader tis;
  
  BitVector deletedDocs = null;
  private boolean deletedDocsDirty = false;

  private InputStream freqStream;
  private InputStream proxStream;


  private static class Norm {
    public Norm(InputStream in) { this.in = in; }
    public InputStream in;
    public byte[] bytes;
  }
  private Hashtable norms = new Hashtable();

  public SegmentReader(SegmentInfo si, boolean closeDir)
       throws IOException {
    this(si);
    closeDirectory = closeDir;
  }

  public SegmentReader(SegmentInfo si)
       throws IOException {
    directory = si.dir;
    segment = si.name;

    fieldInfos = new FieldInfos(directory, segment + ".fnm");
    fieldsReader = new FieldsReader(directory, segment, fieldInfos);

    tis = new TermInfosReader(directory, segment, fieldInfos);

    if (hasDeletions(si))
      deletedDocs = new BitVector(directory, segment + ".del");

    // make sure that all index files have been read or are kept open
    // so that if an index update removes them we'll still have them
    freqStream = directory.openFile(segment + ".frq");
    proxStream = directory.openFile(segment + ".prx");
    openNorms();
  }
  
  public final synchronized void close() throws IOException {
    if (deletedDocsDirty) {
      synchronized (directory) {
	deletedDocs.write(directory, segment + ".tmp");
	directory.renameFile(segment + ".tmp", segment + ".del");
      }
      deletedDocsDirty = false;
    }

    fieldsReader.close();
    tis.close();

    if (freqStream != null)
      freqStream.close();
    if (proxStream != null)
      proxStream.close();

    closeNorms();

    if (closeDirectory)
      directory.close();
  }

  final static boolean hasDeletions(SegmentInfo si) throws IOException {
    return si.dir.fileExists(si.name + ".del");
  }

  public final synchronized void delete(int docNum) throws IOException {
    if (deletedDocs == null)
      deletedDocs = new BitVector(maxDoc());
    deletedDocsDirty = true;
    deletedDocs.set(docNum);
  }

  final Vector files() throws IOException {
    Vector files = new Vector(16);
    files.addElement(segment + ".fnm");
    files.addElement(segment + ".fdx");
    files.addElement(segment + ".fdt");
    files.addElement(segment + ".tii");
    files.addElement(segment + ".tis");
    files.addElement(segment + ".frq");
    files.addElement(segment + ".prx");

    if (directory.fileExists(segment + ".del"))
      files.addElement(segment + ".del");

    for (int i = 0; i < fieldInfos.size(); i++) {
      FieldInfo fi = fieldInfos.fieldInfo(i);
      if (fi.isIndexed)
	files.addElement(segment + ".f" + i);
    }
    return files;
  }

  public final TermEnum terms() throws IOException {
    return tis.terms();
  }

  public final TermEnum terms(Term t) throws IOException {
    return tis.terms(t);
  }

  public final synchronized Document document(int n) throws IOException {
    if (isDeleted(n))
      throw new IllegalArgumentException
	("attempt to access a deleted document");
    return fieldsReader.doc(n);
  }

  public final synchronized boolean isDeleted(int n) {
    return (deletedDocs != null && deletedDocs.get(n));
  }

  public final TermDocs termDocs(Term t) throws IOException {
    TermInfo ti = tis.get(t);
    if (ti != null)
      return new SegmentTermDocs(this, ti);
    else
      return null;
  }

  final InputStream getFreqStream () {
    return (InputStream)freqStream.clone();
  }

  public final TermPositions termPositions(Term t) throws IOException {
    TermInfo ti = tis.get(t);
    if (ti != null)
      return new SegmentTermPositions(this, ti);
    else
      return null;
  }

  final InputStream getProxStream () {
    return (InputStream)proxStream.clone();
  }

  public final int docFreq(Term t) throws IOException {
    TermInfo ti = tis.get(t);
    if (ti != null)
      return ti.docFreq;
    else
      return 0;
  }

  public final int numDocs() {
    int n = maxDoc();
    if (deletedDocs != null)
      n -= deletedDocs.count();
    return n;
  }

  public final int maxDoc() {
    return fieldsReader.size();
  }

  public final byte[] norms(String field) throws IOException {
    Norm norm = (Norm)norms.get(field);
    if (norm == null)
      return null;
    if (norm.bytes == null) {
      byte[] bytes = new byte[maxDoc()];
      norms(field, bytes, 0);
      norm.bytes = bytes;
    }
    return norm.bytes;
  }

  final void norms(String field, byte[] bytes, int offset) throws IOException {
    InputStream normStream = normStream(field);
    if (normStream == null)
      return;					  // use zeros in array
    try {
      normStream.readBytes(bytes, offset, maxDoc());
    } finally {
      normStream.close();
    }
  }

  final InputStream normStream(String field) throws IOException {
    Norm norm = (Norm)norms.get(field);
    if (norm == null)
      return null;
    InputStream result = (InputStream)norm.in.clone();
    result.seek(0);
    return result;
  }

  private final void openNorms() throws IOException {
    for (int i = 0; i < fieldInfos.size(); i++) {
      FieldInfo fi = fieldInfos.fieldInfo(i);
      if (fi.isIndexed) 
	norms.put(fi.name,
		  new Norm(directory.openFile(segment + ".f" + fi.number)));
    }
  }

  private final void closeNorms() throws IOException {
    synchronized (norms) {
      Enumeration element  = norms.elements();
      while (element.hasMoreElements()) {
	Norm norm = (Norm)element.nextElement();
	norm.in.close();
      }
    }
  }
}
