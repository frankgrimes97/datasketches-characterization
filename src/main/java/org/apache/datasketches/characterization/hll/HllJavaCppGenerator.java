/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.characterization.hll;

import static org.apache.datasketches.hll.TgtHllType.HLL_4;
import static org.apache.datasketches.hll.TgtHllType.HLL_6;
import static org.apache.datasketches.hll.TgtHllType.HLL_8;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.datasketches.Files;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.TgtHllType;
import org.apache.datasketches.hll.Union;
//import org.testng.annotations.Test;

/**
 *
 *
 * @author Lee Rhodes
 */
public class HllJavaCppGenerator {
  final String userDir;
  final String subDir = "src/main/resources/hll/data/";
  final String path;
  final String[] modeArr = {"Empty", "List", "Set", "Hll"};
  final int[] nArr = {0, 4, 16, 64};
  final String[] srcGdtArr = {"Src", "Gdt" };
  final int baseLgK = 8;
  final TgtHllType[] tgtHllTypeArr = {HLL_4, HLL_6, HLL_8};
  final int[] lgKSeqArr = {0, 1, 5, 6};
  long vIn = 0;
  private PrintWriter pw = null;
  final Filter filter = new Filter();
  final HashMap<String, byte[]> map = new HashMap<>();
  final List<String> uList = new ArrayList<>();

  /**
   * This constructor discovers the root user package directory for Java, which is the directory
   * just below <i>src/main/java/...</i>. It then constructs a path for the directory containing
   * all of the binary sketch files, which is determined by the <i>subDir</i> constant defined
   * at the top of this class.
   */
  public HllJavaCppGenerator() {
    userDir = System.getProperty("user.dir") + "/";
    path = userDir + subDir;
  }

  /**
   * Builds the binary sketch files in sub directory under "user.dir", see top of this class.
   * This only needs to be done once.
   */
  //@Test
  public void buildBinarySketchFiles() {
    genBaseSketches();
    genResultSketches();
  }

  /**
   * This runs a Java test that confirms that each union pairing results in a binary that is
   * identical with the union binaries created in <i>genResultSketches()</i>. This code should be
   * translated to different languages to confirm binary compatibility with Java.
   */
  //@Test
  public void runJavaTest() {
    buildFileMapAndFileList();
    for (String s : uList) {
      final String[] split1 = s.split("_");
      final int ulgK = Integer.parseInt(split1[0].substring(2));
      final String gdtFname = split1[1] + ".bin";
      final String srcFname = split1[2];
      final Union union1 = new Union(ulgK);
      final HllSketch gdtSk = HllSketch.heapify(map.get(gdtFname));
      union1.update(gdtSk);
      final HllSketch srcSk = HllSketch.heapify(map.get(srcFname));
      union1.update(srcSk);
      final byte[] testBin = union1.toUpdatableByteArray();
      final byte[] trueBin = map.get(s);
      assertEquals(testBin, trueBin);
      //optional printing of sketch summaries for debugging
      //final Union union2 = Union.heapify(trueBin);
      //println(union1.toString(true, false, false, false));
      //println(union2.toString(true, false, false, false));
      //println("");
    }
  }

  private void buildFileMapAndFileList() {
    final File file = new File(path);
    final File[] farr = file.listFiles(filter);
    for (File f : farr) {
      final String fname = f.getName();
      if (fname.startsWith("U")) {
        uList.add(fname);
      }
      map.put(fname, readByteArrayFromFile(f));
    }
  }

  private static class Filter implements java.io.FileFilter {
    @Override
    public boolean accept(final File pathname) {
      return pathname.getName().endsWith(".bin");
    }
  }

  private void genBaseSketches() {
    for (int t = 0; t < 3; t++) {
      final String tStr = t * 2 + 4 + "";
      final TgtHllType tgtHllType = tgtHllTypeArr[t];
      for (int lgK = baseLgK; lgK <= baseLgK + 1; lgK++) {
        for (int m = 0; m < 4; m++) {
          final String mode = modeArr[m];
          final String fname = "Hll" + tStr + "K" + lgK + mode + ".bin";
          final HllSketch sk = new HllSketch(lgK, tgtHllType);
          final int n = nArr[m];
          for (int i = 0; i < n; i++) { sk.update(vIn++); }
          final byte[] arr = sk.toCompactByteArray();
          writeByteArrayToFile(arr, path + fname);
        }
      }
    }
  }

  private void genResultSketches() {
    pw = Files.openPrintWriter(path + "FileCombinationsList.txt");
    for (int i = 0; i < 4; i++) {
      final int lgKSeq = lgKSeqArr[i];
      final int maxLgK = baseLgK + ((lgKSeq & 4) > 0 ? 1 : 0);
      final int gdtLgK = baseLgK + ((lgKSeq & 2) > 0 ? 1 : 0);
      final int srcLgK = baseLgK + ((lgKSeq & 1) > 0 ? 1 : 0);
      for (int m1 = 0; m1 < 4; m1++) {
        final String gdtMode = modeArr[m1];
        for (int m2 = 0; m2 < 4; m2++) {
          final String srcMode = modeArr[m2];
          for (int t = 0; t < 3; t++) {
            final String tStr = t * 2 + 4 + "";
            final String gdtFname = "Hll8" + "K" + gdtLgK + gdtMode;
            final String srcFname = "Hll" + tStr + "K" + srcLgK + srcMode;
            final String uFname = "UK" + maxLgK + "_" + gdtFname + "_" + srcFname;
            println(gdtFname + ".bin\t" + srcFname + ".bin\t" + uFname + ".bin");
            final Union union = new Union(maxLgK);
            final byte[] gdtSkbin = readByteArrayFromFileName(path + gdtFname + ".bin");
            final HllSketch gdtSk = HllSketch.heapify(gdtSkbin);
            union.update(gdtSk);
            final byte[] srcSkbin = readByteArrayFromFileName(path + srcFname + ".bin");
            final HllSketch srcSk = HllSketch.heapify(srcSkbin);
            union.update(srcSk);
            final byte[] uSkbin = union.toUpdatableByteArray();
            writeByteArrayToFile(uSkbin, path + uFname + ".bin");
          }
        }
      }
    }
    pw.flush();
    pw.close();
    pw = null;
  }

  private static byte[] readByteArrayFromFileName(final String fullFileName) {
    Files.checkFileName(fullFileName); //checks for null, empty
    final File file = new File(fullFileName);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalArgumentException(fullFileName + " does not exist.");
    }
    return readByteArrayFromFile(file);
  }

  private static byte[] readByteArrayFromFile(final File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      final int len = fis.available();
      final byte[] out = new byte[len];
      fis.read(out);
      fis.close();
      return out;
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static void writeByteArrayToFile(final byte[] arr, final String fullFileName) {
    Files.checkFileName(fullFileName); //checks for null, empty
    final File file = new File(fullFileName);
    if (file.exists() && file.isFile()) {
      file.delete();
    }
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(arr);
      fos.flush();
      fos.close();
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Outputs a line to the configured PrintWriter and stdOut.
   * @param obj The obj.toString() to print
   */
  private void println(final Object obj) {
    System.out.println(obj.toString());
    if (pw != null) { pw.println(obj.toString()); }
  }

  /**
   * Run the Java test.  This assumes that the sketch files already exist.
   * @param args not used.
   */
  public static void main(final String[] args) {
    final HllJavaCppGenerator gen = new HllJavaCppGenerator();
    gen.runJavaTest();
  }
}
