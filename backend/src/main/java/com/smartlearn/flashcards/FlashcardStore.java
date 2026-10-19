package com.smartlearn.flashcards;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.smartlearn.flashcards.FlashcardModels.*;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class FlashcardStore {
    private final FlashcardDeckRepository decks;
    private final FlashcardRepository cards;
    private final ObjectMapper mapper;
    public record Study(Deck deck, Document document, Card card, Explanation explanation) {}
    public FlashcardStore(FlashcardDeckRepository decks, FlashcardRepository cards, ObjectMapper mapper) {
        this.decks = decks; this.cards = cards; this.mapper = mapper;
    }
    public Deck save(String owner, Deck generated, Document document) {
        var stored = new StoredFlashcardDeck(); stored.id = generated.id(); stored.ownerKey = owner;
        stored.title = generated.title(); stored.filename = generated.filename(); stored.language = generated.language();
        stored.difficulty = generated.difficulty(); stored.sourceLabel = generated.sourceLabel(); stored.totalPages = generated.totalPages();
        stored.createdAt = Instant.now(); stored.updatedAt = stored.createdAt;
        try { stored.sourceJson = mapper.writeValueAsString(document); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize flashcard source", e); }
        int position = 0;
        for (Card value : generated.cards()) {
            var card = new StoredFlashcard(); card.id = value.id(); card.deck = stored; card.position = position++;
            card.question = value.question(); card.answer = value.answer(); card.sourceNumber = value.page(); card.sourceQuote = value.source();
            stored.cards.add(card);
        }
        return view(decks.save(stored));
    }
    @Transactional(readOnly = true)
    public List<FlashcardDeckRepository.Summary> list(String owner) { return decks.listOwned(owner, PageRequest.of(0, 100)); }
    @Transactional(readOnly = true)
    public Deck latest(String owner) { return view(decks.findFirstByOwnerKeyOrderByUpdatedAtDesc(owner).orElseThrow(this::missing)); }
    @Transactional(readOnly = true)
    public Deck load(String owner, String id) { return view(owned(owner, id)); }
    @Transactional(readOnly = true)
    public Document document(String owner, String id) {
        try { return mapper.readValue(owned(owner, id).sourceJson, Document.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot read stored flashcard source", e); }
    }
    public void delete(String owner, String id) { decks.delete(owned(owner, id)); }
    public void deleteLatest(String owner) { decks.findFirstByOwnerKeyOrderByUpdatedAtDesc(owner).ifPresent(decks::delete); }
    public Card edit(String owner, String id, EditRequest request) {
        var card = cards.lockOwned(id, owner).orElseThrow(this::missing);
        card.question = request.question().strip(); card.answer = request.answer().strip(); card.remembered = false;
        card.explanation = null; materializedDeck(card).updatedAt = Instant.now();
        return view(card);
    }
    public Card review(String owner, String id, boolean remembered) {
        var card = cards.lockOwned(id, owner).orElseThrow(this::missing);
        card.remembered = remembered; materializedDeck(card).updatedAt = Instant.now(); return view(card);
    }
    @Transactional(readOnly = true)
    public Study study(String owner, String id) {
        var card = cards.findByIdAndDeckOwnerKey(id, owner).orElseThrow(this::missing);
        var deck = materializedDeck(card);
        try { return new Study(view(deck), mapper.readValue(deck.sourceJson, Document.class), view(card), view(card.explanation)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot read stored flashcard source", e); }
    }
    public Explanation explain(String owner, String id, Card expected, Explanation generated) {
        var card = cards.lockOwned(id, owner).orElseThrow(this::missing);
        if (!card.question.equals(expected.question()) || !card.answer.equals(expected.answer()))
            throw new ResponseStatusException(CONFLICT, "La carte a été modifiée pendant la génération. Redemandez une explication.");
        if (card.explanation == null) {
            var value = new StoredFlashcardExplanation(); value.card = card; value.explanation = generated.explanation();
            value.example = generated.example(); value.takeaway = generated.takeaway(); card.explanation = value;
        }
        return view(card.explanation);
    }
    private StoredFlashcardDeck owned(String owner, String id) { return decks.findByIdAndOwnerKey(id, owner).orElseThrow(this::missing); }
    private ResponseStatusException missing() { return new ResponseStatusException(NOT_FOUND, "Paquet ou carte introuvable dans votre bibliothèque."); }
    private StoredFlashcardDeck materializedDeck(StoredFlashcard card) { return (StoredFlashcardDeck) org.hibernate.Hibernate.unproxy(card.deck); }
    private Deck view(StoredFlashcardDeck value) {
        value = (StoredFlashcardDeck) org.hibernate.Hibernate.unproxy(value);
        return new Deck(value.id, value.title, value.filename, value.language, value.difficulty,
            value.totalPages, value.cards.stream().map(this::view).toList(), value.sourceLabel);
    }
    private Card view(StoredFlashcard value) {
        value = (StoredFlashcard) org.hibernate.Hibernate.unproxy(value);
        return new Card(value.id, value.question, value.answer, value.sourceNumber, value.sourceQuote, value.remembered);
    }
    private Explanation view(StoredFlashcardExplanation value) {
        if (value == null) return null;
        value = (StoredFlashcardExplanation) org.hibernate.Hibernate.unproxy(value);
        return new Explanation(value.explanation, value.example, value.takeaway);
    }
}
