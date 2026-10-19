package com.smartlearn.flashcards;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import static org.springframework.http.HttpStatus.*;
import static com.smartlearn.flashcards.FlashcardModels.*;

@Component
public class PdfTextExtractor {
    public Document extract(MultipartFile file, int firstPage, Integer lastPage) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(BAD_REQUEST, "Choisissez un PDF non vide de 10 Mo maximum.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
                throw new ResponseStatusException(BAD_REQUEST, "Le fichier doit être un véritable document PDF.");
            try (var pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted() || !pdf.getCurrentAccessPermission().canExtractContent())
                    throw new ResponseStatusException(BAD_REQUEST, "Utilisez un PDF déverrouillé autorisant l'extraction de texte.");
                int end = lastPage == null ? pdf.getNumberOfPages() : lastPage;
                if (firstPage < 1 || end < firstPage || end > pdf.getNumberOfPages() || end - firstPage + 1 > DocumentLimits.MAX_UNITS)
                    throw new ResponseStatusException(BAD_REQUEST, "Sélectionnez une plage valide de " + DocumentLimits.MAX_UNITS + " pages maximum. Ce PDF contient " + pdf.getNumberOfPages() + " pages.");
                var pages = new ArrayList<Page>();
                int length = 0;
                var stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                for (int number = firstPage; number <= end; number++) {
                    stripper.setStartPage(number); stripper.setEndPage(number);
                    String text = stripper.getText(pdf).replace('\u0000', ' ').strip();
                    length += text.length();
                    if (length > DocumentLimits.MAX_TEXT_CHARACTERS)
                        throw new ResponseStatusException(BAD_REQUEST, "Cette sélection contient trop de texte. Choisissez moins de pages (600 000 caractères maximum).");
                    if (!text.isBlank()) pages.add(new Page(number, text));
                }
                if (length < 100)
                    throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Pas assez de texte lisible. Pour un PDF scanné, effectuez d'abord une reconnaissance de texte (OCR).");
                String name = file.getOriginalFilename() == null ? "Document.pdf" : file.getOriginalFilename().replace('\\', '/');
                name = name.substring(name.lastIndexOf('/') + 1);
                if (name.length() > 180) name = name.substring(0, 180);
                return new Document(name, pdf.getNumberOfPages(), java.util.List.copyOf(pages));
            }
        } catch (InvalidPasswordException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Ce PDF est protégé par un mot de passe. Déverrouillez-le avant l'envoi.");
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Ce PDF est illisible ou endommagé. Essayez un autre fichier.");
        }
    }
}
