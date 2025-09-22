import { WebPlugin } from '@capacitor/core';
// @ts-ignore
import html2pdf from 'html2pdf.js';
import type { PdfShareOptions, PdfShareResult } from './definitions';

export class PdfShareWeb extends WebPlugin {
  constructor() {
    super();
  }

  /**
   * Validate options and provide defaults
   */
  private getValidatedOptions(options?: PdfShareOptions): Required<PdfShareOptions> {
    return {
      elementId: options?.elementId || 'printPage',
      filename: options?.filename || 'veterinary-dosage',
      title: options?.title || 'Veterinary Dosage',
      orientation: options?.orientation || 'portrait',
      format: options?.format || 'a4',
      margins: {
        top: options?.margins?.top || 20,
        bottom: options?.margins?.bottom || 20,
        left: options?.margins?.left || 20,
        right: options?.margins?.right || 20,
      },
      quality: Math.min(Math.max(options?.quality || 0.98, 0.1), 1.0),
      includeBackground: options?.includeBackground !== false,
      scale: Math.min(Math.max(options?.scale || 2, 1), 4),
    };
  }

  /**
   * Create html2pdf options from validated options
   */
  private createHtml2PdfOptions(validatedOptions: Required<PdfShareOptions>) {
    return {
      margin: [
        validatedOptions.margins.top,
        validatedOptions.margins.right,
        validatedOptions.margins.bottom,
        validatedOptions.margins.left
      ],
      filename: `${validatedOptions.filename}.pdf`,
      image: {
        type: 'jpeg',
        quality: validatedOptions.quality
      },
      html2canvas: {
        scale: validatedOptions.scale,
        useCORS: true,
        letterRendering: true,
        backgroundColor: validatedOptions.includeBackground ? null : '#ffffff',
        allowTaint: true,
        scrollX: 0,
        scrollY: 0
      },
      jsPDF: {
        unit: 'mm',
        format: validatedOptions.format,
        orientation: validatedOptions.orientation,
        compress: true
      }
    };
  }

  async generateAndShare(options?: PdfShareOptions): Promise<PdfShareResult> {
    try {
      console.log('🔧 Web: Starting PDF generation with options:', options);

      const validatedOptions = this.getValidatedOptions(options);
      const element = document.getElementById(validatedOptions.elementId);

      if (!element) {
        console.error(`❌ Element with ID '${validatedOptions.elementId}' not found`);
        return {
          success: false,
          error: `Element with ID '${validatedOptions.elementId}' not found`
        };
      }

      const html2pdfOptions = this.createHtml2PdfOptions(validatedOptions);
      console.log('📄 Generating PDF with options:', html2pdfOptions);

      // Generate and download PDF
      const pdfInstance = html2pdf().set(html2pdfOptions).from(element);

      // Create a download link and trigger it
      const pdfBlob = await pdfInstance.outputPdf('blob');
      const blobUrl = URL.createObjectURL(pdfBlob);

      // Create and trigger download
      const downloadLink = document.createElement('a');
      downloadLink.href = blobUrl;
      downloadLink.download = `${validatedOptions.filename}.pdf`;
      downloadLink.style.display = 'none';
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);

      // Clean up blob URL after a delay
      setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);

      console.log('✅ PDF generated and download triggered');
      return {
        success: true,
        path: `Downloads/${validatedOptions.filename}.pdf`,
        message: `PDF download triggered: "${validatedOptions.filename}.pdf"`
      };

    } catch (error) {
      console.error('❌ Error generating PDF:', error);
      return {
        success: false,
        error: `PDF generation failed: ${error instanceof Error ? error.message : 'Unknown error'}`
      };
    }
  }

  async generatePdfOnly(options?: PdfShareOptions): Promise<PdfShareResult> {
    try {
      console.log('🔧 Web: Generating PDF without download');

      const validatedOptions = this.getValidatedOptions(options);
      const element = document.getElementById(validatedOptions.elementId);

      if (!element) {
        return {
          success: false,
          error: `Element with ID '${validatedOptions.elementId}' not found`
        };
      }

      const html2pdfOptions = this.createHtml2PdfOptions(validatedOptions);

      // Generate PDF as blob instead of downloading
      const pdfBlob = await html2pdf().set(html2pdfOptions).from(element).output('blob');
      const blobUrl = URL.createObjectURL(pdfBlob);

      console.log('✅ PDF blob generated:', blobUrl);
      return {
        success: true,
        path: blobUrl
      };

    } catch (error) {
      console.error('❌ Error generating PDF blob:', error);
      return {
        success: false,
        error: `PDF generation failed: ${error instanceof Error ? error.message : 'Unknown error'}`
      };
    }
  }

  async shareExistingPdf(options: { path: string; title?: string }): Promise<PdfShareResult> {
    try {
      console.log('🔧 Web: Sharing existing PDF:', options.path);

      if (!options.path) {
        return {
          success: false,
          error: 'PDF path is required'
        };
      }

      // Check if Web Share API is available
      if (navigator.share) {
        // Convert blob URL to File if needed
        if (options.path.startsWith('blob:')) {
          const response = await fetch(options.path);
          const blob = await response.blob();
          const file = new File([blob], `${options.title || 'document'}.pdf`, { type: 'application/pdf' });

          await navigator.share({
            title: options.title || 'PDF Document',
            files: [file]
          });
        } else {
          await navigator.share({
            title: options.title || 'PDF Document',
            url: options.path
          });
        }

        console.log('✅ PDF shared successfully via Web Share API');
        return { success: true };
      } else {
        // Fallback: Create download link
        const link = document.createElement('a');
        link.href = options.path;
        link.download = `${options.title || 'document'}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        console.log('✅ PDF downloaded (Web Share API not available)');
        return { success: true };
      }

    } catch (error) {
      console.error('❌ Error sharing PDF:', error);
      return {
        success: false,
        error: `PDF sharing failed: ${error instanceof Error ? error.message : 'Unknown error'}`
      };
    }
  }
}