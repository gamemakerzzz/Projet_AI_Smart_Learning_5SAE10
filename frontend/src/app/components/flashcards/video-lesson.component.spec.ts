import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { VideoLessonComponent } from './video-lesson.component';

describe('Narrated document lesson', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [VideoLessonComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  async function setup(language = 'fr', status = 'none') {
    const fixture = TestBed.createComponent(VideoLessonComponent);
    fixture.componentRef.setInput('deckId', 'saved');
    fixture.componentRef.setInput('language', language);
    fixture.componentRef.setInput('configured', true);
    fixture.detectChanges();
    http
      .expectOne('/api/flashcards/video/capabilities')
      .flush({ languages: ['fr', 'en'], message: '' });
    await Promise.resolve();
    http
      .expectOne('/api/flashcards/decks/saved/video')
      .flush({ status, message: '', duration: 60, lesson: null });
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }
  it('generates from the saved document with CSRF and offers the actual MP4', async () => {
    const fixture = await setup();
    fixture.componentInstance.languages = [];
    const generated = fixture.componentInstance.generate();
    http
      .expectOne('/api/flashcards/video/capabilities')
      .flush({ languages: ['fr', 'en'], message: '' });
    await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    const request = http.expectOne('/api/flashcards/decks/saved/video');
    expect(request.request.method).toBe('POST');
    request.flush({
      status: 'ready',
      duration: 90,
      message: '',
      lesson: { title: 'Comprendre', language: 'fr', sourceLabel: 'Page', scenes: [] },
    });
    await generated;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('video').getAttribute('src')).toBe(
      '/api/flashcards/decks/saved/video/media',
    );
    expect(fixture.nativeElement.querySelector('a[download]').getAttribute('href')).toContain(
      'download=true',
    );
    expect(fixture.nativeElement.textContent).toContain('1.5 min');
    fixture.destroy();
  });
  it('explains unavailable narration languages and does not spend an AI request', async () => {
    const fixture = await setup('ar');
    expect(fixture.componentInstance.supported).toBe(false);
    const generated = fixture.componentInstance.generate();
    http
      .expectOne('/api/flashcards/video/capabilities')
      .flush({ languages: [], message: 'Azure Speech : clé et région manquantes.' });
    await generated;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'clé et région manquantes',
    );
    fixture.destroy();
  });
  it('regenerates a saved lesson with a free local voice without reconnecting the script AI', async () => {
    const fixture = await setup('fr', 'ready');
    fixture.componentRef.setInput('configured', false);
    fixture.detectChanges();
    const regenerated = fixture.componentInstance.generate(true);
    http
      .expectOne('/api/flashcards/video/capabilities')
      .flush({ languages: ['fr', 'en'], message: '' });
    await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    const request = http.expectOne('/api/flashcards/decks/saved/video?regenerate=true');
    expect(request.request.method).toBe('POST');
    request.flush({
      status: 'ready',
      duration: 60,
      lesson: {
        title: 'Leçon',
        language: 'fr',
        sourceLabel: 'Page',
        scenes: [],
        narrator: 'Piper · fr_FR-siwis-medium',
      },
    });
    await regenerated;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Piper · fr_FR-siwis-medium');
    expect(fixture.nativeElement.textContent).toContain('sans clé API');
    expect(fixture.nativeElement.textContent).not.toContain('envoyé à Microsoft');
    fixture.destroy();
  });
  it('ignores a late generation response after switching to another saved deck', async () => {
    const fixture = await setup();
    const generation = fixture.componentInstance.generate();
    http
      .expectOne('/api/flashcards/video/capabilities')
      .flush({ languages: ['fr', 'en'], message: '' });
    await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    const oldRequest = http.expectOne('/api/flashcards/decks/saved/video');
    fixture.componentRef.setInput('deckId', 'other');
    fixture.detectChanges();
    http.expectOne('/api/flashcards/video/capabilities').flush({ languages: ['fr'], message: '' });
    await Promise.resolve();
    http
      .expectOne('/api/flashcards/decks/other/video')
      .flush({ status: 'none', duration: 0, lesson: null });
    oldRequest.flush({
      status: 'ready',
      duration: 30,
      lesson: { title: 'Old lesson', scenes: [] },
    });
    await generation;
    await fixture.whenStable();
    expect(fixture.componentInstance.video?.status).toBe('none');
    expect(fixture.componentInstance.base).toContain('/other/');
    fixture.destroy();
  });
});
