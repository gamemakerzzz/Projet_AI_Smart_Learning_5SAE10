package com.smartlearn.flashcards;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import static com.smartlearn.flashcards.FlashcardModels.*;
import static org.springframework.http.HttpStatus.*;

@Component
public class DocumentTextExtractor {
    private final PdfTextExtractor pdf;
    static { ZipSecureFile.setMaxEntrySize(24L * 1024 * 1024); }
    public DocumentTextExtractor(PdfTextExtractor pdf) { this.pdf = pdf; }
    public Document extract(MultipartFile file, int firstPage, Integer lastPage) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(BAD_REQUEST, "Choisissez un PDF, PPTX ou DOCX non vide de 10 Mo maximum.");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (extension.equals("pdf")) return pdf.extract(file, firstPage, lastPage);
        if (!Set.of("pptx", "docx").contains(extension))
            throw new ResponseStatusException(BAD_REQUEST, "Formats acceptés : PDF, PPTX et DOCX. Les anciens formats PPT et DOC ne sont pas pris en charge.");
        try {
            byte[] bytes = file.getBytes();
            validateArchive(bytes, extension);
            List<Page> units = new ArrayList<>();
            String label;
            if (extension.equals("pptx")) {
                label = "Diapositive";
                try (var presentation = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
                    int index = 1;
                    for (var slide : presentation.getSlides()) {
                        StringBuilder text = new StringBuilder();
                        slideText(slide.getShapes(), text);
                        units.add(new Page(index++, text.toString().strip()));
                    }
                }
            } else {
                label = "Section";
                try (var document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                    StringBuilder text = new StringBuilder(); wordText(document.getBodyElements(), text);
                    String value = text.toString().strip();
                    int start = 0;
                    while (start < value.length()) {
                        int end = Math.min(start + 3000, value.length());
                        if (end < value.length()) {
                            int boundary = value.lastIndexOf(' ', end);
                            if (boundary > start + 2000) end = boundary;
                        }
                        units.add(new Page(units.size() + 1, value.substring(start, end).strip()));
                        start = end;
                    }
                }
            }
            int end = lastPage == null ? units.size() : lastPage;
            if (units.isEmpty()) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Ce document ne contient pas de texte lisible. Le texte présent uniquement dans des images nécessite un OCR.");
            if (firstPage < 1 || end < firstPage || end > units.size() || end - firstPage + 1 > DocumentLimits.MAX_UNITS)
                throw new ResponseStatusException(BAD_REQUEST, "Sélectionnez une plage valide de " + DocumentLimits.MAX_UNITS + " diapositives ou sections maximum. Ce document en contient " + units.size() + ".");
            var selected = units.subList(firstPage - 1, end).stream().filter(p -> !p.text().isBlank()).toList();
            int length = selected.stream().mapToInt(p -> p.text().length()).sum();
            if (length < 100) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Pas assez de texte lisible dans la sélection. Choisissez d'autres diapositives ou sections.");
            if (length > DocumentLimits.MAX_TEXT_CHARACTERS) throw new ResponseStatusException(BAD_REQUEST, "La sélection dépasse 600 000 caractères. Choisissez moins de diapositives ou sections.");
            return new Document(name.substring(0, Math.min(name.length(), 180)), units.size(), List.copyOf(selected), label);
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(BAD_REQUEST, "Ce document Office est illisible, endommagé ou protégé. Utilisez un fichier PPTX ou DOCX déverrouillé."); }
    }
    private void validateArchive(byte[] bytes, String extension) throws IOException {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') throw new ResponseStatusException(BAD_REQUEST, "Le contenu du fichier ne correspond pas au format Office annoncé.");
        boolean main = false, types = false;
        long expanded = 0; int entries = 0;
        byte[] buffer = new byte[8192];
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > 2000) throw new ResponseStatusException(BAD_REQUEST, "Ce document contient trop d'éléments internes.");
                if (entry.getName().equals(extension.equals("docx") ? "word/document.xml" : "ppt/presentation.xml")) main = true;
                if (entry.getName().equals("[Content_Types].xml")) types = true;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expanded += read;
                    if (expanded > 24L * 1024 * 1024) throw new ResponseStatusException(BAD_REQUEST, "Ce document est trop volumineux après décompression.");
                }
            }
        }
        if (!main || !types) throw new ResponseStatusException(BAD_REQUEST, "Le contenu du fichier ne correspond pas au format Office annoncé.");
    }
    private void slideText(List<XSLFShape> shapes, StringBuilder text) {
        for (var shape : shapes) {
            if (shape instanceof XSLFTextShape value) append(text, value.getText());
            else if (shape instanceof XSLFTable table) for (var row : table.getRows()) for (var cell : row.getCells()) append(text, cell.getText());
            else if (shape instanceof XSLFGroupShape group) slideText(group.getShapes(), text);
        }
    }
    private void wordText(List<IBodyElement> elements, StringBuilder text) {
        for (var element : elements) {
            if (element instanceof XWPFParagraph paragraph) append(text, paragraph.getText());
            else if (element instanceof XWPFTable table) for (var row : table.getRows()) for (var cell : row.getTableCells()) wordText(cell.getBodyElements(), text);
        }
    }
    private void append(StringBuilder builder, String text) {
        if (builder.length() + text.length() > 1_000_000) throw new ResponseStatusException(BAD_REQUEST, "Ce document contient trop de texte. Divisez-le en fichiers plus petits.");
        if (!text.isBlank()) builder.append(text.replace('\u0000', ' ')).append('\n');
    }
}
