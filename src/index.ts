import { registerPlugin } from '@capacitor/core';

import type { PdfSharePlugin } from './definitions';

const PdfShare = registerPlugin<PdfSharePlugin>('PdfShare', {
  web: () => import('./web').then((m) => new m.PdfShareWeb()),
});

export * from './definitions';
export { PdfShare };
