import { WebPlugin } from '@capacitor/core';
import html2pdf from 'html2pdf.js';

export class PdfShareWeb extends WebPlugin {
  constructor() {
    super();
  }

  async generateAndShare(): Promise<void> {
    try {
      const element = document.getElementById('printPage');
      if (!element) {
        throw new Error('Print element not found');
      }

      const opt = {
        margin: 20,
        filename: 'veterinary-dosage.pdf',
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { 
          scale: 2,
          useCORS: true,
          letterRendering: true
        },
        jsPDF: { 
          unit: 'mm', 
          format: 'a4', 
          orientation: 'portrait' 
        }
      };

      await html2pdf().set(opt).from(element).save();
      
    } catch (error) {
      console.error('Error generating PDF:', error);
      throw error;
    }
  }
}