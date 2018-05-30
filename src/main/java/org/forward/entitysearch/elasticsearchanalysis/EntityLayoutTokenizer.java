/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.forward.entitysearch.elasticsearchanalysis;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CustomizableCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.SpecialPermission;
import org.forward.entitysearch.ingestion.ESAnnotatedHTMLDocument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;


/**
 * 
 *
 * @author longpt214
 */
public class EntityLayoutTokenizer extends Tokenizer {

	private static final String UNIQUE_ENTITY_TOKEN = "oentityo";

    private int offset = 0;


    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    public EntityLayoutTokenizer() {
        this(true, false);
    }
    
    public EntityLayoutTokenizer(boolean p1, boolean p2) {
        super();
    }

    private void tokenize(Reader input) throws IOException {
        System.out.println("NER tokenizing...");
        int numChars;
        char[] buffer = new char[1024];
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((numChars =
                input.read(buffer, 0, buffer.length)) != -1) {
                stringBuilder.append(buffer, 0, numChars);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // unprivileged code such as scripts do not have SpecialPermission
            sm.checkPermission(new SpecialPermission());
        }
        AccessController.doPrivileged((PrivilegedAction<Void>)
                () -> {
                    // Privileged code goes here, for example:
                    Annotation aDoc = new Annotation(stringBuilder.toString());
                    tokenizer.annotate(aDoc);
                    tokens = aDoc.get(CoreAnnotations.TokensAnnotation.class);
                    tokenIterator = tokens.iterator();

                    return null;
                }
        );
        AccessController.doPrivileged((PrivilegedAction<Void>)
                () -> {
                    ESAnnotatedHTMLDocument doc = null;
                    FileInputStream fin = null;
                    ObjectInputStream ois = null;
                    try {
                        fin = new FileInputStream("/Users/longpham/Workspace/EntityAnnotation/serialized/00000.ser");
                        ois = new ObjectInputStream(fin);
                        doc = (ESAnnotatedHTMLDocument) ois.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (doc != null) {
//                        System.out.println(doc.getTitle());
//                        System.out.println(doc.get(CoreAnnotations.TokensAnnotation.class).size());
//                        for (CoreLabel token : doc.get(CoreAnnotations.TokensAnnotation.class)) {
//                            String word = token.word();
//                            if (token.containsKey(CustomizableCoreAnnotations.TypeAnnotation.class)) {
//                                word = token.get(CustomizableCoreAnnotations.TypeAnnotation.class);
//                            }
//                            System.out.println(word + " " + token.ner() + " " +
//                                    token.get(CustomizableCoreAnnotations.LayoutHeightAnnotation.class) + " " +
//                                    token.get(CustomizableCoreAnnotations.LayoutWidthAnnotation.class));
//                        }
//                        System.out.println(doc.getHeight() + " " + doc.getWidth());
                        tokens = doc.get(CoreAnnotations.TokensAnnotation.class);
                        tokenIterator = tokens.iterator();
                    }
                    return null;
                }
        );

//        ArrayList<Integer> entityPos = new ArrayList<>();
//        StringTokenizer tokenIterator = new StringTokenizer(stringBuilder.toString());
//        while (tokenIterator.hasMoreTokens()) {
//            final String token = tokenIterator.nextToken();
//            if (accept(token)) {
//            	    String[] tmp = token.split("\\Q|\\E");
//            	    int pos = Integer.parseInt(tmp[1].split("\\Q+\\E")[0]);
//            	    entityPos.add(pos);
//            }
//            Collections.sort(entityPos);
//            this.entityPositions = entityPos.iterator();
//        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        if (this.tokenIterator.hasNext()) {
            CoreLabel token = this.tokenIterator.next();
            AccessController.doPrivileged((PrivilegedAction<Void>)
                    () -> {
                        posIncrAtt.setPositionIncrement(1);
                        termAtt.append(token.word());
                        offsetAtt.setOffset(token.beginPosition(),token.endPosition());
                        return null;
                    }
            );
            return true;
        } 
        return false;
    }

//    @Override
//    public final void end() throws IOException {
//        super.end();
//        final int finalOffset = correctOffset(offset);
//        offsetAtt.setOffset(finalOffset, finalOffset);
//        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement());
//    }

    @Override
    public void reset() throws IOException {
        super.reset();
        tokenize(input);
    }

    private Annotator tokenizer = TokenizerFactory.getInstance().getTokenizer();
    List<CoreLabel> tokens;
    private Iterator<CoreLabel> tokenIterator;
}