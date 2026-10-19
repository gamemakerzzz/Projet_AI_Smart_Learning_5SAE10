import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface LessonScene {
  title: string;
  bullets: string[];
  example: string;
  narration: string;
  page: number;
  source: string;
}
interface VideoStatus {
  status: 'none' | 'writing' | 'rendering' | 'ready' | 'failed';
  message: string;
  duration: number;
  lesson: {
    title: string;
    language: string;
    sourceLabel: string;
    scenes: LessonScene[];
    narrator?: string;
  } | null;
}

@Component({
  selector: 'app-video-lesson',
  standalone: true,
  template: `
    <section class="video-lesson" aria-labelledby="video-title">
      <span class="eyebrow">APPRENDRE EN VIDÉO</span>
      <h2 id="video-title">Votre cours, expliqué à voix haute.</h2>
      <p>
        Une courte leçon créée par l’IA à partir du même document : diapositives, exemples
        illustratifs et sous-titres.
      </p>
      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }
      @if (loading) {
        <p role="status">Chargement de la vidéo…</p>
      }
      @if (video?.status === 'ready') {
        <h3>{{ video?.lesson?.title }}</h3>
        <video
          controls
          preload="metadata"
          [attr.aria-label]="video?.lesson?.title"
          [src]="base + '/media'"
        >
          <track
            kind="captions"
            [src]="base + '/captions'"
            [srclang]="language"
            label="Sous-titres"
          />
          Votre navigateur ne prend pas en charge la lecture vidéo.
        </video>
        <div class="actions">
          <span>{{ minutes }} min · {{ video?.lesson?.scenes?.length }} diapositives</span>
          <a [href]="base + '/media?download=true'" download="smartlearn-lesson.mp4"
            >Télécharger le MP4 ↓</a
          >
        </div>
        <p class="small">
          Narration : {{ video?.lesson?.narrator || 'Voix de la génération précédente' }}
        </p>
        <button (click)="generate(true)" [disabled]="pending">
          {{ starting ? 'Préparation…' : 'Régénérer la vidéo' }}
        </button>
        <p class="small">
          La régénération reprend le scénario enregistré et remplace la vidéo avec la voix actuelle.
        </p>
        @if (!supported) {
          <p class="small">{{ capabilityMessage }}</p>
        }
        <details>
          <summary>Lire le cours et vérifier les sources</summary>
          @for (scene of video?.lesson?.scenes; track $index) {
            <article>
              <h4>{{ $index + 1 }}. {{ scene.title }}</h4>
              <p>{{ scene.narration }}</p>
              <strong>{{ video?.lesson?.sourceLabel }} {{ scene.page }}</strong>
              <blockquote>{{ scene.source }}</blockquote>
            </article>
          }
        </details>
      } @else if (pending) {
        <div class="progress" role="status" aria-live="polite">
          <span class="pulse"></span>
          {{ video?.message || 'L’IA prépare le scénario…' }}
        </div>
        <p class="small">
          La création peut prendre quelques minutes. Vous pouvez continuer à réviser et revenir à ce
          paquet.
        </p>
        @if (error) {
          <button (click)="refresh()">Actualiser l’état</button>
        }
      } @else if (!loading) {
        @if (video?.status === 'failed') {
          <p role="status">{{ video?.message }}</p>
        }
        <button (click)="generate()" [disabled]="starting">
          {{ video?.status === 'failed' ? 'Réessayer la vidéo' : 'Générer la leçon vidéo' }}
        </button>
        @if (!configured && !video?.lesson) {
          <p class="small">Connectez votre compte OpenAI ci-dessus pour créer la leçon.</p>
        }
        @if (!supported) {
          <p class="small">
            {{
              capabilityMessage || 'Aucun modèle vocal n’est installé pour la langue de ce paquet.'
            }}
          </p>
        }
        @if (error) {
          <button class="secondary" (click)="refresh(true)">Actualiser l’état</button>
        }
      }
      <p class="small disclosure">
        @if (narrationLocal) {
          Voix IA gratuite Piper. La narration est générée sur ce serveur, sans clé API ni service
          payant.
        } @else {
          Voix IA Azure Speech. Le texte de narration est envoyé à Microsoft pour créer l’audio.
        }
        Sous-titres synchronisés par groupes de phrases. La leçon résume des notions choisies du
        document.
      </p>
    </section>
  `,
  styleUrl: './video-lesson.component.css',
})
export class VideoLessonComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) deckId = '';
  @Input({ required: true }) language = 'fr';
  @Input() configured = false;
  private http = inject(HttpClient);
  private changeDetector = inject(ChangeDetectorRef);
  video: VideoStatus | null = null;
  languages: string[] = [];
  narrationLocal = true;
  capabilityMessage = '';
  loading = false;
  starting = false;
  error = '';
  private timer?: ReturnType<typeof setTimeout>;
  private revision = 0;
  private currentDeck = '';
  private destroyed = false;
  get base(): string {
    return `/api/flashcards/decks/${encodeURIComponent(this.deckId)}/video`;
  }
  get supported(): boolean {
    return this.languages.includes(this.language);
  }
  get pending(): boolean {
    return this.starting || this.video?.status === 'writing' || this.video?.status === 'rendering';
  }
  get minutes(): string {
    return ((this.video?.duration ?? 0) / 60).toFixed(1);
  }
  ngOnChanges(): void {
    if (this.deckId === this.currentDeck) return;
    this.currentDeck = this.deckId;
    this.revision++;
    this.video = null;
    this.starting = false;
    clearTimeout(this.timer);
    void this.refresh(true);
  }
  ngOnDestroy(): void {
    this.destroyed = true;
    this.revision++;
    clearTimeout(this.timer);
  }
  async refresh(capabilities = false): Promise<void> {
    const revision = this.revision;
    const base = this.base;
    this.loading = !this.video;
    this.error = '';
    clearTimeout(this.timer);
    try {
      if (capabilities) {
        const result = await firstValueFrom(
          this.http.get<{ languages: string[]; message: string; narrationLocal?: boolean }>(
            '/api/flashcards/video/capabilities',
          ),
        );
        if (!this.active(revision)) return;
        this.languages = result.languages;
        this.capabilityMessage = result.message;
        this.narrationLocal = result.narrationLocal ?? true;
      }
      const result = await firstValueFrom(this.http.get<VideoStatus>(base));
      if (!this.active(revision)) return;
      this.video = result;
      if (this.pending) this.poll();
    } catch (error: any) {
      if (this.active(revision))
        this.error =
          error?.error?.message || 'Impossible de charger la vidéo. Actualisez son état.';
    } finally {
      if (this.active(revision)) {
        this.loading = false;
        this.changeDetector.markForCheck();
      }
    }
  }
  async generate(regenerate = false): Promise<void> {
    if (this.pending) return;
    const revision = this.revision;
    const base = this.base;
    this.starting = true;
    this.error = '';
    try {
      // Configuration may have been filled in since this deck was opened.
      const capabilities = await firstValueFrom(
        this.http.get<{ languages: string[]; message: string; narrationLocal?: boolean }>(
          '/api/flashcards/video/capabilities',
        ),
      );
      if (!this.active(revision)) return;
      this.languages = capabilities.languages;
      this.capabilityMessage = capabilities.message;
      this.narrationLocal = capabilities.narrationLocal ?? true;
      if (!this.supported) {
        this.error =
          this.capabilityMessage ||
          'Le modèle vocal n’est pas disponible pour la langue de ce paquet.';
        return;
      }
      if (!regenerate && !this.configured && !this.video?.lesson) {
        this.error = 'Connectez votre compte OpenAI ci-dessus pour créer le scénario de la leçon.';
        return;
      }
      await firstValueFrom(this.http.get('/api/auth/csrf'));
      if (!this.active(revision)) return;
      const result = await firstValueFrom(
        this.http.post<VideoStatus>(base + (regenerate ? '?regenerate=true' : ''), {}),
      );
      if (!this.active(revision)) return;
      this.video = result;
      if (result.status === 'writing' || result.status === 'rendering') this.poll();
    } catch (error: any) {
      if (this.active(revision))
        this.error = error?.error?.message || 'La vidéo n’a pas pu être générée. Réessayez.';
    } finally {
      if (this.active(revision)) {
        this.starting = false;
        this.changeDetector.markForCheck();
      }
    }
  }
  private active(revision: number): boolean {
    return !this.destroyed && revision === this.revision;
  }
  private poll(): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => void this.refresh(), 3000);
  }
}
