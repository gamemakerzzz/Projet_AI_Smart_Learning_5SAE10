import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, OnDestroy, Output } from '@angular/core';
let modalId = 0;
@Directive({ selector: '[appModal]', standalone: true })
export class ModalA11yDirective implements AfterViewInit, OnDestroy {
  @Output() dismiss = new EventEmitter<void>();
  private previous: HTMLElement | null = document.activeElement as HTMLElement;
  private previousOverflow = document.body.style.overflow;
  constructor(private element: ElementRef<HTMLElement>) {}
  ngAfterViewInit(): void {
    const modal = this.element.nativeElement;
    modal.setAttribute('role', 'dialog'); modal.setAttribute('aria-modal', 'true');
    const heading = modal.querySelector('h2');
    if (heading) { heading.id ||= `modal-title-${++modalId}`; modal.setAttribute('aria-labelledby', heading.id); }
    document.body.style.overflow = 'hidden';
    (modal.querySelector('input, select, textarea') as HTMLElement | null)?.focus();
  }
  @HostListener('keydown', ['$event']) keydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { event.preventDefault(); this.dismiss.emit(); }
    if (event.key !== 'Tab') return;
    const focusable = Array.from(this.element.nativeElement.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href]')).filter(e => e.getClientRects().length > 0);
    const first = focusable[0], last = focusable[focusable.length - 1];
    if (!first) return;
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
  ngOnDestroy(): void { document.body.style.overflow = this.previousOverflow; this.previous?.focus(); }
}
