import { registerPlugin } from '@capacitor/core';
import type { PdfSharePlugin } from './definitions';
import { PdfShareWeb } from './web';

const PdfShare = registerPlugin<PdfSharePlugin>('PdfShare', {
  web: new PdfShareWeb()
});

export * from './definitions';
export { PdfShare };