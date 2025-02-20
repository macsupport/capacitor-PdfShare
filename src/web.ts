import { WebPlugin } from '@capacitor/core';

import type { PdfSharePlugin } from './definitions';

export class PdfShareWeb extends WebPlugin implements PdfSharePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
