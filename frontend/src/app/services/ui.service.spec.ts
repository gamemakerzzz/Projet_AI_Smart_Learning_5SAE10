import { UiService } from './ui.service';
describe('Action feedback', () => {
  it('requires a positive confirmation before deleting', async () => {
    const ui = new UiService();
    const cancelled = ui.confirm('Supprimer cette formation ?');
    ui.answer(false);
    expect(await cancelled).toBe(false);
    expect(ui.confirmation()).toBeNull();
    const accepted = ui.confirm('Supprimer cette formation ?');
    ui.answer(true);
    expect(await accepted).toBe(true);
  });
  it('cancels a previous confirmation rather than leaving it pending', async () => {
    const ui = new UiService();
    const first = ui.confirm('First');
    const second = ui.confirm('Second');
    expect(await first).toBe(false);
    ui.answer(false);
    expect(await second).toBe(false);
  });
});
