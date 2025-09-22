export interface PdfShareOptions {
  /**
   * HTML element ID to convert to PDF. Defaults to 'printPage'
   */
  elementId?: string;

  /**
   * Output filename (without extension). Defaults to 'veterinary-dosage'
   */
  filename?: string;

  /**
   * Document title for sharing. Defaults to 'Veterinary Dosage'
   */
  title?: string;

  /**
   * Page orientation. Defaults to 'portrait'
   */
  orientation?: 'portrait' | 'landscape';

  /**
   * Paper format. Defaults to 'a4'
   */
  format?: 'a4' | 'letter' | 'legal';

  /**
   * Page margins in mm. Defaults to 20mm on all sides
   */
  margins?: {
    top?: number;
    bottom?: number;
    left?: number;
    right?: number;
  };

  /**
   * Image quality (0.1 to 1.0). Defaults to 0.98
   */
  quality?: number;

  /**
   * Include background colors and images. Defaults to true
   */
  includeBackground?: boolean;

  /**
   * Scale factor for rendering. Defaults to 2
   */
  scale?: number;
}

export interface PdfShareResult {
  /**
   * Whether the operation was successful
   */
  success: boolean;

  /**
   * File path or URL (web only)
   */
  path?: string;

  /**
   * Error message if operation failed
   */
  error?: string;

  /**
   * Success message with details about the operation
   */
  message?: string;
}

export enum PdfShareError {
  ELEMENT_NOT_FOUND = 'ELEMENT_NOT_FOUND',
  GENERATION_FAILED = 'GENERATION_FAILED',
  SHARING_FAILED = 'SHARING_FAILED',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  PLATFORM_NOT_SUPPORTED = 'PLATFORM_NOT_SUPPORTED',
  INVALID_OPTIONS = 'INVALID_OPTIONS'
}

export interface PdfSharePlugin {
  /**
   * Generate PDF from HTML element and share it
   */
  generateAndShare(options?: PdfShareOptions): Promise<PdfShareResult>;

  /**
   * Generate PDF without sharing (returns file path)
   */
  generatePdfOnly(options?: PdfShareOptions): Promise<PdfShareResult>;

  /**
   * Share an existing PDF file
   */
  shareExistingPdf(options: { path: string; title?: string }): Promise<PdfShareResult>;
}