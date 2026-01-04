package com.jvmd.digitalurpaq_ai_agent.service.rag.util;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import com.jvmd.digitalurpaq_ai_agent.service.rag.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DocumentChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 100;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s+");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");

    public List<DocumentChunk> chunkDocument(RetrievalDocument document) {
        return chunkDocument(document, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<DocumentChunk> chunkDocument(RetrievalDocument document, int chunkSize, int overlap) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String text = document.getText();
        
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            

            if (paragraph.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++));
                    currentChunk = new StringBuilder();
                }

                chunks.addAll(chunkLargeParagraph(document, paragraph, chunkSize, overlap, chunkIndex));
                chunkIndex += chunks.size();
            } else {
                if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {

                    chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++));
                    

                    String overlapText = getOverlapText(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                }
                
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }
        

        if (currentChunk.length() > 0) {
            chunks.add(createChunk(document, currentChunk.toString(), chunkIndex));
        }
        
        log.info("Document '{}' chunked into {} parts", document.getName(), chunks.size());
        return chunks;
    }

    private List<DocumentChunk> chunkLargeParagraph(RetrievalDocument document, String paragraph, 
                                                     int chunkSize, int overlap, int startIndex) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_PATTERN.split(paragraph);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = startIndex;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(createChunk(document, currentChunk.toString(), chunkIndex++));
                
                String overlapText = getOverlapText(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText);
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(". ");
            }
            currentChunk.append(sentence);
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(document, currentChunk.toString(), chunkIndex));
        }
        
        return chunks;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        
        String overlapText = text.substring(text.length() - overlapSize);

        int sentenceStart = overlapText.indexOf(". ");
        if (sentenceStart > 0 && sentenceStart < overlapSize / 2) {
            return overlapText.substring(sentenceStart + 2);
        }
        
        return overlapText;
    }

    private DocumentChunk createChunk(RetrievalDocument document, String text, int index) {
        return new DocumentChunk(
                document.getId() + "_chunk_" + index,
                document.getId(),
                document.getName(),
                text.trim(),
                index
        );
    }

}
