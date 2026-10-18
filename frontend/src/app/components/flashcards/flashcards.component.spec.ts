import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FlashcardsComponent } from './flashcards.component';
import { AuthService } from '../../services/auth.service';
import { Component, Input } from '@angular/core';
import { VideoLessonComponent } from './video-lesson.component';

@Component({ selector: 'app-video-lesson', standalone: true, template: '' })
class VideoLessonStub {
  @Input() deckId = '';
  @Input() language = '';
  @Input() configured = false;
}

describe('Signed-in document flashcards', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FlashcardsComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: () => null } },
      ],
    });
    TestBed.overrideComponent(FlashcardsComponent, {
      remove: { imports: [VideoLessonComponent] },
      add: { imports: [VideoLessonStub] },
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('shows the AI configuration state inside the signed-in workspace', async () => {
    const fixture = TestBed.createComponent(FlashcardsComponent);
    fixture.detectChanges();
    http.expectOne('/api/flashcards/status').flush({ configured: false, model: 'gpt-6-luna' });
    await Promise.resolve();
    http
      .expectOne('/api/flashcards')
      .flush({ message: 'No deck' }, { status: 404, statusText: 'Not Found' });
    await Promise.resolve();
    http.expectOne('/api/flashcards/decks').flush([]);
    await fixture.whenStable();
    expect(fixture.componentInstance.configured).toBe(false);
    expect(fixture.componentInstance.error).toBe('');
    expect(fixture.nativeElement.textContent).toContain('Continue with ChatGPT');
    expect(fixture.nativeElement.textContent).toContain('votre espace SmartLearn');
    expect(fixture.nativeElement.textContent).not.toContain('Sans inscription');
    expect(fixture.nativeElement.textContent).not.toContain('Connexion au service…');
  });
  it('rejects non-PDFs before uploading', () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    component.acceptFile(new File(['text'], 'notes.txt'));
    expect(component.file).toBeNull();
    expect(component.error).toContain('PDF');
  });
  it('accepts PowerPoint and Word and uses their source units', () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    component.acceptFile(new File(['test'], 'lesson.PPTX'));
    expect(component.file?.name).toBe('lesson.PPTX');
    expect(component.fileUnit).toBe('diapositive');
    component.firstPage = 3;
    component.acceptFile(new File(['test'], 'notes.docx'));
    expect(component.file?.name).toBe('notes.docx');
    expect(component.fileUnit).toBe('section');
    expect(component.firstPage).toBe(1);
    component.acceptFile(new File(['test'], 'legacy.doc'));
    expect(component.file).toBeNull();
  });
  it('rejects ranges above the new limit before uploading', async () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    component.acceptFile(new File(['%PDF-content'], 'notes.pdf'));
    component.lastPage = 301;
    await component.generate();
    expect(component.error).toContain('300');
    expect(component.busy).toBe(false);
  });
  it('disconnects only this visitors connection without a SmartLearn login', async () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    component.connected = true;
    component.configured = true;
    const disconnected = component.disconnectOpenAi();
    http.expectOne('/api/auth/csrf').flush({ token: 'test' });
    await Promise.resolve();
    http.expectOne('/api/flashcards/openai').flush(null);
    await Promise.resolve();
    http.expectOne('/api/flashcards/status').flush({ configured: false, connected: false });
    await disconnected;
    expect(component.connected).toBe(false);
    expect(component.configured).toBe(false);
  });
  it('uploads with CSRF, keeps the answer hidden and supports source-grounded explanations', async () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    component.acceptFile(new File(['%PDF-content'], 'notes.pdf', { type: 'application/pdf' }));
    component.lastPage = 300;
    const generated = component.generate();
    http.expectOne('/api/auth/csrf').flush({ token: 'test' });
    await Promise.resolve();
    const request = http.expectOne('/api/flashcards');
    expect(request.request.body instanceof FormData).toBe(true);
    expect(request.request.body.get('file').name).toBe('notes.pdf');
    expect(request.request.body.get('lastPage')).toBe('300');
    request.flush({
      id: 'deck',
      title: 'Notes',
      filename: 'notes.pdf',
      language: 'fr',
      difficulty: 'beginner',
      totalPages: 1,
      cards: [
        { id: 'card', question: 'Question?', answer: 'Answer', page: 1, source: 'Source text' },
      ],
    });
    await generated;
    expect(component.revealed).toBe(false);
    const explanation = component.explain();
    http.expectOne('/api/auth/csrf').flush({ token: 'test' });
    await Promise.resolve();
    http
      .expectOne('/api/flashcards/cards/card/explain')
      .flush({ explanation: 'Details', example: 'Example', takeaway: 'Remember this' });
    await explanation;
    expect(component.explanation?.explanation).toBe('Details');
    const marked = component.mark(true);
    http.expectOne('/api/auth/csrf').flush({ token: 'test' });
    await Promise.resolve();
    http.expectOne('/api/flashcards/cards/card/review').flush({
      id: 'card',
      question: 'Question?',
      answer: 'Answer',
      page: 1,
      source: 'Source text',
      remembered: true,
    });
    await marked;
    expect(component.reviewed.size).toBe(1);
    expect(component.revealed).toBe(false);
  });
  it('reopens saved cards and restores persisted study progress', async () => {
    const component = TestBed.createComponent(FlashcardsComponent).componentInstance;
    const opened = component.loadSaved('saved-deck');
    http.expectOne('/api/flashcards/decks/saved-deck').flush({
      id: 'saved-deck',
      title: 'Saved notes',
      filename: 'notes.docx',
      language: 'fr',
      difficulty: 'beginner',
      totalPages: 1,
      sourceLabel: 'Section',
      cards: [
        {
          id: 'saved-card',
          question: 'Question?',
          answer: 'Answer',
          page: 1,
          source: 'Source',
          remembered: true,
        },
      ],
    });
    await opened;
    expect(component.deck?.id).toBe('saved-deck');
    expect(component.reviewed.has('saved-card')).toBe(true);
    expect(component.revealed).toBe(false);
  });
  it('creates a second packet without deleting the first and switches back with its progress', async () => {
    const fixture = TestBed.createComponent(FlashcardsComponent);
    const component = fixture.componentInstance;
    const first = {
      id: 'first',
      title: 'First course',
      filename: 'first.pdf',
      language: 'fr',
      difficulty: 'beginner',
      totalPages: 1,
      cards: [
        {
          id: 'first-card',
          question: 'First?',
          answer: 'First answer',
          page: 1,
          source: 'First source',
          remembered: true,
        },
      ],
    };
    fixture.detectChanges();
    http.expectOne('/api/flashcards/status').flush({ configured: true, model: 'gpt-6-luna' });
    await Promise.resolve();
    http.expectOne('/api/flashcards').flush(first);
    await Promise.resolve();
    http
      .expectOne('/api/flashcards/decks')
      .flush([{ id: first.id, title: first.title, filename: first.filename, cardCount: 1 }]);
    await fixture.whenStable();
    component.acceptFile(new File(['old'], 'old.pdf'));
    component.deleteRequested = true;
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    const button = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (element: any) => element.textContent.includes('Nouveau paquet'),
    ) as HTMLButtonElement;
    // jsdom has no layout/scrolling implementation.
    fixture.nativeElement.querySelector('.upload-panel').scrollIntoView = () => {};
    button.click();
    fixture.detectChanges();
    expect(component.deck).toBeNull();
    expect(component.file).toBeNull();
    expect(component.deleteRequested).toBe(false);
    expect(component.library.map((value) => value.id)).toEqual(['first']);
    http.expectNone((request) => request.method === 'DELETE');
    component.acceptFile(new File(['new course'], 'second.pdf'));
    const generated = component.generate();
    http.expectOne('/api/auth/csrf').flush({ token: 'test' });
    await Promise.resolve();
    const second = {
      ...first,
      id: 'second',
      title: 'Second course',
      filename: 'second.pdf',
      cards: [{ ...first.cards[0], id: 'second-card', remembered: false }],
    };
    http.expectOne('/api/flashcards').flush(second);
    await generated;
    fixture.detectChanges();
    expect(component.library.map((value) => value.id)).toEqual(['second', 'first']);
    expect(fixture.nativeElement.querySelectorAll('.saved-decks button').length).toBe(2);
    expect(component.file).toBeNull();
    const reopened = component.loadSaved('first');
    http.expectOne('/api/flashcards/decks/first').flush(first);
    await reopened;
    fixture.detectChanges();
    expect(component.deck?.id).toBe('first');
    expect(component.reviewed.has('first-card')).toBe(true);
    expect(component.library.length).toBe(2);
    expect(
      fixture.nativeElement.querySelector('.saved-decks button.selected').textContent,
    ).toContain('First course');
  });
});
