export interface PdfSharePlugin {
  generateAndShare(): Promise<void>;
}