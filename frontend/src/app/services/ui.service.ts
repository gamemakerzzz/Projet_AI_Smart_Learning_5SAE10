import { Injectable, signal } from '@angular/core';
export interface Notice { id: number; message: string; kind: 'success' | 'error'; }
@Injectable({ providedIn: 'root' })
export class UiService {
  readonly notices = signal<Notice[]>([]);
  readonly pending = signal(0);
  readonly saving = signal(0);
  readonly confirmation = signal<{ message: string; resolve: (answer: boolean) => void } | null>(null);
  private nextId = 0;
  notify(message: string, kind: 'success' | 'error'): void {
    if (this.notices().some(n => n.message === message)) return;
    const id = ++this.nextId;
    this.notices.update(items => [...items.slice(-3), { id, message, kind }]);
    setTimeout(() => this.dismiss(id), kind === 'error' ? 9000 : 4500);
  }
  dismiss(id: number): void { this.notices.update(items => items.filter(n => n.id !== id)); }
  confirm(message: string): Promise<boolean> {
    this.confirmation()?.resolve(false);
    return new Promise(resolve => this.confirmation.set({ message, resolve }));
  }
  answer(value: boolean): void {
    this.confirmation()?.resolve(value);
    this.confirmation.set(null);
  }
}
