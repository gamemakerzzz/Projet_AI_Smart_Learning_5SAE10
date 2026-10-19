package com.smartlearn;

import com.smartlearn.flashcards.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTests {
    final DocumentTextExtractor extractor = new DocumentTextExtractor(new PdfTextExtractor());
    static final String TEXT = "A primary key uniquely identifies each row in a database table. Two students can share a name but each student has a unique identifier.";
    @Test void acceptsPdfAtNewPageLimitAndOverPreviousTextLimit() throws Exception {
        byte[] bytes;
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 0; i < 300; i++) {
                var page = new PDPage(); document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    content.beginText(); content.setFont(font, 8); content.newLineAtOffset(20, 750);
                    for (int line = 0; line < 2; line++) { content.showText(TEXT); content.newLineAtOffset(0, -12); }
                    content.endText();
                }
            }
            document.save(output); bytes = output.toByteArray();
        }
        var result = extractor.extract(new MockMultipartFile("file", "long.pdf", "application/pdf", bytes), 1, null);
        assertEquals(300, result.pages().size());
        assertTrue(result.pages().stream().mapToInt(p -> p.text().length()).sum() > 60000);
    }
    @Test void acceptsLargeWordAndPowerPointSelections() throws Exception {
        var text = new StringBuilder();
        for (int i = 0; i < 1100; i++) text.append(TEXT).append(java.util.UUID.nameUUIDFromBytes(Integer.toString(i).getBytes())).append('\n');
        var doc = extractor.extract(new MockMultipartFile("file", "long.docx", "application/octet-stream", word(text.toString())), 1, null);
        assertTrue(doc.pages().size() > 40);
        assertTrue(doc.pages().stream().mapToInt(p -> p.text().length()).sum() > 60000);
        try (var slides = new XMLSlideShow(); var output = new ByteArrayOutputStream()) {
            for (int i = 0; i < 50; i++) slides.createSlide().createTextBox().setText(TEXT + i);
            slides.write(output);
            var ppt = extractor.extract(new MockMultipartFile("file", "long.pptx", "application/octet-stream", output.toByteArray()), 1, null);
            assertEquals(50, ppt.pages().size()); assertEquals(50, ppt.pages().get(49).number());
        }
    }
    byte[] word(String text) throws Exception {
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            var table = document.createTable(1, 1); table.getRow(0).getCell(0).setText("Table content is included in the extracted lesson.");
            document.write(output); return output.toByteArray();
        }
    }
    byte[] slides(boolean blank) throws Exception {
        try (var presentation = new XMLSlideShow(); var output = new ByteArrayOutputStream()) {
            presentation.createSlide();
            if (!blank) {
                var slide = presentation.createSlide(); slide.createTextBox().setText(TEXT);
                var table = slide.createTable(1, 1); table.getCell(0, 0).setText("Slide table content.");
            }
            presentation.write(output); return output.toByteArray();
        }
    }
    @Test void readsWordParagraphsAndTablesAndLabelsLogicalSections() throws Exception {
        var result = extractor.extract(new MockMultipartFile("file", "lesson.docx", "application/octet-stream", word(TEXT)), 1, null);
        assertEquals("Section", result.sourceLabel()); assertEquals(1, result.totalPages());
        assertTrue(result.pages().get(0).text().contains(TEXT));
        assertTrue(result.pages().get(0).text().contains("Table content"));
        var longResult = extractor.extract(new MockMultipartFile("file", "long.docx", "application/octet-stream", word(TEXT.repeat(60))), 2, 2);
        assertEquals(2, longResult.pages().get(0).number()); assertTrue(longResult.totalPages() > 2);
    }
    @Test void readsPowerPointShapesAndTablesWithoutRenumberingBlankSlides() throws Exception {
        var result = extractor.extract(new MockMultipartFile("file", "LESSON.PPTX", "application/octet-stream", slides(false)), 2, 2);
        assertEquals("Diapositive", result.sourceLabel()); assertEquals(2, result.totalPages());
        assertEquals(2, result.pages().get(0).number()); assertTrue(result.pages().get(0).text().contains(TEXT));
        assertTrue(result.pages().get(0).text().contains("Slide table content"));
    }
    @Test void rejectsRenamedCorruptBlankAndOutOfRangeDocuments() throws Exception {
        assertThrows(ResponseStatusException.class, () -> extractor.extract(new MockMultipartFile("file", "fake.docx", "application/octet-stream", slides(false)), 1, null));
        assertThrows(ResponseStatusException.class, () -> extractor.extract(new MockMultipartFile("file", "fake.pptx", "application/octet-stream", "not an Office file".getBytes()), 1, null));
        assertThrows(ResponseStatusException.class, () -> extractor.extract(new MockMultipartFile("file", "legacy.doc", "application/octet-stream", word(TEXT)), 1, null));
        assertThrows(ResponseStatusException.class, () -> extractor.extract(new MockMultipartFile("file", "lesson.pptx", "application/octet-stream", slides(false)), 3, 3));
        var blank = assertThrows(ResponseStatusException.class, () -> extractor.extract(new MockMultipartFile("file", "blank.pptx", "application/octet-stream", slides(true)), 1, null));
        assertEquals(422, blank.getStatusCode().value());
    }
}
