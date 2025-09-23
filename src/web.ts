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
      quality: Math.min(Math.max(options?.quality || 0.95, 0.1), 1.0),
      includeBackground: options?.includeBackground !== false,
      scale: Math.min(Math.max(options?.scale || 1.5, 1), 4),
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
        scrollY: 0,
        // Apply print styles during PDF generation
        onclone: (clonedDoc: Document) => {
          this.applyPrintStyles(clonedDoc);
        }
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

      // Generate PDF blob first
      const pdfInstance = html2pdf().set(html2pdfOptions).from(element);
      const pdfBlob = await pdfInstance.outputPdf('blob');

      console.log('📄 PDF blob created:', pdfBlob.size, 'bytes');

      // Try multiple download methods
      let downloadSuccess = false;
      let downloadMethod = '';

      // Method 1: Direct html2pdf save() - most reliable
      try {
        await html2pdf().set(html2pdfOptions).from(element).save();
        downloadSuccess = true;
        downloadMethod = 'html2pdf.save()';
        console.log('✅ Method 1 (html2pdf.save) succeeded');
      } catch (saveError) {
        console.log('❌ Method 1 failed:', saveError instanceof Error ? saveError.message : saveError);

        // Method 2: Programmatic download link
        try {
          const blobUrl = URL.createObjectURL(pdfBlob);

          const downloadLink = document.createElement('a');
          downloadLink.href = blobUrl;
          downloadLink.download = `${validatedOptions.filename}.pdf`;
          downloadLink.style.display = 'none';

          // Add to DOM, click, and remove
          document.body.appendChild(downloadLink);
          downloadLink.click();

          // Clean up
          setTimeout(() => {
            document.body.removeChild(downloadLink);
            URL.revokeObjectURL(blobUrl);
          }, 100);

          downloadSuccess = true;
          downloadMethod = 'programmatic link';
          console.log('✅ Method 2 (programmatic link) succeeded');

        } catch (linkError) {
          console.log('❌ Method 2 failed:', linkError instanceof Error ? linkError.message : linkError);

          // Method 3: Window.open fallback
          try {
            const blobUrl = URL.createObjectURL(pdfBlob);
            const newWindow = window.open(blobUrl, '_blank');

            if (newWindow) {
              downloadSuccess = true;
              downloadMethod = 'window.open()';
              console.log('✅ Method 3 (window.open) succeeded');
            } else {
              throw new Error('Popup blocked');
            }
          } catch (windowError) {
            console.log('❌ Method 3 failed:', windowError instanceof Error ? windowError.message : windowError);
          }
        }
      }

      if (downloadSuccess) {
        console.log(`✅ PDF download successful using ${downloadMethod}`);
        return {
          success: true,
          path: `Downloads/${validatedOptions.filename}.pdf`,
          message: `PDF download successful (${downloadMethod}): "${validatedOptions.filename}.pdf"`
        };
      } else {
        console.log('❌ All download methods failed, returning blob URL');
        const blobUrl = URL.createObjectURL(pdfBlob);
        return {
          success: true,
          path: blobUrl,
          message: `PDF created but auto-download failed. Click link to download: ${blobUrl}`
        };
      }

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

  /**
   * Apply print styles to the cloned document for PDF generation
   */
  private applyPrintStyles(clonedDoc: Document): void {
    try {
      console.log('📋 Applying print styles to PDF...');

      // Create a style element for print styles
      const printStyleElement = clonedDoc.createElement('style');
      printStyleElement.setAttribute('type', 'text/css');

      // Collect all print styles from the original document
      const printStyles = this.extractPrintStyles();

      // Add the print styles to the cloned document
      printStyleElement.textContent = printStyles;
      clonedDoc.head.appendChild(printStyleElement);

      // Apply print-specific classes and attributes
      this.applyPrintClasses(clonedDoc);

      console.log('✅ Print styles applied successfully');
    } catch (error) {
      console.warn('⚠️ Error applying print styles:', error);
    }
  }

  /**
   * Extract print styles from all stylesheets
   */
  private extractPrintStyles(): string {
    let printStyles = '';
    let stylesheetsProcessed = 0;
    let printRulesFound = 0;

    try {
      console.log('🔍 Starting print styles extraction...');

      // Get all stylesheets
      const stylesheets = Array.from(document.styleSheets);
      console.log(`📊 Found ${stylesheets.length} stylesheets to analyze`);

      for (const stylesheet of stylesheets) {
        try {
          const href = stylesheet.href ? new URL(stylesheet.href).pathname : 'inline';

          // Only process app-min.css and tailwind.css
          if (href.includes('app-min.css') || href.includes('tailwind.css') || !stylesheet.href) {
            console.log(`📄 Processing stylesheet: ${href}`);

            // Access CSS rules
            const rules = stylesheet.cssRules || stylesheet.rules;
            if (!rules) {
              console.log(`⚠️ No rules found in ${href}`);
              continue;
            }

            stylesheetsProcessed++;
            console.log(`🔍 Analyzing ${rules.length} rules in ${href}`);

            for (let i = 0; i < rules.length; i++) {
              const rule = rules[i];

              // Check for media rules
              if (rule.type === CSSRule.MEDIA_RULE) {
                const mediaRule = rule as CSSMediaRule;

                // Check if it's a print media rule
                if (mediaRule.media.mediaText.includes('print')) {
                  printRulesFound++;
                  console.log(`🎯 Found print media rule in ${href}:`, mediaRule.media.mediaText);

                  // Extract the print styles and convert them to regular styles
                  const printCSS = mediaRule.cssText;

                  // Remove the @media print wrapper and apply styles directly
                  const innerCSS = printCSS
                    .replace(/@media[^{]+\{/, '') // Remove @media print {
                    .replace(/\}$/, ''); // Remove closing }

                  printStyles += `/* From ${href} */\n${innerCSS}\n\n`;
                  console.log(`✅ Extracted ${innerCSS.length} characters from ${href}`);
                }
              }
            }
          }
        } catch (e) {
          // Skip stylesheets that can't be accessed (CORS)
          const href = stylesheet.href ? new URL(stylesheet.href).pathname : 'inline';
          console.log(`❌ Skipping stylesheet ${href} due to CORS:`, e);
        }
      }

      console.log(`📊 Extraction summary: ${stylesheetsProcessed} stylesheets processed, ${printRulesFound} print rules found`);

      // Add TailwindCSS print utilities fallback if no styles found or few found
      if (!printStyles.trim() || printRulesFound === 0) {
        console.log('⚠️ No print styles found, adding comprehensive defaults...');
        printStyles = this.getDefaultPrintStyles() + this.getTailwindPrintUtilities();
      } else {
        // Always add TailwindCSS utilities as fallback
        console.log('✅ Adding TailwindCSS print utilities as enhancement...');
        printStyles += '\n' + this.getTailwindPrintUtilities();
      }

      console.log(`📋 Final extracted print styles: ${printStyles.length} characters`);
      return printStyles;

    } catch (error) {
      console.warn('⚠️ Error extracting print styles:', error);
      return this.getDefaultPrintStyles() + this.getTailwindPrintUtilities();
    }
  }

  /**
   * Apply print-specific classes and hide elements
   */
  private applyPrintClasses(clonedDoc: Document): void {
    // Hide elements with .hidden-print class
    const hiddenPrintElements = clonedDoc.querySelectorAll('.hidden-print');
    hiddenPrintElements.forEach(el => {
      (el as HTMLElement).style.display = 'none';
    });

    // Hide common UI elements that shouldn't be in print
    const hideSelectors = [
      '.navbar',
      '.toolbar',
      '.searchbar',
      '.tab-link',
      '.floating-button',
      '.back-button',
      '.no-print',
      'button:not(.print-button)',
      '.btn:not(.print-button)'
    ];

    hideSelectors.forEach(selector => {
      const elements = clonedDoc.querySelectorAll(selector);
      elements.forEach(el => {
        (el as HTMLElement).style.display = 'none';
      });
    });

    // Apply print-friendly styling to body
    clonedDoc.body.style.backgroundColor = '#ffffff';
    clonedDoc.body.style.color = '#000000';
    clonedDoc.body.style.fontFamily = 'Arial, sans-serif';
  }

  /**
   * Get default print styles if none can be extracted
   */
  private getDefaultPrintStyles(): string {
    return `
      /* Default print styles for PDF - VetDrugs Optimized */
      body {
        background: white !important;
        color: black !important;
        font-family: Arial, sans-serif !important;
        font-size: 10pt !important;
        line-height: 1.2 !important;
        margin: 0 !important;
        padding: 15px !important;
      }

      /* Minimal fallback - only essential print setup */

      /* Hide navigation and UI elements */
      .navbar, .toolbar, .searchbar, .tab-link,
      .floating-button, .back-button, .hidden-print,
      .no-print, button:not(.print-button),
      .btn:not(.print-button) {
        display: none !important;
      }

      /* Reset dark mode styles */
      .dark\\:bg-gray-800, .dark\\:bg-gray-700, .dark\\:bg-slate-900 {
        background-color: white !important;
      }

      .dark\\:text-white, .dark\\:text-gray-300 {
        color: black !important;
      }

      /* Ensure good page breaks */
      h1, h2, h3 {
        page-break-after: avoid;
        page-break-inside: avoid;
      }

      table {
        page-break-inside: avoid;
        border-collapse: collapse;
      }

      /* Print color support */
      * {
        print-color-adjust: exact !important;
        -webkit-print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
    `;
  }

  /**
   * Get comprehensive TailwindCSS print utilities
   */
  private getTailwindPrintUtilities(): string {
    return `
      /* TailwindCSS Print Utilities */

      /* Minimal body setup - let app-min.css handle specifics */
      body {
        font-family: Arial, sans-serif !important;
        background: white !important;
        color: black !important;
      }

      /* Display utilities */
      .print\\:block { display: block !important; }
      .print\\:inline-block { display: inline-block !important; }
      .print\\:inline { display: inline !important; }
      .print\\:inline-flex { display: inline-flex !important; }
      .print\\:hidden { display: none !important; }
      .print\\:flex { display: flex !important; }
      .print\\:grid { display: grid !important; }
      .print\\:table { display: table !important; }
      .print\\:table-cell { display: table-cell !important; }

      /* Width utilities */
      .print\\:w-full { width: 100% !important; }
      .print\\:w-1\\/2 { width: 50% !important; }
      .print\\:w-1\\/3 { width: 33.333333% !important; }
      .print\\:w-2\\/3 { width: 66.666667% !important; }
      .print\\:w-1\\/4 { width: 25% !important; }
      .print\\:w-3\\/4 { width: 75% !important; }
      .print\\:w-1\\/5 { width: 20% !important; }
      .print\\:w-2\\/5 { width: 40% !important; }
      .print\\:w-3\\/5 { width: 60% !important; }
      .print\\:w-4\\/5 { width: 80% !important; }
      .print\\:w-1\\/6 { width: 16.666667% !important; }
      .print\\:w-5\\/6 { width: 83.333333% !important; }
      .print\\:w-1\\/12 { width: 8.333333% !important; }
      .print\\:w-2\\/12 { width: 16.666667% !important; }
      .print\\:w-3\\/12 { width: 25% !important; }
      .print\\:w-4\\/12 { width: 33.333333% !important; }
      .print\\:w-5\\/12 { width: 41.666667% !important; }
      .print\\:w-6\\/12 { width: 50% !important; }
      .print\\:w-7\\/12 { width: 58.333333% !important; }
      .print\\:w-8\\/12 { width: 66.666667% !important; }
      .print\\:w-9\\/12 { width: 75% !important; }
      .print\\:w-10\\/12 { width: 83.333333% !important; }
      .print\\:w-11\\/12 { width: 91.666667% !important; }

      /* Height utilities */
      .print\\:h-full { height: 100% !important; }
      .print\\:h-screen { height: 100vh !important; }
      .print\\:h-auto { height: auto !important; }

      /* Margin utilities */
      .print\\:m-0 { margin: 0px !important; }
      .print\\:m-1 { margin: 0.25rem !important; }
      .print\\:m-2 { margin: 0.5rem !important; }
      .print\\:m-4 { margin: 1rem !important; }
      .print\\:mx-0 { margin-left: 0px !important; margin-right: 0px !important; }
      .print\\:mx-1 { margin-left: 0.25rem !important; margin-right: 0.25rem !important; }
      .print\\:mx-2 { margin-left: 0.5rem !important; margin-right: 0.5rem !important; }
      .print\\:mx-auto { margin-left: auto !important; margin-right: auto !important; }
      .print\\:my-0 { margin-top: 0px !important; margin-bottom: 0px !important; }
      .print\\:my-1 { margin-top: 0.25rem !important; margin-bottom: 0.25rem !important; }
      .print\\:my-2 { margin-top: 0.5rem !important; margin-bottom: 0.5rem !important; }
      .print\\:mt-0 { margin-top: 0px !important; }
      .print\\:mt-1 { margin-top: 0.25rem !important; }
      .print\\:mt-2 { margin-top: 0.5rem !important; }
      .print\\:mt-4 { margin-top: 1rem !important; }
      .print\\:mb-0 { margin-bottom: 0px !important; }
      .print\\:mb-1 { margin-bottom: 0.25rem !important; }
      .print\\:mb-2 { margin-bottom: 0.5rem !important; }
      .print\\:mb-4 { margin-bottom: 1rem !important; }
      .print\\:ml-0 { margin-left: 0px !important; }
      .print\\:ml-1 { margin-left: 0.25rem !important; }
      .print\\:mr-0 { margin-right: 0px !important; }
      .print\\:mr-1 { margin-right: 0.25rem !important; }

      /* Padding utilities */
      .print\\:p-0 { padding: 0px !important; }
      .print\\:p-1 { padding: 0.25rem !important; }
      .print\\:p-2 { padding: 0.5rem !important; }
      .print\\:p-4 { padding: 1rem !important; }
      .print\\:px-0 { padding-left: 0px !important; padding-right: 0px !important; }
      .print\\:px-1 { padding-left: 0.25rem !important; padding-right: 0.25rem !important; }
      .print\\:px-2 { padding-left: 0.5rem !important; padding-right: 0.5rem !important; }
      .print\\:py-0 { padding-top: 0px !important; padding-bottom: 0px !important; }
      .print\\:py-1 { padding-top: 0.25rem !important; padding-bottom: 0.25rem !important; }
      .print\\:py-2 { padding-top: 0.5rem !important; padding-bottom: 0.5rem !important; }
      .print\\:pt-0 { padding-top: 0px !important; }
      .print\\:pt-1 { padding-top: 0.25rem !important; }
      .print\\:pb-0 { padding-bottom: 0px !important; }
      .print\\:pb-1 { padding-bottom: 0.25rem !important; }
      .print\\:pb-2 { padding-bottom: 0.5rem !important; }
      .print\\:pl-0 { padding-left: 0px !important; }
      .print\\:pr-0 { padding-right: 0px !important; }
      .print\\:pr-2 { padding-right: 0.5rem !important; }

      /* Text utilities */
      .print\\:text-xs { font-size: 0.75rem !important; line-height: 1rem !important; }
      .print\\:text-sm { font-size: 0.875rem !important; line-height: 1.25rem !important; }
      .print\\:text-base { font-size: 1rem !important; line-height: 1.5rem !important; }
      .print\\:text-lg { font-size: 1.125rem !important; line-height: 1.75rem !important; }
      .print\\:text-xl { font-size: 1.25rem !important; line-height: 1.75rem !important; }
      .print\\:text-black { color: rgb(0 0 0) !important; }
      .print\\:text-white { color: rgb(255 255 255) !important; }
      .print\\:text-gray-900 { color: rgb(17 24 39) !important; }
      .print\\:text-blue-600 { color: rgb(37 99 235) !important; }
      .print\\:text-center { text-align: center !important; }
      .print\\:text-left { text-align: left !important; }
      .print\\:text-right { text-align: right !important; }
      .print\\:capitalize { text-transform: capitalize !important; }
      .print\\:tracking-normal { letter-spacing: 0em !important; }

      /* Background utilities */
      .print\\:bg-white { background-color: rgb(255 255 255) !important; }
      .print\\:bg-gray-50 { background-color: rgb(249 250 251) !important; }
      .print\\:bg-gray-100 { background-color: rgb(243 244 246) !important; }
      .print\\:bg-gray-200 { background-color: rgb(229 231 235) !important; }
      .print\\:bg-gray-300 { background-color: rgb(209 213 219) !important; }
      .print\\:bg-transparent { background-color: transparent !important; }

      /* Border utilities */
      .print\\:border { border-width: 1px !important; }
      .print\\:border-b { border-bottom-width: 1px !important; }
      .print\\:border-l-0 { border-left-width: 0px !important; }
      .print\\:border-none { border-style: none !important; }
      .print\\:border-slate-300 { border-color: rgb(203 213 225) !important; }

      /* Grid utilities */
      .print\\:grid-cols-1 { grid-template-columns: repeat(1, minmax(0, 1fr)) !important; }
      .print\\:grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; }
      .print\\:grid-cols-3 { grid-template-columns: repeat(3, minmax(0, 1fr)) !important; }
      .print\\:grid-cols-4 { grid-template-columns: repeat(4, minmax(0, 1fr)) !important; }
      .print\\:gap-0 { gap: 0px !important; }
      .print\\:gap-1 { gap: 0.25rem !important; }
      .print\\:gap-2 { gap: 0.5rem !important; }

      /* Flexbox utilities */
      .print\\:flex-wrap { flex-wrap: wrap !important; }
      .print\\:justify-center { justify-content: center !important; }
      .print\\:items-center { align-items: center !important; }

      /* Misc utilities */
      .print\\:overflow-hidden { overflow: hidden !important; }
      .print\\:shadow-none { box-shadow: none !important; }
      .print\\:ring-0 { box-shadow: none !important; }
      .print\\:transform { transform: translate(var(--tw-translate-x), var(--tw-translate-y)) rotate(var(--tw-rotate)) skewX(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y)) !important; }
      .print\\:scale-y-75 { --tw-scale-y: .75 !important; transform: translate(var(--tw-translate-x), var(--tw-translate-y)) rotate(var(--tw-rotate)) skewX(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y)) !important; }

      /* Special print utilities */
      .print\\:blocked { display: block !important; }
    `;
  }
}