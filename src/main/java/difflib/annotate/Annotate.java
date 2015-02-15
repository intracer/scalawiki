/*
   Copyright 2010 Michael Schierl (schierlm@gmx.de)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package difflib.annotate;

import java.util.*;

import difflib.*;

/**
 * Generates an annotated version of a revision based on a list of older
 * revisions, like <tt>cvs annotate</tt> or <tt>svn blame</tt>.
 * 
 * @author <a href="schierlm@gmx.de">Michael Schierl</a>
 * 
 * @param <R>
 *            Type of the revision metadata
 */
public class Annotate<R, T> {

    private final List<R> revisions;
    private final int[] lineNumbers;
    private R currentRevision;
    private final List<T> currentLines;
    private final List<Integer> currentLineMap;

    /**
     * Creates a new annotation generator.
     * 
     * @param revision
     *            Revision metadata for the revision to be annotated
     * @param targetLines
     *            Lines of the revision to be annotated
     */
    public Annotate(R revision, List<T> targetLines) {
        revisions = new ArrayList<>();
        lineNumbers = new int[targetLines.size()];
        currentRevision = revision;
        currentLines = new ArrayList<>(targetLines);
        currentLineMap = new ArrayList<>();
        for (int i = 0; i < lineNumbers.length; i++) {
            lineNumbers[i] = -1;
            revisions.add(null);
            currentLineMap.add(i);
        }
    }

    /**
     * Check whether there are still lines that are unannotated. In that case,
     * more older revisions should be retrieved and passed to the function. Note
     * that as soon as you pass an empty revision, all lines will be annotated
     * (with a later revision), therefore if you do not have any more revisions,
     * pass an empty revision to annotate the rest of the lines.
     */
    public boolean areLinesUnannotated() {
        for (int i = 0; i < lineNumbers.length; i++) {
            if (lineNumbers[i] == -1 || revisions.get(i) == null)
                return true;
        }
        return false;
    }

    /**
     * Add the previous revision and update annotation info.
     * 
     * @param revision
     *            Revision metadata for this revision
     * @param lines
     *            Lines of this revision
     */
    public void addRevision(R revision, List<T> lines) {
        Patch<T> patch = DiffUtils.diff(currentLines, lines);
        int lineOffset = 0; // remember number of already deleted/added lines
        for (Delta<T> d : patch.getDeltas()) {
            Chunk<T> original = d.getOriginal();
            Chunk<T> revised = d.getRevised();
            int pos = original.getPosition() + lineOffset;
            // delete lines
            for (int i = 0; i < original.size(); i++) {
                int origLine = currentLineMap.remove(pos);
                currentLines.remove(pos);
                if (origLine != -1) {
                    lineNumbers[origLine] = original.getPosition() + i;
                    revisions.set(origLine, currentRevision);
                }
            }
            for (int i = 0; i < revised.size(); i++) {
                currentLines.add(pos + i, revised.getLines().get(i));
                currentLineMap.add(pos + i, -1);
            }
            lineOffset += revised.size() - original.size();
        }

        currentRevision = revision;
        if (!currentLines.equals(lines))
            throw new RuntimeException("Patch application failed");
    }

    /**
     * Return the result of the annotation. It will be a List of the same length
     * as the target revision, for which every entry states the revision where
     * the line appeared last.
     */
    public List<R> getAnnotatedRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    /**
     * Return the result of the annotation. It will be a List of the same length
     * as the target revision, for which every entry states the line number in
     * the revision where the line appeared last.
     */
    public int[] getAnnotatedLineNumbers() {
        return (int[]) lineNumbers.clone();
    }
}