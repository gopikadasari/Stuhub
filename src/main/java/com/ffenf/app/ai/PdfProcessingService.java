package com.ffenf.app.ai;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfProcessingService {

    public String extractTextFromPdf(String filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Configure for better text extraction
            stripper.setSortByPosition(true);
            stripper.setSuppressDuplicateOverlappingText(true);
            
            // Extract text from all pages
            String fullText = stripper.getText(document);
            
            // Clean up the text
            fullText = fullText.replaceAll("\\s+", " ").trim();
            
            System.out.println("PDF Text Extraction - Total characters: " + fullText.length());
            System.out.println("PDF Text Extraction - First 500 chars: " + fullText.substring(0, Math.min(500, fullText.length())));
            
            return fullText;
        }
    }
}
