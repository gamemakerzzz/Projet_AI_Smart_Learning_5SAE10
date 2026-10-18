import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { VideoLessonComponent } from './video-lesson.component';

interface Card {
  id: string;
  question: string;
  answer: string;
  page: number;
  source: string;
  remembered?: boolean;
}
interface Deck {
  id: string;
  title: string;
  filename: string;
  language: string;
  difficulty: string;
  totalPages: number;
  sourceLabel?: string;
  cards: Card[];
}
interface Explanation {
  explanation: string;
  example: string;
  takeaway: string;
}
interface SavedDeck {
  id: string;
  title: string;
  filename: string;
  cardCount: number;
}

@Component({
  selector: 'app-flashcards',
  standalone: true,
  imports: [FormsModule, IconComponent, VideoLessonComponent],
  templateUrl: './flashcards.component.html',
  styleUrl: './flashcards.component.css',
})
export class FlashcardsComponent implements OnInit {
  private readonly changeDetector = inject(ChangeDetectorRef);
  file: File | null = null;
  count = 10;
  language = 'fr';
  difficulty = 'beginner';
  firstPage = 1;
  lastPage: number | null = null;
  configured = false;
  maxUnits = 300;
  connected = false;
  connecting = false;
  checking = true;
  model = 'gpt-6-luna';
  busy = false;
  explaining = false;
  saving = false;
  error = '';
  deck: Deck | null = null;
  index = 0;
  revealed = false;
  editing = false;
  editQuestion = '';
  editAnswer = '';
  explanations: Record<string, Explanation> = {};
  reviewed = new Set<string>();
  library: SavedDeck[] = [];
  deleteRequested = false;
  constructor(
    private http: HttpClient,
    public auth: AuthService,
    private route: ActivatedRoute,
  ) {}
  async ngOnInit(): Promise<void> {
    try {
      const status = await firstValueFrom(
        this.http.get<{
          configured: boolean;
          model: string;
          connected?: boolean;
          maxUnits?: number;
        }>('/api/flashcards/status'),
      );
      this.configured = status.configured;
      this.maxUnits = status.maxUnits ?? 300;
      this.model = status.model;
      this.connected = !!status.connected;
      const result = this.route.snapshot.queryParamMap.get('openai');
      if (result === 'failed')
        this.error =
          'La connexion OpenAI a échoué. Réessayez et vérifiez que votre compte autorise l’utilisation de son forfait pour cette application.';
      if (result === 'cancelled')
        this.error = 'La connexion OpenAI a été annulée. Vous pouvez réessayer.';
      try {
        this.setDeck(await firstValueFrom(this.http.get<Deck>('/api/flashcards')));
      } catch (error: any) {
        if (error.status !== 404) this.error = this.message(error);
      }
      this.library = await firstValueFrom(this.http.get<SavedDeck[]>('/api/flashcards/decks'));
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.checking = false;
      this.changeDetector.markForCheck();
    }
  }
  async loadSaved(id: string): Promise<void> {
    if (this.busy || this.saving || this.explaining) return;
    this.saving = true;
    this.error = '';
    try {
      this.setDeck(await firstValueFrom(this.http.get<Deck>(`/api/flashcards/decks/${id}`)));
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.saving = false;
      this.changeDetector.markForCheck();
    }
  }
  newDeck(): void {
    if (this.busy || this.saving || this.explaining) return;
    this.deck = null;
    this.file = null;
    this.firstPage = 1;
    this.lastPage = null;
    this.index = 0;
    this.revealed = false;
    this.editing = false;
    this.deleteRequested = false;
    this.explanations = {};
    this.reviewed.clear();
    this.error = '';
  }
  private setDeck(deck: Deck): void {
    this.deck = deck;
    this.index = 0;
    this.revealed = false;
    this.editing = false;
    this.deleteRequested = false;
    this.explanations = {};
    this.reviewed.clear();
    deck.cards.filter((card) => card.remembered).forEach((card) => this.reviewed.add(card.id));
  }
  async connectOpenAi(): Promise<void> {
    if (this.connecting) return;
    this.connecting = true;
    this.error = '';
    try {
      await this.csrf();
      const result = await firstValueFrom(
        this.http.post<{ url: string }>('/api/flashcards/openai/connect', {}),
      );
      const url = new URL(result.url);
      if (url.origin !== 'https://auth.openai.com') throw new Error('Invalid sign-in destination');
      window.location.assign(url.href);
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.connecting = false;
      this.changeDetector.markForCheck();
    }
  }
  async disconnectOpenAi(): Promise<void> {
    if (this.connecting) return;
    this.connecting = true;
    this.error = '';
    try {
      await this.csrf();
      await firstValueFrom(this.http.delete('/api/flashcards/openai'));
      const status = await firstValueFrom(
        this.http.get<{ configured: boolean }>('/api/flashcards/status'),
      );
      this.connected = false;
      this.configured = status.configured;
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.connecting = false;
      this.changeDetector.markForCheck();
    }
  }
  get card(): Card | null {
    return this.deck?.cards[this.index] ?? null;
  }
  get fileUnit(): string {
    const name = this.file?.name.toLowerCase() ?? '';
    return name.endsWith('.pptx') ? 'diapositive' : name.endsWith('.docx') ? 'section' : 'page';
  }
  get sourceLabel(): string {
    return this.deck?.sourceLabel || 'Page';
  }
  get explanation(): Explanation | null {
    return this.card ? (this.explanations[this.card.id] ?? null) : null;
  }
  choose(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.acceptFile(input.files?.[0] ?? null);
    input.value = '';
  }
  drop(event: DragEvent): void {
    event.preventDefault();
    if (!this.busy) this.acceptFile(event.dataTransfer?.files[0] ?? null);
  }
  acceptFile(file: File | null): void {
    this.error = '';
    if (!file) return;
    if (!/\.(pdf|pptx|docx)$/i.test(file.name) || file.size === 0 || file.size > 10 * 1024 * 1024) {
      this.file = null;
      this.error = 'Choisissez un fichier PDF, PPTX ou DOCX non vide de 10 Mo maximum.';
      return;
    }
    this.file = file;
    this.firstPage = 1;
    this.lastPage = null;
  }
  async generate(): Promise<void> {
    if (!this.file || this.busy) return;
    if (
      !Number.isInteger(this.firstPage) ||
      this.firstPage < 1 ||
      (this.lastPage !== null &&
        (!Number.isInteger(this.lastPage) ||
          this.lastPage < this.firstPage ||
          this.lastPage - this.firstPage >= this.maxUnits))
    ) {
      this.error = `Sélectionnez une plage valide de ${this.maxUnits} pages, diapositives ou sections maximum.`;
      return;
    }
    this.busy = true;
    this.error = '';
    try {
      await this.csrf();
      const data = new FormData();
      data.append('file', this.file);
      data.append('count', String(this.count));
      data.append('language', this.language);
      data.append('difficulty', this.difficulty);
      data.append('firstPage', String(this.firstPage));
      if (this.lastPage !== null) data.append('lastPage', String(this.lastPage));
      const deck = await firstValueFrom(this.http.post<Deck>('/api/flashcards', data));
      this.setDeck(deck);
      this.file = null;
      this.library = [
        { id: deck.id, title: deck.title, filename: deck.filename, cardCount: deck.cards.length },
        ...this.library.filter((value) => value.id !== deck.id),
      ].slice(0, 100);
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.busy = false;
      this.changeDetector.markForCheck();
    }
  }
  move(offset: number): void {
    if (!this.deck || this.explaining || this.saving) return;
    this.index = (this.index + offset + this.deck.cards.length) % this.deck.cards.length;
    this.revealed = false;
    this.editing = false;
    this.error = '';
  }
  async mark(known: boolean): Promise<void> {
    if (!this.card || !this.deck || this.saving || this.busy || this.explaining) return;
    const id = this.card.id;
    this.saving = true;
    this.error = '';
    let saved = false;
    try {
      await this.csrf();
      const updated = await firstValueFrom(
        this.http.patch<Card>(`/api/flashcards/cards/${id}/review`, { remembered: known }),
      );
      this.deck.cards[this.index] = updated;
      if (known) this.reviewed.add(id);
      else this.reviewed.delete(id);
      saved = true;
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.saving = false;
      if (saved) this.move(1);
      this.changeDetector.markForCheck();
    }
  }
  startEdit(): void {
    if (!this.card) return;
    this.editQuestion = this.card.question;
    this.editAnswer = this.card.answer;
    this.editing = true;
  }
  async save(): Promise<void> {
    if (
      !this.card ||
      !this.deck ||
      this.saving ||
      !this.editQuestion.trim() ||
      !this.editAnswer.trim()
    )
      return;
    this.saving = true;
    this.error = '';
    try {
      await this.csrf();
      const updated = await firstValueFrom(
        this.http.put<Card>(`/api/flashcards/cards/${this.card.id}`, {
          question: this.editQuestion,
          answer: this.editAnswer,
        }),
      );
      delete this.explanations[updated.id];
      this.reviewed.delete(updated.id);
      this.deck.cards[this.index] = updated;
      this.editing = false;
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.saving = false;
      this.changeDetector.markForCheck();
    }
  }
  async explain(): Promise<void> {
    if (!this.card || this.explaining || this.explanation) return;
    const id = this.card.id;
    this.explaining = true;
    this.error = '';
    try {
      await this.csrf();
      this.explanations[id] = await firstValueFrom(
        this.http.post<Explanation>(`/api/flashcards/cards/${id}/explain`, {}),
      );
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.explaining = false;
      this.changeDetector.markForCheck();
    }
  }
  export(): void {
    if (!this.deck) return;
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(this.deck, null, 2)], { type: 'application/json' }),
    );
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'smartlearn-flashcards.json';
    anchor.click();
    URL.revokeObjectURL(url);
  }
  async clear(): Promise<void> {
    if (!this.deck || this.busy || this.explaining || this.saving) return;
    const id = this.deck.id;
    this.saving = true;
    this.error = '';
    try {
      await this.csrf();
      await firstValueFrom(this.http.delete(`/api/flashcards/decks/${id}`));
      this.deck = null;
      this.library = this.library.filter((value) => value.id !== id);
      this.deleteRequested = false;
      this.explanations = {};
      this.reviewed.clear();
    } catch (error) {
      this.error = this.message(error);
    } finally {
      this.saving = false;
      this.changeDetector.markForCheck();
    }
  }
  private csrf(): Promise<unknown> {
    return firstValueFrom(this.http.get('/api/auth/csrf'));
  }
  private message(error: any): string {
    return (
      error?.error?.message ||
      (error?.status === 0
        ? 'Le serveur est indisponible. Réessayez dans un instant.'
        : 'La demande a échoué. Réessayez.')
    );
  }
}
