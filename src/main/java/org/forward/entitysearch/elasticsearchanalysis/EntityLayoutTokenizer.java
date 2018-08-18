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
import edu.stanford.nlp.util.TypesafeMap;
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

    private static final String ENTITY_FIELD_PREFIX = "_entity_";
    private static final String TITLE_FIELD_PREFIX = "_title_";
    private static final String LAYOUT_FIELD_PREFIX = "_layout_";
    private static final String DO_LOAD_SERIALIZED_FILE_PREFIX = "_DO_LOAD_SERIALIZED_FILE_";
    private static final boolean DEBUG = false;
    public static final int NORMALIZATION_FACTOR_FOR_LAYOUT_INFO = 20;


    private int skippedPos = 0;
    private int curPos = 0;
    private boolean isLayoutField = false;
    private String entityTagName = null;

    private Class<? extends TypesafeMap.Key<String>> labelClass;
    private Class<? extends TypesafeMap.Key<Integer>> layoutClass;

    private List<CoreLabel> tokens;
    private Iterator<CoreLabel> tokenIterator;
    private String curSerializedFile = null;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    public EntityLayoutTokenizer() {
        super();
    }
    
//    public EntityLayoutTokenizer(String fieldName) {
//        super();
//    }

    private void tokenize(Reader input) throws IOException {
        if (DEBUG)
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

        String text = stringBuilder.toString();
        if (DEBUG)
            System.out.println(text);
        if (! text.contains(DO_LOAD_SERIALIZED_FILE_PREFIX)) {
            AccessController.doPrivileged((PrivilegedAction<Void>)
                    () -> {
                        // Privileged code goes here, for example:
                        Annotation aDoc = new Annotation(text);
                        Annotator tokenizer = TokenizerFactory.getInstance().getTokenizer();
                        tokenizer.annotate(aDoc);
                        tokens = aDoc.get(CoreAnnotations.TokensAnnotation.class);
                        tokenIterator = tokens.iterator();
                        if (DEBUG) {
                            System.out.println("This analysis is not from import");
                            System.out.println(tokens.size());
                        }
                        return null;
                    }
            );
        } else {
            String[] tmpText = text.split(DO_LOAD_SERIALIZED_FILE_PREFIX);
            String fieldName = tmpText[0];
            String fileName = tmpText[1];
            if (DEBUG) {
                System.out.println(fieldName);
                System.out.println(fileName);
            }

            if (fieldName.contains(TITLE_FIELD_PREFIX)) {
                if (DEBUG) {
                    System.out.println("=======");
                    System.out.println(fileName);
                }
                entityTagName = fieldName.replaceFirst(TITLE_FIELD_PREFIX,"");
            } else if (fieldName.contains(ENTITY_FIELD_PREFIX)) {
                entityTagName = fieldName.replaceFirst(ENTITY_FIELD_PREFIX,"");
            } else if (fieldName.contains(LAYOUT_FIELD_PREFIX)) {
                String[] tmp = fieldName.replaceFirst(LAYOUT_FIELD_PREFIX,"").split("_");
                String layoutField = tmp[0];
                entityTagName=tmp[1];
                try {
                    layoutClass = (Class<? extends TypesafeMap.Key<Integer>>) Class.forName("edu.stanford.nlp.ling.CustomizableCoreAnnotations$Layout"+layoutField+"Annotation");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                isLayoutField = true;
            }
            if (entityTagName != null) {
                try {
                    labelClass = (Class<? extends TypesafeMap.Key<String>>) Class.forName("edu.stanford.nlp.ling.CustomizableCoreAnnotations$"+entityTagName+"Annotation");
                } catch (ClassNotFoundException e) {
                    try {
                        labelClass = (Class<? extends TypesafeMap.Key<String>>) Class.forName("edu.stanford.nlp.ling.CoreAnnotations$"+entityTagName+"Annotation");
                    } catch (ClassNotFoundException e1) {
                        e.printStackTrace();
                        e1.printStackTrace();
                    }
                }
            } else {
                this.labelClass = CoreAnnotations.TextAnnotation.class;
            }

            if (!fileName.equals(curSerializedFile) || tokens == null) {
                curSerializedFile = fileName;
                System.out.println("Loading " + curSerializedFile);

                if (fieldName.contains(TITLE_FIELD_PREFIX)) {
                    AccessController.doPrivileged((PrivilegedAction<Void>)
                                    () -> {
                                        Annotation doc = null;
                                        FileInputStream fin;
                                        ObjectInputStream ois;
                                        try {
                                            fin = new FileInputStream(fileName);
                                            ois = new ObjectInputStream(fin);
                                            doc = (Annotation) ois.readObject();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (doc != null) {
                                            tokens = doc.get(CoreAnnotations.TokensAnnotation.class);
                                            if (DEBUG)
                                                System.out.println(tokens.size());
                                        }
                                        return null;
                                    }
                    );
                } else {
                    AccessController.doPrivileged((PrivilegedAction<Void>)
                                    () -> {
                                        ESAnnotatedHTMLDocument doc = null;
                                        FileInputStream fin;
                                        ObjectInputStream ois;
                                        try {
                                            fin = new FileInputStream(fileName);
                                            ois = new ObjectInputStream(fin);
                                            doc = (ESAnnotatedHTMLDocument) ois.readObject();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (doc != null) {
                                            tokens = doc.get(CoreAnnotations.TokensAnnotation.class);

                                            CoreLabel lastToken = tokens.get(tokens.size()-1);
                                            int lastOffset = lastToken.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

                                            // Add some extra artificial tokens to query with absolute value
                                            for (int i = 0; i <= 10; i++) {
                                                CoreLabel token = new CoreLabel();
                                                token.set(CustomizableCoreAnnotations.TypeAnnotation.class, "markerpage" + i);
                                                token.set(CustomizableCoreAnnotations.LayoutXAnnotation.class, (doc.getWidth()*i)/10);
                                                token.set(CustomizableCoreAnnotations.LayoutYAnnotation.class, (doc.getHeight()*i)/10);
                                                token.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class,lastOffset);
                                                token.set(CoreAnnotations.CharacterOffsetEndAnnotation.class,lastOffset);
                                                tokens.add(token);
                                            }

                                            // TODO: need to add screen size to config file
                                            for (int i = 0; i <= 4; i++) {
                                                CoreLabel token = new CoreLabel();
                                                token.set(CustomizableCoreAnnotations.TypeAnnotation.class, "markerscreen" + i);
                                                token.set(CustomizableCoreAnnotations.LayoutXAnnotation.class, (1024*i)/2);
                                                token.set(CustomizableCoreAnnotations.LayoutYAnnotation.class, (768*i)/2);
                                                token.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class,lastOffset);
                                                token.set(CoreAnnotations.CharacterOffsetEndAnnotation.class,lastOffset);
                                                tokens.add(token);
                                            }

                                            // add sigimg as the image with decent size
                                            // need to be behind the code above
                                            ArrayList<CoreLabel> addingTokens = new ArrayList<>();
                                            for (CoreLabel t : tokens) {
                                                if (t.containsKey(CustomizableCoreAnnotations.TypeAnnotation.class)
                                                        && t.get(CustomizableCoreAnnotations.TypeAnnotation.class).equals("img")
                                                        && t.get(CustomizableCoreAnnotations.LayoutHeightAnnotation.class) > 100
                                                        && t.get(CustomizableCoreAnnotations.LayoutWidthAnnotation.class) > 100) {
                                                    CoreLabel token = new CoreLabel(t);
                                                    token.set(CustomizableCoreAnnotations.TypeAnnotation.class,"sigimg");
                                                    addingTokens.add(token);
                                                    if (t.get(CustomizableCoreAnnotations.LayoutHeightAnnotation.class) > t.get(CustomizableCoreAnnotations.LayoutWidthAnnotation.class)) {
                                                        token = new CoreLabel(t);
                                                        token.set(CustomizableCoreAnnotations.TypeAnnotation.class,"verticalsigimg");
                                                        addingTokens.add(token);
                                                    }
                                                }
                                            }
                                            tokens.addAll(addingTokens);

                                            // Lucene does not allow the first token to have position zero, so add 1 to all of them
//                                for (CoreLabel token : tokens) {
//                                    if (token.containsKey(layoutClass)) {
//                                        token.set(layoutClass, token.get(layoutClass)/NORMALIZATION_FACTOR_FOR_LAYOUT_INFO +1);
//                                    }
//                                }

                                            if (DEBUG)
                                                System.out.println(tokens.size());
                                        }
                                        return null;
                                    }
                    );
                }
            } else {
                if (DEBUG) {
                    System.out.println("Already loaded " + curSerializedFile);
                }
            }

            if (isLayoutField) {
                Collections.sort(tokens, (o1, o2) -> {
                    if (!o1.containsKey(layoutClass) && !o2.containsKey(layoutClass)) {
                        return 0;
                    } if (!o1.containsKey(layoutClass)) {
                        return -1;
                    } else if (!o2.containsKey(layoutClass)) {
                        return 1;
                    } else {
                        return o1.get(layoutClass) - o2.get(layoutClass);
                    }
                });

            }

            tokenIterator = tokens.iterator();
        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        if (this.tokenIterator.hasNext()) {
            return AccessController.doPrivileged((PrivilegedAction<Boolean>)
                    () -> {
                        CoreLabel token = this.tokenIterator.next();
                        if (isLayoutField) {
                            while (!token.containsKey(labelClass)
                                    || token.get(labelClass) == null
                                    || token.get(labelClass).equalsIgnoreCase("O")
                                    || token.get(layoutClass) == null) {
                                if (tokenIterator.hasNext()) {
                                    token = this.tokenIterator.next();
                                } else {
                                    return false;
                                }
                            }
                            // Lucene does not allow the first token to have position zero, so add 1 to all of them
                            int newPos = token.get(layoutClass) / NORMALIZATION_FACTOR_FOR_LAYOUT_INFO + 1;
                            posIncrAtt.setPositionIncrement(newPos - curPos);
                            this.curPos = newPos;
                            offsetAtt.setOffset(newPos,newPos);
                            // so it is impossible to mix layout and regular info in a span near query
                            // because the offset and position basis is different
                            termAtt.append(token.get(labelClass));
                        } else {
                            skippedPos = 0;
                            while (!token.containsKey(labelClass)
                                    || token.get(labelClass) == null
                                    || token.get(labelClass).equalsIgnoreCase("O")) {
                                skippedPos++;
                                if (tokenIterator.hasNext()) {
                                    token = this.tokenIterator.next();
                                } else {
                                    return false;
                                }
                            }
//                        System.out.println(skippedPos);
//                        System.out.println(token.get(labelClass));
                            posIncrAtt.setPositionIncrement(skippedPos + 1);
                            offsetAtt.setOffset(token.beginPosition(),token.endPosition());
                            termAtt.append(token.get(labelClass));
                        }
                        return true;
                    }
            );
        } 
        return false;
    }

    @Override
    public final void end() throws IOException {
        super.end();
        final int finalOffset = tokens.get(tokens.size()-1).endPosition();
        offsetAtt.setOffset(finalOffset, finalOffset);
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement()+skippedPos);
        // for layout, skippedPos is always 0 but it is fine.
        // for entity, the final pos should be tokens.size()
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        skippedPos = 0;
        curPos = 0;
        entityTagName = null;
        isLayoutField = false;
        labelClass = null;
        layoutClass = null;
        tokenIterator = null;
        tokenize(input);
    }
}