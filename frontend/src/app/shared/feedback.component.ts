import { Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { UiService } from '../services/ui.service';
import { IconComponent } from './icon.component';
@Component({
  selector: 'app-feedback', standalone: true, imports: [IconComponent],
  template: `
    <div class="notices" aria-live="polite" aria-relevant="additions">
      @for (notice of ui.notices(); track notice.id) {
        <div class="notice" [class.notice-error]="notice.kind === 'error'">
          <app-icon [name]="notice.kind === 'success' ? 'check' : 'shield'" />
          <span>{{ notice.message }}</span>
          <button class="icon-button" aria-label="Fermer la notification" (click)="ui.dismiss(notice.id)"><app-icon name="close" /></button>
        </div>
      }
    </div>
    <dialog #dialog class="confirm-dialog" aria-labelledby="confirm-title" aria-describedby="confirm-description" (cancel)="cancel($event)">
      <div class="confirm-symbol"><app-icon name="shield" /></div>
      <h2 id="confirm-title">Confirmer la suppression</h2>
      <p id="confirm-description">{{ ui.confirmation()?.message }}</p>
      <p class="muted">Cette action est définitive.</p>
      <div class="dialog-actions">
        <button class="btn btn-secondary" autofocus (click)="ui.answer(false)">Annuler</button>
        <button class="btn btn-danger" (click)="ui.answer(true)">Supprimer</button>
      </div>
    </dialog>
  `
})
export class FeedbackComponent {
  readonly ui = inject(UiService);
  readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');
  constructor() {
    effect(() => {
      const element = this.dialog()?.nativeElement;
      if (!element) return;
      if (this.ui.confirmation() && !element.open) element.showModal();
      if (!this.ui.confirmation() && element.open) element.close();
    });
  }
  cancel(event: Event): void { event.preventDefault(); this.ui.answer(false); }
}
